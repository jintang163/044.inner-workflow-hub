import { Select, Tag, Space } from 'antd'
import { SwapOutlined } from '@ant-design/icons'
import { useUserStore } from '@/store/user'
import { TenantSimpleVO } from '@/types'
import { message } from 'antd'

const businessTypeColorMap: Record<string, string> = {
  HR: 'blue',
  财务: 'green',
  采购: 'orange'
}

export default function TenantSwitcher() {
  const { currentTenantId, tenants, setCurrentTenantId } = useUserStore()

  if (!tenants || tenants.length === 0) {
    return null
  }

  const currentTenant = tenants.find((t: TenantSimpleVO) => t.tenantId === currentTenantId)

  const handleChange = (value: number) => {
    setCurrentTenantId(value)
    message.success(`已切换到租户: ${tenants.find((t: TenantSimpleVO) => t.tenantId === value)?.tenantName}`)
  }

  return (
    <Space size={8}>
      <SwapOutlined style={{ color: '#999' }} />
      <Select
        value={currentTenantId ?? undefined}
        onChange={handleChange}
        style={{ minWidth: 180 }}
        size="small"
        optionLabelProp="label"
        popupMatchSelectWidth={false}
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
      {currentTenant?.businessType && (
        <Tag color={businessTypeColorMap[currentTenant.businessType] || 'default'}>
          {currentTenant.businessType}
        </Tag>
      )}
    </Space>
  )
}
