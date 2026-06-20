import React, { useState } from 'react'
import {
  List,
  Button,
  Space,
  Checkbox,
  Tag,
  Tooltip,
  message,
  Empty,
  Typography,
  Modal
} from 'antd'
import {
  EyeOutlined,
  DownloadOutlined,
  DeleteOutlined,
  FilePdfOutlined,
  FileImageOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FilePptOutlined,
  FileUnknownOutlined,
  InboxOutlined,
  CheckCircleOutlined
} from '@ant-design/icons'
import type { AttachmentVO } from '@/types/approval'
import { approvalApi } from '@/api'
import AttachmentPreviewModal from './AttachmentPreviewModal'
import dayjs from 'dayjs'

const { Text, Paragraph } = Typography

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const getFileIcon = (suffix: string) => {
  const s = suffix?.toLowerCase()
  if (['pdf'].includes(s)) return <FilePdfOutlined style={{ color: '#ff4d4f', fontSize: 24 }} />
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(s)) return <FileImageOutlined style={{ color: '#52c41a', fontSize: 24 }} />
  if (['doc', 'docx'].includes(s)) return <FileWordOutlined style={{ color: '#1890ff', fontSize: 24 }} />
  if (['xls', 'xlsx'].includes(s)) return <FileExcelOutlined style={{ color: '#52c41a', fontSize: 24 }} />
  if (['ppt', 'pptx'].includes(s)) return <FilePptOutlined style={{ color: '#fa8c16', fontSize: 24 }} />
  return <FileUnknownOutlined style={{ color: '#8c8c8c', fontSize: 24 }} />
}

interface AttachmentListProps {
  attachments: AttachmentVO[]
  editable?: boolean
  showCheckbox?: boolean
  onRemove?: (id: number) => void
  onListChange?: (attachments: AttachmentVO[]) => void
  showUpload?: boolean
  bizType?: string
  bizId?: string
  nodeId?: string
}

