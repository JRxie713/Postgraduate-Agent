# Postgraduate-Agent
大学生辅助学习AI数字人系统

## 项目简介
本项目是课程设计实现的前后端分离Web应用，面向大学生群体打造AI学习数字人助教。系统支持学习陪伴、知识点导学、智能答疑等功能，提供一对一学习辅助，帮助学生完成课前预习、课中答疑、课后复习与备考。
项目采用前后端分离架构，前端基于Vue3+Vite+ESLint搭建脚手架；后端基于SpringBoot + Shiro + MyBatis实现，包含权限控制、日志管理、数据库交互等基础能力。

## 技术栈
### 前端
- Vue3
- Vite
- ESLint（代码规范校验）

### 后端
- SpringBoot
- Shiro（权限控制）
- MyBatis
- Slf4j + Log4j2（日志管理）
- MySQL

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
├── docs/                           # 项目文档
│   ├── 系统功能分析说明书.md
│   ├── 系统接口设计文档.md
│   ├── 系统详细设计.md
│   ├── 系统安装部署文档.md
│   ├── 系统操作说明书.md
│   └── 数据库脚本.sql
├── docker-compose.yml              # Docker 多容器编排
├── .gitignore                      # Git 忽略规则
└── README.md
```
## 环境要求
### 后端
- JDK 17
- Maven（内置mvnw，无需本地安装）
- MySQL 8.0
- settings.xml（Maven镜像配置，本机单独配置，不提交仓库）

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


## 功能清单

### 1. AI 数字人陪伴督学
- 拟人化考研助教形象，语音 + 文字 + 动作交互
- 学习提醒、打卡、鼓励、心态安抚
- 支持"学霸模式 / 温柔模式 / 严管模式"切换

### 2. 智能择校与专业分析
- 根据本科专业、目标地区、分数、难度推荐院校
- 报录比、历年分数线、招生人数智能查询
- 院校对比、上岸概率评估、风险提示

### 3. 公共课智能辅导（政治 / 英语 / 数学）
- 知识点精讲：数字人口语化讲解，易懂好记
- 考点梳理：高频考点、易错点、必背清单
- 英语：单词速记、长难句解析、作文模板批改
- 政治：大纲解读、押题带背、时政总结
- 数学：公式推导、题型归类、解题思路引导

### 4. 专业课学习支持
- 按学科构建知识体系（计算机 / 教育学 / 经管 / 医学等）
- 参考书解读、重难点拆解
- 真题思路讲解、考点预测

### 5. AI 答疑与解题
- 自然语言提问：不会的题直接问
- 拍照搜题：提供思路、步骤、方法，不直接给答案
- 多轮追问：不懂可继续讲解

### 6. 刷题、错题本、专项训练
- 自动生成错题本，按知识点分类
- 同类题推送，强化薄弱点
- 每日一练、周测、专项突破

### 7. 个性化学习规划
- 根据备考时间、基础、目标自动生成日 / 周 / 月计划
- 学习进度跟踪，动态调整计划
- 任务完成提醒、超时预警

### 8. 模考与估分系统
- 全真模拟试卷，自动批改、自动评分
- 排名、薄弱科目分析
- 考前估分、冲刺策略建议

### 9. 复试 AI 训练
- 复试常见问题模拟问答
- 英语口语对练、自我介绍批改
- 面试礼仪、流程讲解、压力测试

### 10. 学习数据仪表盘
- 学习时长、专注度、正确率、进步曲线
- 薄弱知识点雷达图
- 每周备考报告与提升建议

### 11. 资讯与政策提醒
- 考研报名、现场确认、打印准考证、初试时间提醒
- 政策变化、院校通知实时推送