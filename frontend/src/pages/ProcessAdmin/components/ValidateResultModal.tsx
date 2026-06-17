import { Modal, Table, Tag, Space, Button, Typography, Alert } from 'antd'
import { CheckCircleOutlined, CloseCircleOutlined, ExclamationCircleOutlined, InfoCircleOutlined } from '@ant-design/icons'
import type { ValidateResultVO } from '@/types'

const { Text, Title } = Typography

interface ValidateResultModalProps {
  open: boolean
  onCancel: () => void
  results: ValidateResultVO[]
}

export default function ValidateResultModal({ open, onCancel, results }: ValidateResultModalProps) {
  const hasError = results.some((r) => r.level === 'error')
  const hasWarning = results.some((r) => r.level === 'warning')

  const getLevelIcon = (level: string) => {
    switch (level) {
      case 'error':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
      case 'warning':
        return <ExclamationCircleOutlined style={{ color: '#faad14' }} />
      default:
        return <InfoCircleOutlined style={{ color: '#1890ff' }} />
    }
  }

  const getLevelTag = (level: string) => {
    switch (level) {
      case 'error':
        return <Tag color="red">错误</Tag>
      case 'warning':
        return <Tag color="orange">警告</Tag>
      default:
        return <Tag color="blue">提示</Tag>
    }
  }

  const columns = [
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 80,
      render: (level: string) => (
        <Space>
          {getLevelIcon(level)}
          {getLevelTag(level)}
        </Space>
      )
    },
    {
      title: '节点',
      key: 'node',
      width: 180,
      render: (_: any, record: ValidateResultVO) => (
        <Space>
          {record.nodeId && (
            <Text code style={{ fontSize: 12 }}>
              {record.nodeId}
            </Text>
          )}
          {record.nodeName && <Text>{record.nodeName}</Text>}
        </Space>
      )
    },
    {
      title: '问题描述',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
      render: (text: string) => <Text>{text}</Text>
    }
  ]

  return (
    <Modal
      title={
        <Space>
          {hasError ? (
            <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
          ) : hasWarning ? (
            <ExclamationCircleOutlined style={{ color: '#faad14' }} />
          ) : (
            <CheckCircleOutlined style={{ color: '#52c41a' }} />
          )}
          <span>流程校验结果</span>
        </Space>
      }
      open={open}
      onCancel={onCancel}
      width={720}
      footer={[
        <Button key="close" type="primary" onClick={onCancel}>
          知道了
        </Button>
      ]}
    >
      {results.length === 0 ? (
        <Alert
          message="校验通过"
          description="流程设计规范，未发现问题"
          type="success"
          showIcon
          icon={<CheckCircleOutlined />}
          style={{ marginBottom: 16 }}
        />
      ) : (
        <>
          <Space style={{ marginBottom: 16, width: '100%' }} direction="vertical">
            {hasError && (
              <Alert
                message="发现错误"
                description={`共 ${results.filter((r) => r.level === 'error').length} 个错误需要修复才能发布`}
                type="error"
                showIcon
              />
            )}
            {!hasError && hasWarning && (
              <Alert
                message="发现警告"
                description={`共 ${results.filter((r) => r.level === 'warning').length} 个警告，建议修复`}
                type="warning"
                showIcon
              />
            )}
          </Space>
          <Table
            rowKey={(record, idx) => `${record.level}-${record.nodeId}-${idx}`}
            columns={columns}
            dataSource={results}
            pagination={false}
            size="middle"
            locale={{ emptyText: '未发现问题' }}
          />
        </>
      )}
    </Modal>
  )
}
