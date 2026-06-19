-- ============================================================
-- 全功能数据库统一升级脚本 (2026-06-19)
-- 包含：子流程调用 + 抄送增强 + 委托转审 + 会签或签
-- 执行前请先备份数据库
-- ============================================================

-- ============================================================
-- 一、子流程调用功能 (upgrade_call_activity_20260618)
-- ============================================================

-- 1.1 新增 wf_node_config 表字段 - 子流程调用配置
ALTER TABLE wf_node_config
    ADD COLUMN IF NOT EXISTS call_activity_process_key VARCHAR(128) COMMENT '子流程调用-流程Key' AFTER parallel_reject_strategy,
    ADD COLUMN IF NOT EXISTS input_variable_mapping JSON COMMENT '输入变量映射（主->子）' AFTER call_activity_process_key,
    ADD COLUMN IF NOT EXISTS output_variable_mapping JSON COMMENT '输出变量映射（子->主）' AFTER input_variable_mapping;

-- 1.2 创建主子流程实例关联表
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


-- ============================================================
-- 二、抄送功能增强 (upgrade_cc_task_20260618)
-- ============================================================

-- 2.1 修改 wf_cc_task 表结构
ALTER TABLE wf_cc_task
    ADD COLUMN IF NOT EXISTS cc_type INT COMMENT '抄送类型:1-手动抄送,2-节点启动自动抄送,3-节点完成自动抄送,4-流程结束自动抄送' AFTER node_name,
    ADD COLUMN IF NOT EXISTS remind_count INT DEFAULT 0 COMMENT '催读次数' AFTER cc_time,
    ADD COLUMN IF NOT EXISTS last_remind_time DATETIME COMMENT '最后催读时间' AFTER remind_count,
    ADD COLUMN IF NOT EXISTS detail_url VARCHAR(500) COMMENT '审批单详情链接' AFTER last_remind_time,
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT COMMENT '租户ID' AFTER detail_url,
    ADD COLUMN IF NOT EXISTS create_by BIGINT COMMENT '创建人' AFTER tenant_id,
    ADD COLUMN IF NOT EXISTS update_by BIGINT COMMENT '更新人' AFTER create_by,
    ADD COLUMN IF NOT EXISTS update_time DATETIME COMMENT '更新时间' AFTER create_time,
    ADD COLUMN IF NOT EXISTS is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除' AFTER update_time;

-- 2.2 更新现有数据的默认值
UPDATE wf_cc_task SET cc_type = 1 WHERE cc_type IS NULL;
UPDATE wf_cc_task SET remind_count = 0 WHERE remind_count IS NULL;
UPDATE wf_cc_task SET is_deleted = 0 WHERE is_deleted IS NULL;

-- 2.3 添加索引
CREATE INDEX IF NOT EXISTS idx_cc_user_id ON wf_cc_task(cc_user_id);
CREATE INDEX IF NOT EXISTS idx_instance_id ON wf_cc_task(instance_id);
CREATE INDEX IF NOT EXISTS idx_is_read ON wf_cc_task(is_read);
CREATE INDEX IF NOT EXISTS idx_cc_time ON wf_cc_task(cc_time);

-- 2.4 为流程版本表添加全局通知配置字段
ALTER TABLE wf_process_version
    ADD COLUMN IF NOT EXISTS global_notify_config TEXT COMMENT '全局通知配置(JSON格式，包含流程结束抄送等配置)' AFTER suspend_status;

-- 2.5 为通知模块添加抄送相关的事件类型消息模板（使用 INSERT IGNORE 避免重复）
INSERT IGNORE INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
VALUES (
    'CC_NOTIFY',
    '抄送通知',
    'CC_NOTIFY',
    '["DINGTALK","WECOM","EMAIL"]',
    '【抄送通知】${processTitle}',
    '<p>您好，${receiverUserName}：</p>
<p>您有一条新的抄送消息，请知悉：</p>
<ul>
<li><strong>流程标题：</strong>${processTitle}</li>
<li><strong>流程编号：</strong>${instanceNo}</li>
<li><strong>发起人：</strong>${startUserName}</li>
<li><strong>抄送节点：</strong>${nodeName}</li>
</ul>
<p><a href="${detailUrl}">点击查看详情</a></p>',
    1,
    NOW(),
    NOW()
);

