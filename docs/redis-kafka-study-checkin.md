# Redis + Kafka 学习打卡模块 · 代码讲解与运行手册

> 所在分支：`data_screen_v2`
> 模块定位：在考研导学系统里新增一个「每日学习打卡」功能，用 **Redis 做在线实时读写**（缓存、计数、去重、分布式锁、排行榜），用 **Kafka 做异步事件聚合**（历史时长累计、缓存刷新、后续落库/通知扩展）。
> 后端端口：`8080`；接口前缀：`/api/study-checkin`

---

## 一、模块是做什么的

### 1. 业务背景

考研学生每天学习后打卡，记录今天学了多久、连续打卡多少天，并在榜单上看到自己的排名。这类场景有两个特点：

1. **在线链路要快**：打卡、查概览、看榜单都是用户实时操作，不能每次都查 MySQL；
2. **后台统计可以慢**：历史总时长累计、写库、推送提醒这类动作不需要阻塞打卡请求，丢给消息队列异步做即可。

所以本模块把两种中间件搭配起来：

- **Redis**：承担高并发下的实时数据读写，覆盖分布式锁、去重、计数器、Hash 缓存、ZSet 排行榜五种最常用结构；
- **Kafka**：打卡成功后发一条事件消息，消费者在后台异步累计历史时长、刷新缓存，主链路和统计链路解耦，顺便削峰。

### 2. 整体链路

```
前端 / 接口调用
    │  POST /api/study-checkin（打卡）
    ▼
Controller（参数校验、Result 封装）
    ▼
StudyCheckinServiceImpl
    │  ① Redis 分布式锁防重复提交
    │  ② Set 判断今日是否已打卡（去重）
    │  ③ String 计数器累加今日分钟数、连续天数
    │  ④ ZSet 写连续榜 / 当日时长榜
    │  ⑤ 删除个人概览 Hash 缓存（Cache-Aside）
    ▼
KafkaProducer ──投递 study-checkin-topic──▶ Kafka Broker
                                                ▼
                                    KafkaConsumer（异步）
                                     ① String 累计历史总时长
                                     ② 再次失效概览缓存
                                     ③ 预留：落库 / 推送 / 周报
```

查询链路（概览、榜单）直接走 Redis，不经过 Kafka。

---

## 二、改动清单

### 新增文件

| 文件 | 作用 |
| --- | --- |
| `backend/src/main/java/com/demo/scaffoldbackend/controller/StudyCheckinController.java` | 打卡模块 REST 接口层 |
| `backend/src/main/java/com/demo/scaffoldbackend/service/StudyCheckinService.java` | 服务接口 |
| `backend/src/main/java/com/demo/scaffoldbackend/service/impl/StudyCheckinServiceImpl.java` | 服务实现：Redis 操作 + 投递 Kafka 事件 |
| `backend/src/main/java/com/demo/scaffoldbackend/kafka/StudyCheckinEvent.java` | Kafka 消息体（事件对象） |
| `backend/src/main/java/com/demo/scaffoldbackend/kafka/StudyCheckinKafkaProducer.java` | Kafka 生产者 |
| `backend/src/main/java/com/demo/scaffoldbackend/kafka/StudyCheckinKafkaConsumer.java` | Kafka 消费者 |
| `backend/src/main/java/com/demo/scaffoldbackend/config/KafkaTopicConfig.java` | 自动注册 Kafka 主题 |
| `backend/src/main/java/com/demo/scaffoldbackend/dto/StudyCheckinDTO.java` | 打卡请求入参 |
| `backend/src/main/java/com/demo/scaffoldbackend/vo/StudyCheckinVO.java` | 打卡结果响应体 |
| `backend/src/main/java/com/demo/scaffoldbackend/vo/StudyOverviewVO.java` | 个人学习概览响应体 |
| `backend/src/main/java/com/demo/scaffoldbackend/vo/StudyRankVO.java` | 排行榜条目响应体 |
| `docker-compose-redis-kafka.yml` | Redis + Kafka 单机演示版编排文件 |

### 修改文件

| 文件 | 改动内容 |
| --- | --- |
| `backend/pom.xml` | 新增 `spring-boot-starter-kafka` 依赖（版本由 Spring Boot 父工程管理） |
| `backend/src/main/resources/application.yml` | 新增 Redis 密码项、Kafka 生产者/消费者/监听器配置 |
| `backend/src/main/java/com/demo/scaffoldbackend/config/SecurityConfig.java` | 放行 `/api/study-checkin/**`，方便本地联调（生产可改回登录鉴权） |
| `backend/.env.example` | 补充 `REDIS_PASSWORD`、`KAFKA_BOOTSTRAP` 示例配置 |

### 依赖版本（由 Spring Boot 4.0.8 自动管理）

- spring-kafka：`4.0.7`
- kafka-clients：`4.1.2`
- Redis 客户端：Lettuce（`spring-boot-starter-data-redis` 默认自带，项目原来已引入）

---

## 三、Redis 数据结构设计

所有 key 统一加 `study:` 前缀，避免和项目原有 key（`auth:`、`captcha:`、`cache:` 等）冲突。

