-- ============================================================
-- 红头文件生成模块升级脚本 (2026-06-20)
-- 包含：红头模板表 + 红头文件生成记录表 + 电子印章配置表
-- 技术栈：Apache POI + iText + 国密签章
-- ============================================================

-- 1. 红头文件模板表
CREATE TABLE IF NOT EXISTS wf_redoc_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_code VARCHAR(128) NOT NULL COMMENT '模板编码(唯一)',
    template_name VARCHAR(256) NOT NULL COMMENT '模板名称',
    category VARCHAR(64) DEFAULT 'general' COMMENT '模板分类: general/finance/personnel/official',
    process_key VARCHAR(128) COMMENT '绑定流程key(可选)',
    template_file_id BIGINT COMMENT 'Word模板文件ID(关联wf_attachment)',
    template_file_name VARCHAR(256) COMMENT '模板文件名称(冗余)',
    template_storage_path VARCHAR(1024) COMMENT '模板文件存储路径(冗余)',
    header_color VARCHAR(16) DEFAULT '#c00000' COMMENT '红头标题颜色',
    header_font_size INT DEFAULT 22 COMMENT '红头标题字号',
    paper_size VARCHAR(16) DEFAULT 'A4' COMMENT '纸张规格: A4/A3',
    orientation TINYINT DEFAULT 1 COMMENT '纸张方向: 1-纵向, 2-横向',
    top_margin DOUBLE DEFAULT 2.54 COMMENT '上边距(厘米)',
    bottom_margin DOUBLE DEFAULT 2.54 COMMENT '下边距(厘米)',
    left_margin DOUBLE DEFAULT 3.17 COMMENT '左边距(厘米)',
    right_margin DOUBLE DEFAULT 3.17 COMMENT '右边距(厘米)',
    seal_enabled TINYINT DEFAULT 1 COMMENT '是否启用印章:0-否,1-是',
    seal_id BIGINT COMMENT '默认印章配置ID',
    seal_position_type TINYINT DEFAULT 1 COMMENT '印章位置类型:1-右下角,2-签字位置,3-自定义坐标',
    seal_offset_x DOUBLE DEFAULT 2.0 COMMENT '印章自定义X偏移(厘米)',
    seal_offset_y DOUBLE DEFAULT 4.0 COMMENT '印章自定义Y偏移(厘米)',
    seal_scale DOUBLE DEFAULT 0.8 COMMENT '印章缩放比例 0.1-2.0',
    signature_enabled TINYINT DEFAULT 0 COMMENT '是否启用国密数字签名:0-否,1-是',
    signature_cert_id BIGINT COMMENT '数字证书ID(关联wf_seal_config)',
    auto_generate TINYINT DEFAULT 0 COMMENT '是否审批完成自动生成:0-否,1-是',
    output_format TINYINT DEFAULT 2 COMMENT '默认输出格式:1-WORD,2-PDF,3-BOTH',
    watermark_enabled TINYINT DEFAULT 0 COMMENT '是否启用水印:0-否,1-是',
    watermark_text VARCHAR(64) COMMENT '水印文字',
    watermark_color VARCHAR(16) DEFAULT '#d9d9d9' COMMENT '水印颜色',
    placeholder_sample TEXT COMMENT '占位符示例(JSON)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(512) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code, tenant_id, is_deleted),
    KEY idx_process_key (process_key),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='红头文件模板表';