const AttachmentList: React.FC<AttachmentListProps> = ({
  attachments,
  editable = false,
  showCheckbox = true,
  onRemove,
  onListChange,
  showUpload = false,
  bizType,
  bizId,
  nodeId
}) => {
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [previewModalVisible, setPreviewModalVisible] = useState(false)
  const [previewAttachment, setPreviewAttachment] = useState<AttachmentVO | null>(null)
  const [batchDownloading, setBatchDownloading] = useState(false)
  const [uploading, setUploading] = useState(false)

  const handlePreview = (attachment: AttachmentVO) => {
    if (!attachment.previewable) {
      message.info('该文件类型不支持在线预览')
      return
    }
    setPreviewAttachment(attachment)
    setPreviewModalVisible(true)
  }

  const handleDownload = (attachment: AttachmentVO) => {
    const url = attachment.downloadUrl || attachment.accessUrl
    if (!url) {
      message.error('下载地址不存在')
      return
    }
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.fileName
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  const handleBatchDownload = async () => {
    if (selectedIds.length === 0) {
      message.warning('请先选择要下载的附件')
      return
    }
    try {
      setBatchDownloading(true)
      const blob = await approvalApi.attachmentBatchDownload(selectedIds)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'attachments.zip'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      message.success(`成功下载 ${selectedIds.length} 个文件`)
    } catch (err: any) {
      message.error(err?.message || '批量下载失败')
    } finally {
      setBatchDownloading(false)
    }
  }

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '删除附件',
      content: '确定删除该附件吗？',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          if (onRemove) {
            onRemove(id)
          } else {
            await approvalApi.attachmentRemove(id)
            if (onListChange) {
              const newList = attachments.filter(a => a.id !== id)
              onListChange(newList)
            }
          }
          message.success('删除成功')
        } catch (err: any) {
          message.error(err?.message || '删除失败')
        }
      }
    })
  }

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedIds(attachments.map(a => a.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelect = (id: number, checked: boolean) => {
    if (checked) {
      setSelectedIds(prev => [...prev, id])
    } else {
      setSelectedIds(prev => prev.filter(i => i !== id))
    }
  }

  const handleUploadClick = () => {
    const input = document.createElement('input')
    input.type = 'file'
    input.multiple = true
    input.accept = '.jpg,.jpeg,.png,.gif,.bmp,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx'
    input.onchange = async (e: any) => {
      const files: FileList = e.target.files
      if (!files || files.length === 0) return

      for (let i = 0; i < files.length; i++) {
        const file = files[i]
        try {
          setUploading(true)
          const result = await approvalApi.attachmentUpload(file, bizType, bizId, nodeId)
          if (onListChange) {
            onListChange([...attachments, result])
          }
          message.success(`${file.name} 上传成功`)
        } catch (err: any) {
          message.error(`${file.name} 上传失败: ${err?.message || '未知错误'}`)
        } finally {
          setUploading(false)
        }
      }
    }
    input.click()
  }

  if (attachments.length === 0) {
    return (
      <div style={{ padding: '40px 0' }}>
        <Empty
          description={
            <Space direction="vertical" size={8}>
              <span>暂无附件</span>
              {showUpload && editable && (
                <Button
                  type="primary"
                  icon={<InboxOutlined />}
                  onClick={handleUploadClick}
                  loading={uploading}
                >
                  上传附件
                </Button>
              )}
            </Space>
          }
        />
      </div>
    )
  }

  return (
    <div>
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          {showCheckbox && (
            <Checkbox
              checked={selectedIds.length === attachments.length && attachments.length > 0}
              indeterminate={selectedIds.length > 0 && selectedIds.length < attachments.length}
              onChange={(e) => handleSelectAll(e.target.checked)}
            >
              全选
            </Checkbox>
          )}
          {showUpload && editable && (
            <Button
              type="primary"
              size="small"
              icon={<InboxOutlined />}
              onClick={handleUploadClick}
              loading={uploading}
            >
              上传
            </Button>
          )}
        </Space>
        <Space>
          {selectedIds.length > 0 && (
            <Tag color="blue">已选 {selectedIds.length} 项</Tag>
          )}
          <Button
            size="small"
            icon={<DownloadOutlined />}
            onClick={handleBatchDownload}
            disabled={selectedIds.length === 0}
            loading={batchDownloading}
          >
            批量下载
          </Button>
        </Space>
      </div>

      <List
        size="small"
        dataSource={attachments}
        renderItem={(item) => (
          <List.Item
            key={item.id}
            style={{
              padding: '10px 12px',
              borderRadius: 6,
              marginBottom: 4,
              background: selectedIds.includes(item.id) ? '#e6f7ff' : '#fafafa',
              border: selectedIds.includes(item.id) ? '1px solid #91d5ff' : '1px solid #f0f0f0'
            }}
          >
            <List.Item.Meta
              avatar={
                <div style={{ width: 40, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {showCheckbox ? (
                    <Checkbox
                      checked={selectedIds.includes(item.id)}
                      onChange={(e) => handleSelect(item.id, e.target.checked)}
                      onClick={(e) => e.stopPropagation()}
                    />
                  ) : (
                    getFileIcon(item.fileSuffix)
                  )}
                </div>
              }
              title={
                <Space>
                  {!showCheckbox && getFileIcon(item.fileSuffix)}
                  <Text ellipsis style={{ maxWidth: 300 }} title={item.fileName}>
                    {item.fileName}
                  </Text>
                  {item.previewable && (
                    <Tag color="green" size="small" style={{ fontSize: 11 }}>
                      可预览
                    </Tag>
                  )}
                </Space>
              }
              description={
                <Space size={16} style={{ fontSize: 12, color: '#8c8c8c' }}>
                  <span>{formatFileSize(item.fileSize)}</span>
                  <span>{dayjs(item.createTime).format('YYYY-MM-DD HH:mm')}</span>
                  {item.uploadUserName && <span>上传人: {item.uploadUserName}</span>}
                </Space>
              }
            />
            <Space size={4}>
              {item.previewable && (
                <Tooltip title="在线预览">
                  <Button
                    type="text"
                    size="small"
                    icon={<EyeOutlined />}
                    onClick={() => handlePreview(item)}
                  />
                </Tooltip>
              )}
              <Tooltip title="下载">
                <Button
                  type="text"
                  size="small"
                  icon={<DownloadOutlined />}
                  onClick={() => handleDownload(item)}
                />
              </Tooltip>
              {editable && (
                <Tooltip title="删除">
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => handleDelete(item.id)}
                  />
                </Tooltip>
              )}
            </Space>
          </List.Item>
        )}
      />

      <AttachmentPreviewModal
        open={previewModalVisible}
        attachment={previewAttachment}
        onCancel={() => setPreviewModalVisible(false)}
      />
    </div>
  )
}

export default AttachmentList