| 业务场景 | Redis 结构 | Key 规则 | 过期时间 | 说明 |
| --- | --- | --- | --- | --- |
| 分布式锁，防并发重复打卡 | String（SET NX EX） | `study:lock:checkin:{userId}:{yyyy-MM-dd}` | 5 秒 | `setIfAbsent` 抢占锁，业务结束主动删除，5 秒兜底防死锁 |
| 当日打卡去重 | Set | `study:checkin:done:{yyyy-MM-dd}` | 不设（按天重建） | 成员是 userId，`isMember` 判断今天是否打过卡 |
| 当日学习分钟数计数 | String（INCRBY） | `study:minutes:{userId}:{yyyy-MM-dd}` | 2 天 | 同一天多次打卡累加当天分钟数 |
| 连续打卡天数 | String | `study:streak:{userId}` | 不设 | 结合上次打卡日期推算连续/中断 |
| 上次打卡日期 | String | `study:last-date:{userId}` | 不设 | 连续天数计算依据 |
| 个人学习概览缓存 | Hash | `study:overview:{userId}` | 30 分钟 | Cache-Aside：读先查缓存，未命中汇总后回填 |
| 连续打卡总榜 | ZSet | `study:rank:streak` | 不设 | score = 连续天数，倒序取 Top N |
| 当日时长榜 | ZSet | `study:rank:minutes:{yyyy-MM-dd}` | 不设 | score = 今日累计分钟数 |
| 历史累计总时长 | String（INCRBY） | `study:total-minutes:{userId}` | 不设 | 由 Kafka 消费者异步累加 |

### 为什么这么设计

- **锁用 String 而不是 Set**：Redis 分布式锁的标准做法是 `SET key value NX EX seconds`，对应 Spring Data 的 `setIfAbsent(key, value, Duration)`，一条命令完成「不存在才写入 + 带过期」，天然防死锁。
- **去重用 Set**：Set 成员天然唯一，判断「今天有没有打过卡」是 O(1) 的 `SISMEMBER`，比查 MySQL 快得多。
- **计数用 String 原子自增**：`INCRBY` 是原子操作，并发累加不会丢数；不需要把整个对象读出来改完再写回。
- **概览用 Hash**：一个用户的概览是一组字段（天数、今日分钟、总时长、名次……），Hash 可以按字段读写，比把整个对象序列化成一个 String 更灵活。
- **排行榜用 ZSet**：ZSet 每个成员带 score，自带按分数排序的能力，`reverseRangeWithScores` 直接拿倒序榜单，不需要自己在内存里排序。

---

## 四、Kafka 设计

### 1. 主题与消费组

| 项 | 值 | 说明 |
| --- | --- | --- |
| Topic | `study-checkin-topic` | 在 `KafkaTopicConfig` 里注册，启动时自动创建 |
| 分区数 | 3 | 演示环境够用，后续多实例消费可并行 |
| 副本数 | 1 | 单机 Docker 演示；集群环境再调高 |
| 消费组 | `study-checkin-group` | 配置在 `application.yml`，同组内一条消息只被一个实例消费 |

### 2. 消息体（`StudyCheckinEvent`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `eventId` | String | 事件唯一 ID（UUID），便于链路追踪和后续幂等去重 |
| `userId` | Integer | 打卡用户 |
| `minutes` | Integer | 本次学习分钟数 |
| `subject` | String | 学习科目（可选） |
| `date` | String | 打卡日期 `yyyy-MM-dd` |
| `streak` | Integer | 打卡时的连续天数 |
| `eventTime` | String | 事件产生时间（ISO-8601） |

### 3. 生产与消费约定

- **消息 key 用 userId**：Kafka 按 key 哈希选分区，同一个用户的事件进同一分区，保证个人事件的先后顺序；不同用户之间互不影响。
- **序列化用 String**：生产者把事件序列化成 JSON 字符串再发，消费者收到字符串后再反序列化。没有用 JSON 自带的类型反序列化，避免跨包类型映射问题，和项目里 Redis 全用字符串的风格保持一致。
- **投递失败不回滚打卡主流程**：Redis 里的实时数据已经落盘，Kafka 偶尔抖动不影响用户打卡；后续可以加补偿任务重放失败事件。
- **消费异常不抛出**：避免一条坏消息反复重放形成「毒消息」；要求严格不丢时，可以再接入死信主题（DLT）。

---

## 五、代码逐文件讲解

### 5.1 入参：`StudyCheckinDTO.java`

```java
@Data
public class StudyCheckinDTO {

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @Min(value = 1, message = "学习时长至少 1 分钟")
    @Max(value = 1440, message = "学习时长不能超过一天（1440 分钟）")
    private Integer minutes;

    private String subject;
}
```

- `@Data` 是 Lombok 注解，自动生成 getter/setter/toString/equals/hashCode，项目所有 DTO/VO 都这么写。
- `@NotNull`、`@Min`、`@Max` 是 Bean Validation 注解，Controller 入参加 `@Valid` 后，参数不合法会被 `GlobalExceptionHandler` 统一转成 `Result.error(400, 提示信息)`，Service 层不用再写重复判空。
- `subject` 没有加校验注解，是可选字段。

### 5.2 响应体：三个 VO

- `StudyCheckinVO`：打卡接口返回，包含 `userId`、`date`、`minutesToday`（今日累计）、`streak`（连续天数）、`firstCheckin`（是否当日首次打卡）。
- `StudyOverviewVO`：概览接口返回，比打卡结果多了 `totalMinutes`（历史总时长）、`rank`（榜单名次）、`cacheTime`（缓存写入时间，用来观察缓存是否命中）。
- `StudyRankVO`：榜单条目，`rank` 名次、`userId`、`score` 分数。

