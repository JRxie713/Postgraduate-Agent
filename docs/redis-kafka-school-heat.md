# Redis + Kafka 院校热度榜模块 · 代码讲解与运行手册

> 所在分支：`data_screen_v2`
> 模块定位：在考研导学系统里新增一个「院校热度」功能，学生浏览 / 关注院校时上报行为，**Redis 做当日去重、实时计数与 ZSet 热度排行**，**Kafka 异步聚合历史浏览量 / 关注量 / 总热度**。
> 后端端口：`8080`；接口前缀：`/api/school-heat`

---

## 一、模块是做什么的

### 1. 业务背景

择校是考研闭环里的关键一环：学生在择校页查看、关注目标院校，平台需要据此统计「哪些院校最受关注」，在数据大屏的「热门院校 TOP 5」里展示。这类行为统计有两个特点：

1. **在线链路要快**：点一次浏览 / 关注要立刻反馈，热度榜实时可读，不能每次都查 MySQL；
2. **后台聚合可以慢**：历史浏览量、关注量的累计不需要阻塞点击请求，丢给消息队列异步做即可。

和已有的「学习打卡」模块相比，本模块换了一组 Redis 场景：打卡侧重**连续天数、当日时长**；院校热度侧重**当日行为去重、动作分类计数、加权热度排行**，并把历史累计拆到 Kafka 消费端。

### 2. 整体链路

```
前端 / 接口调用
    │  POST /api/school-heat（上报浏览 / 关注）
    ▼
Controller（@Valid 参数校验、Result 封装）
    ▼
SchoolHeatServiceImpl
    │  ① Set 当日去重：同一用户当天对同一院校的同一动作只计一次
    │  ② Hash 动作计数：当日浏览 / 关注次数
    │  ③ ZSet 热度榜：浏览计 1 分，关注计 3 分（加权）
    │  ④ 投递 Kafka 事件，异步聚合历史热度
    ▼
SchoolHeatKafkaProducer ──投递 school-heat-topic──▶ Kafka Broker
                                                          ▼
                                              SchoolHeatKafkaConsumer（异步）
                                               ① 累计历史总热度
                                               ② 累计历史浏览量 / 关注量
                                               ③ 预留：热度落库 / 推荐 / 动态推送
```

查询链路（热度榜、可选院校列表）直接走 Redis / MySQL，不经过 Kafka。

> 说明：热度 ZSet `postgraduate:school:hot` 与数据大屏 `/api/dashboard/school-hot` 共用同一结构，大屏 TOP 5 会自动读到这里累计出来的热门院校，无需额外改大屏代码。

---

## 二、改动清单

### 新增文件（后端）

| 文件 | 作用 |
| --- | --- |
| `backend/.../controller/SchoolHeatController.java` | 院校热度 REST 接口层 |
| `backend/.../service/SchoolHeatService.java` | 服务接口 |
| `backend/.../service/impl/SchoolHeatServiceImpl.java` | 服务实现：Redis 去重 / 计数 / 排行 + 投递 Kafka |
| `backend/.../kafka/SchoolHeatEvent.java` | Kafka 消息体（事件对象） |
| `backend/.../kafka/SchoolHeatKafkaProducer.java` | Kafka 生产者 |
| `backend/.../kafka/SchoolHeatKafkaConsumer.java` | Kafka 消费者（异步聚合历史热度） |
| `backend/.../dto/SchoolHeatDTO.java` | 上报请求入参 |
| `backend/.../vo/SchoolHeatVO.java` | 热度榜条目响应体 |
| `backend/.../vo/SchoolOptionVO.java` | 可选院校下拉项响应体 |

### 修改文件（均为追加，不删除原有逻辑）

| 文件 | 改动 |
| --- | --- |
| `backend/.../config/KafkaTopicConfig.java` | 追加注册 `school-heat-topic` 主题（原有 `study-checkin-topic` 保留） |
| `frontend/src/api/study.js` | 追加 `trackSchoolHeat / schoolHeatRank / schoolHeatOptions` 封装 |
| `frontend/src/views/HomeView.vue` | 追加「院校热度」侧边栏入口、面板区块与样式；原有计划 / 打卡 / 资讯 / 大屏 / 用户 / 权限组功能不变 |

