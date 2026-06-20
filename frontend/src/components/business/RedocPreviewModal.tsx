import React, { useEffect, useState } from 'react'
import { Modal, Tabs, Button, Space, Descriptions, Tag, message, Spin, Empty } from 'antd'
import { PrinterOutlined, DownloadOutlined, FilePdfOutlined, FileWordOutlined } from '@ant-design/icons'
import type { WfRedocGeneratedVO } from '@/types/redoc'
import { redocApi } from '@/api'
import dayjs from 'dayjs'

interface RedocPreviewModalProps {
  open: boolean
  generated: WfRedocGeneratedVO | null
  onCancel: () => void
}

const RedocPreviewModal: React.FC<RedocPreviewModalProps> = ({ open, generated, onCancel }) => {
  const [pdfLoading, setPdfLoading] = useState(false)
  const [wordLoading, setWordLoading] = useState(false)

  useEffect(() => {
    if (open) {
      setPdfLoading(!!generated?.pdfPreviewUrl)
      setWordLoading(!!generated?.wordPreviewUrl)
    }
  }, [open, generated])

  const handlePrint = () => {
    if (!generated?.pdfPreviewUrl) {
      message.warning('无PDF文件，无法打印')
      return
    }
    redocApi.markPrinted(generated.id).then(() => {
      const w = window.open(generated.pdfPreviewUrl, '_blank')
      if (w) {
        w.addEventListener('load', () => {
          try { w.print() } catch (e) {}
        })
      }
    })
  }

  const handleDownload = (type: 'pdf' | 'word') => {
    if (!generated) return
    const url = type === 'pdf' ? generated.pdfDownloadUrl : generated.wordDownloadUrl
    if (!url) {
      message.warning(`无${type.toUpperCase()}文件`)
      return
    }
    redocApi.markDownloaded(generated.id).then(() => {
      const a = document.createElement('a')
      a.href = url
      a.target = '_blank'
      a.download = type === 'pdf' ? (generated.pdfFileName || '红头文件.pdf') : (generated.wordFileName || '红头文件.docx')
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    })
  }

  const renderInfo = () => {
    if (!generated) return null
    return (
      <Descriptions size="small" column={2} bordered style={{ marginBottom: 16 }}>
        <Descriptions.Item label="文件标题">{generated.fileTitle}</Descriptions.Item>
        <Descriptions.Item label="模板名称">{generated.templateName}</Descriptions.Item>
        <Descriptions.Item label="审批单号">{generated.instanceNo}</Descriptions.Item>
        <Descriptions.Item label="文号">{generated.approvalNo || '-'}</Descriptions.Item>
        <Descriptions.Item label="生成人">{generated.generateByName || '-'}</Descriptions.Item>
        <Descriptions.Item label="生成时间">{generated.generateTime ? dayjs(generated.generateTime).format('YYYY-MM-DD HH:mm:ss') : '-'}</Descriptions.Item>
        <Descriptions.Item label="盖章">
          {generated.sealApplied === 1 ? <Tag color="green">已盖章</Tag> : <Tag>未盖章</Tag>}
        </Descriptions.Item>
        <Descriptions.Item label="数字签名">
          {generated.signatureApplied === 1 ? <Tag color="blue">已签名</Tag> : <Tag>未签名</Tag>}
        </Descriptions.Item>
        <Descriptions.Item label="打印次数">{generated.printCount || 0} 次</Descriptions.Item>
        <Descriptions.Item label="下载次数">{generated.downloadCount || 0} 次</Descriptions.Item>
      </Descriptions>
    )
  }

  const renderPdf = () => {
    if (!generated?.pdfPreviewUrl) return <Empty description="暂无PDF文件" />
    return (
      <div style={{ height: '60vh', position: 'relative' }}>
        {pdfLoading && (
          <div style={{
            position: 'absolute', inset: 0, display: 'flex',
            justifyContent: 'center', alignItems: 'center', background: 'rgba(255,255,255,0.7)', zIndex: 10
          }}>
            <Spin tip="PDF加载中..." />
          </div>
        )}
        <iframe
          src={generated.pdfPreviewUrl}
          title="PDF预览"
          style={{ width: '100%', height: '100%', border: 'none' }}
          onLoad={() => setPdfLoading(false)}
          onError={() => { setPdfLoading(false); message.error('PDF加载失败') }}
        />
      </div>
    )
  }

  const renderWord = () => {
    if (!generated?.wordPreviewUrl) return <Empty description="暂无WORD文件" />
    return (
      <div style={{ height: '60vh', position: 'relative' }}>
        {wordLoading && (
          <div style={{
            position: 'absolute', inset: 0, display: 'flex',
            justifyContent: 'center', alignItems: 'center', background: 'rgba(255,255,255,0.7)', zIndex: 10
          }}>
            <Spin tip="WORD加载中..." />
          </div>
        )}
        <iframe
          src={generated.wordPreviewUrl}
          title="WORD预览"
          style={{ width: '100%', height: '100%', border: 'none' }}
          onLoad={() => setWordLoading(false)}
          onError={() => { setWordLoading(false); message.warning('在线预览WORD需要浏览器/Office Online支持') }}
        />
      </div>
    )
  }

  return (
    <Modal
      title="红头文件预览"
      open={open}
      onCancel={onCancel}
      width={900}
      destroyOnClose
      maskClosable={false}
      footer={
        <Space>
          <Button onClick={onCancel}>关闭</Button>
          <Button icon={<DownloadOutlined />} onClick={() => handleDownload('word')} disabled={!generated?.wordFileId}>
            下载WORD
          </Button>
          <Button icon={<DownloadOutlined />} type="primary" onClick={() => handleDownload('pdf')} disabled={!generated?.pdfFileId}>
            下载PDF
          </Button>
          <Button icon={<PrinterOutlined />} type="primary" danger onClick={handlePrint} disabled={!generated?.pdfFileId}>
            打印
          </Button>
        </Space>
      }
    >
      {renderInfo()}
      <Tabs
        defaultActiveKey="pdf"
        items={[
          {
            key: 'pdf',
            label: <span><FilePdfOutlined /> PDF</span>,
            children: renderPdf()
          },
          {
            key: 'word',
            label: <span><FileWordOutlined /> WORD</span>,
            children: renderWord()
          }
        ]}
      />
    </Modal>
  )
}

export default RedocPreviewModal
