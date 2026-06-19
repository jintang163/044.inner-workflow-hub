import React from 'react'
import { Card, Tag, Space, Avatar, Typography, Progress, List, Image } from 'antd'
import {
  UserOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  TeamOutlined,
  StopOutlined,
  PaperClipOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import type { MultiInstanceSignVO, SignerStatusVO } from '@/types/approval'
import { formatDuration } from './TaskTableColumns'

const { Text, Paragraph } = Typography

interface MultiInstanceSignCardProps {
  signData: MultiInstanceSignVO
}

const getSignerStatusTag = (status: number, statusName: string) => {
  switch (status) {
    case 1:
      return <Tag icon={<CheckCircleOutlined />} color="success">{statusName}</Tag>
    case 2:
      return <Tag icon={<CloseCircleOutlined />} color="error">{statusName}</Tag>
    default:
      return <Tag icon={<ClockCircleOutlined />} color="processing">{statusName}</Tag>
  }
}

const getSignerStatusColor = (status: number) => {
  switch (status) {
    case 1:
      return '#52c41a'
    case 2:
      return '#ff4d4f'
    default:
      return '#1890ff'
  }
}

const getApproveTypeTag = (data: MultiInstanceSignVO) => {
  if (data.completionTypeName) {
    return <Tag color="purple">{data.completionTypeName}</Tag>
  }
  if (data.approveTypeName) {
    return <Tag color="purple">{data.approveTypeName}</Tag>
  }
  return null
}

const MultiInstanceSignCard: React.FC<MultiInstanceSignCardProps> = ({ signData }) => {
  const {
    nodeName,
    totalSigners,
    approvedCount,
    rejectedCount,
    pendingCount,
    passPercentage,
    vetoEnabled,
    signers,
    progressText
  } = signData

  const approvePercent = totalSigners > 0 ? Math.round((approvedCount / totalSigners) * 100) : 0
  const rejectPercent = totalSigners > 0 ? Math.round((rejectedCount / totalSigners) * 100) : 0

  const statusColor = rejectedCount > 0 ? '#ff4d4f' : (pendingCount > 0 ? '#1890ff' : '#52c41a')

  return (
    <Card
      style={{ borderRadius: 8, marginBottom: 16 }}
      size="small"
      title={
        <Space size={8} wrap>
          <TeamOutlined style={{ color: '#722ed1' }} />
          <Text strong style={{ fontSize: 15 }}>
            {nodeName}
          </Text>
          {getApproveTypeTag(signData)}
          {vetoEnabled && (
            <Tag icon={<StopOutlined />} color="warning">一票否决</Tag>
          )}
          {passPercentage != null && passPercentage > 0 && (
            <Tag color="blue">通过阈值 {passPercentage}%</Tag>
          )}
        </Space>
      }
    >
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Space direction="vertical" size={4} style={{ width: '100%' }}>
          <Space size={16} wrap>
            <Space size={4}>
              <CheckCircleOutlined style={{ color: '#52c41a' }} />
              <Text type="secondary">已通过：</Text>
              <Text strong style={{ color: '#52c41a' }}>{approvedCount}</Text>
              <Text type="secondary">人</Text>
            </Space>
            <Space size={4}>
              <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
              <Text type="secondary">已拒绝：</Text>
              <Text strong style={{ color: '#ff4d4f' }}>{rejectedCount}</Text>
              <Text type="secondary">人</Text>
            </Space>
            <Space size={4}>
              <ClockCircleOutlined style={{ color: '#1890ff' }} />
              <Text type="secondary">待处理：</Text>
              <Text strong style={{ color: '#1890ff' }}>{pendingCount}</Text>
              <Text type="secondary">人</Text>
            </Space>
            <Space size={4}>
              <Text type="secondary">进度：</Text>
              <Text strong style={{ color: statusColor }}>{progressText}</Text>
            </Space>
          </Space>

          <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
            <Progress
              percent={approvePercent}
              showInfo={false}
              strokeColor="#52c41a"
              size="small"
              style={{ flex: 1 }}
            />
            {rejectPercent > 0 && (
              <Progress
                percent={rejectPercent}
                showInfo={false}
                strokeColor="#ff4d4f"
                size="small"
                style={{ flex: rejectPercent > 50 ? 1 : 0.3 }}
              />
            )}
          </div>
        </Space>

        <List
          size="small"
          dataSource={signers}
          renderItem={(signer: SignerStatusVO) => (
            <List.Item
              style={{
                padding: '12px 8px',
                borderLeft: `3px solid ${getSignerStatusColor(signer.signStatus)}`,
                background: signer.signStatus === 0 ? '#fafcff' : '#fff',
                borderRadius: 4,
                marginBottom: 8
              }}
            >
              <Space direction="vertical" size={6} style={{ width: '100%' }}>
                <Space size={8} wrap style={{ width: '100%', justifyContent: 'space-between' }}>
                  <Space size={8} wrap>
                    <Avatar
                      size={28}
                      src={signer.userAvatar}
                      icon={<UserOutlined />}
                    >
                      {signer.userName?.charAt(0)}
                    </Avatar>
                    <Space direction="vertical" size={0}>
                      <Space size={6}>
                        <Text strong>{signer.userName}</Text>
                        {getSignerStatusTag(signer.signStatus, signer.signStatusName)}
                      </Space>
                      {signer.deptName && (
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {signer.deptName}
                        </Text>
                      )}
                    </Space>
                  </Space>
                  <Space direction="vertical" size={0} style={{ textAlign: 'right' }}>
                    {signer.assignTime && (
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        分配：{dayjs(signer.assignTime).format('MM-DD HH:mm')}
                      </Text>
                    )}
                    {signer.handleTime && (
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        处理：{dayjs(signer.handleTime).format('MM-DD HH:mm')}
                      </Text>
                    )}
                  </Space>
                </Space>

                {signer.comment && (
                  <div style={{ background: '#f6f8fa', padding: '8px 12px', borderRadius: 4 }}>
                    <Paragraph style={{ margin: 0, fontSize: 13, whiteSpace: 'pre-wrap' }}>
                      {signer.comment}
                    </Paragraph>
                  </div>
                )}

                {signer.signatureUrl && (
                  <div style={{ marginTop: 4 }}>
                    <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                      手写签名：
                    </Text>
                    <Image
                      src={signer.signatureUrl}
                      alt="签名"
                      width={120}
                      height={48}
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

                {signer.duration !== undefined && signer.handleTime && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    ⏱ 处理用时：{formatDuration(signer.duration)}
                  </Text>
                )}
              </Space>
            </List.Item>
          )}
        />
      </Space>
    </Card>
  )
}

export default MultiInstanceSignCard
