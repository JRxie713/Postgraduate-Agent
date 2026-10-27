# Postgraduate-Agent

> 考研 AI 导学系统
> 个性化 · 智能化 · 全流程的考研备考 AI 副驾

## 项目简介

本项目是前后端分离的 Web 应用，面向 **考研学子** 打造一站式 AI 导学服务。系统以入学诊断为起点，自动生成个人备考画像与学习计划，贯穿 **备考规划 → 知识精讲 → 刷题训练 → 模考冲刺 → 复试指导 → 择校决策** 全周期，实现 "精准诊断 + 智能规划 + 持续陪学" 的个性化备考体验。

系统结合大模型能力提供 AI 答疑（多轮对话 / 拍照搜题）、智能组卷、薄弱点专项训练、知识点掌握图谱、学情周报与驾驶舱、冲稳保择校推荐、模拟面试、主观题批改、错题归因分析、政策信息流推送等核心能力，帮助考研学子高效完成从备考到复试的全过程。

项目采用前后端分离架构：
- **前端**：基于 Vue3 + Vite + ESLint 搭建工程化脚手架；
- **后端**：基于 Spring Boot + Shiro + MyBatis 实现，包含权限控制、日志管理、数据库交互、统一响应封装等基础能力，按 12 个业务模块组织 RESTful API。

## 核心特性

- **精准备考诊断**：入学摸底 → 生成个人画像 → 薄弱点自动识别 → 学习计划智能生成与自适应调整
- **全流程考研闭环**：诊断 → 规划 → 学习 → 训练 → 模考 → 复试 → 择校，每个环节都有 AI 陪伴
- **引导式答疑模式**：通过「思路 → 步骤 → 提示 → 总结」引导自主思考，不直接给答案
- **考研全科覆盖**：公共课（政治 / 英语 / 数学）+ 专业课（理工 / 文史 / 经管 / 艺术 / 教育等）
- **数据驱动备考**：学习打卡、知识点掌握度雷达图、能力诊断、学情周报与驾驶舱、趋势追踪
- **择校 & 复试一体化**：冲稳保三档院校推荐、院校对比、模拟面试与能力报告、陪学对话与情绪疏导

## 技术栈

### 前端
- Vue 3
- Vite
- ESLint（代码规范校验）

### 后端
- Spring Boot
- Shiro（权限控制）
- MyBatis（数据访问层）
- SLF4J + Log4j2（日志管理）
- MySQL（关系型数据库）

## 系统架构

系统采用前后端分离架构，前端通过 RESTful API 与后端通信，后端按经典分层架构组织代码：

```
┌─────────────┐        HTTP / RESTful         ┌──────────────────────────────────┐
│   Frontend  │  ───────────────────────────> │             Backend              │
│  Vue3+Vite  │                               │  ┌────────────────────────────┐  │
│             │  <── JSON / 统一 Result ────  │  │  Controller 接口控制器层    │  │
└─────────────┘                               │  ├────────────────────────────┤  │
                                              │  │  Service 业务逻辑层         │  │
                                              │  ├────────────────────────────┤  │
                                              │  │  Mapper 数据访问层 (MyBatis) │  │
                                              │  ├────────────────────────────┤  │
                                              │  │  MySQL / Redis 存储          │  │
                                              │  └────────────────────────────┘  │
                                              └──────────────────────────────────┘
```

各层职责：
- **Controller**：接收请求、参数校验、调用 Service、封装 `Result<T>` 统一响应
- **Service / impl**：业务逻辑编排、事务控制、与 Mapper 交互
- **Mapper**：基于 MyBatis 的数据访问层，配合 `resources/mapper/*.xml` 完成 SQL 映射
- **common/result**：`Result<T>` 统一响应封装，统一 code / message / data 结构
- **common/exception**：全局异常处理，统一错误返回
- **config**：Shiro 权限配置、CORS 跨域配置等基础设施

## 项目目录结构

