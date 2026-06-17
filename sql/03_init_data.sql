-- ============================================================
-- 企业内部多业务线统一审批流系统 - 初始化数据脚本
-- ============================================================

USE `inner_workflow_hub`;

-- ============================================================
-- 1. 部门数据
-- ============================================================
INSERT INTO `sys_department` (`id`, `parent_id`, `ancestors`, `dept_name`, `dept_code`, `sort_order`, `leader_user_id`, `phone`, `email`, `status`) VALUES
(100, 0, '', '集团总部', 'GROUP', 0, 1, '010-12345678', 'group@company.com', 1),
(101, 100, '100', '总经办', 'CEO_OFFICE', 1, 1, '010-12345679', 'ceo@company.com', 1),
(102, 100, '100', '技术中心', 'TECH', 2, 2, '010-12345680', 'tech@company.com', 1),
(103, 100, '100', '财务中心', 'FINANCE', 3, 3, '010-12345681', 'finance@company.com', 1),
(104, 100, '100', '人力资源中心', 'HR', 4, 4, '010-12345682', 'hr@company.com', 1),
(105, 100, '100', '市场中心', 'MARKETING', 5, 5, '010-12345683', 'marketing@company.com', 1),
(106, 102, '100,102', '前端开发部', 'FRONTEND', 1, 6, '010-12345684', 'frontend@company.com', 1),
(107, 102, '100,102', '后端开发部', 'BACKEND', 2, 7, '010-12345685', 'backend@company.com', 1),
(108, 102, '100,102', '测试部', 'QA', 3, 8, '010-12345686', 'qa@company.com', 1),
(109, 103, '100,103', '会计部', 'ACCOUNTING', 1, 9, '010-12345687', 'accounting@company.com', 1),
(110, 103, '100,103', '资金部', 'TREASURY', 2, 10, '010-12345688', 'treasury@company.com', 1);

-- ============================================================
-- 2. 用户数据（密码为 BCrypt 加密的 123456 / admin123）
-- 加密结果：
-- 123456  -> $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
-- admin123 -> $2a$10$2aKzYdL3sXzQx8Vn9Mp4rOeHtG7fD6sA5dS4cF3eR2yU1gH0jK9L8M
-- ============================================================
INSERT INTO `sys_user` (`id`, `username`, `password`, `nick_name`, `real_name`, `user_type`, `email`, `phone`, `gender`, `dept_id`, `status`, `pwd_update_time`) VALUES
(1, 'admin', '$2a$10$2aKzYdL3sXzQx8Vn9Mp4rOeHtG7fD6sA5dS4cF3eR2yU1gH0jK9L8M', '超级管理员', '张管理', 1, 'admin@company.com', '13800000000', 1, 101, 1, NOW()),
(2, 'tech_director', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '技术总监', '李技术', 1, 'tech_director@company.com', '13800000001', 1, 102, 1, NOW()),
(3, 'finance_director', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '财务总监', '王财务', 1, 'finance_director@company.com', '13800000002', 2, 103, 1, NOW()),
(4, 'hr_director', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '人力资源总监', '赵人力', 1, 'hr_director@company.com', '13800000003', 2, 104, 1, NOW()),
(5, 'marketing_director', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '市场总监', '孙市场', 1, 'marketing_director@company.com', '13800000004', 1, 105, 1, NOW()),
(6, 'frontend_leader', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '前端负责人', '周前端', 1, 'frontend_leader@company.com', '13800000005', 1, 106, 1, NOW()),
(7, 'backend_leader', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '后端负责人', '吴后端', 1, 'backend_leader@company.com', '13800000006', 1, 107, 1, NOW()),
(8, 'qa_leader', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '测试负责人', '郑测试', 1, 'qa_leader@company.com', '13800000007', 2, 108, 1, NOW()),
(9, 'accounting_leader', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '会计主管', '冯会计', 1, 'accounting_leader@company.com', '13800000008', 2, 109, 1, NOW()),
(10, 'treasury_leader', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '资金主管', '陈资金', 1, 'treasury_leader@company.com', '13800000009', 1, 110, 1, NOW()),
(11, 'test01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '测试员工01', '员工01', 2, 'test01@company.com', '13800000011', 1, 106, 1, NOW()),
(12, 'test02', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '测试员工02', '员工02', 2, 'test02@company.com', '13800000012', 1, 107, 1, NOW()),
(13, 'test03', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '测试员工03', '员工03', 2, 'test03@company.com', '13800000013', 2, 109, 1, NOW()),
(14, 'ceo', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '总经理', '钱总', 1, 'ceo@company.com', '13800000099', 1, 101, 1, NOW());

-- ============================================================
-- 3. 角色数据
-- ============================================================
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `data_scope`, `status`, `remark`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 0, 1, 1, '拥有所有系统权限'),
(2, '系统管理员', 'SYSTEM_ADMIN', 1, 1, 1, '系统管理权限'),
(3, '流程管理员', 'PROCESS_ADMIN', 2, 2, 1, '流程设计与发布权限'),
(4, '财务审批岗', 'FINANCE_APPROVER', 3, 3, 1, '财务相关审批权限'),
(5, 'HR审批岗', 'HR_APPROVER', 4, 3, 1, '人事相关审批权限'),
(6, '部门主管', 'DEPT_LEADER', 5, 4, 1, '本部门及以下数据权限'),
(7, '普通员工', 'EMPLOYEE', 99, 5, 1, '仅本人数据权限');

