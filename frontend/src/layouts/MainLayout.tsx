import { useState, useEffect } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Avatar, Dropdown, Button, theme } from 'antd'
import {
  DashboardOutlined,
  AuditOutlined,
  ProjectOutlined,
  SettingOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  ProfileOutlined,
  FormOutlined,
  InboxOutlined,
  CheckCircleOutlined,
  SendOutlined,
  FileTextOutlined,
  AppstoreOutlined,
  DeploymentUnitOutlined,
  TeamOutlined,
  SafetyOutlined,
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  BankOutlined
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api'
import TenantSwitcher from '@/components/business/TenantSwitcher'

const { Header, Sider, Content } = Layout

type MenuItem = Required<MenuProps>['items'][number]

const menuItems: MenuItem[] = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: '首页'
  },
  {
    key: '/approval',
    icon: <AuditOutlined />,
    label: '审批中心',
    children: [
      { key: '/approval/todo', icon: <InboxOutlined />, label: '待办任务' },
      { key: '/approval/done', icon: <CheckCircleOutlined />, label: '已办任务' },
      { key: '/approval/cc', icon: <SendOutlined />, label: '抄送我的' },
      { key: '/approval/my-process', icon: <FileTextOutlined />, label: '我的流程' }
    ]
  },
  {
    key: '/process',
    icon: <ProjectOutlined />,
    label: '流程管理',
    children: [
      { key: '/process/definition', icon: <AppstoreOutlined />, label: '流程定义' },
      { key: '/process/design', icon: <DeploymentUnitOutlined />, label: '流程设计' },
      { key: '/process/form-definition', icon: <FormOutlined />, label: '表单定义' },
      { key: '/process/form-design', icon: <ProfileOutlined />, label: '表单设计' },
      { key: '/process/category', icon: <FileTextOutlined />, label: '流程分类' },
      { key: '/process/business-line', icon: <ProjectOutlined />, label: '业务线' }
    ]
  },
  {
    key: '/system',
    icon: <SettingOutlined />,
    label: '系统管理',
    children: [
      { key: '/system/user', icon: <UserOutlined />, label: '用户管理' },
      { key: '/system/role', icon: <SafetyOutlined />, label: '角色管理' },
      { key: '/system/menu', icon: <MenuUnfoldOutlined />, label: '菜单管理' },
      { key: '/system/dept', icon: <TeamOutlined />, label: '部门管理' },
      { key: '/system/tenant', icon: <BankOutlined />, label: '租户管理' }
    ]
  },
  {
    key: '/notify',
    icon: <BellOutlined />,
    label: '消息通知',
    children: [
      { key: '/notify/template', icon: <FileTextOutlined />, label: '消息模板' },
      { key: '/notify/log', icon: <CheckCircleOutlined />, label: '消息日志' }
    ]
  }
]

function findSelectedKey(pathname: string, items: MenuItem[]): string[] {
  const keys: string[] = []
  for (const item of items) {
    if (!item) continue
    if ('children' in item && item.children) {
      const childKeys = findSelectedKey(pathname, item.children as MenuItem[])
      if (childKeys.length > 0) {
        keys.push(item.key as string)
        keys.push(...childKeys)
        break
      }
    } else if ('key' in item) {
      if (pathname === item.key || pathname.startsWith(item.key as string)) {
        keys.push(item.key as string)
      }
    }
  }
  return keys
}

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const { token } = theme.useToken()
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, logout, setUserInfo, setMenus } = useUserStore()

  useEffect(() => {
    if (!useUserStore.getState().token) {
      navigate('/login')
      return
    }
    loadUserInfo()
  }, [])

  const loadUserInfo = async () => {
    try {
      const [info, menus] = await Promise.all([authApi.getUserInfo(), authApi.getMenus()])
      setUserInfo(info)
      setMenus(menus)
    } catch (e) {
      // ignore
    }
  }

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(key)
  }

  const selectedKeys = findSelectedKey(location.pathname, menuItems)
  const openKeys = selectedKeys.slice(0, -1)

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心'
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      danger: true
    }
  ]

  const handleUserMenuClick: MenuProps['onClick'] = async ({ key }) => {
    if (key === 'logout') {
      try {
        await authApi.logout()
      } catch (e) {
        // ignore
      }
      logout()
      navigate('/login')
    }
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} theme="dark">
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: collapsed ? 14 : 18,
            fontWeight: 'bold',
            borderBottom: '1px solid rgba(255,255,255,0.1)'
          }}
        >
          {collapsed ? 'WF' : '工作流平台'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys.length > 0 ? [selectedKeys[selectedKeys.length - 1]] : []}
          defaultOpenKeys={openKeys}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 16px',
            background: token.colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #f0f0f0'
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{ fontSize: '16px', width: 64, height: 64 }}
          />
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <TenantSwitcher />
            <Dropdown menu={{ items: userMenuItems, onClick: handleUserMenuClick }} placement="bottomRight">
              <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', gap: 8 }}>
                <Avatar icon={<UserOutlined />} src={userInfo?.avatar} />
                <span>{userInfo?.nickname || userInfo?.username || '未登录'}</span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content
          style={{
            margin: 16,
            padding: 24,
            background: token.colorBgContainer,
            borderRadius: token.borderRadiusLG,
            minHeight: 280
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