-- 2. 红头文件生成记录表
CREATE TABLE IF NOT EXISTS wf_redoc_generated (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    instance_no VARCHAR(64) NOT NULL COMMENT '审批单号',
    process_key VARCHAR(128) COMMENT '流程key',
    task_id BIGINT COMMENT '关联任务ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    template_code VARCHAR(128) COMMENT '模板编码(冗余)',
    template_name VARCHAR(256) COMMENT '模板名称(冗余)',
    file_title VARCHAR(512) NOT NULL COMMENT '文件标题',
    approval_no VARCHAR(128) COMMENT '文号/审批编号',
    file_no VARCHAR(128) COMMENT '发文字号',
    output_format TINYINT NOT NULL COMMENT '输出格式:1-WORD,2-PDF,3-BOTH',
    word_file_id BIGINT COMMENT '生成的WORD文件ID',
    word_file_name VARCHAR(256) COMMENT 'WORD文件名',
    word_file_size BIGINT COMMENT 'WORD文件大小(字节)',
    pdf_file_id BIGINT COMMENT '生成的PDF文件ID',
    pdf_file_name VARCHAR(256) COMMENT 'PDF文件名',
    pdf_file_size BIGINT COMMENT 'PDF文件大小(字节)',
    seal_applied TINYINT DEFAULT 0 COMMENT '是否已盖印章:0-否,1-是',
    seal_id BIGINT COMMENT '使用的印章ID',
    signature_applied TINYINT DEFAULT 0 COMMENT '是否已数字签名:0-否,1-是',
    signature_cert_id BIGINT COMMENT '使用的数字证书ID',
    generate_time DATETIME COMMENT '生成时间',
    generate_by BIGINT COMMENT '生成人ID',
    generate_by_name VARCHAR(64) COMMENT '生成人姓名',
    placeholder_values TEXT COMMENT '占位符值(JSON快照)',
    print_count INT DEFAULT 0 COMMENT '打印次数',
    last_print_time DATETIME COMMENT '最近打印时间',
    last_print_by BIGINT COMMENT '最近打印人',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    last_download_time DATETIME COMMENT '最近下载时间',
    last_download_by BIGINT COMMENT '最近下载人',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-已作废,1-有效',
    remark VARCHAR(512) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    KEY idx_instance_no (instance_no),
    KEY idx_template_id (template_id),
    KEY idx_generate_time (generate_time),
    KEY idx_tenant_id (tenant_id),
    KEY idx_generate_by (generate_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='红头文件生成记录表';

-- 3. 电子印章配置表
CREATE TABLE IF NOT EXISTS wf_seal_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    seal_code VARCHAR(128) NOT NULL COMMENT '印章编码(唯一)',
    seal_name VARCHAR(128) NOT NULL COMMENT '印章名称(如:公司公章/财务专用章)',
    seal_type TINYINT NOT NULL DEFAULT 1 COMMENT '印章类型:1-单位公章,2-财务章,3-合同章,4-法人章,5-部门章,6-个人名章',
    seal_owner_id BIGINT COMMENT '印章所属单位/部门ID',
    seal_owner_name VARCHAR(256) COMMENT '印章所属单位/部门名称',
    seal_image_id BIGINT COMMENT '印章图片文件ID(关联wf_attachment)',
    seal_image_url VARCHAR(1024) COMMENT '印章图片URL(冗余)',
    seal_text VARCHAR(128) COMMENT '印章文字(自动生成图片时使用)',
    seal_shape TINYINT DEFAULT 1 COMMENT '印章形状:1-圆形,2-椭圆,3-方形',
    seal_diameter INT DEFAULT 40 COMMENT '印章直径(毫米)',
    seal_color VARCHAR(16) DEFAULT '#c00000' COMMENT '印章颜色',
    digital_cert_id BIGINT COMMENT '关联数字证书ID(国密)',
    digital_cert_alias VARCHAR(128) COMMENT '数字证书别名',
    cert_password VARCHAR(128) COMMENT '证书密码(生产环境建议加密存储)',
    signature_algorithm VARCHAR(32) DEFAULT 'SM3withSM2' COMMENT '签名算法: SM3withSM2/SHA256withRSA',
    keep_certificate TINYINT DEFAULT 1 COMMENT '签名时是否嵌入证书:0-否,1-是',
    timestamp_enabled TINYINT DEFAULT 0 COMMENT '是否加时间戳:0-否,1-是',
    timestamp_url VARCHAR(512) COMMENT '时间戳服务URL',
    allowed_user_ids VARCHAR(2048) COMMENT '允许使用的用户ID列表(逗号分隔,空=全部)',
    allowed_dept_ids VARCHAR(2048) COMMENT '允许使用的部门ID列表(逗号分隔,空=全部)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(512) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识:0-未删除,1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seal_code (seal_code, tenant_id, is_deleted),
    KEY idx_seal_type (seal_type),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子印章配置表';

-- 4. 初始红头模板数据
INSERT INTO wf_redoc_template
(template_code, template_name, category, header_color, header_font_size,
 seal_enabled, seal_position_type, seal_scale, output_format,
 paper_size, orientation, status, remark)
VALUES
('general_approval', '通用审批红头', 'general', '#c00000', 22, 1, 1, 0.8, 2, 'A4', 1, 1, '通用审批单红头模板'),
('finance_payment', '财务付款红头', 'finance', '#c00000', 20, 1, 1, 0.8, 2, 'A4', 1, 1, '财务付款审批红头模板'),
('hr_official', '人事任免红头', 'personnel', '#c00000', 22, 1, 1, 0.9, 2, 'A4', 1, 1, '人事任免正式公文模板'),
('contract_official', '合同审批红头', 'official', '#c00000', 18, 1, 2, 0.95, 3, 'A4', 1, 1, '合同类审批红头模板(同时输出WORD和PDF)');

-- 5. 初始印章配置
INSERT INTO wf_seal_config
(seal_code, seal_name, seal_type, seal_text, seal_shape, seal_diameter,
 seal_color, signature_algorithm, status, remark)
VALUES
('official_seal', '公司公章', 1, '北京示例科技有限公司', 1, 42, '#c00000', 'SM3withSM2', 1, '公司对外公文通用印章'),
('finance_seal', '财务专用章', 2, '北京示例科技有限公司财务专用章', 1, 38, '#c00000', 'SM3withSM2', 1, '财务单据专用印章'),
('contract_seal', '合同专用章', 3, '北京示例科技有限公司合同专用章', 1, 40, '#c00000', 'SM3withSM2', 1, '合同签署专用印章'),
('legal_seal', '法定代表人章', 4, '张三印', 3, 22, '#c00000', 'SM3withSM2', 1, '法定代表人个人名章');

-- ============================================================
-- 升级完成
-- ============================================================
