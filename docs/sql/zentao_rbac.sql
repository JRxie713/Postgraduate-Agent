-- ============================================================
-- 禅道式 RBAC 权限模型（考研导学系统）
-- 思路与禅道一致：
--   zt_group      权限组（相当于禅道的“权限分组”）
--   zt_grouppriv  组权限（模块 module + 方法 method，如 user/view）
--   zt_usergroup  用户与权限组的多对多关系
-- ADMIN 角色在代码中视为超级管理员，始终拥有全部权限。
-- ============================================================

USE kaoyan;

CREATE TABLE IF NOT EXISTS zt_group (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '权限组名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '说明',
    status      VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DISABLED',
    create_time DATETIME     DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限组';

CREATE TABLE IF NOT EXISTS zt_grouppriv (
    id       INT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id INT         NOT NULL COMMENT '权限组ID',
    module   VARCHAR(32) NOT NULL COMMENT '模块，如 user',
    method   VARCHAR(32) NOT NULL COMMENT '方法，如 view',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_priv (group_id, module, method)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组权限（模块-方法）';

CREATE TABLE IF NOT EXISTS zt_usergroup (
    id       INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id  INT NOT NULL COMMENT '用户ID',
    group_id INT NOT NULL COMMENT '权限组ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_group (user_id, group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户-权限组';

-- ---------- 三个默认权限组 ----------
INSERT INTO zt_group (id, name, description, status, create_time, update_time)
VALUES (1, '学生组', '只读：查看政策资讯、使用学习功能', 'NORMAL', NOW(), NOW()),
       (2, '教师组', '可发布、修改政策资讯', 'NORMAL', NOW(), NOW()),
       (3, '管理员组', '拥有全部权限（ADMIN 角色另有超级管理员直通）', 'NORMAL', NOW(), NOW())
ON DUPLICATE KEY UPDATE description = VALUES(description), status = 'NORMAL', update_time = NOW();

-- ---------- 组权限（模块:方法） ----------
-- 学生组：只看资讯
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (1, 'policy', 'view');
-- 教师组：看 + 发 + 改资讯
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (2, 'policy', 'view');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (2, 'policy', 'create');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (2, 'policy', 'update');
-- 管理员组：全部权限点
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'user', 'view');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'user', 'create');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'user', 'update');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'user', 'delete');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'user', 'unlock');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'policy', 'view');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'policy', 'create');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'policy', 'update');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'policy', 'delete');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'rbac', 'view');
INSERT IGNORE INTO zt_grouppriv (group_id, module, method) VALUES (3, 'rbac', 'manage');

-- ---------- 按现有 user_role 给存量用户分组 ----------
-- ADMIN -> 管理员组(3)，TEACHER -> 教师组(2)，其余 -> 学生组(1)
INSERT IGNORE INTO zt_usergroup (user_id, group_id)
SELECT id, CASE user_role WHEN 'ADMIN' THEN 3 WHEN 'TEACHER' THEN 2 ELSE 1 END
FROM user;
