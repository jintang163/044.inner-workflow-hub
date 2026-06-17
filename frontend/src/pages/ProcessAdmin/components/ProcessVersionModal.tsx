import { useState, useEffect } from 'react'
import { Modal, Table, Button, Tag, Space, App, Row, Col, Input } from 'antd'
import { HistoryOutlined, RollbackOutlined, EyeOutlined, CopyOutlined } from '@ant-design/icons'
import { processApi } from '@/api'
import type { ProcessVersionVO } from '@/types'

interface ProcessVersionModalProps {
  open: boolean
  onCancel: () => void
  processDefinitionId: number
  onSuccess?: () => void
}

export default function ProcessVersionModal({
  open,
  onCancel,
  processDefinitionId,
  onSuccess
}: ProcessVersionModalProps) {
  const { message, modal } = App.useApp()
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<ProcessVersionVO[]>([])
  const [xmlModalVisible, setXmlModalVisible] = useState(false)
  const [currentXml, setCurrentXml] = useState('')
  const [currentVersion, setCurrentVersion] = useState<ProcessVersionVO | null>(null)

  const loadData = async () => {
    setLoading(true)
    try {
      const data = (await processApi.versionList(processDefinitionId)) as unknown as ProcessVersionVO[]
      setList(data || [])
    } catch (e) {
      // error handled
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open && processDefinitionId) {
      loadData()
    }
  }, [open, processDefinitionId])

  const handleRollback = (record: ProcessVersionVO) => {
    modal.confirm({
      title: '确认回滚',
      content: `确定要回滚到版本 v${record.version} 吗？当前版本的数据将被覆盖。`,
      okText: '确认回滚',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await processApi.versionRollback(record.id)
          message.success('回滚成功')
          onSuccess?.()
          loadData()
        } catch (e) {
          // error handled
        }
      }
    })
  }

  const handleViewXml = (record: ProcessVersionVO) => {
    setCurrentVersion(record)
    setCurrentXml(record.bpmnXml || '')
    setXmlModalVisible(true)
  }

  const handleCopyXml = async () => {
    try {
      await navigator.clipboard.writeText(currentXml)
      message.success('已复制到剪贴板')
    } catch (e) {
      message.error('复制失败，请手动选择复制')
    }
  }

  const columns = [
    {
      title: '版本号',
      dataIndex: 'version',
      key: 'version',
      width: 100,
      render: (v: number, record: ProcessVersionVO) => (
        <Space>
          <Tag color="blue">v{v}</Tag>
          {record.status === 1 && <Tag color="green">当前</Tag>}
        </Space>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: number) =>
        v === 1 ? <Tag color="green">已发布</Tag> : <Tag color="default">历史</Tag>
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      ellipsis: true
    },
    {
      title: '发布时间',
      dataIndex: 'deployTime',
      key: 'deployTime',
      width: 180
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_: any, record: ProcessVersionVO) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewXml(record)}
          >
            查看XML
          </Button>
          {record.status !== 1 && (
            <Button
              type="link"
              size="small"
              danger
              icon={<RollbackOutlined />}
              onClick={() => handleRollback(record)}
            >
              回滚
            </Button>
          )}
        </Space>
      )
    }
  ]

  return (
    <>
      <Modal
        title={
          <Space>
            <HistoryOutlined />
            版本历史
          </Space>
        }
        open={open}
        onCancel={onCancel}
        width={800}
        footer={null}
        destroyOnClose
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={list}
          loading={loading}
          pagination={false}
          size="middle"
        />
      </Modal>

      <Modal
        title={`BPMN XML - 版本 v${currentVersion?.version}`}
        open={xmlModalVisible}
        onCancel={() => setXmlModalVisible(false)}
        width={900}
        footer={[
          <Button key="copy" icon={<CopyOutlined />} onClick={handleCopyXml}>
            复制
          </Button>,
          <Button key="close" type="primary" onClick={() => setXmlModalVisible(false)}>
            关闭
          </Button>
        ]}
      >
        <Input.TextArea
          value={currentXml}
          readOnly
          rows={20}
          style={{ fontFamily: 'Consolas, Monaco, monospace', fontSize: 12 }}
        />
      </Modal>
    </>
  )
}