VO 只放接口需要暴露的字段，不直接把 Redis key 或内部结构吐出去。

### 5.3 事件对象：`StudyCheckinEvent.java`

就是一个普通 POJO + Lombok 注解，字段见上表。它是生产者和消费者之间的契约：两边都引用同一个类，字段改了编译期就能发现不一致。

### 5.4 生产者：`StudyCheckinKafkaProducer.java`

```java
@Component
public class StudyCheckinKafkaProducer {

    public static final String TOPIC = "study-checkin-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendCheckinEvent(StudyCheckinEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), payload);
            log.info("打卡事件已投递 Kafka：userId={}, minutes={}, topic={}",
                    event.getUserId(), event.getMinutes(), TOPIC);
        } catch (Exception e) {
            log.error("打卡事件投递 Kafka 失败：userId={}, 原因={}", event.getUserId(), e.getMessage());
        }
    }
}
```

逐点说明：

- `@Component` 交给 Spring 容器管理，Service 里直接构造注入。
- `KafkaTemplate<String, String>` 是 Spring Kafka 封装的发送模板，泛型对应「key 类型、value 类型」，这里都是 String。
- `ObjectMapper` 用的是项目统一的 Jackson 3（`tools.jackson.databind.ObjectMapper`），由 Spring Boot 自动注入。
- `send(topic, key, payload)`：第二个参数是消息 key，用 userId 保证同人事件有序；第三个参数是 JSON 消息体。
- catch 住异常只记日志：这是有意为之，投递失败不能把已经成功的打卡回滚掉。

### 5.5 消费者：`StudyCheckinKafkaConsumer.java`

```java
@Component
public class StudyCheckinKafkaConsumer {

    @KafkaListener(topics = StudyCheckinKafkaProducer.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onCheckinEvent(String payload) {
        try {
            StudyCheckinEvent event = objectMapper.readValue(payload, StudyCheckinEvent.class);
            Integer userId = event.getUserId();

            // 累计历史总学习分钟数（长期保留，不设过期）
            redis.opsForValue().increment("study:total-minutes:" + userId, event.getMinutes());

            // 失效个人概览 Hash 缓存，下次查询读穿透重建
            redis.delete("study:overview:" + userId);

            log.info("打卡事件异步处理完成：userId={}, 本次={}分钟, 连续={}天, 日期={}",
                    userId, event.getMinutes(), event.getStreak(), event.getDate());
        } catch (Exception e) {
            log.error("打卡事件处理失败，消息体={}, 原因={}", payload, e.getMessage());
        }
    }
}
```

逐点说明：

- `@KafkaListener` 标注在方法上，Spring Kafka 会自动启动消费者容器订阅主题；`groupId` 用占位符 `${...}` 从配置文件读，不用硬编码。
- 方法参数 `String payload` 对应配置里的 String 反序列化器，收到的是 JSON 字符串，再用 `readValue` 反序列化成事件对象。
- 第一个 Redis 操作：`increment` 给历史总时长累加本次分钟数。这个 key 不设过期，长期保留。
- 第二个 Redis 操作：删除概览缓存。打卡数据变了，缓存先失效，等下次查询时按最新数据重建，避免读到脏数据。
- 异常同样只记日志，不向上抛，防止消费位点一直回退、同一条消息无限重放。

### 5.6 主题注册：`KafkaTopicConfig.java`

```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic studyCheckinTopic() {
        return TopicBuilder.name("study-checkin-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

- `@Configuration` 表示这是一个 Spring 配置类，里面的 `@Bean` 方法会被容器执行。
- 应用启动时，Spring Kafka 会把这里声明的主题提交给 Broker 创建；如果主题已经存在，Broker 不会改动它。
- `partitions(3)` 分区数、`replicas(1)` 副本数，按单机演示环境设置。

### 5.7 服务接口：`StudyCheckinService.java`

定义五个业务方法：

| 方法 | 作用 |
| --- | --- |
| `checkIn(dto)` | 每日打卡 |
| `overview(userId)` | 查询个人概览（缓存读穿透） |
| `streakRank(top)` | 连续打卡榜 |
| `todayMinutesRank(top)` | 今日时长榜 |
| `evictOverviewCache(userId)` | 主动失效概览缓存 |

接口和实现分开，和项目里 `LearningLogService` / `LearningLogServiceImpl` 的写法保持一致。

### 5.8 核心实现：`StudyCheckinServiceImpl.java`

#### （1）依赖注入

```java
private final StringRedisTemplate redis;
private final StudyCheckinKafkaProducer producer;