-- ============================================================
-- 4. 用户角色关联
-- ============================================================
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2), (2, 3), (2, 6),
(3, 4), (3, 6),
(4, 5), (4, 6),
(5, 6),
(6, 6),
(7, 6),
(8, 6),
(9, 6),
(10, 6),
(14, 1), (14, 6),
(11, 7),
(12, 7),
(13, 7);

-- ============================================================
-- 5. 菜单权限数据
-- ============================================================
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `path`, `component`, `permission`, `menu_type`, `visible`, `sort_order`, `icon`, `status`) VALUES
-- 一级目录
(1, 0, '审批中心', '/approval', '', '', 'M', 1, 1, 'CheckCircleOutlined', 1),
(2, 0, '流程管理', '/process', '', '', 'M', 1, 2, 'NodeIndexOutlined', 1),
(3, 0, '系统管理', '/system', '', '', 'M', 1, 99, 'SettingOutlined', 1),

-- 审批中心菜单
(11, 1, '待办任务', 'todo', 'ApprovalCenter/TodoList', 'approval:todo:list', 'C', 1, 1, 'ToDoOutlined', 1),
(12, 1, '已办任务', 'done', 'ApprovalCenter/DoneList', 'approval:done:list', 'C', 1, 2, 'CheckSquareOutlined', 1),
(13, 1, '发起审批', 'apply', 'ApprovalCenter/ApplyForm', 'approval:apply', 'C', 1, 3, 'FormOutlined', 1),
(14, 1, '我发起的', 'my-apply', 'ApprovalCenter/MyApplyList', 'approval:myApply:list', 'C', 1, 4, 'UserOutlined', 1),
(15, 1, '抄送我的', 'cc', 'ApprovalCenter/CcList', 'approval:cc:list', 'C', 1, 5, 'ShareAltOutlined', 1),
(16, 1, '审批详情', 'detail/:id', 'ApprovalCenter/ApprovalDetail', 'approval:detail', 'C', 0, 6, '', 1),

-- 流程管理菜单
(21, 2, '流程列表', 'list', 'ProcessAdmin/ProcessList', 'process:list', 'C', 1, 1, 'UnorderedListOutlined', 1),
(22, 2, '流程设计', 'design/:id', 'ProcessAdmin/ProcessDesign', 'process:design', 'C', 0, 2, '', 1),
(23, 2, '表单设计', 'form/:id', 'ProcessAdmin/FormDesign', 'process:form:design', 'C', 0, 3, '', 1),
(24, 2, '流程监控', 'monitor', 'ProcessAdmin/ProcessMonitor', 'process:monitor', 'C', 1, 4, 'MonitorOutlined', 1),
(25, 2, '业务线管理', 'business-line', 'ProcessAdmin/BusinessLineList', 'process:businessLine', 'C', 1, 5, 'ApartmentOutlined', 1),

-- 系统管理菜单
(31, 3, '用户管理', 'user', 'System/UserManage', 'system:user:list', 'C', 1, 1, 'UserOutlined', 1),
(32, 3, '角色管理', 'role', 'System/RoleManage', 'system:role:list', 'C', 1, 2, 'TeamOutlined', 1),
(33, 3, '部门管理', 'dept', 'System/DepartmentManage', 'system:dept:list', 'C', 1, 3, 'ApartmentOutlined', 1),
(34, 3, '菜单管理', 'menu', 'System/MenuManage', 'system:menu:list', 'C', 1, 4, 'MenuOutlined', 1),
(35, 3, '操作日志', 'oper-log', 'System/OperLog', 'system:operLog:list', 'C', 1, 5, 'FileTextOutlined', 1),
(36, 3, '登录日志', 'login-log', 'System/LoginLog', 'system:loginLog:list', 'C', 1, 6, 'LoginOutlined', 1),