---

## 三、Redis 键设计

| 键 | 类型 | 含义 | 过期策略 |
| --- | --- | --- | --- |
| `postgraduate:school:hot` | ZSet | 热度总榜，member=院校名称，score=总热度（浏览 + 加权关注） | 长期保留 |
| `postgraduate:heat:seen:{userId}:{yyyy-MM-dd}` | Set | 当日去重，member=`院校名称:动作`，防止同一用户当天重复刷热度 | 2 天 |
| `postgraduate:heat:action:{院校名称}` | Hash | 当日动作计数，字段 `views` / `follows` | 2 天 |
| `postgraduate:heat:total:{院校名称}` | String | 消费端累计的历史总热度 | 长期保留 |
| `postgraduate:heat:views:{院校名称}` | String | 消费端累计的历史浏览量 | 长期保留 |
| `postgraduate:heat:follows:{院校名称}` | String | 消费端累计的历史关注量 | 长期保留 |

**加权规则**：一次浏览记 1 分；一次关注记 3 分（`FOLLOW_WEIGHT = 3`）。关注代表更强的意向，因此在总热度里权重更高。

---

## 四、Kafka 设计

| 项 | 取值 | 说明 |
| --- | --- | --- |
| 主题 | `school-heat-topic` | 在 `KafkaTopicConfig` 启动时自动创建，已存在则保持不变 |
| 分区数 | 3 | 便于后续多消费实例并行处理 |
| 副本数 | 1 | 适配本地 / 单机 Docker 环境 |
| 消费组 | `${spring.kafka.consumer.group-id}`（复用 `study-checkin-group`） | 同组内每条消息只被一个实例消费 |
| 分区键 | 院校名称 | 同一所院校的事件落到同一分区，保证单校事件顺序 |

**消息体 `SchoolHeatEvent`**：

| 字段 | 含义 |
| --- | --- |
| `eventId` | 事件唯一标识，便于链路追踪 / 幂等去重 |
| `userId` | 操作用户 ID |
| `schoolId` | 院校专业记录 ID（链路追踪用，排行按院校名称聚合） |
| `schoolName` | 院校名称（热度聚合维度） |
| `action` | `VIEW` 浏览 / `FOLLOW` 关注 |
| `eventTime` | 事件产生时间（ISO-8601 字符串） |

**生产 / 消费分工**：

- 生产者只负责把事件 JSON 投递到主题；投递失败只记日志，不回滚 Redis 里的实时计数，事件可由补偿任务重放。
- 消费者异步累计历史总热度、浏览量、关注量；异常不向上抛出，避免毒消息导致位点反复重放，需要严格不丢时可再写死信主题。

---

## 五、逐文件讲解

### 5.1 上报入参 `SchoolHeatDTO`

```java
@Data
public class SchoolHeatDTO {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotNull(message = "院校ID不能为空")
    private Integer schoolId;

    @NotBlank(message = "院校名称不能为空")
    private String schoolName;

    /** 动作类型：VIEW=浏览，FOLLOW=关注；不传默认 VIEW */
    private String action;
}
```

- `@NotNull / @NotBlank` 由 Spring Validation 在 Controller 入参绑定阶段校验，失败会被全局异常处理器统一返回。
- `action` 不在 DTO 上写死枚举校验，而是在 Service 里归一化：空值默认 `VIEW`，非法值抛 `BusinessException(400, ...)`，这样错误信息更贴近业务。

### 5.2 服务接口 `SchoolHeatService`

```java
public interface SchoolHeatService {
    SchoolHeatVO record(SchoolHeatDTO dto);          // 上报一次浏览 / 关注
    List<SchoolHeatVO> rank(int top);                // 热度排行榜
    List<SchoolOptionVO> options();                  // 可选院校列表
}
```

接口只定义业务能力，不暴露 Redis / Kafka 细节；实现下沉到 `impl`，符合项目「Controller 只校验封装、逻辑进 Service」的规范。

### 5.3 服务实现 `SchoolHeatServiceImpl`

#### （1）上报 `record`

核心步骤：