public StudyCheckinServiceImpl(StringRedisTemplate redis, StudyCheckinKafkaProducer producer) {
    this.redis = redis;
    this.producer = producer;
}
```

- `StringRedisTemplate` 是 Spring Data Redis 提供的字符串模板，项目原有代码（会话、验证码、限流等）全都用它，这里沿用。
- 构造器注入而不是字段注入，方便单元测试替换 mock 对象。

#### （2）打卡 `checkIn`

整段流程分五步：

**第一步：抢分布式锁**

```java
String lockKey = "study:lock:checkin:" + userId + ":" + today;
Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
if (Boolean.FALSE.equals(locked)) {
    throw new BusinessException(429, "操作太频繁，请勿重复提交打卡");
}
```

`setIfAbsent` 对应 Redis 的 `SET NX EX`：key 不存在才写入成功，返回 `true`；已经存在返回 `false`。返回 `false` 说明上一次打卡还在处理中，直接抛业务异常 `429`（请求过于频繁）。锁 5 秒自动过期，即使程序崩溃没来得及删锁，也不会永久死锁。

**第二步：finally 里释放锁**

```java
try {
    // ... 业务逻辑
} finally {
    redis.delete(lockKey);
}
```

不管业务成功还是抛异常，`finally` 一定会执行删锁；主动删锁 + 5 秒过期双保险。

**第三步：Set 去重，判断是否当日首次打卡**

```java
boolean firstCheckin = !Boolean.TRUE.equals(
        redis.opsForSet().isMember(doneKey, String.valueOf(userId)));
```

`isMember` 判断成员是否在集合里。不在集合里就是今天第一次打卡；已经在集合里，说明是当天补充学习时长，不重复计算连续天数。

**第四步：累加分钟数，推算连续天数**

```java
long minutesToday = incrementMinutes(minutesKey, minutes);

if (firstCheckin) {
    redis.opsForSet().add(doneKey, String.valueOf(userId));
    // 上次打卡日期 → 推算连续天数
    streak = calcStreak(last, LocalDate.now(), prevStreak);
    redis.opsForValue().set(lastDateKey, today);
    redis.opsForValue().set(streakKey, String.valueOf(streak));
    redis.opsForZSet().add("study:rank:streak", String.valueOf(userId), streak);
} else {
    streak = readLong(streakKey, 0);
}
```

连续天数的推算规则在 `calcStreak` 里：

- 上次没打过卡（`last == null`）：连续 1 天；
- 上次就是今天：保持原天数（重复提交兜底）；
- 上次是昨天：连续天数 +1；
- 上次早于昨天：中断，重置为 1。

**第五步：写当日时长榜、失效缓存、发 Kafka 事件**

```java
redis.opsForZSet().add("study:rank:minutes:" + today, String.valueOf(userId), minutesToday);
redis.delete("study:overview:" + userId);

StudyCheckinEvent event = new StudyCheckinEvent(...);
producer.sendCheckinEvent(event);
```

- ZSet 的 `add`：成员存在就更新 score，不存在就新增，正好适合榜单分数刷新。
- 删概览缓存：打卡后概览数据变了，先让旧缓存失效。
- 发 Kafka 事件：后面的历史时长累计交给消费者异步做。

#### （3）概览 `overview`（Cache-Aside 读穿透）

这是标准的「缓存旁路」模式：

```java
// 先查缓存
Map<Object, Object> cached = redis.opsForHash().entries(hashKey);
if (!cached.isEmpty()) {
    // 命中：直接组装 VO 返回
    ...
    return vo;
}

// 未命中：从各个 Redis 结构汇总
long streak = readLong("study:streak:" + userId, 0);
String lastDate = redis.opsForValue().get("study:last-date:" + userId);
long minutesToday = readLong("study:minutes:" + userId + ":" + today, 0);
long totalMinutes = readLong("study:total-minutes:" + userId, 0);
Long reverseRank = redis.opsForZSet().reverseRank("study:rank:streak", String.valueOf(userId));

// 回填 Hash 缓存并设 30 分钟过期
redis.opsForHash().putAll(hashKey, entry);
redis.expire(hashKey, OVERVIEW_TTL);
```

要点：

- `entries` 取整个 Hash；空 Map 表示缓存未命中。
- 名次用 `reverseRank`：ZSet 默认按 score 正序排名，`reverseRank` 是倒序排名（分数越高名次越靠前），返回 0 开始的下标，所以 `+1` 转成 1 开始的名次；未上榜返回 `null`。
- 回填时把所有字段写进 Hash，`expire` 设 30 分钟 TTL。缓存到期后下次查询重新走一遍汇总，避免陈旧数据永久残留。
- `cacheTime` 字段记录缓存写入时间，调接口时对比它就能判断这次是命中缓存还是重建的。

#### （4）排行榜 `streakRank` / `todayMinutesRank`

两个榜单共用私有方法 `buildRank`：

```java
Set<ZSetOperations.TypedTuple<String>> tuples =
        redis.opsForZSet().reverseRangeWithScores(key, 0, top - 1);
```

- `reverseRangeWithScores(key, start, end)`：按 score 倒序取 `[start, end]` 区间的成员，同时带出分数；`0` 到 `top - 1` 就是前 top 名。
- `TypedTuple` 里 `getValue()` 是成员（userId），`getScore()` 是分数。
- 方法里先校验 `top` 在 1~100 之间，防止一次查太多。

#### （5）辅助方法

- `incrementMinutes`：`increment(key, minutes)` 原子累加，再 `expire` 设 2 天过期。
- `readLong`：把 Redis 里读出来的字符串安全转成 long；空值或格式错误返回默认值，避免 `NumberFormatException` 打断接口。它有两个重载：`readLong(String key, long defaultValue)` 负责按 key 去 Redis 取值，`readLong(Object raw, long defaultValue)` 负责把拿到的值解析成 long。**注意第一个重载里调用第二个重载时必须把返回值显式强转成 `Object`**：

```java
private long readLong(String key, long defaultValue) {
    // 必须显式转 Object，否则编译期按 String 类型再次绑定到本方法（把返回值当 key），
    // 缓存缺失时 get 返回 null，被当成 key 传给 Redis 就会抛 "non null key required"。
    return readLong((Object) redis.opsForValue().get(key), defaultValue);
}
```

这是 Java 静态重载解析的常见坑：方法重载在编译期按实参**静态类型**选方法，`get()` 返回 String，不加强转就会匹配到第一个 `String` 重载而不是 `Object` 重载。

### 5.9 接口层：`StudyCheckinController.java`

```java
@RestController
@RequestMapping("/api/study-checkin")
public class StudyCheckinController {

