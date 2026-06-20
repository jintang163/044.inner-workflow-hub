-- ============================================================
-- 审批单草稿与临时保存功能升级脚本 (2026-06-20)
-- 包含：草稿增强 + 自动保存 + 定时清理
-- 执行前请先备份数据库
-- ============================================================

-- 1.1 修改 wf_form_draft 表 - 增加审批草稿相关字段
ALTER TABLE wf_form_draft
    ADD COLUMN IF NOT EXISTS process_version_id BIGINT COMMENT '流程版本ID' AFTER process_key,
    ADD COLUMN IF NOT EXISTS process_name VARCHAR(128) COMMENT '流程名称' AFTER process_version_id,
    ADD COLUMN IF NOT EXISTS title VARCHAR(256) COMMENT '草稿标题' AFTER process_name,
    ADD COLUMN IF NOT EXISTS form_definition_id BIGINT COMMENT '表单定义ID' AFTER title,
    ADD COLUMN IF NOT EXISTS draft_status TINYINT DEFAULT 1 COMMENT '草稿状态:1-手动保存,2-自动保存' AFTER form_data,
    ADD COLUMN IF NOT EXISTS last_auto_save_time DATETIME COMMENT '最后自动保存时间' AFTER draft_status,
    ADD COLUMN IF NOT EXISTS auto_save_count INT DEFAULT 0 COMMENT '自动保存次数' AFTER last_auto_save_time,
    ADD COLUMN IF NOT EXISTS attachment_ids VARCHAR(1024) COMMENT '附件ID列表(逗号分隔)' AFTER auto_save_count,
    ADD COLUMN IF NOT EXISTS cc_user_ids VARCHAR(1024) COMMENT '抄送人ID列表(逗号分隔)' AFTER attachment_ids,
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT COMMENT '租户ID' AFTER cc_user_ids,
    ADD COLUMN IF NOT EXISTS create_by BIGINT COMMENT '创建人' AFTER tenant_id,
    ADD COLUMN IF NOT EXISTS update_by BIGINT COMMENT '更新人' AFTER create_by;

-- 1.2 迁移已有的 creator_id 到 create_by
UPDATE wf_form_draft SET create_by = creator_id WHERE create_by IS NULL;

-- 1.3 迁移 form_id 到 form_definition_id (兼容)
UPDATE wf_form_draft SET form_definition_id = form_id WHERE form_definition_id IS NULL;

-- 1.4 添加索引
CREATE INDEX IF NOT EXISTS idx_draft_creator_process ON wf_form_draft(creator_id, process_key);
CREATE INDEX IF NOT EXISTS idx_draft_status ON wf_form_draft(draft_status);
CREATE INDEX IF NOT EXISTS idx_draft_update_time ON wf_form_draft(update_time);
CREATE INDEX IF NOT EXISTS idx_draft_tenant ON wf_form_draft(tenant_id);

-- ============================================================
-- 升级完成
-- ============================================================