INSERT IGNORE INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
VALUES (
    'CC_REMIND',
    '抄送催读',
    'CC_REMIND',
    '["DINGTALK","WECOM","EMAIL"]',
    '【催读提醒】${processTitle}',
    '<p>您好，${receiverUserName}：</p>
<p>发起人 ${startUserName} 提醒您查看以下抄送消息：</p>
<ul>
<li><strong>流程标题：</strong>${processTitle}</li>
<li><strong>流程编号：</strong>${instanceNo}</li>
<li><strong>催读备注：</strong>${remark!}</li>
</ul>
<p><a href="${detailUrl}">点击立即查看</a></p>',
    1,
    NOW(),
    NOW()
);


-- ============================================================
-- 三、委托代理与转审功能 (upgrade_delegation_20260619)
-- ============================================================

-- 3.1 创建委托关系表
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
    process_keys TEXT COMMENT '指定流程key(逗号分隔,为空表示全部流程)',
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

-- 3.2 创建转审记录表
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

-- 3.3 为审批历史表增加委托关联字段
ALTER TABLE wf_approval_history
    ADD COLUMN IF NOT EXISTS delegation_id BIGINT COMMENT '关联委托ID' AFTER target_node_name,
    ADD COLUMN IF NOT EXISTS transfer_type INT COMMENT '转审类型:1-手动转审,2-委托自动转审,3-批量转审' AFTER delegation_id;

-- 3.4 添加通知消息模板（使用 INSERT IGNORE 避免重复）
INSERT IGNORE INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
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

INSERT IGNORE INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
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

INSERT IGNORE INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
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


-- ============================================================
-- 四、会签或签功能 (upgrade_multi_instance_sign_20260619)
-- ============================================================

-- 4.1 新增 wf_node_config 表字段 - 会签配置
ALTER TABLE wf_node_config
    ADD COLUMN IF NOT EXISTS multi_instance_completion_type INT DEFAULT NULL COMMENT '多实例完成条件类型:1-全部通过,2-任一通过,3-百分比通过' AFTER parallel_reject_strategy,
    ADD COLUMN IF NOT EXISTS pass_percentage INT DEFAULT NULL COMMENT '通过百分比阈值(0-100)' AFTER multi_instance_completion_type,
    ADD COLUMN IF NOT EXISTS veto_enabled INT DEFAULT 0 COMMENT '是否启用一票否决:0-否,1-是' AFTER pass_percentage;

-- 4.2 会签/或签功能消息模板（可选，使用 INSERT IGNORE 避免重复）
INSERT IGNORE INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
VALUES (
    'MULTI_SIGN_COMPLETE',
    '会签节点完成通知',
    'MULTI_SIGN_COMPLETE',
    '["DINGTALK","WECOM","EMAIL"]',
    '【会签通知】${nodeName} 已完成会签',
    '<p>您好，${startUserName}：</p>
<p>会签节点 <strong>${nodeName}</strong> 已完成审批，详情如下：</p>
<ul>
<li><strong>流程标题：</strong>${processTitle}</li>
<li><strong>流程编号：</strong>${instanceNo}</li>
<li><strong>通过人数：</strong>${approvedCount}/${totalSigners}</li>
<li><strong>拒绝人数：</strong>${rejectedCount}</li>
<li><strong>会签结果：</strong>${signResult}</li>
</ul>
<p><a href="${detailUrl}">点击查看详情</a></p>',
    1,
    NOW(),
    NOW()
);


-- ============================================================
-- 升级完成
-- ============================================================
-- 执行说明：
-- 1. 本脚本使用 IF NOT EXISTS / INSERT IGNORE 避免重复执行报错
-- 2. 适用于 MySQL 5.7+ / MySQL 8.0+
-- 3. 执行完毕后请重启后端服务
-- ============================================================
