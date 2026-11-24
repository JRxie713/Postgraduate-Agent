-- ============================================================
-- kaoyan 数据库完整导出（建表 + 数据）
-- 数据库：kaoyan    字符集：utf8mb4
-- ============================================================
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `consultation`;
CREATE TABLE `consultation` (
                                `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `user_id` int(11) NOT NULL COMMENT '用户ID',
                                `session_id` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '一轮对话唯一 ID',
                                `question` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户提问',
                                `answer` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回复内容',
                                `consult_source` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '答疑来源',
                                `ref_id` int(11) DEFAULT NULL COMMENT '引用资源id',
                                `round` int(11) DEFAULT NULL COMMENT '追问轮次',
                                `create_time` datetime NOT NULL COMMENT '记录创建时间',
                                `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                PRIMARY KEY (`id`),
                                KEY `FK_consultation_user_id` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表9答疑会话表';

DROP TABLE IF EXISTS `exam_paper`;
CREATE TABLE `exam_paper` (
                              `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                              `paper_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '试卷名',
                              `exam_subject` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考试科目',
                              `total_score` int(11) NOT NULL COMMENT '试卷总分',
                              `paper_type` tinyint(4) NOT NULL COMMENT '试卷类型',
                              `create_time` datetime NOT NULL COMMENT '记录创建时间',
                              `update_time` datetime NOT NULL COMMENT '记录修改时间',
                              PRIMARY KEY (`id`),
                              KEY `IDX_exam_paper_exam_suect8594` (`exam_subject`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷模板表';

DROP TABLE IF EXISTS `exam_question`;
CREATE TABLE `exam_question` (
                                 `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `paper_id` int(11) NOT NULL COMMENT '试卷ID',
                                 `content_id` int(11) NOT NULL COMMENT '题目内容ID',
                                 `exam_subject` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考试科目',
                                 `chapter` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属章节',
                                 `knowledge_point` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '知识点',
                                 `q_type` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '题型',
                                 `difficulty` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '难度等级',
                                 `score` float DEFAULT NULL COMMENT '分值',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                 `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                 PRIMARY KEY (`id`),
                                 KEY `IDX_exam_quion_paper_id3155` (`paper_id`),
                                 KEY `FK_exam_quion_content_idCF2D` (`content_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表6.2题目基础信息表';

DROP TABLE IF EXISTS `interview_record`;
CREATE TABLE `interview_record` (
                                    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` int(11) NOT NULL COMMENT '用户ID',
                                    `major` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '复试专业',
                                    `question` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '面试问题',
                                    `answer_audio` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '音频地址',
                                    `score` float DEFAULT NULL COMMENT '面试得分',
                                    `pressure_test` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '压力测试评价',
                                    `create_time` datetime NOT NULL COMMENT '记录创建时间',
                                    `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                    PRIMARY KEY (`id`),
                                    KEY `FK_interviord_user_idC7AE` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表10复试训练记录表';

DROP TABLE IF EXISTS `learning_log`;
CREATE TABLE `learning_log` (
                                `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `user_id` int(11) NOT NULL COMMENT '用户ID',
                                `log_date` datetime DEFAULT NULL COMMENT '记录时间',
                                `duration` int(11) DEFAULT NULL COMMENT '学习时长',
                                `focus` float DEFAULT NULL COMMENT '专注度',
                                `emotion_tag` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '情绪标签',
                                `check_in` bit(1) DEFAULT NULL COMMENT '是否打卡',
                                `create_time` datetime NOT NULL COMMENT '记录创建时间',
                                `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                PRIMARY KEY (`id`),
                                KEY `FK_learning_log_user_id` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表11学习日志表';

DROP TABLE IF EXISTS `mock_exam`;
CREATE TABLE `mock_exam` (
                             `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                             `user_id` int(11) NOT NULL COMMENT '用户ID',
                             `paper_id` int(11) NOT NULL COMMENT '试卷ID',
                             `start_time` datetime DEFAULT NULL COMMENT '开考时间',
                             `submit_time` datetime DEFAULT NULL COMMENT '交卷时间',
                             `score` float DEFAULT NULL COMMENT '本次得分',
                             `exam_rank` int(11) DEFAULT NULL COMMENT '排名',
                             `estimate_range` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '估分区间',
                             `exam_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考试状态',
                             `create_time` datetime NOT NULL COMMENT '记录创建时间',
                             `update_time` datetime NOT NULL COMMENT '记录修改时间',
                             PRIMARY KEY (`id`),
                             KEY `FK_mock_exam_user_id` (`user_id`),
                             KEY `FK_mock_exam_paper_id` (`paper_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表8模考记录表';

DROP TABLE IF EXISTS `plan_task`;
CREATE TABLE `plan_task` (
                             `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                             `plan_id` int(11) NOT NULL COMMENT '计划ID',
                             `task_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
                             `task_subject` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考试科目',
                             `chapter` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '章节',
                             `due_date` datetime DEFAULT NULL COMMENT '截止时间',
                             `task_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务状态',
                             `is_timeout` bit(1) DEFAULT NULL COMMENT '是否超时',
                             `create_time` datetime NOT NULL COMMENT '记录创建时间',
                             `update_time` datetime NOT NULL COMMENT '记录修改时间',
                             PRIMARY KEY (`id`),
                             KEY `FK_plan_task_plan_id` (`plan_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表5计划任务表';

INSERT INTO `plan_task` (`id`, `plan_id`, `task_name`, `task_subject`, `chapter`, `due_date`, `task_status`, `is_timeout`, `create_time`, `update_time`) VALUES (1,1,'高数第三章习题','数学',NULL,'2026-11-15 00:00:00','DONE',_binary '\0','2026-11-14 14:46:26','2026-11-17 16:16:53');

INSERT INTO `plan_task` (`id`, `plan_id`, `task_name`, `task_subject`, `chapter`, `due_date`, `task_status`, `is_timeout`, `create_time`, `update_time`) VALUES (2,1,'英语真题阅读 2 篇','英语',NULL,'2026-11-16 00:00:00','DONE',_binary '\0','2026-11-14 14:46:49','2026-11-14 14:46:49');

INSERT INTO `plan_task` (`id`, `plan_id`, `task_name`, `task_subject`, `chapter`, `due_date`, `task_status`, `is_timeout`, `create_time`, `update_time`) VALUES (3,1,'政治马原第五章背诵','政治',NULL,'2026-11-17 00:00:00','TODO',_binary '\0','2026-11-14 14:46:49','2026-11-14 14:46:49');

INSERT INTO `plan_task` (`id`, `plan_id`, `task_name`, `task_subject`, `chapter`, `due_date`, `task_status`, `is_timeout`, `create_time`, `update_time`) VALUES (4,2,'英语6级考试','英语',NULL,'2026-12-12 00:00:00','TODO',_binary '\0','2026-11-14 14:54:48','2026-11-14 14:54:48');

INSERT INTO `plan_task` (`id`, `plan_id`, `task_name`, `task_subject`, `chapter`, `due_date`, `task_status`, `is_timeout`, `create_time`, `update_time`) VALUES (5,3,'英语6级','英语',NULL,'2026-12-12 00:00:00','TODO',_binary '\0','2026-11-15 15:28:27','2026-11-15 15:28:27');

DROP TABLE IF EXISTS `policy_notice`;
CREATE TABLE `policy_notice` (
                                 `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
                                 `notice_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知类型',
                                 `notice_source` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '信息来源',
                                 `effect_time` datetime DEFAULT NULL COMMENT '生效时间',
                                 `sub_tags` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签',
                                 `create_time` datetime NOT NULL COMMENT '记录创建时间',
                                 `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                 PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表12政策通知表';

INSERT INTO `policy_notice` (`id`, `title`, `notice_type`, `notice_source`, `effect_time`, `sub_tags`, `create_time`, `update_time`) VALUES (1,'2027 年全国硕士研究生招生考试报名时间公布','招生','中国研究生招生信息网','2026-09-24 09:00:00','预报名|正式报名|网上确认','2026-11-14 14:42:16','2026-11-14 14:42:16');

INSERT INTO `policy_notice` (`id`, `title`, `notice_type`, `notice_source`, `effect_time`, `sub_tags`, `create_time`, `update_time`) VALUES (2,'多所院校发布 2027 年硕士研究生招生简章','院校','各院校研究生院','2026-09-18 10:00:00','招生简章|专业目录|参考书目','2026-11-14 14:42:16','2026-11-14 14:42:16');

INSERT INTO `policy_notice` (`id`, `title`, `notice_type`, `notice_source`, `effect_time`, `sub_tags`, `create_time`, `update_time`) VALUES (3,'教育部：进一步加强研究生招生考试考务管理','政策','教育部','2026-09-10 15:30:00','考务管理|考场规则','2026-11-14 14:42:16','2026-11-14 14:42:16');

INSERT INTO `policy_notice` (`id`, `title`, `notice_type`, `notice_source`, `effect_time`, `sub_tags`, `create_time`, `update_time`) VALUES (4,'英语一与英语二题型结构调整说明','考试','教育部教育考试院','2026-08-28 11:00:00','英语|题型|备考建议','2026-11-14 14:42:16','2026-11-14 14:42:16');

INSERT INTO `policy_notice` (`id`, `title`, `notice_type`, `notice_source`, `effect_time`, `sub_tags`, `create_time`, `update_time`) VALUES (5,'考研数学大纲变化解读：高数部分新增考点','考试','教育部教育考试院','2026-08-20 14:00:00','数学|大纲|新增考点','2026-11-14 14:42:16','2026-11-14 14:42:16');

INSERT INTO `policy_notice` (`id`, `title`, `notice_type`, `notice_source`, `effect_time`, `sub_tags`, `create_time`, `update_time`) VALUES (6,'推免服务系统开放时间及操作流程','招生','中国研究生招生信息网','2026-09-28 09:00:00','推免|志愿填报','2026-11-14 14:42:16','2026-11-14 14:42:16');

INSERT INTO `policy_notice` (`id`, `title`, `notice_type`, `notice_source`, `effect_time`, `sub_tags`, `create_time`, `update_time`) VALUES (7,'部分院校调整初试科目与自命题范围','院校','院校研究生招生办','2026-09-05 16:00:00','初试科目|自命题|改考提醒','2026-11-14 14:42:16','2026-11-14 14:42:16');

INSERT INTO `policy_notice` (`id`, `title`, `notice_type`, `notice_source`, `effect_time`, `sub_tags`, `create_time`, `update_time`) VALUES (8,'考研报名常见问题与资格审核说明','政策','省级教育招生考试院','2026-09-30 09:30:00','报名|资格审核|常见问题','2026-11-14 14:42:16','2026-11-14 14:42:16');

DROP TABLE IF EXISTS `question_content`;
CREATE TABLE `question_content` (
                                    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `question_text` varchar(4000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题干',
                                    `option_info` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选项信息',
                                    `ref_answer` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参考答案',
                                    `analysis` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '解析思路',
                                    `create_time` datetime NOT NULL COMMENT '记录创建时间',
                                    `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                    PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表6.1题目内容表';

DROP TABLE IF EXISTS `school_major`;
CREATE TABLE `school_major` (
                                `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `school` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '院校名称',
                                `major` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业名称',
                                `enroll_num` int(11) DEFAULT NULL COMMENT '招生人数',
                                `ratio` float DEFAULT NULL COMMENT '报录比',
                                `score_line` float DEFAULT NULL COMMENT '分数线',
                                `exam_subjects` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考试科目',
                                `ref_books` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参考书目',
                                `create_time` datetime NOT NULL COMMENT '记录创建时间',
                                `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表2院校专业表';

DROP TABLE IF EXISTS `study_plan`;
CREATE TABLE `study_plan` (
                              `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                              `user_id` int(11) NOT NULL COMMENT '用户ID',
                              `plan_cycle` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计划周期',
                              `start_date` datetime DEFAULT NULL COMMENT '计划开始时间',
                              `end_date` datetime DEFAULT NULL COMMENT '计划结束时间',
                              `plan_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计划状态',
                              `completion_rate` float DEFAULT NULL COMMENT '完成率',
                              `create_time` datetime NOT NULL COMMENT '记录创建时间',
                              `update_time` datetime NOT NULL COMMENT '记录修改时间',
                              PRIMARY KEY (`id`),
                              KEY `FK_study_plan_user_id` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表4学习计划表';

INSERT INTO `study_plan` (`id`, `user_id`, `plan_cycle`, `start_date`, `end_date`, `plan_status`, `completion_rate`, `create_time`, `update_time`) VALUES (1,1598697474,'本周','2026-11-10 00:00:00','2026-11-16 00:00:00','ACTIVE',66.7,'2026-11-14 14:46:26','2026-11-17 16:16:53');

INSERT INTO `study_plan` (`id`, `user_id`, `plan_cycle`, `start_date`, `end_date`, `plan_status`, `completion_rate`, `create_time`, `update_time`) VALUES (2,1829453827,'本周',NULL,NULL,'ACTIVE',0,'2026-11-14 14:53:44','2026-11-14 14:54:48');

INSERT INTO `study_plan` (`id`, `user_id`, `plan_cycle`, `start_date`, `end_date`, `plan_status`, `completion_rate`, `create_time`, `update_time`) VALUES (3,1829453836,'本周','2026-11-15 00:00:00','2026-12-12 00:00:00','ACTIVE',0,'2026-11-15 15:27:56','2026-11-15 15:28:27');

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                        `user_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号',
                        `pwd` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密密码',
                        `real_name` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户姓名',
                        `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                        `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                        `user_role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN',
                        `student_no` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生学号',
                        `target_major` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '意向报考专业',
                        `supervise_mode` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '督导模式',
                        `user_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '账号状态',
                        `create_time` datetime NOT NULL COMMENT '记录创建时间',
                        `update_time` datetime NOT NULL COMMENT '记录修改时间',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `IDU_user_name` (`user_name`),
                        UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表1用户表 存储平台账号基础信息，user_name唯一';

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (1598697474,'demo_student','$2a$10$SXOEk.e7/qn3TofarxY6CeP0V/GW1WBEoTG7PJmnwmg0FdAquB7b.',NULL,'',NULL,'STUDENT',NULL,NULL,NULL,'NORMAL','2026-11-14 13:51:06','2026-11-14 13:51:06');

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (-1039527935,'13700137001','$2a$10$9dcSKpH.iNrrpI6n1xgjCePdzf6z3rbtbCpp4/ODWG8rnM40bku8.',NULL,'13700137001',NULL,'STUDENT',NULL,NULL,NULL,'NORMAL','2026-11-14 14:07:42','2026-11-14 14:07:42');

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (701108225,'13800138000','$2a$10$CdReesZNwBDk6C0iJ/qi0uQQLuPU3ltDv9hJU.6OwO0dShkS3SmGa',NULL,'13800138000',NULL,'STUDENT',NULL,NULL,NULL,'NORMAL','2026-11-14 14:05:52','2026-11-14 14:05:52');

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (-951414782,'zhangsan','$2a$10$K/bSbBjVP82.RT7HdS8.MONmBtdnCcZUXhe..njICyLiVCxcp9D3m',NULL,'',NULL,'STUDENT',NULL,NULL,NULL,'NORMAL','2026-11-14 14:20:30','2026-11-14 14:20:30');

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (71970817,'stu7008','$2a$10$QQlhtAkz2iHK8y94P47IN.59aOJzvtpSZKLSMQPeaXZgsricAU48i',NULL,'13700007008',NULL,'STUDENT',NULL,NULL,NULL,'NORMAL','2026-11-14 14:36:28','2026-11-14 14:36:28');

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (1829453836,'lily','$2a$10$D6e1yNgPdVylsz6BdVvOuuflpm1zmWT2wOxshP8zmJ3rB1T0NBXLm',NULL,NULL,'xxxjr713@qq.com','STUDENT',NULL,NULL,NULL,'NORMAL','2026-11-15 15:25:41','2026-11-15 15:25:41');

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (1829453837,'fcy','$2a$10$qlyNmvQJLbUtKYQpmYqnPOYst4whBqfa/inYekZ2UawXRn.PEuEMi',NULL,NULL,NULL,'STUDENT',NULL,NULL,NULL,'DISABLED','2026-11-15 15:55:19','2026-11-17 16:26:57');

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (1829453832,'admin','$2a$10$MLdg4jRDCkAd.aGdpZzIzeej2wmJhtztOiHzIbGx31n0kGiKhzgyK','SystemAdmin',NULL,NULL,'ADMIN',NULL,NULL,NULL,'NORMAL','2026-11-15 14:27:11','2026-11-15 14:27:11');

INSERT INTO `user` (`id`, `user_name`, `pwd`, `real_name`, `phone`, `email`, `user_role`, `student_no`, `target_major`, `supervise_mode`, `user_status`, `create_time`, `update_time`) VALUES (1829453833,'teacher','$2a$10$Zne2/LMd2m9J9bIOo4juH.FBB9WUNUwlNZwj.XiTRIQFcX3euvfsy','DemoTeacher',NULL,NULL,'TEACHER',NULL,NULL,NULL,'NORMAL','2026-11-15 14:27:12','2026-11-15 14:27:12');

DROP TABLE IF EXISTS `user_answer_record`;
CREATE TABLE `user_answer_record` (
                                      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `mock_exam_id` int(11) NOT NULL COMMENT '模考ID',
                                      `user_id` int(11) NOT NULL COMMENT '用户ID',
                                      `paper_id` int(11) NOT NULL COMMENT '试卷ID',
                                      `question_id` int(11) NOT NULL COMMENT '题目ID',
                                      `user_answer` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户答案',
                                      `is_correct` bit(1) DEFAULT NULL COMMENT '是否正确',
                                      `score_obtain` float DEFAULT NULL COMMENT '本题得分',
                                      `answer_time` datetime NOT NULL COMMENT '答题时间',
                                      `create_time` datetime NOT NULL COMMENT '记录创建时间',
                                      `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                      PRIMARY KEY (`id`),
                                      KEY `IDX_user_anord_user_id55C4` (`user_id`),
                                      KEY `IDX_user_anord_paper_id6CDE` (`paper_id`),
                                      KEY `FK_user_anord_mock_ex_id4D92` (`mock_exam_id`),
                                      KEY `FK_user_anord_questio_id5950` (`question_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户答题分区表';

DROP TABLE IF EXISTS `user_profile`;
CREATE TABLE `user_profile` (
                                `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `user_id` int(11) NOT NULL COMMENT '用户ID',
                                `undergrad_major` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '本科专业',
                                `target_school` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标院校',
                                `target_major` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标专业',
                                `target_region` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标地区',
                                `exam_year` int(11) DEFAULT NULL COMMENT '考研年份',
                                `base_score` float DEFAULT NULL COMMENT '基础评测分',
                                `predict_score` float DEFAULT NULL COMMENT '预估分数',
                                `difficulty_pref` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '择校偏好',
                                `weak_points` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '薄弱点',
                                `exam_stage` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备考阶段',
                                `emotion_state` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '情绪状态',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                PRIMARY KEY (`id`),
                                KEY `IDX_user_profile_user_id` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表3备考画像表';

DROP TABLE IF EXISTS `wrong_question`;
CREATE TABLE `wrong_question` (
                                  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `user_id` int(11) NOT NULL COMMENT '用户ID',
                                  `question_id` int(11) NOT NULL COMMENT '题目ID',
                                  `wrong_reason` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误原因',
                                  `mastery_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '掌握程度',
                                  `add_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入错题时间',
                                  `review_count` int(11) DEFAULT NULL COMMENT 'DEFAULT 0',
                                  `create_time` datetime NOT NULL COMMENT '记录创建时间',
                                  `update_time` datetime NOT NULL COMMENT '记录修改时间',
                                  PRIMARY KEY (`id`),
                                  KEY `FK_wrong_qion_user_idD640` (`user_id`),
                                  KEY `FK_wrong_qion_questio_id30FD` (`question_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表7错题本表';

DROP TABLE IF EXISTS `zt_group`;
CREATE TABLE `zt_group` (
                            `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                            `name` varchar(64) NOT NULL COMMENT '权限组名称',
                            `description` varchar(255) DEFAULT NULL COMMENT '说明',
                            `status` varchar(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DISABLED',
                            `create_time` datetime DEFAULT NULL,
                            `update_time` datetime DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_group_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限组';

INSERT INTO `zt_group` (`id`, `name`, `description`, `status`, `create_time`, `update_time`) VALUES (1,'学生组','只读：查看政策资讯、使用学习功能','NORMAL','2026-11-17 12:06:19','2026-11-17 12:38:01');

INSERT INTO `zt_group` (`id`, `name`, `description`, `status`, `create_time`, `update_time`) VALUES (2,'教师组','可发布、修改政策资讯','NORMAL','2026-11-17 12:06:19','2026-11-17 12:30:52');

INSERT INTO `zt_group` (`id`, `name`, `description`, `status`, `create_time`, `update_time`) VALUES (3,'管理员组','拥有全部权限（ADMIN 角色另有超级管理员直通）','NORMAL','2026-11-17 12:06:19','2026-11-17 12:30:58');

DROP TABLE IF EXISTS `zt_grouppriv`;
CREATE TABLE `zt_grouppriv` (
                                `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `group_id` int(11) NOT NULL COMMENT '权限组ID',
                                `module` varchar(32) NOT NULL COMMENT '模块，如 user',
                                `method` varchar(32) NOT NULL COMMENT '方法，如 view',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_group_priv` (`group_id`,`module`,`method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组权限（模块-方法）';

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (35,1,'policy','view');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (19,2,'policy','create');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (20,2,'policy','update');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (21,2,'policy','view');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (22,3,'policy','create');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (23,3,'policy','delete');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (24,3,'policy','update');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (25,3,'policy','view');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (26,3,'rbac','manage');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (27,3,'rbac','view');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (28,3,'user','create');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (29,3,'user','delete');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (30,3,'user','unlock');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (31,3,'user','update');

INSERT INTO `zt_grouppriv` (`id`, `group_id`, `module`, `method`) VALUES (32,3,'user','view');

DROP TABLE IF EXISTS `zt_usergroup`;
CREATE TABLE `zt_usergroup` (
                                `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `user_id` int(11) NOT NULL COMMENT '用户ID',
                                `group_id` int(11) NOT NULL COMMENT '权限组ID',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_user_group` (`user_id`,`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-权限组';

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (2,-1039527935,1);

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (4,-951414782,1);

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (5,71970817,1);

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (3,701108225,1);

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (18,1598697474,1);

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (8,1829453832,3);

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (22,1829453833,2);

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (21,1829453836,1);

INSERT INTO `zt_usergroup` (`id`, `user_id`, `group_id`) VALUES (20,1829453837,1);



-- ============================================================
-- 实时聊天模块建表脚本（MySQL 8.0+）
-- 说明：会话包含私聊（PRIVATE）与群聊/自习室（GROUP），消息支持文本、图片、文件
-- ============================================================

CREATE TABLE IF NOT EXISTS `chat_session` (
                                              `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
                                              `type`            VARCHAR(16)  NOT NULL COMMENT '会话类型：PRIVATE 私聊 / GROUP 群聊（自习室）',
                                              `name`            VARCHAR(64)  DEFAULT NULL COMMENT '群聊名称；私聊为空',
                                              `pair_key`        VARCHAR(64)  DEFAULT NULL COMMENT '私聊唯一键：minUserId:maxUserId，群聊为空',
                                              `owner_id`        INT          DEFAULT NULL COMMENT '群主用户ID；私聊为空',
                                              `last_msg_id`     BIGINT       DEFAULT NULL COMMENT '最后一条消息ID',
                                              `last_msg_preview` VARCHAR(128) DEFAULT NULL COMMENT '最后一条消息摘要',
                                              `last_msg_time`   DATETIME     DEFAULT NULL COMMENT '最后一条消息时间',
                                              `member_count`    INT          NOT NULL DEFAULT 0 COMMENT '成员数量',
                                              `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                              PRIMARY KEY (`id`),
                                              UNIQUE KEY `uk_pair_key` (`pair_key`),
                                              KEY `idx_last_msg_time` (`last_msg_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '聊天会话表';

CREATE TABLE IF NOT EXISTS `chat_session_member` (
                                                     `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                     `session_id`       BIGINT      NOT NULL COMMENT '会话ID',
                                                     `user_id`          INT         NOT NULL COMMENT '用户ID',
                                                     `member_role`      VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色：OWNER / MEMBER',
                                                     `last_read_msg_id` BIGINT      NOT NULL DEFAULT 0 COMMENT '已读游标：该用户最后一条已读消息ID',
                                                     `unread_count`     INT         NOT NULL DEFAULT 0 COMMENT '未读消息数',
                                                     `join_time`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
                                                     PRIMARY KEY (`id`),
                                                     UNIQUE KEY `uk_session_user` (`session_id`, `user_id`),
                                                     KEY `idx_user` (`user_id`),
                                                     KEY `idx_session_read` (`session_id`, `last_read_msg_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会话成员表';

CREATE TABLE IF NOT EXISTS `chat_message` (
                                              `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                                              `session_id`    BIGINT       NOT NULL COMMENT '会话ID',
                                              `sender_id`     INT          NOT NULL COMMENT '发送者用户ID',
                                              `client_msg_id` VARCHAR(64)  NOT NULL COMMENT '客户端消息ID，用于发送幂等',
                                              `msg_type`      VARCHAR(16)  NOT NULL COMMENT '消息类型：TEXT / IMAGE / FILE',
                                              `content`       VARCHAR(4000) DEFAULT NULL COMMENT '文本内容；撤回后保留原文仅用于审计',
                                              `file_id`       BIGINT       DEFAULT NULL COMMENT '附件ID（chat_file.id）',
                                              `status`        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '消息状态：NORMAL / RECALLED',
                                              `recall_time`   DATETIME     DEFAULT NULL COMMENT '撤回时间',
                                              `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
                                              PRIMARY KEY (`id`),
                                              UNIQUE KEY `uk_client_msg` (`session_id`, `client_msg_id`),
                                              KEY `idx_session_id` (`session_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '聊天消息表';

CREATE TABLE IF NOT EXISTS `chat_file` (
                                           `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
                                           `session_id`    BIGINT       NOT NULL COMMENT '所属会话ID',
                                           `uploader_id`   INT          NOT NULL COMMENT '上传者用户ID',
                                           `file_name`     VARCHAR(255) NOT NULL COMMENT '原始文件名',
                                           `file_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
                                           `content_type`  VARCHAR(128) DEFAULT NULL COMMENT 'MIME 类型',
                                           `storage_key`   VARCHAR(512) NOT NULL COMMENT '对象存储 key（OSS 或本机相对路径）',
                                           `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_session` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '聊天附件表';
