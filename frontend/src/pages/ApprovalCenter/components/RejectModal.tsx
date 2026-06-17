import React, { useState, useRef } from 'react'
import { Modal, Form, Input, Select, Alert, Upload, Button } from 'antd'
import { ExclamationCircleOutlined } from '@ant-design/icons'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import SignaturePad, { SignaturePadRef } from './SignaturePad'
import type { ApprovalTaskVO, ApprovalHistoryVO } from '@/types/approval'

const { TextArea } = Input

interface RejectModalProps {
  open: boolean
  task: ApprovalTaskVO
  history?: ApprovalHistoryVO[]
  onCancel: () => void
  onOk: (data: any) => Promise<void> | void
  loading?: boolean
}

const RejectModal: React.FC<RejectModalProps> = ({
  open,
  task,
  history = [],
  onCancel,
  onOk,
  loading
}) => {
  const [form] = Form.useForm()
  const sigPadRef = useRef<SignaturePadRef>(null)
  const [fileList, setFileList] = useState<UploadFile[]>([])

  const nodeOptions = [
    { value: 'start', label: '驳回到发起人（重新提交）' },
    ...history
      .filter(h => h.nodeId && h.nodeId !== task.nodeId && h.activityType !== 1)
      .map(h => ({
        value: h.nodeId,
        label: `驳回到 ${h.nodeName}（${h.operatorName}）`
      }))
  ]

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
        targetNodeId: values.targetNodeId,
        targetNodeName: nodeOptions.find(n => n.value === values.targetNodeId)?.label,
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
      title="驳回"
      onCancel={onCancel}
      onOk={handleOk}
      okText="确认驳回"
      okButtonProps={{ danger: true }}
      confirmLoading={loading}
      width={560}
      destroyOnClose
    >
      <Alert
        message="驳回将使审批流程回退，被驳回人可修改后重新提交审批"
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
      />
      <Form form={form} layout="vertical" initialValues={{ targetNodeId: 'start' }}>
        <Form.Item
          name="targetNodeId"
          label="驳回目标节点"
          rules={[{ required: true, message: '请选择驳回节点' }]}
        >
          <Select
            placeholder="请选择驳回到哪个节点"
            options={nodeOptions}
            allowClear
          />
        </Form.Item>
        <Form.Item
          name="comment"
          label="驳回理由"
          rules={[{ required: true, message: '请输入驳回理由' }]}
          style={{ marginBottom: task.needSignature ? 8 : 0 }}
        >
          <TextArea
            rows={4}
            placeholder="请详细说明驳回原因，便于申请人修改"
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

export default RejectModal
