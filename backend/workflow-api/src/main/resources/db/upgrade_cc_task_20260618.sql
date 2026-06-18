-- 抄送功能增强 - 数据库变更脚本
-- 执行日期: 2026-06-18

-- 1. 修改 wf_cc_task 表结构
ALTER TABLE wf_cc_task
    ADD COLUMN cc_type INT COMMENT '抄送类型:1-手动抄送,2-节点启动自动抄送,3-节点完成自动抄送,4-流程结束自动抄送' AFTER node_name,
    ADD COLUMN remind_count INT DEFAULT 0 COMMENT '催读次数' AFTER cc_time,
    ADD COLUMN last_remind_time DATETIME COMMENT '最后催读时间' AFTER remind_count,
    ADD COLUMN detail_url VARCHAR(500) COMMENT '审批单详情链接' AFTER last_remind_time,
    ADD COLUMN tenant_id BIGINT COMMENT '租户ID' AFTER detail_url,
    ADD COLUMN create_by BIGINT COMMENT '创建人' AFTER tenant_id,
    ADD COLUMN update_by BIGINT COMMENT '更新人' AFTER create_by,
    ADD COLUMN update_time DATETIME COMMENT '更新时间' AFTER create_time,
    ADD COLUMN is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除' AFTER update_time;

-- 2. 更新现有数据的默认值
UPDATE wf_cc_task SET cc_type = 1 WHERE cc_type IS NULL;
UPDATE wf_cc_task SET remind_count = 0 WHERE remind_count IS NULL;
UPDATE wf_cc_task SET is_deleted = 0 WHERE is_deleted IS NULL;

-- 3. 添加索引
CREATE INDEX idx_cc_user_id ON wf_cc_task(cc_user_id);
CREATE INDEX idx_instance_id ON wf_cc_task(instance_id);
CREATE INDEX idx_is_read ON wf_cc_task(is_read);
CREATE INDEX idx_cc_time ON wf_cc_task(cc_time);

-- 4. 为流程版本表添加全局通知配置字段（用于流程结束自动抄送等）
ALTER TABLE wf_process_version
    ADD COLUMN global_notify_config TEXT COMMENT '全局通知配置(JSON格式，包含流程结束抄送等配置)' AFTER suspend_status;

-- 5. 为通知模块添加抄送相关的事件类型消息模板
-- 抄送通知模板
INSERT INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
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

-- 抄送催读模板
INSERT INTO wf_message_template (template_code, template_name, event_type, channel_types, email_subject_template, email_content_template, status, create_time, update_time)
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
