-- ============================================================
-- 多租户迁移脚本
-- 执行顺序：在 01_schema.sql 之后执行
-- ============================================================

USE `inner_workflow_hub`;

-- ============================================================
-- 1. 租户核心表
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_tenant` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花算法）',
    `tenant_name` VARCHAR(100) NOT NULL COMMENT '租户名称',
    `tenant_code` VARCHAR(50) NOT NULL COMMENT '租户编码（唯一标识）',
    `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人',
    `contact_email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `business_type` VARCHAR(50) NOT NULL COMMENT '业务线类型：HR/财务/采购/IT/行政/其他',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核 1-已启用 2-已禁用',
    `expire_time` DATETIME DEFAULT NULL COMMENT '到期时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`),
    KEY `idx_status` (`status`),
    KEY `idx_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

CREATE TABLE IF NOT EXISTS `sys_tenant_user` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花算法）',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tenant_role` VARCHAR(50) NOT NULL DEFAULT 'TENANT_USER' COMMENT '租户内角色：TENANT_ADMIN/TENANT_USER',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tenant_role` (`tenant_id`, `tenant_role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户-用户关联表';

CREATE TABLE IF NOT EXISTS `sys_tenant_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_sort` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `data_scope` TINYINT NOT NULL DEFAULT 1 COMMENT '数据范围：1-全部数据 2-自定义数据 3-本部门数据 4-本部门及以下 5-仅本人',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-停用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_code` (`tenant_id`, `role_code`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户级角色表';

CREATE TABLE IF NOT EXISTS `sys_tenant_role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_role_id` BIGINT NOT NULL COMMENT '租户角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_menu` (`tenant_role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色-菜单关联表';

CREATE TABLE IF NOT EXISTS `sys_tenant_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tenant_role_id` BIGINT NOT NULL COMMENT '租户角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_role` (`tenant_id`, `user_id`, `tenant_role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tenant_role_id` (`tenant_role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户-用户-角色关联表';

-- ============================================================
-- 2. 为所有业务表添加 tenant_id 字段
-- ============================================================

ALTER TABLE `wf_business_line` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_business_line` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_category` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_category` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_process_definition` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_process_definition` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_form_definition` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_form_definition` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_process_instance` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_process_instance` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_approval_task` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_approval_task` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_approval_history` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_approval_history` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_cc_task` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_cc_task` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_form_draft` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_form_draft` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_message_template` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_message_template` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_message_log` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_message_log` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_attachment` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_attachment` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_timeout_remind` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_timeout_remind` ADD KEY `idx_tenant_id` (`tenant_id`);

ALTER TABLE `wf_task_relation` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `wf_task_relation` ADD KEY `idx_tenant_id` (`tenant_id`);

-- ============================================================
-- 3. sys_role 增加 tenant_id 字段，实现租户级角色隔离
-- ============================================================

ALTER TABLE `sys_role` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID（NULL表示系统全局角色）' AFTER `id`;
ALTER TABLE `sys_role` ADD KEY `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_role` DROP INDEX `uk_role_code`;
ALTER TABLE `sys_role` ADD UNIQUE KEY `uk_tenant_role_code` (`tenant_id`, `role_code`);

-- ============================================================
-- 4. sys_department 增加 tenant_id 字段
-- ============================================================

ALTER TABLE `sys_department` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `id`;
ALTER TABLE `sys_department` ADD KEY `idx_tenant_id` (`tenant_id`);

-- ============================================================
-- 5. 初始化超管租户与默认数据
-- ============================================================

INSERT INTO `sys_tenant` (`id`, `tenant_name`, `tenant_code`, `contact_name`, `contact_email`, `business_type`, `status`, `remark`)
VALUES (1, '系统管理租户', 'SYSTEM', '超级管理员', 'admin@system.com', '其他', 1, '系统内置超管租户，不可删除');

INSERT INTO `sys_tenant_role` (`tenant_id`, `role_name`, `role_code`, `role_sort`, `data_scope`, `status`, `remark`)
VALUES (1, '超级管理员', 'SUPER_ADMIN', 1, 1, 1, '系统超级管理员，拥有全部权限'),
       (1, '租户管理员', 'TENANT_ADMIN', 2, 1, 1, '租户管理员'),
       (1, '普通用户', 'TENANT_USER', 3, 5, 1, '租户普通用户');
