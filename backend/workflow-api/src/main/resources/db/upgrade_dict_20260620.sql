-- ============================================================
-- 数据字典与下拉选项源升级脚本 (2026-06-20)
-- 包含：字典类型表 + 字典数据表 + API数据源配置表
-- 支持级联选择、Redis缓存、WebSocket实时推送
-- 执行前请先备份数据库
-- ============================================================

-- 1.1 创建字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    dict_code VARCHAR(128) NOT NULL COMMENT '字典编码(唯一标识)',
    source_type TINYINT NOT NULL DEFAULT 0 COMMENT '数据来源:0-字典表静态数据,1-API动态获取,2-混合(字典表+API)',
    api_url VARCHAR(512) COMMENT 'API数据源地址(source_type=1或2时使用)',
    api_method VARCHAR(10) DEFAULT 'GET' COMMENT 'API请求方法:GET/POST',
    api_headers VARCHAR(1024) COMMENT 'API请求头(JSON格式)',
    api_params VARCHAR(1024) COMMENT 'API请求参数(JSON格式)',
    api_response_path VARCHAR(256) COMMENT 'API响应数据提取路径(如data.list)',
    cascade_field VARCHAR(128) COMMENT '级联关联字段(父级字典编码)',
    cascade_parent VARCHAR(128) COMMENT '父级字典编码(级联选择时使用)',
    cache_enabled TINYINT DEFAULT 1 COMMENT '是否启用缓存:0-否,1-是',
    cache_ttl INT DEFAULT 3600 COMMENT '缓存过期时间(秒),默认1小时',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(512) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code (dict_code, tenant_id, is_deleted),
    KEY idx_dict_code (dict_code),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status),
    KEY idx_cascade_parent (cascade_parent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

-- 1.2 创建字典数据表
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dict_code VARCHAR(128) NOT NULL COMMENT '所属字典编码',
    dict_label VARCHAR(256) NOT NULL COMMENT '字典标签(显示文本)',
    dict_value VARCHAR(256) NOT NULL COMMENT '字典值(实际值)',
    dict_sort INT DEFAULT 0 COMMENT '排序序号',
    color_tag VARCHAR(32) COMMENT '颜色标签(前端展示用)',
    parent_value VARCHAR(256) COMMENT '父级字典值(级联选择时使用)',
    css_class VARCHAR(128) COMMENT '样式类名',
    list_class VARCHAR(128) COMMENT '列表样式类名',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认:0-否,1-是',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(512) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    KEY idx_dict_code (dict_code),
    KEY idx_dict_value (dict_code, dict_value),
    KEY idx_parent_value (dict_code, parent_value),
    KEY idx_tenant_id (tenant_id),
    KEY idx_sort (dict_code, dict_sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

-- 1.3 创建API数据源配置表
CREATE TABLE IF NOT EXISTS wf_data_source_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    source_code VARCHAR(128) NOT NULL COMMENT '数据源编码(唯一标识)',
    source_name VARCHAR(128) NOT NULL COMMENT '数据源名称',
    source_type TINYINT NOT NULL DEFAULT 1 COMMENT '数据源类型:1-内部API,2-外部API,3-数据库查询',
    api_url VARCHAR(512) NOT NULL COMMENT 'API地址',
    api_method VARCHAR(10) DEFAULT 'GET' COMMENT '请求方法:GET/POST',
    api_headers VARCHAR(2048) COMMENT '请求头(JSON格式,支持模板变量)',
    api_body VARCHAR(2048) COMMENT '请求体(JSON格式,POST时使用,支持模板变量)',
    api_params_template VARCHAR(1024) COMMENT '请求参数模板(JSON格式,支持模板变量如${parentId})',
    response_path VARCHAR(256) COMMENT '响应数据提取路径(如data.list)',
    label_field VARCHAR(128) DEFAULT 'label' COMMENT '标签字段名',
    value_field VARCHAR(128) DEFAULT 'value' COMMENT '值字段名',
    children_field VARCHAR(128) DEFAULT 'children' COMMENT '子级字段名(级联数据)',
    cache_enabled TINYINT DEFAULT 1 COMMENT '是否启用缓存:0-否,1-是',
    cache_ttl INT DEFAULT 1800 COMMENT '缓存过期时间(秒),默认30分钟',
    timeout INT DEFAULT 5000 COMMENT '请求超时时间(毫秒)',
    retry_count INT DEFAULT 0 COMMENT '失败重试次数',
    auth_type TINYINT DEFAULT 0 COMMENT '认证类型:0-无,1-Bearer Token,2-Basic Auth,3-API Key',
    auth_config VARCHAR(1024) COMMENT '认证配置(JSON格式)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(512) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_code (source_code, tenant_id, is_deleted),
    KEY idx_source_code (source_code),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API数据源配置表';

-- 2.1 插入初始字典类型数据
INSERT INTO sys_dict_type (dict_name, dict_code, source_type, cache_enabled, status, remark) VALUES
('审批状态', 'approval_status', 0, 1, 1, '流程实例审批状态'),
('优先级', 'priority', 0, 1, 1, '流程优先级'),
('省市区', 'region', 0, 1, 1, '省市区级联选择'),
('性别', 'gender', 0, 1, 1, '性别选择'),
('成本中心', 'cost_center', 1, 1, 1, '成本中心-来自API动态获取');

-- 2.2 插入初始字典数据 - 审批状态
INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, color_tag, status) VALUES
('approval_status', '待审批', 'pending', 1, 'blue', 1),
('approval_status', '审批中', 'approving', 2, 'orange', 1),
('approval_status', '已通过', 'approved', 3, 'green', 1),
('approval_status', '已驳回', 'rejected', 4, 'red', 1),
('approval_status', '已撤回', 'withdrawn', 5, 'default', 1);

-- 2.3 插入初始字典数据 - 优先级
INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, color_tag, status) VALUES
('priority', '普通', 'normal', 1, 'default', 1),
('priority', '紧急', 'urgent', 2, 'orange', 1),
('priority', '特急', 'critical', 3, 'red', 1);

