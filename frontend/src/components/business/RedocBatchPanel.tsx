import React, { useEffect, useState, useMemo } from 'react'
import {
  Modal, Table, Checkbox, Button, Space, message, Spin, Empty, Tag, Tooltip
} from 'antd'
import { PrinterOutlined, DownloadOutlined, FilePdfOutlined } from '@ant-design/icons'
import type { WfRedocGeneratedVO } from '@/types/redoc'
import { redocApi } from '@/api'
import dayjs from 'dayjs'

interface RedocBatchPanelProps {
  open: boolean
  instanceNo?: string
  instanceNos?: string[]
  onCancel: () => void
}

const RedocBatchPanel: React.FC<RedocBatchPanelProps> = ({ open, instanceNo, instanceNos, onCancel }) => {
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [list, setList] = useState<WfRedocGeneratedVO[]>([])
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  useEffect(() => {
    if (open) {
      loadList()
    }
  }, [open, instanceNo, instanceNos])

  const loadList = async () => {
    setLoading(true)
    try {
      if (instanceNo) {
        const data = await redocApi.listByInstance(instanceNo)
        setList(data || [])
      } else if (instanceNos && instanceNos.length > 0) {
        const all: WfRedocGeneratedVO[] = []
        for (const no of instanceNos) {
          const data = await redocApi.listByInstance(no)
          if (data) all.push(...data)
        }
        setList(all)
      } else {
        const data = await redocApi.pageGenerated(1, 100)
        setList((data as any)?.records || (data as any)?.list || [])
      }
    } catch (e: any) {
      message.error(e?.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }

  const selectedAll = useMemo(() => list.length > 0 && selectedIds.length === list.length, [list, selectedIds])
  const indeterminate = selectedIds.length > 0 && selectedIds.length < list.length

  const toggleAll = (checked: boolean) => {
    setSelectedIds(checked ? list.map(l => l.id) : [])
  }

  const toggleRow = (id: number, checked: boolean) => {
    setSelectedIds(checked ? [...selectedIds, id] : selectedIds.filter(x => x !== id))
  }

  const handleBatchPrint = async () => {
    if (selectedIds.length === 0) {
      message.warning('请先选择要打印的文件')
      return
    }
    try {
      setGenerating(true)
      const blob = await redocApi.batchPrint({ ids: selectedIds })
      triggerDownload(blob, `红头文件_批量打印_${dayjs().format('YYYYMMDDHHmmss')}.pdf`)
      message.success(`已合并 ${selectedIds.length} 个PDF用于打印`)
    } catch (e: any) {
      message.error(e?.message || '批量打印失败')
    } finally {
      setGenerating(false)
    }
  }

  const handleBatchDownload = async () => {
    if (selectedIds.length === 0) {
      message.warning('请先选择要下载的文件')
      return
    }
    try {
      setGenerating(true)
      const blob = await redocApi.batchDownload({ ids: selectedIds })
      triggerDownload(blob, `红头文件_批量下载_${dayjs().format('YYYYMMDDHHmmss')}.zip`)
      message.success(`已打包 ${selectedIds.length} 个文件`)
    } catch (e: any) {
      message.error(e?.message || '批量下载失败')
    } finally {
      setGenerating(false)
    }
  }

  const triggerDownload = (blob: Blob, filename: string) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    setTimeout(() => URL.revokeObjectURL(url), 10000)
  }

  const columns = [
    {
      title: '选择',
      width: 60,
      render: (_: any, record: WfRedocGeneratedVO) => (
        <Checkbox
          checked={selectedIds.includes(record.id)}
          onChange={(e) => toggleRow(record.id, e.target.checked)}
        />
      )
    },
    {
      title: '文件标题',
      dataIndex: 'fileTitle',
      ellipsis: true,
      render: (v: string, r: WfRedocGeneratedVO) => (
        <Space>
          <FilePdfOutlined style={{ color: '#d4380d' }} />
          <Tooltip title={v}>{v}</Tooltip>
          {r.sealApplied === 1 && <Tag color="green" style={{ marginLeft: 4 }}>已盖章</Tag>}
          {r.signatureApplied === 1 && <Tag color="blue">已签名</Tag>}
        </Space>
      )
    },
    { title: '审批单号', dataIndex: 'instanceNo', width: 180 },
    { title: '模板', dataIndex: 'templateName', width: 160 },
    { title: '文号', dataIndex: 'approvalNo', width: 180, render: (v: string) => v || '-' },
    { title: '生成人', dataIndex: 'generateByName', width: 100 },
    {
      title: '生成时间',
      dataIndex: 'generateTime',
      width: 160,
      render: (v: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'
    },
    { title: '打印次数', dataIndex: 'printCount', width: 80, render: (v: number) => `${v || 0}次` }
  ]

  return (
    <Modal
      title="红头文件批量打印/下载"
      open={open}
      onCancel={onCancel}
      width={900}
      destroyOnClose
      maskClosable={false}
      footer={
        <Space>
          <Checkbox indeterminate={indeterminate} checked={selectedAll} onChange={(e) => toggleAll(e.target.checked)}>
            全选（{selectedIds.length}/{list.length}）
          </Checkbox>
          <div style={{ flex: 1 }} />
          <Button onClick={onCancel}>关闭</Button>
          <Button
            icon={<DownloadOutlined />}
            onClick={handleBatchDownload}
            loading={generating}
            disabled={selectedIds.length === 0}
          >
            批量下载
          </Button>
          <Button
            icon={<PrinterOutlined />}
            type="primary"
            danger
            onClick={handleBatchPrint}
            loading={generating}
            disabled={selectedIds.length === 0}
          >
            批量打印(合并PDF)
          </Button>
        </Space>
      }
    >
      <Spin spinning={loading}>
        {list.length === 0 ? (
          <Empty description="暂无已生成的红头文件" style={{ padding: 40 }} />
        ) : (
          <Table
            rowKey="id"
            size="small"
            dataSource={list}
            columns={columns}
            pagination={{ pageSize: 8, showSizeChanger: false }}
            scroll={{ x: 1000 }}
          />
        )}
      </Spin>
    </Modal>
  )
}

export default RedocBatchPanel
