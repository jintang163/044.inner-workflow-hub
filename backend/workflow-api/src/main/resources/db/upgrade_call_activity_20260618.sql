-- 子流程调用功能数据库升级脚本
-- 执行日期: 2026-06-18

-- 1. 新增 wf_node_config 表字段 - 子流程调用配置
ALTER TABLE wf_node_config
ADD COLUMN call_activity_process_key VARCHAR(128) COMMENT '子流程调用-流程Key' AFTER parallel_reject_strategy,
ADD COLUMN input_variable_mapping JSON COMMENT '输入变量映射（主->子）' AFTER call_activity_process_key,
ADD COLUMN output_variable_mapping JSON COMMENT '输出变量映射（子->主）' AFTER input_variable_mapping;

-- 2. 创建主子流程实例关联表
CREATE TABLE IF NOT EXISTS wf_process_instance_relation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    parent_instance_id BIGINT COMMENT '父流程实例ID',
    parent_flowable_inst_id VARCHAR(64) COMMENT '父流程Flowable实例ID',
    parent_node_id VARCHAR(64) COMMENT '父流程节点ID',
    child_instance_id BIGINT COMMENT '子流程实例ID',
    child_flowable_inst_id VARCHAR(64) COMMENT '子流程Flowable实例ID',
    child_process_key VARCHAR(128) COMMENT '子流程Key',
    child_process_name VARCHAR(128) COMMENT '子流程名称',
    relation_type TINYINT DEFAULT 1 COMMENT '关联类型: 1-子流程调用',
    call_activity_node_id VARCHAR(64) COMMENT 'CallActivity节点ID',
    call_activity_node_name VARCHAR(128) COMMENT 'CallActivity节点名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_instance_id (parent_instance_id),
    KEY idx_child_instance_id (child_instance_id),
    KEY idx_child_flowable_inst_id (child_flowable_inst_id),
    KEY idx_parent_node (parent_instance_id, call_activity_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主子流程实例关联表';

-- 3. 新增 NodeTypeEnum 枚举值（已在代码中添加，无需数据库操作）
-- CALL_ACTIVITY('CALL_ACTIVITY', '子流程')

-- 4. 新增 TaskActionEnum 枚举值（已在之前添加）
-- TERMINATE(9, '终止')

-- 5. 新增 HistoryActivityTypeEnum 枚举值（已在之前添加）
-- TERMINATE(12, '终止')
