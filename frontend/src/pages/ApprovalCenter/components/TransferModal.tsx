import React, { useState, useRef } from 'react'
import { Modal, Form, Input, Select, Alert, Upload, Button } from 'antd'
import { ExclamationCircleOutlined } from '@ant-design/icons'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import SignaturePad, { SignaturePadRef } from './SignaturePad'
import type { ApprovalTaskVO } from '@/types/approval'

const { TextArea } = Input

interface TransferModalProps {
  open: boolean
  task: ApprovalTaskVO
  onCancel: () => void
  onOk: (data: any) => Promise<void> | void
  loading?: boolean
}

const mockUsers = [
  { value: 1, label: '张三 (技术部)' },
  { value: 2, label: '李四 (产品部)' },
  { value: 3, label: '王五 (市场部)' },
  { value: 4, label: '赵六 (财务部)' },
  { value: 5, label: '钱七 (人事部)' }
]

const TransferModal: React.FC<TransferModalProps> = ({
  open,
  task,
  onCancel,
  onOk,
  loading
}) => {
  const [form] = Form.useForm()
  const sigPadRef = useRef<SignaturePadRef>(null)
  const [fileList, setFileList] = useState<UploadFile[]>([])

  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    setFileList(prev => [...prev, file as UploadFile])
    return false
  }

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      let signatureUrl: string | undefined
      if (task.needSignature && sigPadRef.current) {
        signatureUrl = sigPadRef.current.save() || undefined
        if (!signatureUrl) return
      }
      await onOk({
        targetUserId: values.targetUserId,
        targetUserName: mockUsers.find(u => u.value === values.targetUserId)?.label,
        comment: values.comment,
        signatureUrl,
        attachmentIds: fileList.map(f => f.uid as number).filter(Boolean)
      })
      form.resetFields()
      sigPadRef.current?.clear()
      setFileList([])
    } catch (_) {}
  }

  return (
    <Modal
      open={open}
      title="转审"
      onCancel={onCancel}
      onOk={handleOk}
      okText="确认转审"
      confirmLoading={loading}
      width={560}
      destroyOnClose
    >
      <Alert
        message="转审后该任务将移交给被转审人处理，您将不再处理此任务"
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />
      <Form form={form} layout="vertical">
        <Form.Item
          name="targetUserId"
          label="选择转审人"
          rules={[{ required: true, message: '请选择转审人' }]}
        >
          <Select
            placeholder="请选择转审给哪位人员"
            options={mockUsers}
            showSearch
            optionFilterProp="label"
            allowClear
          />
        </Form.Item>
        <Form.Item
          name="comment"
          label="转审意见"
          rules={task.needComment ? [{ required: true, message: '请输入转审意见' }] : []}
          style={{ marginBottom: task.needSignature ? 8 : 0 }}
        >
          <TextArea
            rows={3}
            placeholder="请输入转审理由"
            showCount
            maxLength={500}
          />
        </Form.Item>
        {task.needSignature && (
          <Form.Item label="手写签名" required style={{ marginBottom: 8 }}>
            <SignaturePad ref={sigPadRef} width={480} height={140} />
          </Form.Item>
        )}
        <Form.Item label="附件上传" style={{ marginBottom: 0 }}>
          <Upload
            fileList={fileList}
            beforeUpload={beforeUpload}
            onRemove={(file) => setFileList(prev => prev.filter(f => f.uid !== file.uid))}
            multiple
          >
            <Button icon={<ExclamationCircleOutlined />}>上传附件</Button>
          </Upload>
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default TransferModal
