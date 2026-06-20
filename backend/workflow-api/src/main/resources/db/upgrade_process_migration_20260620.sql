-- =============================================
-- 历史流程实例迁移功能升级脚本
-- 日期: 2026-06-20
-- =============================================

-- 1. 流程实例迁移记录表
DROP TABLE IF EXISTS `wf_process_migration_record`;
CREATE TABLE `wf_process_migration_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `migration_no` varchar(64) NOT NULL COMMENT '迁移批次号',
    `process_key` varchar(128) NOT NULL COMMENT '流程标识',
    `process_definition_id` bigint NOT NULL COMMENT '流程定义ID',
    `source_version_id` bigint NOT NULL COMMENT '源版本ID',
    `source_version` int NOT NULL COMMENT '源版本号',
    `target_version_id` bigint NOT NULL COMMENT '目标版本ID',
    `target_version` int NOT NULL COMMENT '目标版本号',
    `total_count` int NOT NULL DEFAULT 0 COMMENT '总迁移数',
    `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
    `skip_count` int NOT NULL DEFAULT 0 COMMENT '跳过数',
    `migration_status` tinyint NOT NULL DEFAULT 0 COMMENT '迁移状态(0-待执行 1-执行中 2-成功 3-部分成功 4-失败)',
    `backup_snapshot` longtext COMMENT '备份快照(JSON)',
    `error_message` varchar(1000) COMMENT '错误信息',
    `remark` varchar(500) COMMENT '备注',
    `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_migration_no` (`migration_no`),
    KEY `idx_process_key` (`process_key`),
    KEY `idx_process_def_id` (`process_definition_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例迁移记录表';

-- 2. 流程实例迁移明细表
DROP TABLE IF EXISTS `wf_process_migration_detail`;
CREATE TABLE `wf_process_migration_detail` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `record_id` bigint NOT NULL COMMENT '迁移记录ID',
    `migration_no` varchar(64) NOT NULL COMMENT '迁移批次号',
    `instance_id` bigint NOT NULL COMMENT '流程实例ID',
    `instance_no` varchar(64) NOT NULL COMMENT '流程实例编号',
    `title` varchar(500) COMMENT '流程标题',
    `start_user_id` bigint COMMENT '发起人ID',
    `start_user_name` varchar(100) COMMENT '发起人名称',
    `source_flowable_proc_inst_id` varchar(64) COMMENT '源流程实例ID(Flowable)',
    `source_flowable_proc_def_id` varchar(64) COMMENT '源流程定义ID(Flowable)',
    `source_version_id` bigint COMMENT '源版本ID',
    `source_current_node_ids` varchar(500) COMMENT '源当前节点ID(JSON数组)',
    `source_current_approver_ids` varchar(500) COMMENT '源当前审批人ID(JSON数组)',
    `target_flowable_proc_inst_id` varchar(64) COMMENT '目标流程实例ID(Flowable)',
    `target_flowable_proc_def_id` varchar(64) COMMENT '目标流程定义ID(Flowable)',
    `target_version_id` bigint COMMENT '目标版本ID',
    `target_process_version_id` bigint COMMENT '目标版本ID(冗余)',
    `migration_result` tinyint NOT NULL COMMENT '迁移结果(0-待迁移 1-成功 2-失败 3-跳过)',
    `skip_reason` varchar(500) COMMENT '跳过原因',
    `error_message` varchar(1000) COMMENT '错误信息',
    `compatibility_check` longtext COMMENT '兼容性检查结果(JSON)',
    `backup_instance_data` longtext COMMENT '实例备份数据(JSON)',
    `backup_tasks_data` longtext COMMENT '任务备份数据(JSON)',
    `backup_variables_data` longtext COMMENT '变量备份数据(JSON)',
    `migrate_time` datetime COMMENT '迁移时间',
    `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_migration_no` (`migration_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例迁移明细表';

-- 3. 预置迁移记录的枚举配置不需要建表，通过状态字段管理