    @PostMapping
    public Result<StudyCheckinVO> checkIn(@Valid @RequestBody StudyCheckinDTO dto) {
        return Result.success(studyCheckinService.checkIn(dto));
    }

    @GetMapping("/overview/{userId}")
    public Result<StudyOverviewVO> overview(@PathVariable Integer userId) { ... }

    @GetMapping("/rank/streak")
    public Result<List<StudyRankVO>> streakRank(@RequestParam(defaultValue = "10") int top) { ... }

    @GetMapping("/rank/today")
    public Result<List<StudyRankVO>> todayRank(@RequestParam(defaultValue = "10") int top) { ... }

    @DeleteMapping("/cache/{userId}")
    public Result<Boolean> evictCache(@PathVariable Integer userId) { ... }
}
```

- 遵循项目规范：Controller 只做参数绑定、校验和 `Result` 封装，业务逻辑全部下沉到 Service。
- 所有返回统一包成 `Result<T>`（`code` / `message` / `data`），和项目原有接口一致。
- `@Valid` 触发 DTO 上的注解校验；`@PathVariable` 取路径参数；`@RequestParam` 是可选查询参数，默认前 10 名。

### 5.10 配置改动

**`pom.xml`**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-kafka</artifactId>
</dependency>
```

不需要写版本号，Spring Boot 父工程 `4.0.8` 已经管理了兼容的 spring-kafka / kafka-clients 版本。

**`application.yml`**

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 3000

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:127.0.0.1:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: 1
      retries: 3
    consumer:
      group-id: study-checkin-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    listener:
      missing-topics-fatal: false
```

- 地址都走环境变量占位符，默认 `127.0.0.1`；Redis/Kafka 在 Ubuntu Docker 里时，在 `.env` 里填 Ubuntu 的 IP 即可，不用改代码。
- `acks: 1`：Leader 确认写入就算成功，演示环境够用；`retries: 3`：发送失败自动重试 3 次。
- `auto-offset-reset: earliest`：新消费组第一次消费时从头开始读，方便联调时看到历史消息。
- `missing-topics-fatal: false`：主题没创建或 Broker 暂时不可用时，不让应用启动失败，消费者在后台自动重连。

**`SecurityConfig.java`**

```java
.requestMatchers("/api/study-checkin/**").permitAll()
```

本地联调阶段先放行，不用先登录拿 token。上线前如果要接入登录鉴权，删掉这一行即可，接口会走原来的 JWT 过滤器。

---

## 六、怎么运行

### 6.1 环境要求

| 软件 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 17 及以上（本机装的 19 也能跑） | 编译目标是 17 |
| Maven | 项目自带 Wrapper（`mvnw.cmd`，会自动下载 3.9.16） | 不强制本机装 Maven |
| Docker | 在 Ubuntu 上，能跑容器 | 用来起 Redis 和 Kafka |
| MySQL | 8.0 | 打卡模块本身不读 MySQL，但项目其他模块和启动校验需要，按原项目 `application.yml` 配好 |

### 6.2 启动 Redis 和 Kafka（Ubuntu Docker）

> 本机实际情况：WSL Ubuntu 的 Docker 里已有 `kaoyan-redis`（Redis，6379，无密码）在运行，**直接复用，不要再起第二个 Redis**，否则 6379 端口冲突；Kafka 用仓库编排文件启动。

#### 重要：WSL 里访问 Windows D 盘的路径

仓库克隆在 Windows 的 `D:\Postgraduate-Agent`，WSL Ubuntu 不能直接用这个 Windows 路径，要换成 `/mnt/d/Postgraduate-Agent`。如果在 WSL 家目录直接执行 `docker compose -f docker-compose-redis-kafka.yml ...`，会报 `no such file or directory`。

#### 方式 A：启动 Kafka（推荐，复用已有 Redis）

```bash
docker compose -f /mnt/d/Postgraduate-Agent/docker-compose-redis-kafka.yml up -d kafka
```

查看容器状态：

```bash
docker ps | grep -E 'kaoyan-redis|demo-kafka'
```

Kafka 镜像用的是 Apache 官方 `apache/kafka:3.9.0`（KRaft 模式，不需要 ZooKeeper）。

只有本机完全没有 Redis 时，才显式启动编排里的可选 Redis（profile 隔离，默认不启动）：

```bash
docker compose -f /mnt/d/Postgraduate-Agent/docker-compose-redis-kafka.yml --profile redis up -d redis
```

停止 / 清空数据：

```bash
docker compose -f /mnt/d/Postgraduate-Agent/docker-compose-redis-kafka.yml down
docker compose -f /mnt/d/Postgraduate-Agent/docker-compose-redis-kafka.yml down -v   # 连数据卷一起删
```

#### 方式 B：连接其他现成的 Redis / Kafka

如果 Ubuntu 里是另外的现成容器，不用启动编排服务，直接在后端 `.env` 里填它们的地址即可（见 6.3）。

#### 跨机器访问注意事项

如果后端不在这台 Ubuntu 本机（比如后端在 Windows、Kafka 在局域网另一台 Ubuntu），Kafka 的 `advertised.listeners` 必须填 Ubuntu 的实际 IP，不能是 `127.0.0.1`：

- 客户端先连 Broker 拿到 advertised 地址，再用这个地址建立正式连接；advertised 写 `127.0.0.1` 的话，跨机器客户端会去连自己本机，导致连不上。
- compose 文件里这一行按需修改：

```yaml
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://192.168.x.x:9092
```

WSL2 本机回环转发正常时（Windows 经 `127.0.0.1:9092` 能访问到 WSL 里的 Kafka），保持 `127.0.0.1:9092` 即可。Redis 没有 advertised 问题，直接在 `.env` 填可达 IP 即可。

### 6.3 配置后端环境变量

在 `backend` 目录复制模板：

```powershell
cd D:\Postgraduate-Agent\backend
Copy-Item .env.example .env
```

编辑 `backend/.env`，按下表填写：

| 变量 | 是否必填 | 说明 |
| --- | --- | --- |
| `DB_USERNAME` | 必填 | MySQL 用户名 |
| `DB_PASSWORD` | 必填 | MySQL 密码 |
| `DB_PORT` / `DB_URL` | 视情况 | 默认连 `127.0.0.1:3307/kaoyan`；本机 MySQL 若在 3306，设 `DB_PORT=3306`，或用 `DB_URL` 整条覆盖 |
| `JWT_SECRET` | 必填 | 至少 32 字节随机字符串，项目登录组件启动需要 |
| `REDIS_HOST` | 选填 | Redis 所在 IP，默认 `127.0.0.1`；Docker 在 Ubuntu 就填 Ubuntu IP |
| `REDIS_PORT` | 选填 | 默认 `6379` |
| `REDIS_PASSWORD` | 选填 | Redis 有密码就填，没有留空 |
| `KAFKA_BOOTSTRAP` | 选填 | 默认 `127.0.0.1:9092`；填 Ubuntu IP:9092 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | 选填 | 项目里 AMQP 目前没有业务代码，保持默认 guest 即可 |

`.env` 不会被打包进 jar，只在本地启动时由 `application.yml` 的 `config.import` 读取。

### 6.4 编译并启动后端

在 `backend` 目录下：

```powershell
# 编译（验证代码无误）
.\mvnw.cmd -DskipTests compile