-- 审批中心按钮
(111, 11, '审批同意', '', '', 'approval:task:agree', 'F', 1, 1, '', 1),
(112, 11, '审批拒绝', '', '', 'approval:task:reject', 'F', 1, 2, '', 1),
(113, 11, '转审', '', '', 'approval:task:transfer', 'F', 1, 3, '', 1),
(114, 11, '加签', '', '', 'approval:task:addSign', 'F', 1, 4, '', 1),
(115, 11, '委派', '', '', 'approval:task:delegate', 'F', 1, 5, '', 1),
(116, 11, '驳回', '', '', 'approval:task:sendBack', 'F', 1, 6, '', 1),
(117, 11, '批量审批', '', '', 'approval:task:batch', 'F', 1, 7, '', 1),

-- 流程管理按钮
(211, 21, '新增流程', '', '', 'process:add', 'F', 1, 1, '', 1),
(212, 21, '编辑流程', '', '', 'process:edit', 'F', 1, 2, '', 1),
(213, 21, '删除流程', '', '', 'process:remove', 'F', 1, 3, '', 1),
(214, 21, '发布流程', '', '', 'process:publish', 'F', 1, 4, '', 1),
(215, 21, '流程校验', '', '', 'process:validate', 'F', 1, 5, '', 1),
(216, 21, '模拟运行', '', '', 'process:simulate', 'F', 1, 6, '', 1),
(217, 21, '版本管理', '', '', 'process:version', 'F', 1, 7, '', 1),

-- 系统管理按钮
(311, 31, '新增用户', '', '', 'system:user:add', 'F', 1, 1, '', 1),
(312, 31, '编辑用户', '', '', 'system:user:edit', 'F', 1, 2, '', 1),
(313, 31, '删除用户', '', '', 'system:user:remove', 'F', 1, 3, '', 1),
(314, 31, '重置密码', '', '', 'system:user:resetPwd', 'F', 1, 4, '', 1),
(321, 32, '新增角色', '', '', 'system:role:add', 'F', 1, 1, '', 1),
(322, 32, '编辑角色', '', '', 'system:role:edit', 'F', 1, 2, '', 1),
(323, 32, '删除角色', '', '', 'system:role:remove', 'F', 1, 3, '', 1),
(324, 32, '角色授权', '', '', 'system:role:permission', 'F', 1, 4, '', 1);

