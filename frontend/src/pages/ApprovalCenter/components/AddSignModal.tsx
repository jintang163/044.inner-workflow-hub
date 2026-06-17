import React, { useState, useRef } from 'react'
import { Modal, Form, Input, Select, Radio, Alert, Upload, Button } from 'antd'
import { ExclamationCircleOutlined } from '@ant-design/icons'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import SignaturePad, { SignaturePadRef } from './SignaturePad'
import type { ApprovalTaskVO } from '@/types/approval'

const { TextArea } = Input

interface AddSignModalProps {
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

const AddSignModal: React.FC<AddSignModalProps> = ({
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
      const targetUserNames = (values.targetUserIds || [])
        .map((id: number) => mockUsers.find(u => u.value === id)?.label)
        .filter(Boolean)

      await onOk({
        targetUserIds: values.targetUserIds || [],
        targetUserNames,
        signType: values.signType,
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
      title="加签"
      onCancel={onCancel}
      onOk={handleOk}
      okText="确认加签"
      confirmLoading={loading}
      width={560}
      destroyOnClose
    >
      <Alert
        message="加签：在审批流程中插入额外的审批人员，加签完成后流程继续"
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />
      <Form form={form} layout="vertical" initialValues={{ signType: 'BEFORE' }}>
        <Form.Item
          name="signType"
          label="加签类型"
          rules={[{ required: true, message: '请选择加签类型' }]}
        >
          <Radio.Group>
            <Radio value="BEFORE">前加签（在我之前审批）</Radio>
            <Radio value="AFTER">后加签（在我之后审批）</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item
          name="targetUserIds"
          label="选择加签人"
          rules={[{ required: true, message: '请选择至少一位加签人' }]}
        >
          <Select
            mode="multiple"
            placeholder="请选择加签人员（可多选）"
            options={mockUsers}
            showSearch
            optionFilterProp="label"
            allowClear
            maxTagCount="responsive"
          />
        </Form.Item>
        <Form.Item
          name="comment"
          label="加签意见"
          rules={task.needComment ? [{ required: true, message: '请输入加签意见' }] : []}
          style={{ marginBottom: task.needSignature ? 8 : 0 }}
        >
          <TextArea
            rows={3}
            placeholder="请输入加签理由"
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

export default AddSignModal