# 启动后端
.\mvnw.cmd spring-boot:run
```

启动成功后控制台会看到 Spring Boot 启动日志，端口 `8080`。Kafka 主题会在启动时自动创建；如果 Broker 还没起，日志里会有连接重试告警，不影响进程，Broker 起来后会自动连上。

### 6.5 接口联调

#### 方式一：浏览器内置联调页面（最省事）

后端启动后，浏览器直接打开：

```
http://localhost:8080/checkin.html
```

页面里分三块：打卡表单、个人概览、排行榜，填好点按钮即可，返回 JSON 显示在下方文本框。页面的请求由浏览器内 JS 发出：打卡是 `POST` + JSON，概览和榜单是 `GET`。页面文件位于 `backend/src/main/resources/static/checkin.html`，已在 `SecurityConfig` 放行。

> 注意：浏览器地址栏本身只能发 GET，不能直接提交 POST JSON；这个页面就是用内嵌 JS 绕开地址栏限制，在网页里完成 POST 调用。

#### 方式二：命令行 / 接口工具

下面以用户 `1001` 为例。后端地址 `http://localhost:8080`。

#### ① 打卡（第一次）

PowerShell：

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/study-checkin" `
  -ContentType "application/json" `
  -Body '{"userId":1001,"minutes":45,"subject":"数学"}'
```

curl（Git Bash / WSL）：

```bash
curl -X POST http://localhost:8080/api/study-checkin \
  -H "Content-Type: application/json" \
  -d '{"userId":1001,"minutes":45,"subject":"数学"}'
```

预期返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "date": "2026-11-22",
    "minutesToday": 45,
    "streak": 1,
    "firstCheckin": true
  }
}
```

#### ② 当天再打一次卡（补充时长）

```bash
curl -X POST http://localhost:8080/api/study-checkin \
  -H "Content-Type: application/json" \
  -d '{"userId":1001,"minutes":30,"subject":"英语"}'
```

预期：`minutesToday` 累加到 `75`，`firstCheckin` 为 `false`，`streak` 仍是 1。

换几个用户多打几次，榜单才有数据：

```bash
curl -X POST http://localhost:8080/api/study-checkin -H "Content-Type: application/json" -d '{"userId":1002,"minutes":60,"subject":"政治"}'
curl -X POST http://localhost:8080/api/study-checkin -H "Content-Type: application/json" -d '{"userId":1003,"minutes":20,"subject":"专业课"}'
```

#### ③ 查询个人概览

```bash
curl http://localhost:8080/api/study-checkin/overview/1001
```

预期返回（首次查询是缓存重建，`cacheTime` 是当前时间；紧接着再请求一次，字段不变即缓存命中）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "streak": 1,
    "lastDate": "2026-11-22",
    "minutesToday": 75,
    "totalMinutes": 75,
    "rank": 1,
    "cacheTime": "2026-11-22T20:10:30.123"
  }
}
```

