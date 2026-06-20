-- ============================================================
-- 流程催办与升级功能升级脚本 (2026-06-20)
-- 包含：催办记录表扩展 + 升级规则表 + 升级历史表
-- 执行前请先备份数据库
-- ============================================================

-- 1. 扩展 wf_timeout_remind 表 - 增加催办相关字段
ALTER TABLE wf_timeout_remind
    ADD COLUMN IF NOT EXISTS remind_source TINYINT DEFAULT 1 COMMENT '催办来源:1-系统自动,2-手动催办' AFTER remind_type,
    ADD COLUMN IF NOT EXISTS remind_by BIGINT COMMENT '催办人ID(手动催办时)' AFTER remind_source,
    ADD COLUMN IF NOT EXISTS remark VARCHAR(500) COMMENT '催办备注' AFTER remind_by,
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT COMMENT '租户ID' AFTER remark,
    ADD COLUMN IF NOT EXISTS create_by BIGINT COMMENT '创建人' AFTER create_time,
    ADD COLUMN IF NOT EXISTS update_time DATETIME COMMENT '更新时间' AFTER create_by;

-- 2. 创建升级规则表 wf_escalation_rule
CREATE TABLE IF NOT EXISTS wf_escalation_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
    process_key VARCHAR(64) COMMENT '流程定义Key(为空则为全局规则)',
    node_id VARCHAR(64) COMMENT '节点ID(为空则为全流程)',
    escalate_level INT NOT NULL DEFAULT 1 COMMENT '升级级别:1-一级,2-二级,3-三级...',
    timeout_hours INT NOT NULL COMMENT '超时时间(小时)',
    escalate_type TINYINT NOT NULL DEFAULT 1 COMMENT '升级目标类型:1-部门主管,2-指定角色,3-指定用户,4-管理员',
    escalate_target VARCHAR(512) COMMENT '升级目标值(角色ID列表/用户ID列表)',
    escalate_action TINYINT NOT NULL DEFAULT 1 COMMENT '升级动作:1-加签通知,2-转办,3-仅通知',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用:0-禁用,1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_rule_code (rule_code, tenant_id),
    KEY idx_process_node (process_key, node_id),
    KEY idx_escalate_level (escalate_level),
    KEY idx_enabled (enabled),
    KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程升级规则表';

-- 3. 创建升级历史表 wf_escalation_history
CREATE TABLE IF NOT EXISTS wf_escalation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    rule_id BIGINT COMMENT '触发的升级规则ID',
    escalate_level INT NOT NULL COMMENT '升级级别',
    escalate_type TINYINT NOT NULL COMMENT '升级目标类型:1-部门主管,2-指定角色,3-指定用户,4-管理员',
    escalate_target VARCHAR(512) COMMENT '升级目标值',
    escalate_action TINYINT NOT NULL COMMENT '升级动作:1-加签通知,2-转办,3-仅通知',
    from_user_id BIGINT COMMENT '原审批人ID',
    to_user_ids VARCHAR(1024) COMMENT '升级目标用户ID列表(逗号分隔)',
    trigger_time DATETIME NOT NULL COMMENT '触发时间',
    trigger_type TINYINT NOT NULL DEFAULT 1 COMMENT '触发类型:1-超时自动,2-手动触发',
    remark VARCHAR(500) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_instance (instance_id),
    KEY idx_task (task_id),
    KEY idx_rule (rule_id),
    KEY idx_trigger_time (trigger_time),
    KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程升级历史表';

-- 4. 添加索引
CREATE INDEX IF NOT EXISTS idx_timeout_remind_instance ON wf_timeout_remind(instance_id);
CREATE INDEX IF NOT EXISTS idx_timeout_remind_task ON wf_timeout_remind(task_id);
CREATE INDEX IF NOT EXISTS idx_timeout_remind_remind_time ON wf_timeout_remind(remind_time);
CREATE INDEX IF NOT EXISTS idx_timeout_remind_tenant ON wf_timeout_remind(tenant_id);

-- 5. 初始化默认升级规则
INSERT IGNORE INTO wf_escalation_rule (rule_name, rule_code, escalate_level, timeout_hours, escalate_type, escalate_action, enabled, sort_order) VALUES
('一级升级-转交主管', 'ESCALATE_LEVEL_1', 1, 48, 1, 2, 1, 1),
('二级升级-通知管理员', 'ESCALATE_LEVEL_2', 2, 72, 4, 3, 1, 2);

-- ============================================================
-- 升级完成
-- ============================================================
