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
├── backend                # 后端 SpringBoot 项目
│   ├── src
│   │   ├── main
│   │   │   └── java/com/demo/scaffoldbackend  # 后端源码
│   │   │       ├── config       # 配置类（Shiro 等）
│   │   │       ├── controller   # 接口控制器
│   │   │       ├── entity       # 数据库实体
│   │   │       ├── mapper       # MyBatis 映射
│   │   │       ├── service      # 业务逻辑
│   │   │       ├── common       # 通用工具类
│   │   │       └── dto          # 数据传输对象
│   │   │   └── resources        # 配置文件、日志配置
│   │   └── test                 # 单元测试
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── frontend               # Vue 前端项目
├── docs                   # 项目文档
│   ├── 系统功能分析说明书.md
│   ├── 系统接口设计文档.md
│   ├── 系统详细设计.md
│   ├── 系统安装部署文档.md
│   ├── 系统操作说明书.md
│   └── 数据库脚本.sql
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

1. 所有代码提交前，本地测试可正常运行
2. 业务代码修改通过 Git 提交到仓库
3. `settings.xml`为本机 Maven 环境配置，禁止提交到仓库
4. 文档统一放在 docs 文件夹


## 功能清单

1. 数字人学习陪伴：虚拟助教形象、文字 + 语音对话交互、学习状态提醒
2. 课程知识智能导学：按课程章节生成学习路径，拆解重难点考点
3. AI 智能答疑：针对学科问题进行解答辅导