-- ============================================================
-- 6. 角色菜单关联（超级管理员拥有所有菜单权限）
-- ============================================================
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu`;

-- 系统管理员：审批中心 + 系统管理
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15), (2, 16),
(2, 111), (2, 112), (2, 113), (2, 114), (2, 115), (2, 116), (2, 117),
(2, 3), (2, 31), (2, 32), (2, 33), (2, 34), (2, 35), (2, 36),
(2, 311), (2, 312), (2, 313), (2, 314), (2, 321), (2, 322), (2, 323), (2, 324);

-- 流程管理员：审批中心 + 流程管理
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3, 1), (3, 11), (3, 12), (3, 13), (3, 14), (3, 15), (3, 16),
(3, 111), (3, 112), (3, 113), (3, 114), (3, 115), (3, 116), (3, 117),
(3, 2), (3, 21), (3, 22), (3, 23), (3, 24), (3, 25),
(3, 211), (3, 212), (3, 213), (3, 214), (3, 215), (3, 216), (3, 217);

-- 财务审批岗：审批中心（无流程管理）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 1), (4, 11), (4, 12), (4, 13), (4, 14), (4, 15), (4, 16),
(4, 111), (4, 112), (4, 113), (4, 114), (4, 115), (4, 116), (4, 117);

-- HR审批岗
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(5, 1), (5, 11), (5, 12), (5, 13), (5, 14), (5, 15), (5, 16),
(5, 111), (5, 112), (5, 113), (5, 114), (5, 115), (5, 116), (5, 117);

-- 部门主管
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(6, 1), (6, 11), (6, 12), (6, 13), (6, 14), (6, 15), (6, 16),
(6, 111), (6, 112), (6, 113), (6, 114), (6, 115), (6, 116), (6, 117);

-- 普通员工
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(7, 1), (7, 11), (7, 12), (7, 13), (7, 14), (7, 15), (7, 16),
(7, 111), (7, 112);

-- ============================================================
-- 7. 业务线和分类数据
-- ============================================================
INSERT INTO `wf_business_line` (`id`, `line_name`, `line_code`, `description`, `sort_order`, `status`) VALUES
(1, '财务管理线', 'FINANCE', '财务相关审批流程', 1, 1),
(2, '人事管理线', 'HR', '人事相关审批流程', 2, 1),
(3, '行政管理线', 'ADMIN', '行政相关审批流程', 3, 1),
(4, '技术管理线', 'TECH', '技术相关审批流程', 4, 1),
(5, '市场营销线', 'MARKETING', '市场销售相关审批流程', 5, 1);

INSERT INTO `wf_category` (`id`, `business_line_id`, `category_name`, `category_code`, `description`, `sort_order`, `status`) VALUES
(1, 1, '费用报销', 'EXPENSE', '日常费用报销流程', 1, 1),
(2, 1, '付款申请', 'PAYMENT', '对外付款申请流程', 2, 1),
(3, 1, '借款申请', 'LOAN', '员工借款申请流程', 3, 1),
(4, 2, '请假申请', 'LEAVE', '员工请假流程', 1, 1),
(5, 2, '入职申请', 'ONBOARD', '新员工入职流程', 2, 1),
(6, 2, '离职申请', 'RESIGN', '员工离职流程', 3, 1),
(7, 2, '调岗调薪', 'TRANSFER', '员工调岗调薪流程', 4, 1),
(8, 3, '采购申请', 'PURCHASE', '物品采购申请流程', 1, 1),
(9, 3, '用章申请', 'SEAL', '印章使用申请流程', 2, 1),
(10, 3, '会议预约', 'MEETING', '会议室预约流程', 3, 1),
(11, 4, '立项申请', 'PROJECT', '项目立项流程', 1, 1),
(12, 4, '发布审批', 'RELEASE', '系统发布审批流程', 2, 1),
(13, 5, '合同审批', 'CONTRACT', '合同审批流程', 1, 1),
(14, 5, '客户审批', 'CUSTOMER', '客户准入审批流程', 2, 1),
(15, 5, '折扣审批', 'DISCOUNT', '销售折扣审批流程', 3, 1);

-- ============================================================
-- 8. 消息模板数据
-- ============================================================
INSERT INTO `wf_message_template` (`id`, `template_code`, `template_name`, `business_line_id`, `event_type`, `channel_types`, `email_subject_template`, `email_content_template`, `status`) VALUES
(1, 'TASK_CREATE_DEFAULT', '任务创建通知模板', NULL, 'TASK_CREATE',
 '["DINGTALK","WECOM","EMAIL"]',
 '【审批通知】您有待处理的审批任务 - ${title}',
 '<div><p>尊敬的 ${userName}：</p><p>您有一条新的审批任务待处理，请及时处理。</p><ul><li>审批单号：${instanceNo}</li><li>审批标题：${title}</li><li>发起人：${startUserName}</li><li>发起时间：${startTime}</li><li>任务节点：${nodeName}</li><li>截止时间：${dueTime!""}</li></ul><p><a href="${detailUrl}" target="_blank">点击查看详情</a></p><p>此邮件由系统自动发送，请勿直接回复。</p></div>',
 1),
(2, 'PROCESS_START_DEFAULT', '流程发起通知模板', NULL, 'PROCESS_START',
 '["DINGTALK","WECOM"]',
 NULL, NULL, 1),
(3, 'TASK_COMPLETE_DEFAULT', '任务完成通知模板', NULL, 'TASK_COMPLETE',
 '["DINGTALK","WECOM"]',
 NULL, NULL, 1),
(4, 'PROCESS_END_DEFAULT', '流程结束通知模板', NULL, 'PROCESS_END',
 '["DINGTALK","WECOM","EMAIL"]',
 '【审批结果】${title} - 审批已完成',
 '<div><p>尊敬的 ${userName}：</p><p>您发起的审批流程已处理完成。</p><ul><li>审批单号：${instanceNo}</li><li>审批标题：${title}</li><li>发起时间：${startTime}</li><li>结束时间：${endTime}</li><li>审批结果：${result}</li></ul><p><a href="${detailUrl}" target="_blank">点击查看详情</a></p></div>',
 1),
(5, 'TIMEOUT_REMIND_DEFAULT', '超时提醒模板', NULL, 'TIMEOUT_REMIND',
 '["DINGTALK","WECOM","EMAIL"]',
 '【超时提醒】您的审批任务即将/已超时 - ${title}',
 '<div><p>尊敬的 ${userName}：</p><p style="color:red;font-weight:bold;">您有一条审批任务已超时，请尽快处理！</p><ul><li>审批单号：${instanceNo}</li><li>审批标题：${title}</li><li>任务节点：${nodeName}</li><li>分配时间：${assignTime}</li><li>截止时间：${dueTime}</li><li>超时时长：${timeoutDuration}</li></ul><p><a href="${detailUrl}" target="_blank">点击立即处理</a></p></div>',
 1);