> `totalMinutes` 由 Kafka 消费者异步累加，打卡后稍等一两秒再查概览才能看到；如果消息还没消费完，这里可能是 0。

#### ④ 连续打卡榜

```bash
curl "http://localhost:8080/api/study-checkin/rank/streak?top=10"
```

#### ⑤ 今日时长榜

```bash
curl "http://localhost:8080/api/study-checkin/rank/today?top=10"
```

榜单返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "rank": 1, "userId": "1001", "score": 75.0 },
    { "rank": 2, "userId": "1002", "score": 60.0 }
  ]
}
```

#### ⑥ 手动失效概览缓存

```bash
curl -X DELETE http://localhost:8080/api/study-checkin/cache/1001
```

### 6.6 用 Redis CLI 观察数据

进入 Redis 容器（本机复用的是 `kaoyan-redis`；容器名按 `docker ps` 实际为准）：

```bash
docker exec -it kaoyan-redis redis-cli
```

常用查看命令：

```redis
# 查看所有 study: 开头的 key
KEYS study:*

# 今日已打卡用户集合
SMEMBERS study:checkin:done:2026-11-22

# 今日分钟数 / 连续天数 / 历史总时长
GET study:minutes:1001:2026-11-22
GET study:streak:1001
GET study:total-minutes:1001

# 概览 Hash 缓存
HGETALL study:overview:1001

# 连续榜 / 当日时长榜（带分数）
ZRANGE study:rank:streak 0 -1 WITHSCORES
ZRANGE study:rank:minutes:2026-11-22 0 -1 WITHSCORES
```

### 6.7 用 Kafka 控制台观察消息

查看主题列表：

```bash
docker exec -it demo-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

应该能看到 `study-checkin-topic`（后端启动时由 `KafkaTopicConfig` 自动创建）。

消费最新消息（新开一个终端，打卡后就能看到 JSON）：

```bash
docker exec -it demo-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic study-checkin-topic --from-beginning
```

消息示例：

```json
{
  "eventId": "3e9c...-xxxx",
  "userId": 1001,
  "minutes": 45,
  "subject": "数学",
  "date": "2026-11-22",
  "streak": 1,
  "eventTime": "2026-11-22T20:10:25.123"
}
```

### 6.8 前端对接

打卡模块已接进首页 `frontend/src/views/HomeView.vue`：侧边栏新增「学习打卡」标签页，包含今日学习分钟、连续天数、历史总时长、当前名次四张概览卡片，打卡表单，以及连续榜 / 今日时长榜两个列表。前端接口封装在 `frontend/src/api/study.js`（`checkin` / `checkinOverview` / `checkinRank`），统一走 `src/api/http.js` 的请求封装，`/api` 由 Vite 代理到后端 `127.0.0.1:8080`。

页面里直接请求相对路径即可，例如：

```js
// 打卡
fetch('/api/study-checkin', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ userId: 1001, minutes: 45, subject: '数学' })
})

// 概览
fetch('/api/study-checkin/overview/1001')

// 榜单
fetch('/api/study-checkin/rank/streak?top=10')
```

前端启动：

```powershell
cd D:\Postgraduate-Agent\frontend
npm install
npm run dev
```

启动后访问 `http://localhost:5173/`，Vite 会把 `/api` 代理到后端 `127.0.0.1:8080`。

> 说明：原 `vite.config.js` 按 HTTPS + 443 端口 + 域名 `kaoyan.localhost` 配置，依赖未提交到仓库的 `frontend/certs/*.pem` 证书；本机联调已改为普通 HTTP `localhost:5173`，去掉了证书读取和 hosts 映射。若要恢复原 HTTPS 方案，需自签证书放入 `frontend/certs/`，并在 hosts 加 `127.0.0.1 kaoyan.localhost`。

---

## 七、常见问题排查

### 1. 后端启动报 Redis 连接超时 / 拒绝连接

- 确认 Ubuntu 上 Redis 容器在跑：`docker ps`；
- 确认 `.env` 里 `REDIS_HOST` 是 Ubuntu 的可达 IP（本机回环才用 `127.0.0.1`）；
- 确认防火墙放行了 6379 端口；
- Redis 设了密码却没在 `REDIS_PASSWORD` 填，会报认证错误。

### 2. Kafka 能连上但生产/消费一直不通，报 advertised listener 错误

这是 advertised 地址问题：按 6.2 节把 `KAFKA_ADVERTISED_LISTENERS` 改成 Ubuntu 实际 IP，重启 Kafka 容器。

### 3. 日志里 Kafka 主题不存在 / 连接重试

`missing-topics-fatal: false` 保证应用不会因此启动失败。先把 Broker 起起来，主题会在首次发送或消费者订阅时自动创建；也可以用 6.7 的命令手动建。

### 4. 概览里 `totalMinutes` 是 0

`totalMinutes` 是消费者异步累加的，打卡后等 1~2 秒再查；如果一直是 0，去 Kafka 控制台看消息有没有进来，再看后端日志有没有「打卡事件异步处理完成」。

