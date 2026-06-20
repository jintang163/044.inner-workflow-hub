-- ============================================================
-- 审批意见模板升级脚本 (2026-06-20)
-- 包含：意见模板分类表 + 意见模板表
-- 支持个人模板、部门公共模板、全局模板
-- 执行前请先备份数据库
-- ============================================================

-- 1.1 创建意见模板分类表
CREATE TABLE IF NOT EXISTS wf_comment_template_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    category_name VARCHAR(128) NOT NULL COMMENT '分类名称',
    category_code VARCHAR(128) NOT NULL COMMENT '分类编码',
    scope_type TINYINT NOT NULL DEFAULT 0 COMMENT '适用范围:0-个人模板,1-部门公共模板,2-全局模板',
    dept_id BIGINT COMMENT '部门ID(scope_type=1时使用)',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(512) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    KEY idx_category_code (category_code),
    KEY idx_scope_type (scope_type),
    KEY idx_dept_id (dept_id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status),
    KEY idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意见模板分类表';

-- 1.2 创建意见模板表
CREATE TABLE IF NOT EXISTS wf_comment_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    template_content TEXT NOT NULL COMMENT '模板内容(审批意见文本)',
    scope_type TINYINT NOT NULL DEFAULT 0 COMMENT '适用范围:0-个人模板,1-部门公共模板,2-全局模板',
    dept_id BIGINT COMMENT '部门ID(scope_type=1时使用)',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    use_count INT DEFAULT 0 COMMENT '使用次数(用于热门排序)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(512) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_scope_type (scope_type),
    KEY idx_dept_id (dept_id),
    KEY idx_create_by (create_by),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status),
    KEY idx_sort (sort_order),
    KEY idx_use_count (use_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意见模板表';

-- 2.1 插入预置分类数据 - 全局分类
INSERT INTO wf_comment_template_category (category_name, category_code, scope_type, sort_order, status, remark, create_by) VALUES
('常用意见', 'common_opinion', 2, 1, 1, '全局常用审批意见分类', 1),
('同意类', 'agree_type', 2, 2, 1, '全局同意类意见分类', 1),
('驳回类', 'reject_type', 2, 3, 1, '全局驳回类意见分类', 1);

-- 2.2 插入预置模板数据 - 全局常用意见
INSERT INTO wf_comment_template (category_id, template_name, template_content, scope_type, sort_order, status, remark, create_by) VALUES
(1, '同意', '同意', 2, 1, 1, '快速同意', 1),
(1, '已核实', '已核实，情况属实。', 2, 2, 1, '核实通过', 1),
(1, '按规定办理', '按规定办理。', 2, 3, 1, '按规定办理', 1),
(2, '同意，流程规范', '同意，申请材料齐全，流程规范。', 2, 1, 1, '同意类-流程规范', 1),
(2, '同意，建议采纳', '同意，建议采纳。', 2, 2, 1, '同意类-建议采纳', 1),
(2, '同意，请后续跟进', '同意，请相关部门后续跟进落实。', 2, 3, 1, '同意类-后续跟进', 1),
(3, '驳回，材料不全', '驳回，申请材料不齐全，请补充后重新提交。', 2, 1, 1, '驳回类-材料不全', 1),
(3, '驳回，信息有误', '驳回，申请信息有误，请核实后重新提交。', 2, 2, 1, '驳回类-信息有误', 1),
(3, '驳回，不符合规定', '驳回，不符合相关规定，具体原因如下：', 2, 3, 1, '驳回类-不符合规定', 1);

-- ============================================================
-- 升级完成
-- ============================================================