```
Postgraduate-Agent
├── backend/                        # 后端 SpringBoot 项目
│   ├── src/main/java/com/demo/scaffoldbackend/
│   │   ├── common/                 # 通用模块
│   │   │   ├── exception/          # 全局异常处理
│   │   │   └── result/             # 统一响应封装（Result 类）
│   │   ├── config/                 # 配置类（Shiro、CORS 等）
│   │   ├── controller/             # 接口控制器层
│   │   ├── dto/                    # 数据传输对象
│   │   ├── entity/                 # 数据库实体类
│   │   ├── mapper/                 # MyBatis 数据访问层
│   │   ├── service/                # 业务逻辑层
│   │   │   └── impl/               # 业务实现类
│   │   └── ScaffoldBackendApplication.java  # 项目启动入口
│   ├── src/main/resources/
│   │   ├── application.yml         # 应用主配置
│   │   ├── log4j2.xml              # 日志配置
│   │   └── mapper/                 # MyBatis XML 映射文件
│   ├── src/test/                   # 单元测试
│   ├── pom.xml                     # Maven 依赖管理
│   ├── mvnw / mvnw.cmd             # Maven Wrapper
│   └── Dockerfile                  # Docker 容器化配置
├── frontend/                       # 前端 Vue3 项目
│   ├── src/
│   │   ├── assets/                 # 静态资源（图片、字体等）
│   │   ├── components/             # 公共组件
│   │   ├── App.vue                 # 根组件
│   │   └── main.js                 # 入口文件
│   ├── public/                     # 公共资源
│   ├── index.html                  # HTML 模板
│   ├── vite.config.js              # Vite 构建配置
│   └── package.json                # 前端依赖管理
├── docs                            # 项目文档
├── docker-compose.yml              # Docker 多容器编排
├── .gitignore                      # Git 忽略规则
└── README.md
```

## 环境要求

### 后端
- JDK 17
- Maven（内置 mvnw，无需本地安装）
- MySQL 8.0
- settings.xml（Maven 镜像配置，本机单独配置，不提交仓库）

### 前端
- Node.js 18+
- npm

## 本地启动步骤

### 后端启动

1. 克隆仓库到本地
```bash
git clone https://github.com/JRxie713/Postgraduate-Agent.git
cd Postgraduate-Agent/backend
```

2. 修改 `src/main/resources/application.yml`，配置数据库账号密码，后端默认端口：8080
3. 执行启动命令
```
.\mvnw.cmd spring-boot:run
```

4. 启动成功后访问测试接口：`http://localhost:8080/api/test/hello`


### 前端启动

1. 进入 frontend 文件夹
```
cd ../frontend
```

2. 安装依赖
```
npm install
```

3. 启动开发服务
```
npm run dev
```

> 前端默认运行在 3000 端口，后端默认运行在 8080 端口。

## 接口规范

后端所有接口统一返回 `Result<T>` 结构，定义在 [common/result/Result.java](backend/src/main/java/com/demo/scaffoldbackend/common/result/Result.java)：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

- `code`：状态码，`200` 表示成功，其它值表示业务错误
- `message`：状态描述信息
- `data`：业务数据载荷，泛型 `T` 按接口语义确定具体类型