1. **归一化动作**：`VIEW` / `FOLLOW` 大写匹配，非法值抛业务异常；
2. **Set 当日去重**：`seenKey` 里查 `院校名称:动作`，已存在说明今天计过，直接返回当前热度，不再重复计数；
3. **Hash 当日计数**：按动作累加 `views` / `follows` 字段；
4. **ZSet 加权上榜**：浏览 +1，关注 +3；
5. **投递 Kafka**：封装 `SchoolHeatEvent` 交给生产者异步聚合。

关键片段：

```java
String seenKey = "postgraduate:heat:seen:" + userId + ":" + today;
String member = schoolName + ":" + action;
boolean firstTime = !Boolean.TRUE.equals(redis.opsForSet().isMember(seenKey, member));

if (firstTime) {
    redis.opsForSet().add(seenKey, member);
    redis.expire(seenKey, Duration.ofDays(2));

    String actionHashKey = "postgraduate:heat:action:" + schoolName;
    redis.opsForHash().increment(actionHashKey, follow ? "follows" : "views", 1);
    redis.expire(actionHashKey, Duration.ofDays(2));

    double weight = follow ? FOLLOW_WEIGHT : 1;
    redis.opsForZSet().incrementScore(HOT_ZSET, schoolName, weight);

    SchoolHeatEvent event = new SchoolHeatEvent(
            UUID.randomUUID().toString(), userId, dto.getSchoolId(),
            schoolName, action, LocalDate.now().toString());
    producer.sendHeatEvent(event);
}
```

语法 / 逻辑要点：

- `opsForSet().isMember` 判断成员是否存在；`add` 写入去重集合。
- `opsForHash().increment(key, field, delta)` 直接对 Hash 字段做原子累加，省去「读出来 +1 再写回」。
- `opsForZSet().incrementScore(key, member, delta)` 对 ZSet 分数做原子累加；member 不存在会自动新建。
- 去重通过后才投递 Kafka，重复点击不会产生重复消息。

#### （2）热度榜 `rank`

```java
Map<String, Integer> nameToId = new HashMap<>();
for (SchoolOptionVO option : options()) {
    nameToId.put(option.getSchoolName(), option.getSchoolId());
}

Set<ZSetOperations.TypedTuple<String>> tuples =
        redis.opsForZSet().reverseRangeWithScores(HOT_ZSET, 0, top - 1);
```

- `reverseRangeWithScores` 按分数**倒序**取前 N 名，并同时拿到 member 与 score。
- 院校名称 → 记录 ID 的映射来自 `options()`，用于榜单回填 `schoolId`。
- 浏览 / 关注量取消费端累计的历史计数器（`views` / `follows` 键），缺失按 0 处理。

#### （3）可选院校 `options`

直接用 `JdbcTemplate` 按院校分组取最小记录 ID：

```java
SELECT MIN(id) AS id, school FROM school_major GROUP BY school ORDER BY id
```

同一院校在 `school_major` 里可能对应多个专业，按 `school` 分组后每个院校只出现在下拉项里一次。

#### （4）数值兜底 `readLong`

```java
private long readLong(String key, long defaultValue) {
    return readLong((Object) redis.opsForValue().get(key), defaultValue);
}

private long readLong(Object raw, long defaultValue) {
    if (raw == null || String.valueOf(raw).isBlank()) return defaultValue;
    try { return Long.parseLong(String.valueOf(raw)); }
    catch (NumberFormatException e) { return defaultValue; }
}
```

缓存缺失时 `get` 返回 `null`，这里统一兜底成默认值，避免下游 `Long.parseLong` 抛空指针或数字格式异常。注意委托第二个重载时显式 `(Object)` 强转，防止编译期按静态类型重新绑定回 String 重载（与打卡模块 `readLong` 是同一个坑）。

### 5.4 生产者 `SchoolHeatKafkaProducer`

```java
public static final String TOPIC = "school-heat-topic";

public void sendHeatEvent(SchoolHeatEvent event) {
    try {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, event.getSchoolName(), payload);
        log.info("院校热度事件已投递 Kafka：school={}, action={}, topic={}",
                event.getSchoolName(), event.getAction(), TOPIC);
    } catch (Exception e) {
        log.error("院校热度事件投递 Kafka 失败：school={}, 原因={}", event.getSchoolName(), e.getMessage());
    }
}
```

