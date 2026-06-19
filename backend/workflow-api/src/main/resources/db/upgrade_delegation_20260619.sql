-- 委托代理与转审功能 - 数据库变更脚本
-- 执行日期: 2026-06-19

-- 1. 创建委托关系表
CREATE TABLE IF NOT EXISTS wf_delegation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    delegator_id BIGINT NOT NULL COMMENT '委托人ID',
    delegator_name VARCHAR(100) COMMENT '委托人姓名',
    delegatee_id BIGINT NOT NULL COMMENT '代理人ID',
    delegatee_name VARCHAR(100) COMMENT '代理人姓名',
    start_time DATETIME NOT NULL COMMENT '委托开始时间',
    end_time DATETIME NOT NULL COMMENT '委托结束时间',
    delegation_reason VARCHAR(500) COMMENT '委托原因',
    delegation_status INT DEFAULT 0 COMMENT '委托状态:0-待生效,1-生效中,2-已过期,3-已撤销',
    process_keys TEXT COMMENT '指定流程key(逗号分隔,为空表示全部流程',
    remark VARCHAR(500) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    INDEX idx_delegator_id (delegator_id),
    INDEX idx_delegatee_id (delegatee_id),
    INDEX idx_delegation_status (delegation_status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委托代理关系表';

-- 2. 创建转审记录表
CREATE TABLE IF NOT EXISTS wf_transfer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    instance_id BIGINT COMMENT '流程实例ID',
    instance_no VARCHAR(100) COMMENT '流程编号',
    task_id BIGINT COMMENT '任务ID',
    flowable_task_id VARCHAR(100) COMMENT 'Flowable任务ID',
    node_id VARCHAR(100) COMMENT '节点ID',
    node_name VARCHAR(200) COMMENT '节点名称',
    transfer_type INT COMMENT '转审类型:1-手动转审,2-委托自动转审,3-批量转审',
    source_user_id BIGINT COMMENT '转出人ID',
    source_user_name VARCHAR(100) COMMENT '转出人姓名',
    target_user_id BIGINT COMMENT '转入人ID',
    target_user_name VARCHAR(100) COMMENT '转入人姓名',
    transfer_reason VARCHAR(500) COMMENT '转审原因',
    delegation_id BIGINT COMMENT '关联委托ID(委托转审时使用)',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME COMMENT '创建时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    INDEX idx_instance_id (instance_id),
    INDEX idx_task_id (task_id),
    INDEX idx_source_user_id (source_user_id),
    INDEX idx_target_user_id (target_user_id),
    INDEX idx_transfer_type (transfer_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转审记录表';

-- 3. 为审批历史表增加委托关联字段
ALTER TABLE wf_approval_history
    ADD COLUMN delegation_id BIGINT COMMENT '关联委托ID' AFTER target_node_name,
    ADD COLUMN transfer_type INT COMMENT '转审类型:1-手动转审,2-委托自动转审,3-批量转审' AFTER delegation_id;

-- 4. 添加通知消息模板
-- 委托生效通知
INSERT INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
VALUES (
    'DELEGATION_START',
    '委托生效通知',
    'DELEGATION_START',
    '["DINGTALK","WECOM","EMAIL"]',
    '【委托通知】您有新的委托任务',
    '<p>您好，${delegateeUserName}：</p>
<p>${delegatorUserName} 已将审批任务委托给您，委托详情如下：</p>
<ul>
<li><strong>委托人：</strong>${delegatorUserName}</li>
<li><strong>委托开始时间：</strong>${startTime}</li>
<li><strong>委托结束时间：</strong>${endTime}</li>
<li><strong>委托原因：</strong>${delegationReason}</li>
</ul>
<p>委托期间的待办任务将自动转移给您处理。</p>',
    1,
    NOW(),
    NOW()
);

-- 委托到期提醒
INSERT INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
VALUES (
    'DELEGATION_END',
    '委托到期通知',
    'DELEGATION_END',
    '["DINGTALK","WECOM","EMAIL"]',
    '【委托通知】委托即将到期',
    '<p>您好，${delegatorUserName}：</p>
<p>您设置的委托代理即将到期，详情如下：</p>
<ul>
<li><strong>代理人：</strong>${delegateeUserName}</li>
<li><strong>委托开始时间：</strong>${startTime}</li>
<li><strong>委托结束时间：</strong>${endTime}</li>
</ul>
<p>到期后待办任务将自动恢复到您的账户，请及时处理。</p>',
    1,
    NOW(),
    NOW()
);

-- 转审通知
INSERT INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
VALUES (
    'TASK_TRANSFER',
    '任务转审通知',
    'TASK_TRANSFER',
    '["DINGTALK","WECOM","EMAIL"]',
    '【转审通知】您有新的转审任务',
    '<p>您好，${targetUserName}：</p>
<p>${sourceUserName} 将一项审批任务转审给您，请及时处理：</p>
<ul>
<li><strong>流程标题：</strong>${processTitle}</li>
<li><strong>流程编号：</strong>${instanceNo}</li>
<li><strong>当前节点：</strong>${nodeName}</li>
<li><strong>转审原因：</strong>${transferReason}</li>
</ul>
<p><a href="${detailUrl}">点击查看详情</a></p>',
    1,
    NOW(),
    NOW()
);
