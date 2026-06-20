import React, { useState, useEffect } from 'react'
import { Modal, Image, Spin, message, Empty } from 'antd'
import type { AttachmentVO } from '@/types/approval'

interface AttachmentPreviewModalProps {
  open: boolean
  attachment: AttachmentVO | null
  onCancel: () => void
}

const AttachmentPreviewModal: React.FC<AttachmentPreviewModalProps> = ({ open, attachment, onCancel }) => {
  const [loading, setLoading] = useState(false)
  const [previewUrl, setPreviewUrl] = useState<string>('')

  useEffect(() => {
    if (open && attachment) {
      if (attachment.previewable && attachment.previewUrl) {
        setPreviewUrl(attachment.previewUrl)
      }
    }
  }, [open, attachment])

  const isImage = (suffix: string) => {
    const imageSuffixes = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
    return imageSuffixes.includes(suffix?.toLowerCase())
  }

  const isPdf = (suffix: string) => {
    return suffix?.toLowerCase() === 'pdf'
  }

  const renderContent = () => {
    if (!attachment) return <Empty description="暂无附件" />

    if (!attachment.previewable) {
      return <Empty description="该文件类型不支持在线预览" />
    }

    if (loading) {
      return (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
          <Spin tip="加载中..." />
        </div>
      )
    }

    if (isImage(attachment.fileSuffix)) {
      return (
        <div style={{ textAlign: 'center', maxHeight: '70vh', overflow: 'auto' }}>
          <Image
            src={previewUrl || attachment.accessUrl}
            alt={attachment.fileName}
            style={{ maxWidth: '100%', maxHeight: '70vh' }}
            onLoadStart={() => setLoading(true)}
            onLoad={() => setLoading(false)}
            onError={() => {
              setLoading(false)
              message.error('图片加载失败')
            }}
          />
        </div>
      )
    }

    if (isPdf(attachment.fileSuffix)) {
      return (
        <div style={{ height: '70vh' }}>
          <iframe
            src={previewUrl || attachment.accessUrl}
            title={attachment.fileName}
            style={{ width: '100%', height: '100%', border: 'none' }}
            onLoad={() => setLoading(false)}
            onError={() => {
              setLoading(false)
              message.error('PDF加载失败')
            }}
          />
        </div>
      )
    }

    return <Empty description="该文件类型不支持在线预览" />
  }

  return (
    <Modal
      title={attachment?.fileName || '附件预览'}
      open={open}
      onCancel={onCancel}
      footer={null}
      width={800}
      destroyOnClose
      maskClosable={false}
    >
      {renderContent()}
    </Modal>
  )
}

export default AttachmentPreviewModal
