-- ============================================================
-- 组员4 · 复试 AI 训练 模块 SQL
-- 文件: docs/sql/modules/04-interview.sql
-- 分支: codex/interview
-- 数据库: MySQL 8.0 以上
-- 使用说明: 在已有的 kaoyan 库中直接执行本文件即可
--           本文件独立于总 SQL 文件 docs/sql/kaoyan.sql
-- ============================================================

-- 先关闭外键检查：本脚本可重复执行，DROP 顺序不会因外键依赖而报错
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. 面试题库表 (核心: 题目来源, AI 不负责出题)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS interview_question_bank;
CREATE TABLE interview_question_bank (
    id            INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    major         VARCHAR(50)  NOT NULL COMMENT '目标专业: 计算机/软件工程/电子信息/管理学/教育学/其他',
    category      VARCHAR(30)  NOT NULL COMMENT '训练类型: comprehensive(综合)/professional(专业)/pressure(压力)/english(英语)',
    difficulty    VARCHAR(20)  NOT NULL DEFAULT 'medium' COMMENT '难度: easy/medium/hard',
    question      TEXT         NOT NULL COMMENT '面试题正文',
    key_points    TEXT                  COMMENT '考察要点(逗号分隔,用于关键词评分兜底)',
    reference     TEXT                  COMMENT '参考答案要点(JSON或文本,供规则化打分用)',
    is_pressure   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否压力面试题(1=是,0=否)',
    is_followup   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否追问模板(1=是,0=否,追问题不单独组卷,由系统动态选)',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用(1=启用,0=禁用)',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_major_category (major, category),
    INDEX idx_difficulty (difficulty),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题库';


-- ------------------------------------------------------------
-- 2. 复试会话表 (一次模拟面试 = 一个 session)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS interview_session;
CREATE TABLE interview_session (
    id            INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id       INT          NOT NULL COMMENT '用户ID',
    major         VARCHAR(50)  NOT NULL COMMENT '训练目标专业',
    training_type VARCHAR(30)  NOT NULL COMMENT '训练类型',
    difficulty    VARCHAR(20)  NOT NULL DEFAULT 'medium' COMMENT '难度',
    pressure_mode TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否开启压力面试(1=是,0=否)',
    total_rounds  INT          NOT NULL DEFAULT 0 COMMENT '计划轮次(由题库抽题数量决定)',
    current_round INT          NOT NULL DEFAULT 0 COMMENT '已完成轮次',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ongoing' COMMENT '会话状态: ongoing/finished/abandoned',
    score         DOUBLE                 COMMENT '最终总分(0-100),结束时由报告回填',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    finish_time   DATETIME              COMMENT '结束时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_major (major)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话';


-- ------------------------------------------------------------
-- 3. 面试轮次表 (每一轮 = 一道题库题 + 学生回答 + AI 点评)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS interview_turn;
CREATE TABLE interview_turn (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    session_id      INT          NOT NULL COMMENT '所属会话ID',
    round_index     INT          NOT NULL COMMENT '第几轮(从1开始)',
    question_id     INT                   COMMENT '对应题库ID(首轮/正常题有值,动态追问为空)',
    question        TEXT         NOT NULL COMMENT '当轮展示给学生的问题正文(追问时是动态生成的)',
    is_followup     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否由 AI 动态生成的追问(1=是)',
    student_answer  TEXT                  COMMENT '学生回答(文本)',
    ai_feedback     TEXT                  COMMENT 'AI 对这一轮的点评(单题评语)',
    ai_expression   DOUBLE                COMMENT 'AI 评 - 表达能力(0-100)',
    ai_professional DOUBLE                COMMENT 'AI 评 - 专业知识(0-100)',
    ai_logic        DOUBLE                COMMENT 'AI 评 - 逻辑思维(0-100)',
    ai_adaptability DOUBLE                COMMENT 'AI 评 - 应变能力(0-100)',
    rule_based      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '本次评分是否为规则化兜底(1=是,无AI Key时)',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '学生回答时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_question (question_id),
    UNIQUE INDEX uq_session_round (session_id, round_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试轮次(问答)';


-- ------------------------------------------------------------
-- 4. 面试最终报告表 (五维评分 + 综合评语)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS interview_report;
CREATE TABLE interview_report (
    id                  INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    session_id          INT          NOT NULL COMMENT '所属会话ID',
    expression_score    DOUBLE       NOT NULL DEFAULT 0 COMMENT '表达能力(0-100)',
    professional_score  DOUBLE       NOT NULL DEFAULT 0 COMMENT '专业知识(0-100)',
    logic_score         DOUBLE       NOT NULL DEFAULT 0 COMMENT '逻辑思维(0-100)',
    english_score       DOUBLE       NOT NULL DEFAULT 0 COMMENT '英语能力(0-100,未练英语时与表达分一致)',
    adaptability_score  DOUBLE       NOT NULL DEFAULT 0 COMMENT '应变能力(0-100)',
    total_score         DOUBLE       NOT NULL DEFAULT 0 COMMENT '加权总分(0-100)',
    summary             TEXT                  COMMENT 'AI 综合总评(一段话总结)',
    main_issues         TEXT                  COMMENT '主要问题(JSON数组或换行文本)',
    suggestions         TEXT                  COMMENT '改进建议(JSON数组或换行文本)',
    report_read         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '用户是否已阅读(1=已读,0=未读,定时任务扫描用)',
    rule_based          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '本次报告是否为规则化兜底',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_report_read (report_read),
    CONSTRAINT fk_report_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试综合报告';


-- ------------------------------------------------------------
-- 5. 旧表增强: interview_record 补字段
--    (保留旧表做历史兼容, 新业务用上面 4 张表)
-- ------------------------------------------------------------
-- 执行前先判断列是否存在, 避免重复执行报错
SET @dbname = DATABASE();
SET @tablename = 'interview_record';

-- 补: 新会话ID关联
SET @columnname = 'session_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE interview_record ADD COLUMN session_id INT COMMENT "关联的 interview_session.id" AFTER id'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 补: 轮次
SET @columnname = 'round_index';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE interview_record ADD COLUMN round_index INT DEFAULT 0 COMMENT "第几轮" AFTER session_id'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;


-- ============================================================
-- 初始化题库数据 (每个专业 ~10 题, 确保演示时不会空)
-- 删表时已经被 DROP, 这里直接 INSERT
-- ============================================================

-- ---- 计算机 专业题 (comprehensive / professional) ----
INSERT INTO interview_question_bank (major, category, difficulty, question, key_points, reference, is_pressure, is_followup) VALUES
('计算机', 'professional', 'easy',   '请简单介绍一下你熟悉的排序算法,时间复杂度分别是多少?',
 '时间复杂度,空间复杂度,稳定性',
 '冒泡/插入 O(n²),选择 O(n²),快排 O(nlogn),归并 O(nlogn),堆排 O(nlogn);稳定性:冒泡插入归并稳定,快排堆排不稳定',
 0, 0),

('计算机', 'professional', 'medium', 'HashMap 在 JDK 8 之后的底层实现有什么变化?为什么要这样设计?',
 '数组+链表+红黑树,阈值8,树退化阈值6,O(1)查找',
 'JDK8 把 JDK7 的 数组+链表 改成 数组+链表+红黑树;当链表长度>8 时转红黑树(<6 再退化);解决 hash 冲突严重时链表 O(n) 退化问题',
 0, 0),

('计算机', 'professional', 'medium', 'Spring 事务的传播行为有哪些?默认是哪个?你在什么场景下用过 REQUIRES_NEW?',
 'PROPAGATION_*, REQUIRED默认, REQUIRES_NEW, NOT_SUPPORTED',
 'REQUIRED(默认):有则加入,无则新建;REQUIRES_NEW:总是新建;MANDATORY:必须有;NESTED:嵌套;NOT_SUPPORTED:挂起;NEVER:禁止;SUPPORTS:有就用没有就非事务;REQUIRES_NEW 用于主事务回滚时某些子操作仍需提交(如记录日志)',
 0, 0),

('计算机', 'professional', 'hard',   '请解释一下 Kafka 的高可用机制。为什么用 KRaft 取代 Zookeeper?',
 'Leader-Follower,ISR,AR,KRaft Raft协议,去Zookeeper',
 '每个 Topic 分多 Partition,每个 Partition 多 Replica,一 Leader 多 Follower;ISR=In-Sync Replicas;生产端acks=all等 ISR 确认;KRaft 用 Raft 协议取代 ZK 的一致性管理,减少组件依赖,性能更好',
 0, 0),

('计算机', 'comprehensive', 'easy',   '你为什么选择计算机专业研究生?',
 '读研动机,职业规划,技术热情',
 '从本科兴趣、职业目标、想深入的方向三个角度阐述',
 0, 0),

('计算机', 'comprehensive', 'medium', '读研期间你希望重点研究哪个方向?打算怎么规划这两三年?',
 '方向明确,规划具体,可落地',
 '先确定方向(如分布式/AI/系统),第一年基础课+读论文,第二年做项目+写小论文,第三年大论文+找工作',
 0, 0),

-- ---- 软件工程 专业题 ----
('软件工程', 'professional', 'easy',   '软件生命周期包含哪些阶段?你更倾向哪种模型?',
 '需求分析,设计,编码,测试,维护;敏捷 vs 瀑布',
 '需求→设计→编码→测试→部署→维护;小项目/需求稳定用瀑布,变化多/快速交付用敏捷(Scrum/Sprint)',
 0, 0),

('软件工程', 'professional', 'medium', '你做过的项目里,数据库表是怎么设计的?遇到过哪些坑?',
 '范式,索引,外键,慢查询,反范式',
 '先 3NF,查询频繁时反范式加冗余;合理建索引(联合索引最左原则);避免 SELECT *;大表考虑分库分表或冷热分离',
 0, 0),

('软件工程', 'professional', 'hard',   '请讲一下微服务拆分的原则。你认为单体 vs 微服务的权衡点有哪些?',
 '单一职责,限界上下文,服务自治;复杂度 vs 弹性',
 '按业务域拆分,每个服务独立部署;权衡:微服务运维成本高(注册中心/配置中心/链路追踪),但弹性好、技术异构、独立扩展;单体开发简单但扩展困难,适合业务初期',
 0, 0),

('软件工程', 'comprehensive', 'medium', '如果导师让你做一个"考研 AI 助手"项目,你觉得难点会在哪里?',
 '需求理解,技术选型,AI模型,可演示',
 '需求拆解(导学/刷题/复试/择校),前端 Vite+Vue,后端 Spring Boot,AI 调用(无Key时规则兜底),关键是"没AI Key也能跑完整演示"',
 0, 0),

-- ---- 压力面试题 (category=pressure) ----
('计算机', 'pressure', 'medium', '刚才那道 HashMap 题你好像说得不太清楚,再准确讲一遍数组、链表、红黑树三者的关系和转换阈值。',
 '追问细节,阈值8和6,hash分布不均场景',
 'JDK8:链表长度>8 且数组长度>=64 时链表转红黑树;红黑树节点数<6 退化回链表;数组长度<64 时优先扩容而非转树',
 1, 0),

('软件工程', 'pressure', 'hard',   '你说你了解微服务,但我看你简历上只有单体项目。你怎么证明你真的懂微服务?给我讲讲你自己看过什么源码或做过什么 Demo。',
 '压力情境,真实深度,源码或Demo',
 '承认没生产经验但可谈学习路径(如 Spring Cloud Alibaba 全家桶 Demo、Nacos/Eureka 源码阅读、Docker Compose 部署多服务、Sentinel 限流实验)',
 1, 0),

-- ---- 追问模板 (category=pressure, is_followup=1) ----
('计算机',   'pressure', 'medium', '你刚才提到了 {keyword},能具体展开说一下吗?', '{keyword}', '', 1, 1),
('软件工程', 'pressure', 'medium', '你说 {keyword},但有没有考虑过如果规模变大了会怎么样?', '{keyword},扩展性', '', 1, 1),
('计算机',   'pressure', 'hard',   '换个角度——如果 {keyword} 出问题导致线上故障,你会怎么排查?', '{keyword},故障排查', '', 1, 1),

-- ---- 英语题 (category=english) ----
('其他', 'english', 'easy', 'Please introduce yourself briefly in English (about 1-2 minutes).',
 '自我介绍,结构清晰,词汇丰富,语法正确',
 '建议结构:问候→教育背景→研究方向→项目经验→为什么选这个学校',
 0, 0),

('其他', 'english', 'medium', 'Describe a technical project you are most proud of. What was your role and what did you learn?',
 'STAR原则(Situation,Task,Action,Result),技术细节,反思',
 '明确场景、任务、自己的具体行动、量化结果、学到了什么',
 0, 0),

('其他', 'english', 'medium', 'Why do you want to pursue a master\'s degree? Why this university specifically?',
 '动机,具体,学校特色,匹配度',
 '从个人兴趣、职业规划、学校/导师/实验室特色三个角度说明匹配度',
 0, 0);


-- ============================================================
-- 额外: 复试会话表的外键约束说明
--   interview_report.session_id 已加外键 ON DELETE CASCADE
--   interview_turn.session_id 建议应用层保证一致性 (MyBatis-Plus 用 wrapper 删除)
--   不硬加外键避免批量删除时锁等待
-- ============================================================

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;