-- 2.4 插入初始字典数据 - 性别
INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, status) VALUES
('gender', '男', 'male', 1, 1),
('gender', '女', 'female', 2, 1);

-- 2.5 插入初始字典数据 - 省市区(示例级联数据)
INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, parent_value, status) VALUES
('region', '北京市', 'beijing', 1, NULL, 1),
('region', '上海市', 'shanghai', 2, NULL, 1),
('region', '广东省', 'guangdong', 3, NULL, 1),
('region', '朝阳区', 'chaoyang', 1, 'beijing', 1),
('region', '海淀区', 'haidian', 2, 'beijing', 1),
('region', '浦东新区', 'pudong', 1, 'shanghai', 1),
('region', '徐汇区', 'xuhui', 2, 'shanghai', 1),
('region', '广州市', 'guangzhou', 1, 'guangdong', 1),
('region', '深圳市', 'shenzhen', 2, 'guangdong', 1);

-- 2.6 插入初始字典类型 - 扩展(API来源)
INSERT INTO sys_dict_type (dict_name, dict_code, source_type, cache_enabled, cache_ttl, status, remark) VALUES
('员工列表', 'employee', 1, 1, 1800, 1, '员工列表-来自userApi动态获取'),
('部门列表', 'dept', 1, 1, 3600, 1, '部门列表-来自deptApi动态获取'),
('成本中心', 'cost_center', 1, 1, 1800, 1, '成本中心列表-来自HR系统API'),
('供应商列表', 'supplier', 1, 1, 1800, 1, '供应商列表-来自ERP系统API'),
('客户列表', 'customer', 1, 1, 1800, 1, '客户列表-来自CRM系统API'),
('预算科目', 'budget_subject', 1, 1, 3600, 1, '预算科目-来自预算系统API');

-- ============================================================
-- 3. 插入预置API数据源配置(员工/成本中心等)
-- ============================================================

-- 3.1 员工列表数据源配置 - 调用内部用户API /api/user/list
INSERT INTO wf_data_source_config
(source_code, source_name, source_type, api_url, api_method, api_params_template,
 response_path, label_field, value_field,
 cache_enabled, cache_ttl, timeout, auth_type, status, remark)
VALUES
('employee', '员工列表(HR接口)', 1, '/api/user/list', 'GET', 'pageSize=9999',
 'data.list', 'nickname', 'id',
  1, 1800, 5000, 0, 1, '员工选择器专用-拉取全部在职员工');

-- 3.2 部门列表数据源配置
INSERT INTO wf_data_source_config
(source_code, source_name, source_type, api_url, api_method,
 response_path, label_field, value_field, children_field,
 cache_enabled, cache_ttl, timeout, auth_type, status, remark)
VALUES
('dept', '部门列表(HR接口)', 1, '/api/dept/tree', 'GET',
 'data', 'deptName', 'id', 'children',
 1, 3600, 5000, 0, 1, '部门树-含下级部门嵌套');

-- 3.3 成本中心数据源配置
INSERT INTO wf_data_source_config
(source_code, source_name, source_type, api_url, api_method, api_headers, api_params_template,
 response_path, label_field, value_field,
 cache_enabled, cache_ttl, timeout, auth_type, status, remark)
VALUES
('cost_center', '成本中心(HR接口)', 1, '/api/hr/cost-center/list', 'GET', '{"X-Service":"hr-service"}', 'status=active',
 'data.items', 'centerName', 'centerCode',
  1, 1800, 5000, 0, 1, '所有启用状态的成本中心');

-- 3.4 供应商数据源配置
INSERT INTO wf_data_source_config
(source_code, source_name, source_type, api_url, api_method, api_headers,
 response_path, label_field, value_field,
 cache_enabled, cache_ttl, timeout, auth_type, status, remark)
VALUES
('supplier', '供应商列表(ERP接口)', 1, '/api/erp/supplier/list', 'GET', '{"X-Service":"erp-service"}',
 'data.list', 'supplierName', 'supplierId',
  1, 1800, 8000, 0, 1, '合格供应商名录');

-- 3.5 客户数据源配置
INSERT INTO wf_data_source_config
(source_code, source_name, source_type, api_url, api_method, api_headers,
 response_path, label_field, value_field,
 cache_enabled, cache_ttl, timeout, auth_type, status, remark)
VALUES
('customer', '客户列表(CRM接口)', 1, '/api/crm/customer/list', 'GET', '{"X-Service":"crm-service"}',
 'data.records', 'customerName', 'customerId',
  1, 1800, 8000, 0, 1, '签约客户名录');

-- 3.6 预算科目数据源配置
INSERT INTO wf_data_source_config
(source_code, source_name, source_type, api_url, api_method, api_params_template,
 response_path, label_field, value_field, children_field,
 cache_enabled, cache_ttl, timeout, auth_type, status, remark)
VALUES
('budget_subject', '预算科目(预算系统)', 1, '/api/budget/subject/tree', 'GET', 'fiscalYear=${currentYear}',
 'data', 'subjectName', 'subjectCode', 'children',
  1, 3600, 5000, 0, 1, '当前会计年度预算科目树');

-- ============================================================
-- 升级完成
-- ============================================================
-- ============================================================
-- 升级完成
-- ============================================================