- `KafkaTemplate<String, String>` 收发 JSON 字符串，序列化统一用 String，避免类型映射问题。
- `send(topic, key, payload)` 的 key 用院校名称：同校事件进同分区，保证单校顺序；不同院校可并行。
- 投递异常被捕获只记日志：主链路（Redis 实时计数）不被 Kafka 可用性拖垮。

### 5.5 消费者 `SchoolHeatKafkaConsumer`

```java
@KafkaListener(topics = SchoolHeatKafkaProducer.TOPIC,
        groupId = "${spring.kafka.consumer.group-id}")
public void onHeatEvent(String payload) {
    try {
        SchoolHeatEvent event = objectMapper.readValue(payload, SchoolHeatEvent.class);
        String name = event.getSchoolName();
        boolean follow = "FOLLOW".equalsIgnoreCase(event.getAction());

        redis.opsForValue().increment(totalKey(name), follow ? FOLLOW_WEIGHT : 1);
        if (follow) {
            redis.opsForValue().increment(followKey(name), 1);
        } else {
            redis.opsForValue().increment(viewKey(name), 1);
        }
        log.info("院校热度事件异步处理完成：school={}, action={}", name, event.getAction());
    } catch (Exception e) {
        log.error("院校热度事件处理失败，消息体={}, 原因={}", payload, e.getMessage());
    }
}
```

- `@KafkaListener` 订阅热度主题，消费组复用 `application.yml` 配置。
- 总热度按与线上一致的加权规则累计：浏览 +1，关注 +3；同时分别累计浏览量 / 关注量。
- 异常不向上抛，避免一条坏消息卡住整个分区的消费位点。

### 5.6 主题注册 `KafkaTopicConfig`

在原有打卡主题 Bean 之外，追加一个热度主题 Bean：

```java
@Bean
public NewTopic schoolHeatTopic() {
    return TopicBuilder.name("school-heat-topic")
            .partitions(3)
            .replicas(1)
            .build();
}
```

应用启动时自动创建；Broker 上已有同名主题则保持不变。

### 5.7 前端接入

`study.js` 追加接口封装：

```js
export const trackSchoolHeat = (payload) =>
  request('/school-heat', { method: 'POST', body: payload })

export const schoolHeatOptions = () => request('/school-heat/options')

export const schoolHeatMine = (userId) =>
  request(`/school-heat/mine?userId=${encodeURIComponent(userId)}`)

// 备用：后端 /rank 端点保留，面板不再调用总榜
export const schoolHeatRank = (top = 5) =>
  request(`/school-heat/rank?top=${encodeURIComponent(top)}`)
```

`HomeView.vue` 追加：

- `heatOptions / myTracks / heatLoading` 状态；
- `loadSchoolHeat()` 并行拉取可选院校与「我的今日浏览 / 关注」明细；
- `trackSchool(option, action)` 上报浏览 / 关注，成功后刷新明细；
- 侧边栏「院校热度」入口与面板：院校卡片 + 浏览 / 关注按钮 + 我的今日行为明细。

> 面板不再重复展示热门总榜：热门排行统一由数据大屏 TOP 5 读取同一个 ZSet 展示，避免两处榜单重复。

登录态失效时 `trackSchool` 先拦截空用户，提示重新登录，避免空指针。

---

## 六、接口列表与调用示例

### 6.1 上报浏览 / 关注

`POST /api/school-heat`

```json
{
  "userId": 1,
  "schoolId": 1,
  "schoolName": "四川大学",
  "action": "VIEW"
}
```

PowerShell 示例：

```powershell
$body = @{ userId = 1; schoolId = 1; schoolName = '四川大学'; action = 'VIEW' } | ConvertTo-Json -Compress
Invoke-RestMethod 'http://127.0.0.1:8080/api/school-heat' -Method Post `
  -ContentType 'application/json' -Headers @{ Authorization = "Bearer <token>" } -Body $body
