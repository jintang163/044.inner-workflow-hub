# 企业内部多业务线统一审批流系统 (Inner Workflow Hub)

## 项目概述

面向企业多业务线的统一审批流平台，通过可视化拖拽方式设计BPMN流程和动态表单，避免各业务线重复开发审批功能。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.x
- **流程引擎**: Flowable 7.0.x
- **ORM**: MyBatis-Plus 3.5.x
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.x
- **消息队列**: Kafka 3.6.x
- **认证**: Spring Security + JWT
- **接口文档**: Knife4j (OpenAPI 3)

### 前端
- **构建工具**: Vite 5.x
- **框架**: React 18 + TypeScript
- **UI组件**: Ant Design 5.x
- **表单引擎**: Formily 2.x
- **流程设计**: bpmn-js 17.x
- **状态管理**: Zustand
- **路由**: React Router 6.x

## 核心功能

### 1. 可视化流程设计器 (BPMN)
- 拖拽式设计审批流程，支持审批节点、条件分支、会签/或签、超时处理
- 节点支持多种审批人配置：固定人员、部门主管、角色、动态脚本
- BPMN 2.0标准存储，支持版本发布和模拟运行
- 流程校验（孤立节点检测、必填项校验）
- 场景示例：金额超5万自动转总经理审批

### 2. 动态表单设计
- 拖拽设计表单（文本、数字、日期、下拉、表格、附件等控件）
- 支持必填、正则、联动校验
- JSON Schema存储，Formily动态渲染
- 节点级表单权限控制（只读/编辑/隐藏）
- 表单数据自动映射为流程变量
- 草稿保存功能

### 3. 审批流转
- Flowable引擎驱动待办任务推送
- 批量审批（同意/拒绝/转审/加签/驳回）
- 超时自动催办 + 升级上级主管
- 审批历史时间轴 + 流程图高亮当前节点
- 手写签名 + 审批意见附件
- 操作日志全记录
- 移动端同步支持

### 4. 通知中心
- 钉钉/企微/邮件多渠道通知
- Kafka异步消息队列
- 内置消息模板引擎

## 目录结构

```
inner-workflow-hub/
├── backend/                          # 后端Maven多模块项目
│   ├── pom.xml                       # 父POM
│   ├── workflow-common/              # 公共模块
│   │   ├── src/main/java/.../common/
│   │   │   ├── entity/               # 基础实体类
│   │   │   ├── dto/                  # 通用DTO
│   │   │   ├── enums/                # 枚举定义
│   │   │   ├── exception/            # 自定义异常
│   │   │   ├── result/               # 统一响应封装
│   │   │   └── util/                 # 工具类
│   ├── workflow-auth/                # 认证授权模块
│   │   ├── src/main/java/.../auth/
│   │   │   ├── config/               # Security配置
│   │   │   ├── filter/               # JWT过滤器
│   │   │   ├── service/              # 认证服务
│   │   │   └── controller/           # 登录/权限API
│   ├── workflow-bpmn/                # BPMN流程模块
│   │   ├── src/main/java/.../bpmn/
│   │   │   ├── controller/           # 流程设计/部署/版本API
│   │   │   ├── service/              # 流程定义服务
│   │   │   ├── listener/             # 全局监听器
│   │   │   └── handler/              # 审批人解析处理器
│   ├── workflow-form/                # 动态表单模块
│   │   ├── src/main/java/.../form/
│   │   │   ├── controller/           # 表单设计/草稿API
│   │   │   └── service/              # 表单配置服务
│   ├── workflow-approval/            # 审批流转模块
│   │   ├── src/main/java/.../approval/
│   │   │   ├── controller/           # 待办/已办/审批操作API
│   │   │   ├── service/              # 审批服务
│   │   │   ├── task/                 # 超时处理定时任务
│   │   │   └── cmd/                  # Flowable自定义命令
│   ├── workflow-notify/              # 通知模块
│   │   ├── src/main/java/.../notify/
│   │   │   ├── consumer/             # Kafka消费者
│   │   │   ├── sender/               # 钉钉/企微/邮件发送器
│   │   │   └── template/             # 消息模板引擎
│   └── workflow-api/                 # API启动模块
│       ├── src/main/java/.../WorkflowApplication.java
│       └── src/main/resources/
│           ├── application.yml
│           └── mapper/               # MyBatis XML映射文件
├── frontend/                         # 前端项目
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── router/                   # 路由配置
│       ├── store/                    # Zustand状态管理
│       ├── api/                      # API请求封装
│       ├── components/               # 公共组件
│       │   ├── BpmnDesigner/         # BPMN流程设计器
│       │   │   ├── index.tsx
│       │   │   ├── PropertyPanel/    # 属性面板
│       │   │   └── Palette/          # 自定义左侧面板
│       │   ├── FormDesigner/         # 动态表单设计器
│       │   │   ├── index.tsx
│       │   │   ├── SchemaEditor/     # Schema编辑
│       │   │   └── WidgetPanel/      # 控件面板
│       │   └── ApprovalTimeline/     # 审批时间轴
│       ├── pages/                    # 页面
│       │   ├── Login/                # 登录
│       │   ├── ProcessAdmin/         # 流程管理（管理员）
│       │   │   ├── ProcessList.tsx
│       │   │   ├── ProcessDesign.tsx
│       │   │   └── FormDesign.tsx
│       │   ├── ApprovalCenter/       # 审批中心
│       │   │   ├── TodoList.tsx      # 待办
│       │   │   ├── DoneList.tsx      # 已办
│       │   │   ├── ApplyForm.tsx     # 发起审批
│       │   │   └── ApprovalDetail.tsx# 审批详情
│       │   └── System/               # 系统管理
│       │       ├── UserManage.tsx
│       │       ├── RoleManage.tsx
│       │       └── DepartmentManage.tsx
│       └── utils/                    # 工具函数
├── sql/                              # 数据库脚本
│   ├── 01_schema.sql                 # 表结构
│   ├── 02_flowable_all.sql          # Flowable引擎表
│   └── 03_init_data.sql              # 初始化数据
└── docker/                           # 容器化部署
    ├── docker-compose.yml
    └── .env
```

## 快速开始

### 数据库初始化
```bash
# 1. 创建数据库
mysql -u root -p < sql/01_schema.sql
# 2. 初始化Flowable表（首次启动自动创建，或手动执行）
mysql -u root -p < sql/02_flowable_all.sql
# 3. 初始化基础数据
mysql -u root -p < sql/03_init_data.sql
```

### 后端启动
```bash
cd backend
mvn clean install -DskipTests
cd workflow-api
mvn spring-boot:run
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

### 默认账号
- 管理员: admin / admin123
- 普通用户: test01 / 123456
