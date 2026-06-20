-- ============================================================
-- 附件管理增强升级脚本 (2026-06-20)
-- 包含：MinIO存储 + 附件权限联动 + 预览/批量下载
-- 执行前请先备份数据库
-- ============================================================

-- 1.1 修改 wf_attachment 表 - 增加节点关联和存储增强字段
ALTER TABLE wf_attachment
    ADD COLUMN IF NOT EXISTS node_id VARCHAR(64) COMMENT '关联节点ID(附件权限联动)' AFTER biz_id,
    ADD COLUMN IF NOT EXISTS bucket_name VARCHAR(128) COMMENT 'MinIO Bucket名称' AFTER storage_type,
    ADD COLUMN IF NOT EXISTS object_name VARCHAR(512) COMMENT 'MinIO对象名称' AFTER bucket_name,
    ADD COLUMN IF NOT EXISTS preview_url VARCHAR(1024) COMMENT '预览地址(预签名URL)' AFTER access_url,
    ADD COLUMN IF NOT EXISTS download_url VARCHAR(1024) COMMENT '下载地址(预签名URL)' AFTER preview_url,
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT COMMENT '租户ID' AFTER download_url,
    ADD COLUMN IF NOT EXISTS create_by BIGINT COMMENT '创建人' AFTER tenant_id,
    ADD COLUMN IF NOT EXISTS update_by BIGINT COMMENT '更新人' AFTER create_by,
    ADD COLUMN IF NOT EXISTS update_time DATETIME COMMENT '更新时间' AFTER create_time;

-- 1.2 添加索引
CREATE INDEX IF NOT EXISTS idx_attachment_biz ON wf_attachment(biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_attachment_node_id ON wf_attachment(node_id);
CREATE INDEX IF NOT EXISTS idx_attachment_upload_user ON wf_attachment(upload_user_id);
CREATE INDEX IF NOT EXISTS idx_attachment_bucket ON wf_attachment(bucket_name);

-- 1.3 创建附件权限配置表
CREATE TABLE IF NOT EXISTS wf_attachment_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    process_version_id BIGINT NOT NULL COMMENT '流程版本ID',
    node_id VARCHAR(64) NOT NULL COMMENT '节点ID',
    node_name VARCHAR(128) COMMENT '节点名称',
    attachment_visible TINYINT DEFAULT 1 COMMENT '附件是否可见:0-隐藏,1-可见',
    attachment_editable TINYINT DEFAULT 1 COMMENT '附件是否可编辑(上传/删除):0-只读,1-可编辑',
    max_file_size BIGINT DEFAULT 104857600 COMMENT '最大文件大小(字节),默认100MB',
    allowed_types VARCHAR(1024) DEFAULT 'jpg,jpeg,png,gif,bmp,webp,pdf,doc,docx,xls,xlsx,ppt,pptx' COMMENT '允许的文件类型(逗号分隔)',
    max_file_count INT DEFAULT 20 COMMENT '最大附件数量',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_version_node (process_version_id, node_id, is_deleted),
    KEY idx_process_version_id (process_version_id),
    KEY idx_node_id (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件权限配置表(与节点表单权限联动)';

-- 1.4 更新已有附件数据 - 设置默认bucket和object_name
UPDATE wf_attachment SET bucket_name = 'workflow-attachments' WHERE bucket_name IS NULL AND storage_type = 1;
UPDATE wf_attachment SET object_name = storage_path WHERE object_name IS NULL AND storage_type = 1;

-- ============================================================
-- 升级完成
-- ============================================================