```

### 6.2 热度排行榜

`GET /api/school-heat/rank?top=5`

返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "rank": 1, "schoolId": 2, "schoolName": "电子科技大学", "heat": 3, "views": 0, "follows": 1 },
    { "rank": 2, "schoolId": 1, "schoolName": "四川大学",   "heat": 1, "views": 1, "follows": 0 }
  ]
}
```

### 6.3 可选院校列表

`GET /api/school-heat/options`

返回去重后的院校 `schoolId + schoolName`，供择校页下拉 / 卡片使用。

### 6.4 我的今日浏览 / 关注明细

`GET /api/school-heat/mine?userId=1`

读取当日去重集合 `postgraduate:heat:seen:{userId}:{date}`，返回当前用户今天浏览 / 关注过的院校明细：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "schoolName": "四川大学",   "action": "VIEW" },
    { "schoolName": "电子科技大学", "action": "FOLLOW" },
    { "schoolName": "重庆大学",   "action": "FOLLOW" }
  ]
}
```

---

## 七、本地运行与验证

### 7.1 前置中间件

```bash
# Redis、Kafka 容器应处于 Up 状态
docker ps
```

### 7.2 启动后端

```powershell
cd D:\Postgraduate-Agent\backend
.\mvnw.cmd -DskipTests spring-boot:run
```

启动日志出现 `Started ScaffoldBackendApplication`，且 `school-heat-topic` 分区被消费者分配即就绪。

### 7.3 启动前端

```powershell
cd D:\Postgraduate-Agent\frontend
npm run dev
```

访问 `http://localhost:5173/`，登录后侧边栏进入「院校热度」。

### 7.4 验证点

1. 点击某院校「浏览 +1」，明细里追加一条「浏览」，Redis 中该校热度 +1、浏览量 +1；
2. 点击「关注 +3」，明细里追加一条「关注」，Redis 中该校热度 +3、关注量 +1；
3. 同一用户当天重复点同一动作，明细与热度都不重复增加（去重生效）；
4. 等 1~2 秒后，历史浏览 / 关注量由 Kafka 消费端累计回填；
5. 打开「数据大屏」，热门院校 TOP 5 展示累计热度排行，面板本身不重复出总榜。

### 7.5 Redis 现场排查命令

```bash
# 热度总榜（member=院校名称，score=热度）
docker exec kaoyan-redis redis-cli zrange postgraduate:school:hot 0 -1 WITHSCORES

# 某院校历史累计浏览 / 关注 / 总热度
docker exec kaoyan-redis redis-cli get postgraduate:heat:views:四川大学
docker exec kaoyan-redis redis-cli get postgraduate:heat:follows:电子科技大学
docker exec kaoyan-redis redis-cli get postgraduate:heat:total:四川大学
```

---

## 八、常见问题排查

| 现象 | 原因与处理 |
| --- | --- |
| 上报返回 500，日志 `JSON parse error` | 请求体不是合法 JSON；用对象 `ConvertTo-Json` 序列化，不要手写拼接字符串 |
| 榜单一直为空 | 先确认有上报事件；检查 `postgraduate:school:hot` ZSet 是否有 member；消费者是否订阅了 `school-heat-topic` |
| 浏览 / 关注量为 0，但热度有值 | 历史计数器由 Kafka 消费端异步写，等 1~2 秒再查；一直为 0 则看后端日志「院校热度事件处理失败」 |
| 重复点击热度一直涨 | 检查 `postgraduate:heat:seen:{userId}:{date}` 去重集合是否生效；member 为 `院校名称:动作` |
| 大屏 TOP5 不更新 | 大屏读的是同一个 `postgraduate:school:hot` ZSet；确认上报链路写榜成功，刷新大屏即可 |

---

## 九、可扩展点

- **热度落库**：消费者累计到阈值后把热度写入 MySQL `school_major` 或独立统计表，Redis 只做热点缓存；
- **择校推荐**：基于关注院校的热度、专业、地域做冲稳保推荐；
- **动态推送**：热门院校上新 / 扩招时，向关注过该校的用户推送动态；
- **时间窗口榜**：增加「本周热榜」「本月热榜」ZSet，按时间窗口分别累计；
- **防刷加固**：接入接口限流与异常行为识别，对短时高频点击做降级。
