-- ============================================================
-- 审批人休假自动感知功能升级脚本 (2026-06-20)
-- 包含：用户休假记录表 + 代理人配置表
-- 执行前请先备份数据库
-- ============================================================

-- 1. 创建用户休假记录表 wf_user_vacation
CREATE TABLE IF NOT EXISTS wf_user_vacation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(64) COMMENT '用户姓名',
    vacation_type TINYINT NOT NULL DEFAULT 1 COMMENT '休假类型:1-年假,2-事假,3-病假,4-出差,5-调休,6-其他',
    vacation_title VARCHAR(256) COMMENT '休假标题',
    start_time DATETIME NOT NULL COMMENT '休假开始时间',
    end_time DATETIME NOT NULL COMMENT '休假结束时间',
    full_day TINYINT DEFAULT 1 COMMENT '是否全天:0-否,1-是',
    source_type TINYINT NOT NULL DEFAULT 1 COMMENT '来源类型:1-手动设置,2-钉钉,3-飞书,4-Outlook,5-企业微信',
    source_id VARCHAR(128) COMMENT '来源ID(第三方日历事件ID)',
    vacation_status TINYINT DEFAULT 1 COMMENT '状态:0-已取消,1-有效',
    auto_delegate TINYINT DEFAULT 1 COMMENT '是否自动委托:0-否,1-是',
    agent_user_id BIGINT COMMENT '指定代理人ID(为空则使用代理人配置)',
    agent_user_name VARCHAR(64) COMMENT '指定代理人姓名',
    remark VARCHAR(500) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_time (user_id, start_time, end_time),
    KEY idx_status (vacation_status),
    KEY idx_source (source_type, source_id),
    KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户休假记录表';

-- 2. 创建代理人配置表 wf_agent_config
CREATE TABLE IF NOT EXISTS wf_agent_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '委托人用户ID',
    user_name VARCHAR(64) COMMENT '委托人姓名',
    agent_user_id BIGINT NOT NULL COMMENT '代理人用户ID',
    agent_user_name VARCHAR(64) COMMENT '代理人姓名',
    config_type TINYINT NOT NULL DEFAULT 1 COMMENT '配置类型:1-常驻代理人,2-休假默认代理人',
    process_keys VARCHAR(512) COMMENT '适用流程Key(逗号分隔,为空表示全部)',
    priority INT DEFAULT 0 COMMENT '优先级(数字越小优先级越高)',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用:0-禁用,1-启用',
    remark VARCHAR(500) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_agent_type (user_id, agent_user_id, config_type, tenant_id),
    KEY idx_user_enabled (user_id, enabled),
    KEY idx_config_type (config_type),
    KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理人配置表';

-- 3. 为 wf_delegation 增加来源类型字段
ALTER TABLE wf_delegation
    ADD COLUMN IF NOT EXISTS source_type TINYINT DEFAULT 1 COMMENT '来源类型:1-手动设置,2-休假自动触发,3-升级触发' AFTER delegation_status;

-- ============================================================
-- 升级完成
-- ============================================================
