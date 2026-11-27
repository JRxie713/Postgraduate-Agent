-- ============================================================
-- 02-practice.sql —— 组员 2：刷题训练与错题本
-- 说明：复用现有 question_content / exam_question / wrong_question，
--       新增 practice_record 记录日常练习行为，并补充演示题库数据。
-- 可重复执行：建表/索引均做了存在性判断，种子数据使用 INSERT IGNORE。
-- ============================================================
USE kaoyan;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 日常练习记录表（只记录练习行为，题目内容仍复用题库表）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `practice_record` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         INT          NOT NULL COMMENT '用户ID',
    `question_id`     INT          NOT NULL COMMENT '题目ID（exam_question.id）',
    `content_id`      INT          DEFAULT NULL COMMENT '题目内容ID（question_content.id）',
    `exam_subject`    VARCHAR(20)  DEFAULT NULL COMMENT '考试科目',
    `chapter`         VARCHAR(200) DEFAULT NULL COMMENT '所属章节',
    `knowledge_point` VARCHAR(100) DEFAULT NULL COMMENT '知识点',
    `q_type`          VARCHAR(20)  DEFAULT NULL COMMENT '题型',
    `difficulty`      VARCHAR(20)  DEFAULT NULL COMMENT '难度等级',
    `user_answer`     VARCHAR(400) DEFAULT NULL COMMENT '用户答案',
    `correct_answer`  VARCHAR(400) DEFAULT NULL COMMENT '正确答案',
    `is_correct`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否正确：1 正确 0 错误',
    `answer_time`     DATETIME     NOT NULL COMMENT '答题时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_practice_user_time` (`user_id`, `answer_time`),
    KEY `idx_practice_user_subject` (`user_id`, `exam_subject`),
    KEY `idx_practice_question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '日常练习记录表';

-- ------------------------------------------------------------
-- 2. wrong_question 补充“累计答错次数”字段（存在则跳过）
-- ------------------------------------------------------------
SET @add_wrong_count := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `wrong_question` ADD COLUMN `wrong_count` INT NOT NULL DEFAULT 1 COMMENT ''累计答错次数''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wrong_question' AND COLUMN_NAME = 'wrong_count'
);
PREPARE stmt_wrong_count FROM @add_wrong_count;
EXECUTE stmt_wrong_count;
DEALLOCATE PREPARE stmt_wrong_count;

SET @add_wrong_idx := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `wrong_question` ADD INDEX `idx_wrong_user_question` (`user_id`, `question_id`)',
              'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wrong_question' AND INDEX_NAME = 'idx_wrong_user_question'
);
PREPARE stmt_wrong_idx FROM @add_wrong_idx;
EXECUTE stmt_wrong_idx;
DEALLOCATE PREPARE stmt_wrong_idx;

-- ------------------------------------------------------------
-- 3. 演示题库：练习试卷 + 题目内容 + 题目
-- ------------------------------------------------------------
INSERT IGNORE INTO `exam_paper` (`id`, `paper_name`, `exam_subject`, `total_score`, `paper_type`, `create_time`, `update_time`)
VALUES (9001, '日常练习题库', '综合', 100, 1, NOW(), NOW());

INSERT IGNORE INTO `question_content` (`id`, `question_text`, `option_info`, `ref_answer`, `analysis`, `create_time`, `update_time`) VALUES
(9001, '当 x 趋近于 0 时，sin x / x 的极限是多少？',
 '[{"key":"A","text":"0"},{"key":"B","text":"1"},{"key":"C","text":"2"},{"key":"D","text":"不存在"}]',
 'B', '这是重要极限 lim(x→0) sin x / x = 1，需要重点记忆。', NOW(), NOW()),

(9002, '函数 f(x) = x² ln x 在 x = 1 处的导数值是？',
 '[{"key":"A","text":"0"},{"key":"B","text":"1"},{"key":"C","text":"2"},{"key":"D","text":"e"}]',
 'B', 'f''(x) = 2x ln x + x，代入 x = 1 得 f''(1) = 1。', NOW(), NOW()),

(9003, '二阶行列式 |1 2; 3 4| 的值是多少？',
 '[{"key":"A","text":"-2"},{"key":"B","text":"2"},{"key":"C","text":"5"},{"key":"D","text":"10"}]',
 'A', '二阶行列式按主对角线相乘再相减：1×4 − 2×3 = −2。', NOW(), NOW()),

(9004, '已知 P(A) = 0.5，P(B|A) = 0.6，则 P(AB) 等于？',
 '[{"key":"A","text":"0.1"},{"key":"B","text":"0.3"},{"key":"C","text":"0.8"},{"key":"D","text":"1.1"}]',
 'B', '由条件概率公式 P(B|A) = P(AB) / P(A)，得 P(AB) = 0.5 × 0.6 = 0.3。', NOW(), NOW()),

(9005, '英语单词 abandon 最接近下列哪个含义？',
 '[{"key":"A","text":"放弃"},{"key":"B","text":"获得"},{"key":"C","text":"遵守"},{"key":"D","text":"增加"}]',
 'A', 'abandon 意为“放弃、抛弃”，常见搭配 abandon a plan。', NOW(), NOW()),

(9006, 'By the time he arrived, the meeting ______.',
 '[{"key":"A","text":"started"},{"key":"B","text":"has started"},{"key":"C","text":"had started"},{"key":"D","text":"starts"}]',
 'C', '“By the time + 过去时”主句表示过去的过去，用过去完成时 had started。', NOW(), NOW()),

(9007, 'The scientist''s theory was eventually ______ by a series of experiments.',
 '[{"key":"A","text":"confirmed"},{"key":"B","text":"refused"},{"key":"C","text":"denied"},{"key":"D","text":"ignored"}]',
 'A', 'confirm 表示“证实”，与 theory 搭配表示理论被实验证实。', NOW(), NOW()),

(9008, '马克思主义认为，物质的唯一特性是？',
 '[{"key":"A","text":"运动性"},{"key":"B","text":"客观实在性"},{"key":"C","text":"可知性"},{"key":"D","text":"矛盾性"}]',
 'B', '物质的唯一特性是客观实在性；运动是物质的根本属性。', NOW(), NOW()),

(9009, '矛盾的两个基本属性是？',
 '[{"key":"A","text":"普遍性和特殊性"},{"key":"B","text":"同一性和斗争性"},{"key":"C","text":"主要方面和次要方面"},{"key":"D","text":"内因和外因"}]',
 'B', '矛盾即对立统一，同一性和斗争性是矛盾的两种基本属性。', NOW(), NOW()),

(9010, '中国共产党成立于哪一年？',
 '[{"key":"A","text":"1919 年"},{"key":"B","text":"1921 年"},{"key":"C","text":"1927 年"},{"key":"D","text":"1949 年"}]',
 'B', '1921 年 7 月中国共产党第一次全国代表大会召开，宣告中国共产党成立。', NOW(), NOW()),

(9011, '在有序数组上进行二分查找，平均时间复杂度是？',
 '[{"key":"A","text":"O(1)"},{"key":"B","text":"O(log n)"},{"key":"C","text":"O(n)"},{"key":"D","text":"O(n log n)"}]',
 'B', '二分查找每次将区间缩小一半，因此时间复杂度为 O(log n)。', NOW(), NOW()),

(9012, 'TCP 三次握手的主要目的是？',
 '[{"key":"A","text":"提高数据传输速度"},{"key":"B","text":"建立可靠连接并同步序号"},{"key":"C","text":"对数据加密"},{"key":"D","text":"压缩数据"}]',
 'B', '三次握手用于确认双方收发能力正常，并同步初始序号，建立可靠连接。', NOW(), NOW()),

(9013, '同一进程内的多个线程共享下列哪项资源？',
 '[{"key":"A","text":"线程栈"},{"key":"B","text":"寄存器"},{"key":"C","text":"进程地址空间"},{"key":"D","text":"程序计数器"}]',
 'C', '线程共享进程的地址空间和打开的文件等资源，栈、寄存器、程序计数器为线程私有。', NOW(), NOW()),

(9014, 'SQL 中用于消除查询结果重复行的关键字是？',
 '[{"key":"A","text":"DISTINCT"},{"key":"B","text":"UNIQUE"},{"key":"C","text":"GROUP"},{"key":"D","text":"HAVING"}]',
 'A', 'SELECT DISTINCT 会对结果集去重；UNIQUE 是约束，GROUP BY 是分组。', NOW(), NOW());

INSERT IGNORE INTO `exam_question`
(`id`, `paper_id`, `content_id`, `exam_subject`, `chapter`, `knowledge_point`, `q_type`, `difficulty`, `score`, `create_time`, `update_time`) VALUES
(9001, 9001, 9001, '数学', '高等数学', '极限', '单选题', '基础', 5, NOW(), NOW()),
(9002, 9001, 9002, '数学', '高等数学', '导数', '单选题', '中等', 5, NOW(), NOW()),
(9003, 9001, 9003, '数学', '线性代数', '行列式', '单选题', '中等', 5, NOW(), NOW()),
(9004, 9001, 9004, '数学', '概率论', '条件概率', '单选题', '基础', 5, NOW(), NOW()),
(9005, 9001, 9005, '英语', '阅读理解', '词汇理解', '单选题', '基础', 5, NOW(), NOW()),
(9006, 9001, 9006, '英语', '语法', '时态', '单选题', '中等', 5, NOW(), NOW()),
(9007, 9001, 9007, '英语', '翻译', '词义辨析', '单选题', '中等', 5, NOW(), NOW()),
(9008, 9001, 9008, '政治', '马原', '唯物论', '单选题', '基础', 5, NOW(), NOW()),
(9009, 9001, 9009, '政治', '马原', '辩证法', '单选题', '中等', 5, NOW(), NOW()),
(9010, 9001, 9010, '政治', '史纲', '重要事件', '单选题', '基础', 5, NOW(), NOW()),
(9011, 9001, 9011, '专业课', '数据结构', '时间复杂度', '单选题', '中等', 5, NOW(), NOW()),
(9012, 9001, 9012, '专业课', '计算机网络', '传输层', '单选题', '中等', 5, NOW(), NOW()),
(9013, 9001, 9013, '专业课', '操作系统', '进程管理', '单选题', '中等', 5, NOW(), NOW()),
(9014, 9001, 9014, '专业课', '数据库', 'SQL 基础', '单选题', '基础', 5, NOW(), NOW());