例如测试接口 `GET /api/test/hello` 返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "message": "考研AI导学系统后端启动成功！",
    "status": "running"
  }
}
```

## 功能清单

### 1. 入学诊断与备考画像
- 入学摸底测试：获取诊断卷、提交作答、AI 自动批改
- 生成个人备考画像：学科基础、薄弱知识点、预估分数线
- 画像重算：随学习进度动态更新薄弱点识别结果
- 薄弱知识点专项分析：定位根因并给出补漏建议

### 2. 智能学习计划（Plan + AI）
- AI 生成个性化备考计划：基于画像、目标院校、可用时长
- 计划自适应调整：根据完成率、模考成绩自动优化后续安排
- 今日导学任务卡：每日任务清单 + 优先级 + 预计时长
- 学习任务 CRUD：手动增删改查任务、调整状态

### 3. AI 智能答疑（Consultation + AI）
- 自然语言多轮问答：概念、公式、原理、案例
- 拍照答疑（OCR）：上传题目图片，自动识别并生成回答
- 会话历史查询、AI 自动会话总结
- **不直接给答案**：提供「思路 → 步骤 → 提示 → 总结」引导式讲解

### 4. 智能组卷与训练（QuestionBank + AI）
- 智能组卷：按科目、章节、难度生成标准试卷
- 自适应出题：基于薄弱点动态推题
- 薄弱点专项卷：聚焦高频错点集中训练
- 题库管理：试卷 CRUD、题目 CRUD

### 5. 模考与主观题批改（MockExam + AI）
- 模考全流程：试卷下发 → 作答 → 提交 → AI 批改
- 主观题 AI 评分：自动打分 + 评语 + 改进建议
- 模考后能力诊断：各科得分分析 + 提升方向
- 模考成绩分布统计（Stat 模块）

### 6. 错题本与归因分析（WrongQuestion + AI）
- 错题自动收录：按科目、知识点分组
- AI 错题归因分析：错因诊断（知识盲点 / 粗心 / 方法不当）
- 相似题推荐：举一反三，强化同考点训练

### 7. 知识点掌握图谱（AI）
- 知识地图可视化：各科目知识点掌握度雷达图
- 知识点详情：讲解 + 典型例题 + 掌握状态

### 8. 学情报告与数据看板（Stat + AI）
- 学情周报：模考趋势、知识点变化、下阶段建议
- 学情驾驶舱：能力雷达图、目标院校分数线 / 预估分数 / 差距
- 平台运营统计：概览、学情分布、题库质量、择校热度

### 9. 择校推荐与院校查询（School + AI）
- 冲稳保三档院校推荐：基于分数 / 专业 / 地域偏好
- 院校对比：多维度横向对比
- 院校信息 CRUD

### 10. 考研政策信息流（Policy + AI）
- 个性化政策推送：招生简章、调剂信息、报录公告等
- 政策信息 CRUD
- 全局语义检索：跨知识库的自然语言搜索

### 11. 模拟面试与复试（Interview + AI）
- 模拟面试全流程：开启 → 作答 → AI 评分
- 复试能力报告：面试表现 + 改进建议
- 面试记录 CRUD

### 12. 陪学导师与督导（AI）
- 陪学对话与情绪疏导：缓解备考压力
- AI 督导提醒：计划完成、作息、心理建设
- 学情预警与干预建议：连续缺勤 / 成绩下滑等异常主动预警

### 13. 用户体系（User）
- 注册、登录、Token 刷新、登出
- 当前用户信息、密码修改、账号安全校验

### 14. 学习日志（LearningLog）
- 学习打卡、学习时长记录
- 日志 CRUD、按用户查询

## 组员协作规范

### 常用 Git 命令

- 提交代码至远程仓库：依次执行 `git add .`、`git commit -m "类型: 描述内容"`、`git push`
    - 提交类型说明：feat 新功能、fix 修复缺陷、docs 文档更新、refactor 代码重构
- 拉取远程仓库最新代码：执行 `git pull`
- 强制同步为远程版本（会覆盖本地修改，请谨慎使用）：执行 `git fetch origin` 后再执行 `git reset --hard origin/main`

### 协作要求
1. 所有代码提交前，须在本地测试确保功能正常运行
2. 业务代码修改应通过 Git 提交至远程仓库
3. `settings.xml` 为本机 Maven 环境配置，禁止提交至远程仓库
4. 项目文档统一存放于 docs 目录
5. 推送代码前先执行 `git pull` 拉取最新版本，避免产生冲突

### 代码规范
- 前端代码需通过 ESLint 校验，提交前执行 `npm run lint`
- 后端遵循阿里巴巴 Java 开发手册风格，类名使用大驼峰、方法名小驼峰
- Controller 层只做参数校验与响应封装，业务逻辑一律下沉到 Service 层
- 统一通过 `Result<T>` 封装返回，禁止直接返回裸对象或 Map

