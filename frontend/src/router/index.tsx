import { Navigate, RouteObject } from 'react-router-dom'
import MainLayout from '@/layouts/MainLayout'
import Login from '@/pages/Login'
import TodoList from '@/pages/ApprovalCenter/TodoList'
import DoneList from '@/pages/ApprovalCenter/DoneList'
import MyApplyList from '@/pages/ApprovalCenter/MyApplyList'
import CcList from '@/pages/ApprovalCenter/CcList'
import ApplyForm from '@/pages/ApprovalCenter/ApplyForm'
import ApprovalDetail from '@/pages/ApprovalCenter/ApprovalDetail'
import DelegationList from '@/pages/ApprovalCenter/DelegationList'
import TenantManagement from '@/pages/System/TenantManagement'

const ProcessDefinition = () => <div>流程定义</div>
const ProcessDesign = () => <div>流程设计</div>
const FormDefinition = () => <div>表单定义</div>
const FormDesign = () => <div>表单设计</div>
const Category = () => <div>流程分类</div>
const BusinessLine = () => <div>业务线</div>

const UserManage = () => <div>用户管理</div>
const RoleManage = () => <div>角色管理</div>
const MenuManage = () => <div>菜单管理</div>
const DeptManage = () => <div>部门管理</div>

const MessageTemplate = () => <div>消息模板</div>
const MessageLog = () => <div>消息日志</div>

const Dashboard = () => <div>首页仪表盘</div>

export const routes: RouteObject[] = [
  {
    path: '/login',
    element: <Login />
  },
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard /> },
      {
        path: 'approval',
        children: [
          { index: true, element: <Navigate to="/approval/todo" replace /> },
          { path: 'todo', element: <TodoList /> },
          { path: 'done', element: <DoneList /> },
          { path: 'cc', element: <CcList /> },
          { path: 'my-apply', element: <MyApplyList /> },
          { path: 'apply', element: <ApplyForm /> },
          { path: 'detail/:id', element: <ApprovalDetail /> },
          { path: 'delegation', element: <DelegationList /> }
        ]
      },
      {
        path: 'process',
        children: [
          { index: true, element: <Navigate to="/process/definition" replace /> },
          { path: 'definition', element: <ProcessDefinition /> },
          { path: 'design', element: <ProcessDesign /> },
          { path: 'design/:id', element: <ProcessDesign /> },
          { path: 'form-definition', element: <FormDefinition /> },
          { path: 'form-design', element: <FormDesign /> },
          { path: 'form-design/:id', element: <FormDesign /> },
          { path: 'category', element: <Category /> },
          { path: 'business-line', element: <BusinessLine /> }
        ]
      },
      {
        path: 'system',
        children: [
          { index: true, element: <Navigate to="/system/user" replace /> },
          { path: 'user', element: <UserManage /> },
          { path: 'role', element: <RoleManage /> },
          { path: 'menu', element: <MenuManage /> },
          { path: 'dept', element: <DeptManage /> },
          { path: 'tenant', element: <TenantManagement /> }
        ]
      },
      {
        path: 'notify',
        children: [
          { index: true, element: <Navigate to="/notify/template" replace /> },
          { path: 'template', element: <MessageTemplate /> },
          { path: 'log', element: <MessageLog /> }
        ]
      }
    ]
  }
]

export default routes
