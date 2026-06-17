import { useState } from 'react'
import { Collapse, Tag } from 'antd'
import {
  FormOutlined,
  EditOutlined,
  NumberOutlined,
  SelectOutlined,
  CheckSquareOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  FieldTimeOutlined,
  DotChartOutlined,
  CheckCircleOutlined,
  SwitcherOutlined,
  StarOutlined,
  ColumnHeightOutlined,
  UploadOutlined,
  PictureOutlined,
  UserOutlined,
  TeamOutlined,
  ApartmentOutlined,
  TableOutlined,
  MenuUnfoldOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import type { WidgetConfig } from '@/types/form'
import { BASIC_WIDGETS, ADVANCED_WIDGETS } from './widgetPresets'

const ICON_MAP: Record<string, React.ReactNode> = {
  FormOutlined: <FormOutlined />,
  EditOutlined: <EditOutlined />,
  NumberOutlined: <NumberOutlined />,
  SelectOutlined: <SelectOutlined />,
  CheckSquareOutlined: <CheckSquareOutlined />,
  CalendarOutlined: <CalendarOutlined />,
  ClockCircleOutlined: <ClockCircleOutlined />,
  FieldTimeOutlined: <FieldTimeOutlined />,
  DotChartOutlined: <DotChartOutlined />,
  CheckCircleOutlined: <CheckCircleOutlined />,
  SwitcherOutlined: <SwitcherOutlined />,
  StarOutlined: <StarOutlined />,
  ColumnHeightOutlined: <ColumnHeightOutlined />,
  UploadOutlined: <UploadOutlined />,
  PictureOutlined: <PictureOutlined />,
  UserOutlined: <UserOutlined />,
  TeamOutlined: <TeamOutlined />,
  ApartmentOutlined: <ApartmentOutlined />,
  TableOutlined: <TableOutlined />,
  MenuUnfoldOutlined: <MenuUnfoldOutlined />,
  InfoCircleOutlined: <InfoCircleOutlined />
}

interface WidgetPaletteProps {
  onDragStart?: (widget: WidgetConfig) => void
}

function WidgetPalette(props: WidgetPaletteProps) {
  const { onDragStart } = props
  const [activeKeys, setActiveKeys] = useState<string[]>(['basic', 'advanced'])

  const handleDragStart = (e: React.DragEvent, widget: WidgetConfig) => {
    e.dataTransfer.effectAllowed = 'copy'
    e.dataTransfer.setData('application/json', JSON.stringify(widget))
    onDragStart?.(widget)
  }

  const renderWidgetList = (widgets: WidgetConfig[]) => {
    return (
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8, padding: '8px 4px' }}>
        {widgets.map(widget => (
          <div
            key={widget.name}
            draggable
            onDragStart={(e) => handleDragStart(e, widget)}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '12px 8px',
              border: '1px solid #e8e8e8',
              borderRadius: 6,
              cursor: 'grab',
              transition: 'all 0.2s',
              background: '#fff',
              userSelect: 'none',
              fontSize: 12
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.borderColor = '#1677ff'
              e.currentTarget.style.boxShadow = '0 2px 8px rgba(22, 119, 255, 0.15)'
              e.currentTarget.style.transform = 'translateY(-1px)'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.borderColor = '#e8e8e8'
              e.currentTarget.style.boxShadow = 'none'
              e.currentTarget.style.transform = 'translateY(0)'
            }}
          >
            <div style={{ fontSize: 20, color: '#1677ff', marginBottom: 6 }}>
              {ICON_MAP[widget.icon] || <FormOutlined />}
            </div>
            <Tag color="blue" style={{ margin: 0, fontSize: 11 }}>{widget.label}</Tag>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div
      style={{
        width: 240,
        height: '100%',
        borderRight: '1px solid #f0f0f0',
        background: '#fafafa',
        overflow: 'auto',
        display: 'flex',
        flexDirection: 'column'
      }}
    >
      <div
        style={{
          padding: '16px 16px 8px',
          borderBottom: '1px solid #f0f0f0',
          fontWeight: 600,
          fontSize: 14,
          background: '#fff'
        }}
      >
        控件库
      </div>
      <Collapse
        activeKey={activeKeys}
        onChange={(keys) => setActiveKeys(keys as string[])}
        ghost
        style={{ border: 'none' }}
        items={[
          {
            key: 'basic',
            label: (
              <span style={{ fontWeight: 500 }}>
                📝 基础控件
              </span>
            ),
            children: renderWidgetList(BASIC_WIDGETS)
          },
          {
            key: 'advanced',
            label: (
              <span style={{ fontWeight: 500 }}>
                ⚙️ 高级控件
              </span>
            ),
            children: renderWidgetList(ADVANCED_WIDGETS)
          }
        ]}
      />
    </div>
  )
}

export { WidgetPalette }
export default WidgetPalette
