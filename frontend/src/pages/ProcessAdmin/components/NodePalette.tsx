import React from 'react'
import { Typography, Space, Tooltip } from 'antd'

const { Title, Text } = Typography

interface NodeItem {
  type: string
  label: string
  icon: React.ReactNode
  className: string
  tooltip?: string
}

interface NodePaletteProps {
  onDragStart?: (type: string, event: React.DragEvent) => void
}

const createNodeSVG = (color: string, innerHTML: string) => (
  <svg
    width="28"
    height="28"
    viewBox="0 0 32 32"
    style={{ flexShrink: 0 }}
  >
    <g dangerouslySetInnerHTML={{ __html: innerHTML }} />
  </svg>
)

const startEventIcon = createNodeSVG(
  '#52c41a',
  `
  <circle cx="16" cy="16" r="13" fill="none" stroke="#52c41a" stroke-width="2" />
  <circle cx="16" cy="16" r="10" fill="#f6ffed" />
  `
)

const endEventIcon = createNodeSVG(
  '#ff4d4f',
  `
  <circle cx="16" cy="16" r="13" fill="none" stroke="#ff4d4f" stroke-width="3" />
  <circle cx="16" cy="16" r="9" fill="#fff1f0" />
  `
)

const userTaskIcon = createNodeSVG(
  '#1890ff',
  `
  <rect x="3" y="8" width="26" height="18" rx="4" ry="4" fill="#e6f7ff" stroke="#1890ff" stroke-width="1.5" />
  <circle cx="12" cy="16" r="3" fill="#1890ff" />
  <path d="M7 23c0-3 2.5-5 5-5s5 2 5 5" fill="none" stroke="#1890ff" stroke-width="2" stroke-linecap="round" />
  <path d="M19 14h7M19 18h5" stroke="#1890ff" stroke-width="2" stroke-linecap="round" />
  `
)

const exclusiveGatewayIcon = createNodeSVG(
  '#faad14',
  `
  <polygon points="16,2 30,16 16,30 2,16" fill="#fffbe6" stroke="#faad14" stroke-width="1.5" />
  <path d="M11 11l10 10M21 11l-10 10" stroke="#faad14" stroke-width="2" stroke-linecap="round" />
  `
)

const parallelGatewayIcon = createNodeSVG(
  '#722ed1',
  `
  <polygon points="16,2 30,16 16,30 2,16" fill="#f9f0ff" stroke="#722ed1" stroke-width="1.5" />
  <path d="M16 9v14M9 16h14" stroke="#722ed1" stroke-width="2" stroke-linecap="round" />
  `
)

const subProcessIcon = createNodeSVG(
  '#13c2c2',
  `
  <rect x="3" y="5" width="26" height="22" rx="3" ry="3" fill="#e6fffb" stroke="#13c2c2" stroke-width="1.5" stroke-dasharray="3,2" />
  <rect x="7" y="10" width="7" height="6" rx="2" fill="none" stroke="#13c2c2" stroke-width="1" />
  <rect x="18" y="10" width="7" height="6" rx="2" fill="none" stroke="#13c2c2" stroke-width="1" />
  <path d="M14 13h4" stroke="#13c2c2" stroke-width="1.5" stroke-linecap="round" marker-end="url(#arrow)" />
  `
)

const nodeGroups: { title: string; items: NodeItem[] }[] = [
  {
    title: '事件',
    items: [
      {
        type: 'startEvent',
        label: '开始事件',
        icon: startEventIcon,
        className: 'bpmn-icon-start-event-none',
        tooltip: '流程起始节点'
      },
      {
        type: 'endEvent',
        label: '结束事件',
        icon: endEventIcon,
        className: 'bpmn-icon-end-event-none',
        tooltip: '流程结束节点'
      }
    ]
  },
  {
    title: '任务',
    items: [
      {
        type: 'userTask',
        label: '审批节点',
        icon: userTaskIcon,
        className: 'bpmn-icon-user-task',
        tooltip: '用户审批任务节点'
      }
    ]
  },
  {
    title: '网关',
    items: [
      {
        type: 'exclusiveGateway',
        label: '条件分支',
        icon: exclusiveGatewayIcon,
        className: 'bpmn-icon-gateway-xor',
        tooltip: '排他网关，根据条件走其中一条分支'
      },
      {
        type: 'parallelGateway',
        label: '并行网关',
        icon: parallelGatewayIcon,
        className: 'bpmn-icon-gateway-parallel',
        tooltip: '并行网关，所有分支同时执行，会签/或签'
      }
    ]
  },
  {
    title: '流程',
    items: [
      {
        type: 'subProcess',
        label: '子流程',
        icon: subProcessIcon,
        className: 'bpmn-icon-subprocess-expanded',
        tooltip: '嵌入子流程（可选）'
      }
    ]
  }
]

export default function NodePalette({ onDragStart }: NodePaletteProps) {
  const handleDragStart = (e: React.DragEvent, type: string) => {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('application/bpmn', JSON.stringify({ type }))
    onDragStart?.(type, e)
  }

  return (
    <div
      style={{
        height: '100%',
        overflowY: 'auto',
        padding: 12,
        background: '#fafafa',
        borderRight: '1px solid #f0f0f0'
      }}
    >
      <Title level={5} style={{ margin: '0 0 16px 0', color: '#1f1f1f' }}>
        节点面板
      </Title>
      {nodeGroups.map((group) => (
        <div key={group.title} style={{ marginBottom: 20 }}>
          <Text
            type="secondary"
            style={{ fontSize: 12, display: 'block', marginBottom: 8, fontWeight: 500 }}
          >
            {group.title}
          </Text>
          <Space direction="vertical" style={{ width: '100%' }} size={6}>
            {group.items.map((item) => (
              <Tooltip key={item.type} title={item.tooltip} placement="right">
                <div
                  draggable
                  onDragStart={(e) => handleDragStart(e, item.type)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    padding: '8px 10px',
                    background: '#fff',
                    border: '1px solid #e8e8e8',
                    borderRadius: 6,
                    cursor: 'move',
                    userSelect: 'none',
                    transition: 'all 0.2s'
                  }}
                  className="palette-item"
                >
                  {item.icon}
                  <span style={{ fontSize: 13, color: '#333' }}>{item.label}</span>
                </div>
              </Tooltip>
            ))}
          </Space>
        </div>
      ))}
      <div
        style={{
          marginTop: 24,
          padding: 12,
          background: '#f0f7ff',
          borderRadius: 6,
          fontSize: 12,
          color: '#666',
          lineHeight: 1.6
        }}
      >
        <Text strong style={{ color: '#1890ff', display: 'block', marginBottom: 6 }}>
          💡 操作提示
        </Text>
        拖拽节点到画布<br />
        点击节点可查看属性<br />
        悬停节点可拖拽连线
      </div>
    </div>
  )
}
