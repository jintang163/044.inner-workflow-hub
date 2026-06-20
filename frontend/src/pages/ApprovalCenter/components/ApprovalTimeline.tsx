import React from 'react'
import { Timeline, Avatar, Tag, Space, Typography, Card, Image } from 'antd'
import {
  UserOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
  TeamOutlined,
  RollbackOutlined,
  UndoOutlined,
  PaperClipOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import type { ApprovalHistoryVO } from '@/types/approval'
import { formatDuration } from './TaskTableColumns'

const { Text, Paragraph } = Typography

interface ApprovalTimelineProps {
  history: ApprovalHistoryVO[]
  loading?: boolean
}

const getActivityIcon = (activityType: number, status?: string) => {
  const defaultStyle = { fontSize: 16 }
  if (status === 'current' || activityType === 1) {
    return <ClockCircleOutlined style={{ ...defaultStyle, color: '#1890ff' }} />
  }
  switch (activityType) {
    case 1:
      return <UserOutlined style={{ ...defaultStyle, color: '#1890ff' }} />
    case 2:
      return <CheckCircleOutlined style={{ ...defaultStyle, color: '#52c41a' }} />
    case 3:
    case 8:
      return <CloseCircleOutlined style={{ ...defaultStyle, color: '#ff4d4f' }} />
    case 4:
      return <SyncOutlined style={{ ...defaultStyle, color: '#1890ff' }} />
    case 5:
      return <TeamOutlined style={{ ...defaultStyle, color: '#722ed1' }} />
    case 6:
      return <UndoOutlined style={{ ...defaultStyle, color: '#fa8c16' }} />
    case 7:
      return <RollbackOutlined style={{ ...defaultStyle, color: '#ff4d4f' }} />
    default:
      return <UserOutlined style={{ ...defaultStyle, color: '#999' }} />
  }
}

const getActivityColor = (activityType: number, status?: string): string => {
  if (status === 'current') return 'blue'
  switch (activityType) {
    case 1: return 'blue'
    case 2: return 'green'
    case 3:
    case 7:
    case 8: return 'red'
    case 4: return 'blue'
    case 5: return 'purple'
    case 6: return 'orange'
    default: return 'gray'
  }
}

const getActivityTag = (activityType: number) => {
  const map: Record<number, { color: string; label: string }> = {
    1: { color: 'blue', label: '发起' },
    2: { color: 'green', label: '同意' },
    3: { color: 'red', label: '拒绝' },
    4: { color: 'blue', label: '转审' },
    5: { color: 'purple', label: '加签' },
    6: { color: 'orange', label: '委派' },
    7: { color: 'red', label: '驳回' },
    8: { color: 'default', label: '撤回' }
  }
  const item = map[activityType] || { color: 'default', label: '未知' }
  return <Tag color={item.color}>{item.label}</Tag>
}

const ApprovalTimeline: React.FC<ApprovalTimelineProps> = ({ history = [], loading = false }) => {
  if (!history.length) {
    return (
      <div style={{ padding: 40, textAlign: 'center', color: '#999' }}>
        <ClockCircleOutlined style={{ fontSize: 48, marginBottom: 12 }} />
        <div>暂无审批历史</div>
      </div>
    )
  }

  const items = history.map((record, index) => {
    const isCurrent = record.status === 'current'
    const color = getActivityColor(record.activityType, record.status)

    return {
      color,
      dot: getActivityIcon(record.activityType, record.status),
      children: (
        <Card
          size="small"
          style={{
            marginBottom: 16,
            border: isCurrent ? '1px solid #1890ff' : undefined,
            boxShadow: isCurrent ? '0 2px 8px rgba(24,144,255,0.15)' : undefined
          }}
        >
          <Space direction="vertical" size={8} style={{ width: '100%' }}>
            <Space size={8} wrap>
              <Text strong style={{ fontSize: 15 }}>
                {record.nodeName}
              </Text>
              {getActivityTag(record.activityType)}
              {isCurrent && <Tag color="processing">审批中</Tag>}
            </Space>

            <Space size={8}>
              <Avatar size={24} src={record.operatorAvatar} icon={<UserOutlined />}>
                {record.operatorName?.charAt(0)}
              </Avatar>
              <Space direction="vertical" size={0}>
                <Space size={8}>
                  <Text strong>{record.operatorName}</Text>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {record.operatorDeptName}
                  </Text>
                </Space>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {dayjs(record.operateTime).format('YYYY-MM-DD HH:mm:ss')}
                </Text>
              </Space>
            </Space>

            {(record.targetUserName || record.targetNodeName) && (
              <Space size={8} style={{ fontSize: 13 }}>
                {record.targetUserId && (
                  <Text type="secondary">
                    → {record.activityType === 5 ? '加签给' : record.activityType === 7 ? '驳回到' : '转交'}：
                    <Text strong>{record.targetUserName}</Text>
                  </Text>
                )}
                {record.targetNodeName && record.activityType === 7 && (
                  <Text type="secondary">
                    节点：<Text strong>{record.targetNodeName}</Text>
                  </Text>
                )}
              </Space>
            )}

            {record.actionRemark && (
              <div style={{ background: '#f6f8fa', padding: 8, borderRadius: 4 }}>
                <Paragraph style={{ margin: 0, fontSize: 13 }}>
                  {record.actionRemark}
                </Paragraph>
              </div>
            )}

            {record.attachmentList?.length ? (
              <Space size={[8, 4]} wrap>
                <Text type="secondary">
                  <PaperClipOutlined style={{ marginRight: 4 }} />
                  附件：
                </Text>
                {record.attachmentList.map(att => (
                  <a key={att.id} href={att.accessUrl || att.downloadUrl} target="_blank" rel="noreferrer">
                    {att.fileName}
                  </a>
                ))}
              </Space>
            ) : null}

            {record.signatureUrl && (
              <div style={{ marginTop: 4 }}>
                <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                  手写签名：
                </Text>
                <Image
                  src={record.signatureUrl}
                  alt="签名"
                  width={150}
                  height={60}
                  style={{
                    border: '1px dashed #d9d9d9',
                    borderRadius: 4,
                    objectFit: 'contain',
                    background: '#fff'
                  }}
                  preview={{ mask: '查看签名' }}
                />
              </div>
            )}

            {record.duration !== undefined && index > 0 && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                ⏱ 处理用时：{formatDuration(record.duration)}
              </Text>
            )}
          </Space>
        </Card>
      )
    }
  })

  return (
    <Timeline
      mode="left"
      items={items}
      style={{ padding: '16px 0' }}
    />
  )
}

export default ApprovalTimeline
