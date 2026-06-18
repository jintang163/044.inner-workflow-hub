-- ============================================================
-- 企业内部多业务线统一审批流系统 - 数据库脚本
-- 数据库版本: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `inner_workflow_hub` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `inner_workflow_hub`;

-- ============================================================
-- 1. RBAC权限相关表
-- ============================================================

-- 部门表
CREATE TABLE IF NOT EXISTS `sys_department` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示顶级部门',
    `ancestors` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祖级列表，用逗号分隔',
    `dept_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
    `dept_code` VARCHAR(50) NOT NULL COMMENT '部门编码',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `leader_user_id` BIGINT DEFAULT NULL COMMENT '部门负责人ID',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-停用',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`dept_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(200) NOT NULL COMMENT '密码（BCrypt加密）',
    `nick_name` VARCHAR(50) NOT NULL COMMENT '用户昵称',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `user_type` TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型：1-系统用户 2-业务用户',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
    `ding_user_id` VARCHAR(100) DEFAULT NULL COMMENT '钉钉用户ID',
    `wecom_user_id` VARCHAR(100) DEFAULT NULL COMMENT '企业微信用户ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-停用',
    `login_ip` VARCHAR(128) DEFAULT NULL COMMENT '最后登录IP',
    `login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `pwd_update_time` DATETIME DEFAULT NULL COMMENT '密码最后修改时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_status` (`status`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
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
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 菜单权限表
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
    `path` VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    `permission` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `menu_type` CHAR(1) NOT NULL DEFAULT 'M' COMMENT '菜单类型：M-目录 C-菜单 F-按钮',
    `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：1-显示 0-隐藏',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-停用',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 角色部门关联表（数据权限）
CREATE TABLE IF NOT EXISTS `sys_role_dept` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_dept` (`role_id`, `dept_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色部门关联表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `sys_oper_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    `title` VARCHAR(50) DEFAULT NULL COMMENT '模块标题',
    `business_type` TINYINT DEFAULT 0 COMMENT '业务类型：0-其它 1-新增 2-修改 3-删除 4-授权 5-导出 6-导入 7-强退 8-生成代码 9-清空数据',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '方法名称',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方式',
    `oper_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人员',
    `dept_name` VARCHAR(50) DEFAULT NULL COMMENT '部门名称',
    `oper_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `oper_ip` VARCHAR(128) DEFAULT NULL COMMENT '主机地址',
    `oper_location` VARCHAR(255) DEFAULT NULL COMMENT '操作地点',
    `oper_param` TEXT DEFAULT NULL COMMENT '请求参数',
    `json_result` TEXT DEFAULT NULL COMMENT '返回参数',
    `status` TINYINT DEFAULT 1 COMMENT '操作状态：1-正常 0-异常',
    `error_msg` TEXT DEFAULT NULL COMMENT '错误消息',
    `cost_time` BIGINT DEFAULT 0 COMMENT '耗时（毫秒）',
    `oper_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_business_type` (`business_type`),
    KEY `idx_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '访问ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户账号',
    `login_ip` VARCHAR(128) DEFAULT NULL COMMENT '登录IP',
    `login_location` VARCHAR(255) DEFAULT NULL COMMENT '登录地点',
    `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器类型',
    `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
    `status` TINYINT DEFAULT 1 COMMENT '登录状态：1-成功 0-失败',
    `msg` VARCHAR(255) DEFAULT NULL COMMENT '提示消息',
    `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (`id`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ============================================================
-- 2. 业务线与流程分类表
-- ============================================================

-- 业务线表
CREATE TABLE IF NOT EXISTS `wf_business_line` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `line_name` VARCHAR(100) NOT NULL COMMENT '业务线名称',
    `line_code` VARCHAR(50) NOT NULL COMMENT '业务线编码',
    `line_icon` VARCHAR(255) DEFAULT NULL COMMENT '业务线图标',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_line_code` (`line_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务线表';

-- 流程分类表
CREATE TABLE IF NOT EXISTS `wf_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `business_line_id` BIGINT NOT NULL COMMENT '业务线ID',
    `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `category_code` VARCHAR(50) NOT NULL COMMENT '分类编码',
    `category_icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`),
    KEY `idx_business_line_id` (`business_line_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程分类表';

-- ============================================================
-- 3. 流程定义相关表
-- ============================================================

-- 流程定义主表
CREATE TABLE IF NOT EXISTS `wf_process_definition` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `process_key` VARCHAR(100) NOT NULL COMMENT '流程标识（BPMN ID）',
    `process_name` VARCHAR(200) NOT NULL COMMENT '流程名称',
    `business_line_id` BIGINT NOT NULL COMMENT '业务线ID',
    `category_id` BIGINT NOT NULL COMMENT '流程分类ID',
    `description` VARCHAR(1000) DEFAULT NULL COMMENT '流程描述',
    `icon` VARCHAR(500) DEFAULT NULL COMMENT '流程图标',
    `current_version` INT NOT NULL DEFAULT 0 COMMENT '当前发布版本号，0表示未发布',
    `process_status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已停用',
    `form_id` BIGINT DEFAULT NULL COMMENT '关联的表单ID（发布版本时冗余）',
    `start_permission_type` TINYINT NOT NULL DEFAULT 1 COMMENT '发起人权限类型：1-全部 2-指定部门 3-指定角色 4-指定人员',
    `start_permission_json` JSON DEFAULT NULL COMMENT '发起人权限配置JSON',
    `admin_user_ids` JSON DEFAULT NULL COMMENT '流程管理员用户ID列表JSON',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_process_key` (`process_key`),
    KEY `idx_business_line_id` (`business_line_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_process_status` (`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义主表';

-- 流程版本表
CREATE TABLE IF NOT EXISTS `wf_process_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `process_definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
    `process_key` VARCHAR(100) NOT NULL COMMENT '流程标识',
    `version` INT NOT NULL COMMENT '版本号（从1开始）',
    `flowable_deployment_id` VARCHAR(64) DEFAULT NULL COMMENT 'Flowable部署ID',
    `flowable_process_def_id` VARCHAR(64) DEFAULT NULL COMMENT 'Flowable流程定义ID',
    `bpmn_xml` MEDIUMTEXT NOT NULL COMMENT 'BPMN 2.0 XML内容',
    `form_id` BIGINT NOT NULL COMMENT '关联的表单ID',
    `form_version` INT NOT NULL COMMENT '表单版本号',
    `version_remark` VARCHAR(500) DEFAULT NULL COMMENT '版本说明',
    `publish_by` BIGINT DEFAULT NULL COMMENT '发布人ID',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `is_current` TINYINT NOT NULL DEFAULT 1 COMMENT '是否当前版本：1-是 0-否',
    `suspend_status` TINYINT NOT NULL DEFAULT 0 COMMENT '挂起状态：0-激活 1-挂起',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_process_version` (`process_definition_id`, `version`),
    KEY `idx_process_key` (`process_key`),
    KEY `idx_flowable_def_id` (`flowable_process_def_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程版本表';

-- 流程节点扩展配置表
CREATE TABLE IF NOT EXISTS `wf_node_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `process_version_id` BIGINT NOT NULL COMMENT '流程版本ID',
    `process_key` VARCHAR(100) NOT NULL COMMENT '流程标识',
    `node_id` VARCHAR(100) NOT NULL COMMENT 'BPMN节点ID',
    `node_name` VARCHAR(200) NOT NULL COMMENT '节点名称',
    `node_type` VARCHAR(50) NOT NULL COMMENT '节点类型：startEvent-开始 userTask-审批 exclusiveGateway-条件 parallelGateway-并行 endEvent-结束',
    `approve_type` TINYINT DEFAULT NULL COMMENT '审批类型：1-或签（一人通过即可）2-会签（全员通过）3-依次审批',
    `multi_instance` TINYINT NOT NULL DEFAULT 0 COMMENT '是否多实例：0-否 1-是',
    `assignee_type` TINYINT DEFAULT NULL COMMENT '审批人类型：1-固定人员 2-部门主管 3-角色 4-发起人 5-发起人主管 6-动态脚本',
    `assignee_value` JSON DEFAULT NULL COMMENT '审批人配置值JSON',
    `assignee_script` TEXT DEFAULT NULL COMMENT '动态脚本内容（Groovy/JavaScript）',
    `form_permission` JSON DEFAULT NULL COMMENT '节点表单字段权限JSON，key为字段名，value为：R-只读 W-可写 H-隐藏',
    `timeout_strategy` TINYINT DEFAULT NULL COMMENT '超时策略：1-自动通过 2-自动拒绝 3-转上级审批 4-仅提醒',
    `timeout_hours` INT DEFAULT NULL COMMENT '超时时间（小时）',
    `timeout_escalate_levels` INT DEFAULT NULL COMMENT '超时升级级数',
    `notify_config` JSON DEFAULT NULL COMMENT '通知配置JSON，配置哪些事件通知哪些渠道',
    `empty_assignee_strategy` TINYINT DEFAULT 1 COMMENT '审批人为空策略：1-自动通过 2-转管理员 3-报错',
    `refuse_strategy` TINYINT NOT NULL DEFAULT 1 COMMENT '拒绝策略：1-直接结束流程 2-驳回到上一节点 3-驳回到指定节点',
    `refuse_target_node_id` VARCHAR(100) DEFAULT NULL COMMENT '驳回目标节点ID',
    `can_add_sign` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许加签：1-允许 0-不允许',
    `can_transfer` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许转审：1-允许 0-不允许',
    `can_delegate` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许委派：1-允许 0-不允许',
    `need_signature` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要手写签名：1-需要 0-不需要',
    `need_comment` TINYINT NOT NULL DEFAULT 1 COMMENT '审批意见是否必填：1-必填 0-不必填',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version_node` (`process_version_id`, `node_id`),
    KEY `idx_process_key_node` (`process_key`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点扩展配置表';

-- 条件分支配置表
CREATE TABLE IF NOT EXISTS `wf_sequence_flow_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `process_version_id` BIGINT NOT NULL COMMENT '流程版本ID',
    `process_key` VARCHAR(100) NOT NULL COMMENT '流程标识',
    `flow_id` VARCHAR(100) NOT NULL COMMENT 'BPMN连线ID',
    `flow_name` VARCHAR(200) DEFAULT NULL COMMENT '连线名称',
    `source_node_id` VARCHAR(100) NOT NULL COMMENT '源节点ID',
    `target_node_id` VARCHAR(100) NOT NULL COMMENT '目标节点ID',
    `condition_type` TINYINT DEFAULT NULL COMMENT '条件类型：1-表达式 2-脚本 3-规则',
    `condition_expression` VARCHAR(1000) DEFAULT NULL COMMENT '条件表达式',
    `condition_script` TEXT DEFAULT NULL COMMENT '条件脚本',
    `condition_priority` INT DEFAULT 0 COMMENT '条件优先级（数字越小越优先）',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认分支：1-是 0-否',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version_flow` (`process_version_id`, `flow_id`),
    KEY `idx_process_key` (`process_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='条件分支配置表';

-- ============================================================
-- 4. 动态表单相关表
-- ============================================================

-- 表单定义主表
CREATE TABLE IF NOT EXISTS `wf_form_definition` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `form_key` VARCHAR(100) NOT NULL COMMENT '表单标识',
    `form_name` VARCHAR(200) NOT NULL COMMENT '表单名称',
    `business_line_id` BIGINT NOT NULL COMMENT '业务线ID',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '表单描述',
    `current_version` INT NOT NULL DEFAULT 0 COMMENT '当前版本号',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已停用',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_form_key` (`form_key`),
    KEY `idx_business_line_id` (`business_line_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单定义主表';

-- 表单版本表
CREATE TABLE IF NOT EXISTS `wf_form_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `form_definition_id` BIGINT NOT NULL COMMENT '表单定义ID',
    `form_key` VARCHAR(100) NOT NULL COMMENT '表单标识',
    `version` INT NOT NULL COMMENT '版本号（从1开始）',
    `form_schema` LONGTEXT NOT NULL COMMENT '表单JSON Schema（Formily标准）',
    `field_mapping` JSON DEFAULT NULL COMMENT '字段与流程变量映射配置JSON',
    `version_remark` VARCHAR(500) DEFAULT NULL COMMENT '版本说明',
    `publish_by` BIGINT DEFAULT NULL COMMENT '发布人ID',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `is_current` TINYINT NOT NULL DEFAULT 1 COMMENT '是否当前版本：1-是 0-否',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_form_version` (`form_definition_id`, `version`),
    KEY `idx_form_key` (`form_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单版本表';

-- 表单草稿表
CREATE TABLE IF NOT EXISTS `wf_form_draft` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `draft_no` VARCHAR(50) NOT NULL COMMENT '草稿编号',
    `process_definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
    `process_key` VARCHAR(100) NOT NULL COMMENT '流程标识',
    `form_id` BIGINT NOT NULL COMMENT '表单ID',
    `form_version` INT NOT NULL COMMENT '表单版本',
    `form_data` JSON NOT NULL COMMENT '表单数据JSON',
    `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_draft_no` (`draft_no`),
    KEY `idx_creator_process` (`creator_id`, `process_definition_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单草稿表';

-- ============================================================
-- 5. 审批实例相关表
-- ============================================================

-- 流程实例业务表
CREATE TABLE IF NOT EXISTS `wf_process_instance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_no` VARCHAR(50) NOT NULL COMMENT '审批单号',
    `process_definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
    `process_key` VARCHAR(100) NOT NULL COMMENT '流程标识',
    `process_version_id` BIGINT NOT NULL COMMENT '流程版本ID',
    `flowable_process_inst_id` VARCHAR(64) NOT NULL COMMENT 'Flowable流程实例ID',
    `flowable_process_def_id` VARCHAR(64) NOT NULL COMMENT 'Flowable流程定义ID',
    `business_line_id` BIGINT NOT NULL COMMENT '业务线ID',
    `category_id` BIGINT NOT NULL COMMENT '流程分类ID',
    `title` VARCHAR(500) NOT NULL COMMENT '审批标题（摘要）',
    `form_id` BIGINT NOT NULL COMMENT '表单ID',
    `form_version` INT NOT NULL COMMENT '表单版本号',
    `form_data` JSON NOT NULL COMMENT '表单数据JSON',
    `instance_status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-审批中 1-已通过 2-已拒绝 3-已撤销 4-已挂起',
    `start_user_id` BIGINT NOT NULL COMMENT '发起人ID',
    `start_dept_id` BIGINT DEFAULT NULL COMMENT '发起人部门ID',
    `start_time` DATETIME NOT NULL COMMENT '发起时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `duration` BIGINT DEFAULT NULL COMMENT '耗时（毫秒）',
    `current_node_ids` JSON DEFAULT NULL COMMENT '当前审批节点ID列表JSON',
    `current_approver_ids` JSON DEFAULT NULL COMMENT '当前审批人ID列表JSON',
    `priority` TINYINT NOT NULL DEFAULT 0 COMMENT '优先级：0-普通 1-紧急 2-特急',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是（发起人删除）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance_no` (`instance_no`),
    UNIQUE KEY `uk_flowable_inst_id` (`flowable_process_inst_id`),
    KEY `idx_process_key` (`process_key`),
    KEY `idx_start_user` (`start_user_id`),
    KEY `idx_instance_status` (`instance_status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_business_line` (`business_line_id`, `instance_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例业务表';

-- 审批任务业务表
CREATE TABLE IF NOT EXISTS `wf_approval_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_no` VARCHAR(50) NOT NULL COMMENT '任务编号',
    `instance_id` BIGINT NOT NULL COMMENT '流程实例ID（业务）',
    `flowable_task_id` VARCHAR(64) NOT NULL COMMENT 'Flowable任务ID',
    `flowable_execution_id` VARCHAR(64) DEFAULT NULL COMMENT 'Flowable执行ID',
    `process_key` VARCHAR(100) NOT NULL COMMENT '流程标识',
    `node_id` VARCHAR(100) NOT NULL COMMENT 'BPMN节点ID',
    `node_name` VARCHAR(200) NOT NULL COMMENT '节点名称',
    `node_type` TINYINT NOT NULL COMMENT '节点类型：1-审批 2-抄送',
    `approve_type` TINYINT DEFAULT NULL COMMENT '审批类型：1-或签 2-会签 3-依次审批',
    `multi_instance_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '多实例标识：0-否 1-是',
    `assignee_id` BIGINT NOT NULL COMMENT '处理人ID',
    `assign_time` DATETIME NOT NULL COMMENT '分配时间',
    `due_time` DATETIME DEFAULT NULL COMMENT '截止时间',
    `action` TINYINT DEFAULT NULL COMMENT '操作结果：1-同意 2-拒绝 3-转审 4-加签 5-委派 6-驳回 7-撤回 8-超时自动处理',
    `action_remark` VARCHAR(2000) DEFAULT NULL COMMENT '审批意见',
    `action_user_id` BIGINT DEFAULT NULL COMMENT '实际操作人ID（如转审的接收人）',
    `action_time` DATETIME DEFAULT NULL COMMENT '操作时间',
    `action_duration` BIGINT DEFAULT NULL COMMENT '处理耗时（毫秒）',
    `signature_url` VARCHAR(500) DEFAULT NULL COMMENT '手写签名图片URL',
    `attachment_ids` JSON DEFAULT NULL COMMENT '审批附件ID列表JSON',
    `task_status` TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态：0-待处理 1-已处理 2-已取消 3-已转审 4-已委派 5-已超时',
    `source_type` TINYINT DEFAULT NULL COMMENT '任务来源类型：1-正常指派 2-加签 3-转审 4-委派 5-超时升级',
    `source_task_id` BIGINT DEFAULT NULL COMMENT '来源任务ID（如加签的父任务）',
    `escalate_level` INT DEFAULT 0 COMMENT '超时升级级数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_no` (`task_no`),
    UNIQUE KEY `uk_flowable_task_id` (`flowable_task_id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_assignee_status` (`assignee_id`, `task_status`),
    KEY `idx_process_key` (`process_key`),
    KEY `idx_assign_time` (`assign_time`),
    KEY `idx_due_time` (`due_time`, `task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务业务表';

-- 审批操作历史表（用于时间轴展示）
CREATE TABLE IF NOT EXISTS `wf_approval_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
    `flowable_act_inst_id` VARCHAR(64) DEFAULT NULL COMMENT 'Flowable活动实例ID',
    `node_id` VARCHAR(100) NOT NULL COMMENT '节点ID',
    `node_name` VARCHAR(200) NOT NULL COMMENT '节点名称',
    `activity_type` TINYINT NOT NULL COMMENT '活动类型：1-发起 2-审批通过 3-审批拒绝 4-转审 5-加签 6-委派 7-驳回 8-撤回 9-抄送 10-超时自动处理 11-流程结束',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `operator_dept_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人部门',
    `target_user_id` BIGINT DEFAULT NULL COMMENT '目标用户ID（转审/加签/委派时）',
    `target_user_name` VARCHAR(50) DEFAULT NULL COMMENT '目标用户姓名',
    `target_node_id` VARCHAR(100) DEFAULT NULL COMMENT '目标节点ID（驳回时）',
    `target_node_name` VARCHAR(200) DEFAULT NULL COMMENT '目标节点名称',
    `action_remark` VARCHAR(2000) DEFAULT NULL COMMENT '操作意见/备注',
    `signature_url` VARCHAR(500) DEFAULT NULL COMMENT '手写签名URL',
    `attachment_ids` JSON DEFAULT NULL COMMENT '附件ID列表JSON',
    `duration` BIGINT DEFAULT NULL COMMENT '耗时（毫秒）',
    `is_valid` TINYINT NOT NULL DEFAULT 1 COMMENT '是否有效记录（撤回后原操作标记无效）',
    `operate_time` DATETIME NOT NULL COMMENT '操作时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批操作历史表';

-- 抄送人表
CREATE TABLE IF NOT EXISTS `wf_cc_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
    `process_key` VARCHAR(100) NOT NULL COMMENT '流程标识',
    `cc_user_id` BIGINT NOT NULL COMMENT '抄送人ID',
    `node_id` VARCHAR(100) DEFAULT NULL COMMENT '产生抄送的节点ID',
    `node_name` VARCHAR(200) DEFAULT NULL COMMENT '产生抄送的节点名称',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：1-是 0-否',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `cc_time` DATETIME NOT NULL COMMENT '抄送时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`cc_user_id`, `is_read`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_cc_time` (`cc_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抄送人表';

-- 加签/转审链路表
CREATE TABLE IF NOT EXISTS `wf_task_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_task_id` BIGINT NOT NULL COMMENT '父任务ID',
    `child_task_id` BIGINT NOT NULL COMMENT '子任务ID',
    `relation_type` TINYINT NOT NULL COMMENT '关系类型：1-前加签 2-后加签 3-转审 4-委派',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_task` (`parent_task_id`),
    KEY `idx_child_task` (`child_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加签/转审链路表';

-- 超时催办记录表
CREATE TABLE IF NOT EXISTS `wf_timeout_remind` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '审批任务ID',
    `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
    `assignee_id` BIGINT NOT NULL COMMENT '处理人ID',
    `remind_type` TINYINT NOT NULL COMMENT '催办类型：1-即将超时提醒 2-已超时催办 3-超时升级通知',
    `remind_count` INT NOT NULL DEFAULT 1 COMMENT '催办次数',
    `escalate_level` INT DEFAULT 0 COMMENT '升级级数',
    `escalate_to_user_id` BIGINT DEFAULT NULL COMMENT '升级到的用户ID',
    `remind_time` DATETIME NOT NULL COMMENT '催办时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_remind_time` (`remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='超时催办记录表';

-- ============================================================
-- 6. 消息通知相关表
-- ============================================================

-- 消息模板表
CREATE TABLE IF NOT EXISTS `wf_message_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `business_line_id` BIGINT DEFAULT NULL COMMENT '业务线ID，NULL为全局模板',
    `event_type` VARCHAR(50) NOT NULL COMMENT '触发事件类型：TASK_CREATE-任务创建 TASK_COMPLETE-任务完成 PROCESS_START-流程发起 PROCESS_END-流程结束 TIMEOUT_REMIND-超时提醒',
    `channel_types` JSON NOT NULL COMMENT '通知渠道列表JSON：DINGTALK-钉钉 WECOM-企微 EMAIL-邮件 SMS-短信',
    `ding_template_id` VARCHAR(100) DEFAULT NULL COMMENT '钉钉消息模板ID',
    `wecom_template_id` VARCHAR(100) DEFAULT NULL COMMENT '企微消息模板ID',
    `email_subject_template` VARCHAR(200) DEFAULT NULL COMMENT '邮件标题模板',
    `email_content_template` MEDIUMTEXT DEFAULT NULL COMMENT '邮件内容模板（FreeMarker/Thymeleaf）',
    `sms_template_id` VARCHAR(100) DEFAULT NULL COMMENT '短信模板ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板表';

-- 消息发送记录表
CREATE TABLE IF NOT EXISTS `wf_message_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `message_no` VARCHAR(50) NOT NULL COMMENT '消息编号',
    `template_id` BIGINT DEFAULT NULL COMMENT '消息模板ID',
    `template_code` VARCHAR(50) DEFAULT NULL COMMENT '消息模板编码',
    `business_type` VARCHAR(50) NOT NULL COMMENT '业务类型：APPROVAL-审批通知 TIMEOUT-超时通知 SYSTEM-系统通知',
    `instance_id` BIGINT DEFAULT NULL COMMENT '流程实例ID',
    `task_id` BIGINT DEFAULT NULL COMMENT '审批任务ID',
    `channel_type` VARCHAR(20) NOT NULL COMMENT '渠道类型：DINGTALK WECOM EMAIL SMS',
    `sender` VARCHAR(100) DEFAULT NULL COMMENT '发送方标识',
    `receiver_user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `receiver_account` VARCHAR(200) NOT NULL COMMENT '接收账号（手机号/邮箱/钉钉ID等）',
    `message_title` VARCHAR(200) DEFAULT NULL COMMENT '消息标题',
    `message_content` MEDIUMTEXT NOT NULL COMMENT '消息内容',
    `message_params` JSON DEFAULT NULL COMMENT '消息参数JSON',
    `send_status` TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态：0-待发送 1-发送中 2-发送成功 3-发送失败',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `max_retry` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `send_time` DATETIME DEFAULT NULL COMMENT '实际发送时间',
    `fail_reason` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    `third_party_msg_id` VARCHAR(200) DEFAULT NULL COMMENT '第三方消息ID（用于查询状态）',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读（仅应用内消息）：1-是 0-否',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_no` (`message_no`),
    KEY `idx_receiver_status` (`receiver_user_id`, `send_status`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_send_status` (`send_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录表';

-- ============================================================
-- 7. 附件表
-- ============================================================

-- 附件信息表
CREATE TABLE IF NOT EXISTS `wf_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件原始名称',
    `file_suffix` VARCHAR(20) DEFAULT NULL COMMENT '文件后缀名',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT 'MIME类型',
    `storage_type` TINYINT NOT NULL DEFAULT 1 COMMENT '存储类型：1-本地 2-OSS 3-其他',
    `storage_path` VARCHAR(500) NOT NULL COMMENT '存储路径',
    `access_url` VARCHAR(500) DEFAULT NULL COMMENT '访问URL',
    `md5` VARCHAR(32) DEFAULT NULL COMMENT '文件MD5校验值',
    `upload_user_id` BIGINT NOT NULL COMMENT '上传人ID',
    `biz_type` VARCHAR(50) DEFAULT NULL COMMENT '业务类型：APPROVAL_COMMENT-审批意见附件 FORM-表单字段附件',
    `biz_id` VARCHAR(64) DEFAULT NULL COMMENT '业务ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_md5` (`md5`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_upload_user` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件信息表';

-- ============================================================
-- 8. AI智能审批相关表
-- ============================================================

-- AI审批推荐记录表
CREATE TABLE IF NOT EXISTS `wf_ai_recommendation` (
    `id` BIGINT NOT NULL,
    `task_id` BIGINT NOT NULL,
    `instance_id` BIGINT NOT NULL,
    `approver_id` BIGINT NOT NULL,
    `approve_probability` DOUBLE DEFAULT NULL,
    `recommended_action` TINYINT DEFAULT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `factors_json` TEXT DEFAULT NULL,
    `model_version` VARCHAR(50) DEFAULT NULL,
    `inference_ms` BIGINT DEFAULT NULL,
    `adopted` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `action_time` DATETIME DEFAULT NULL,
    `tenant_id` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_approver_id` (`approver_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI审批推荐记录表';

-- 审批任务表增加AI推荐ID字段
ALTER TABLE `wf_approval_task` ADD COLUMN `ai_recommendation_id` BIGINT DEFAULT NULL COMMENT 'AI推荐ID' AFTER `escalate_level`;
