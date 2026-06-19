-- 驳回与回退功能升级脚本
-- 2026-06-19

-- wf_process_instance 增加驳回次数字段
ALTER TABLE wf_process_instance ADD COLUMN reject_count INT NOT NULL DEFAULT 0 COMMENT '驳回次数';
ALTER TABLE wf_process_instance ADD COLUMN max_reject_count INT NOT NULL DEFAULT 5 COMMENT '最大驳回次数';
ALTER TABLE wf_process_instance ADD COLUMN form_data_version INT NOT NULL DEFAULT 1 COMMENT '表单数据版本号';
ALTER TABLE wf_process_instance ADD COLUMN start_user_name VARCHAR(64) DEFAULT NULL COMMENT '发起人姓名（冗余）';
ALTER TABLE wf_process_instance ADD COLUMN start_dept_name VARCHAR(128) DEFAULT NULL COMMENT '发起部门名称（冗余）';
ALTER TABLE wf_process_instance ADD COLUMN start_user_avatar VARCHAR(512) DEFAULT NULL COMMENT '发起人头像（冗余）';

-- wf_approval_history 增加目标节点名称字段（已有 target_node_id）
ALTER TABLE wf_approval_history ADD COLUMN target_node_name VARCHAR(128) DEFAULT NULL COMMENT '驳回目标节点名称';

-- wf_approval_task 冗余发起人信息（待办列表直接展示用）
ALTER TABLE wf_approval_task ADD COLUMN start_user_name VARCHAR(64) DEFAULT NULL COMMENT '发起人姓名';
ALTER TABLE wf_approval_task ADD COLUMN start_dept_name VARCHAR(128) DEFAULT NULL COMMENT '发起部门名称';
ALTER TABLE wf_approval_task ADD COLUMN start_user_avatar VARCHAR(512) DEFAULT NULL COMMENT '发起人头像';