### 5. 接口返回 401

#### 5.1 打卡接口被拦截

`/api/study-checkin/**` 已在 `SecurityConfig` 放行；如果被拦截，检查是否改动了该配置，或请求路径前缀是否拼错。

#### 5.2 登录后仍提示「未登录或令牌已失效」（数据大屏 / 打卡入口一起消失、点击打卡无反应）

**现象**：账号密码登录成功、本地也存了令牌，但首页横幅一直是「未登录或令牌已失效」，顶栏用户名不显示；侧边栏没有「数据大屏」，「学习打卡」点了也没反应；`/api/user/current` 实际返回 401。

**根因**：数据库缺少三张 RBAC 表 `zt_group`、`zt_grouppriv`、`zt_usergroup`。JWT 过滤器在令牌解析通过、Redis 会话比对一致后，还要调 `RbacService.permissionsOf(userId, role)` 查当前用户权限；这一步查不到表会抛异常，被过滤器外层 `catch` 兜底后清空 `SecurityContext`，后续请求就被认证入口判成 401。前端拿不到用户信息，`isAdmin / isStudent` 都是 `false`，于是大屏导航被隐藏；打卡方法取到空用户，表现为「点击没反应」。

**修复**：导入仓库自带的 RBAC 初始化脚本（脚本是 `CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`，只补表和初始数据，不删除原有数据）：

```powershell
# MySQL 在 3306、root 无密码时
& "D:\phpstudy_pro\Extensions\MySQL8.0.12\bin\mysql.exe" `
  -h 127.0.0.1 -P 3306 -u root kaoyan `
  -e "source D:/Postgraduate-Agent/docs/sql/zentao_rbac.sql"
```

**验证**：带令牌访问 `/api/user/current` 返回 200 且带出 `userRole`；刷新首页后顶栏显示用户名、侧边栏出现「数据大屏」，打卡可正常提交。

> 全新初始化数据库时，除了业务表，务必把 `docs/sql/zentao_rbac.sql` 一并导入，否则任何登录用户都会卡在这个 401。

### 6. 端口被占用

- 后端 8080：改 `application.yml` 的 `server.port`；
- Redis 6379 / Kafka 9092：改 compose 里宿主机端口映射，同时同步修改 `.env` 里的连接地址。

### 7. MySQL 连不上会不会影响打卡模块

打卡模块的数据全部在 Redis/Kafka，不读 MySQL；但项目启动时 MyBatis-Plus / 其他模块可能依赖数据源，按原项目把 MySQL 配好更稳妥。

### 8. 打卡接口返回 400：`non null key required`

这个异常来自 Spring Data Redis 的非空断言，说明某次 Redis 操作把 `null` 当成了 key 传进去。本模块最容易触发它的就是上面 5.8 节讲的 `readLong` 重载坑：

- 现象：首次打卡写了当日分钟数和「已打卡」集合后，在读连续天数时直接 400；当天第二次打卡也在补时长分支同样报错。
- 原因：`readLong(String key, ...)` 把 `opsForValue().get(key)` 的结果（String，缓存缺失时为 `null`）再传给 `readLong`，编译期按静态类型绑定回了第一个重载，`null` 被当成 Redis key。
- 修复：委托第二个重载时显式 `(Object)` 强转，让它绑定到值解析重载，见 5.8 节代码。
- 排查手段：在 `GlobalExceptionHandler` 的 `IllegalArgumentException` 分支把堆栈打出来（本项目已保留 `log.error("参数/断言异常: ", e)`），顺着 `StudyCheckinServiceImpl` 的调用帧定位到具体方法。

### 9. 浏览器地址栏打开打卡接口返回 500

打卡接口 `POST /api/study-checkin` 只接受 POST 且必须带 JSON 请求体；浏览器地址栏只能发 GET，方法对不上就会被全局异常兜底成 500。注意区分：

- **能用浏览器直接打开的是 GET 接口**：`/api/study-checkin/overview/{userId}`、`/api/study-checkin/rank/streak`、`/api/study-checkin/rank/today`。
- **打卡是 POST 接口**，必须用 PowerShell / curl / Postman / IDEA HTTP Client 这类工具发 JSON，不能在地址栏直接访问：

  ```bash
  curl -X POST http://localhost:8080/api/study-checkin \
    -H "Content-Type: application/json" \
    -d '{"userId":1001,"minutes":45,"subject":"数学"}'
  ```

项目的 `GlobalExceptionHandler` 已对「请求方法不支持」单独处理，重启后这类情况会返回 405 和明确提示，而不是笼统的 500。

---

## 八、后续可扩展点

1. **事件落库**：消费者收到打卡事件后写 `learning_log` 表，Redis 只做热点缓存，MySQL 做持久化。
2. **死信队列**：消费多次失败的消息转发到 `study-checkin-topic.DLT`，人工或定时补偿。
3. **打卡提醒 / 周报**：消费者按事件触发「连续 7 天打卡」鼓励推送，或汇总周数据生成周报。
4. **缓存预热**：参考项目已有 `CacheWarmupRunner`，启动时把热门榜单提前刷进 Redis。
5. **接口权限收紧**：上线前移除 `SecurityConfig` 里的放行规则，从 JWT 令牌里取当前登录用户，而不是前端传 userId。
