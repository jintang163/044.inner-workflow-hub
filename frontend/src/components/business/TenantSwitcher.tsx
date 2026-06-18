import { Select, Tag, Space, Spin } from 'antd'
import { SwapOutlined } from '@ant-design/icons'
import { useUserStore } from '@/store/user'
import { TenantSimpleVO } from '@/types'
import { authApi } from '@/api'
import { message } from 'antd'
import { useState } from 'react'

const businessTypeColorMap: Record<string, string> = {
  HR: 'blue',
  财务: 'green',
  采购: 'orange',
  IT: 'purple',
  行政: 'cyan',
  其他: 'default'
}

export default function TenantSwitcher() {
  const { currentTenantId, tenants, setCurrentTenantId, setUserInfo, setMenus } = useUserStore()
  const [switching, setSwitching] = useState(false)

  if (!tenants || tenants.length === 0) {
    return null
  }

  const currentTenant = tenants.find((t: TenantSimpleVO) => t.tenantId === currentTenantId)

  const handleChange = async (value: number) => {
    if (value === currentTenantId) return

    setSwitching(true)
    try {
      const newUserInfo = await authApi.switchTenant(value)
      setUserInfo(newUserInfo)
      setCurrentTenantId(value)

      const menus = await authApi.getMenus()
      setMenus(menus)

      message.success(`已切换到租户: ${tenants.find((t: TenantSimpleVO) => t.tenantId === value)?.tenantName}`)
    } catch {
      message.error('租户切换失败')
    } finally {
      setSwitching(false)
    }
  }

  return (
    <Space size={8}>
      <SwapOutlined style={{ color: switching ? '#1890ff' : '#999', transition: 'color 0.3s' }} />
      {switching ? (
        <Spin size="small" />
      ) : (
        <Select
          value={currentTenantId ?? undefined}
          onChange={handleChange}
          style={{ minWidth: 180 }}
          size="small"
          optionLabelProp="label"
          popupMatchSelectWidth={false}
          disabled={switching}
          options={tenants.map((t: TenantSimpleVO) => ({
            value: t.tenantId,
            label: t.tenantName,
            children: (
              <Space>
                <span>{t.tenantName}</span>
                <Tag color={businessTypeColorMap[t.businessType] || 'default'} style={{ marginLeft: 4 }}>
                  {t.businessType}
                </Tag>
              </Space>
            )
          }))}
        />
      )}
      {currentTenant?.businessType && (
        <Tag color={businessTypeColorMap[currentTenant.businessType] || 'default'}>
          {currentTenant.businessType}
        </Tag>
      )}
    </Space>
  )
}
