import React, { useState, useRef } from 'react'
import { Button, Space, Modal, Form, Input, Upload, Alert, Tag, Tooltip, message } from 'antd'
import {
  CheckOutlined,
  CloseOutlined,
  SwapOutlined,
  TeamOutlined,
  UserOutlined,
  RollbackOutlined,
  ExclamationCircleOutlined,
  SettingOutlined
} from '@ant-design/icons'
import type { ApprovalHistoryVO } from '@/types/approval'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import SignaturePad, { SignaturePadRef } from './SignaturePad'
import TransferModal from './TransferModal'
import AddSignModal from './AddSignModal'
import DelegateModal from './DelegateModal'
import RejectModal from './RejectModal'
import CommentTemplateSelect from '@/components/business/CommentTemplateSelect'
import type { ApprovalTaskVO } from '@/types/approval'

const { TextArea } = Input
const { confirm } = Modal

interface ApprovalActionBarProps {
  task: ApprovalTaskVO
  history?: ApprovalHistoryVO[]
  onApprove?: (data: { comment?: string; signatureUrl?: string; attachmentIds?: number[] }) => Promise<void> | void
  onReject?: (data: { comment?: string; signatureUrl?: string; attachmentIds?: number[]; targetNodeId?: string; targetNodeName?: string; resetFormData?: boolean }) => Promise<void> | void
  onTransfer?: (data: any) => Promise<void> | void
  onAddSign?: (data: any) => Promise<void> | void
  onDelegate?: (data: any) => Promise<void> | void
  loading?: boolean
}

interface ApprovalModalProps {
  open: boolean
  title: string
  type: 'approve' | 'reject'
  needComment: boolean
  needSignature: boolean
  onCancel: () => void
  onConfirm: (data: { comment?: string; signatureUrl?: string; attachmentIds?: number[] }) => Promise<void> | void
  loading?: boolean
}

const BaseApprovalModal: React.FC<ApprovalModalProps> = ({
  open,
  title,
  type,
  needComment,
  needSignature,
  onCancel,
  onConfirm,
  loading
}) => {
  const [form] = Form.useForm()
  const sigPadRef = useRef<SignaturePadRef>(null)
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const typeConfig = {
    approve: { okText: '确认同意', okType: 'primary' as const, danger: false },
    reject: { okText: '确认拒绝', okType: 'primary' as const, danger: true }
  }[type]

  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    setFileList(prev => [...prev, file as UploadFile])
    return false
  }

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      let signatureUrl: string | undefined
      if (needSignature && sigPadRef.current) {
        signatureUrl = sigPadRef.current.save() || undefined
        if (!signatureUrl) {
          return
        }
      }
      await onConfirm({
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
      title={title}
      onCancel={onCancel}
      onOk={handleOk}
      okText={typeConfig.okText}
      okButtonProps={{ danger: typeConfig.danger, type: typeConfig.okType }}
      confirmLoading={loading}
      width={560}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        {needComment && (
          <Alert
            message="审批意见为必填项"
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}
        <Form.Item
          name="comment"
          label="审批意见"
          rules={needComment ? [{ required: true, message: '请输入审批意见' }] : []}
          style={{ marginBottom: needSignature ? 8 : 0 }}
        >
          <CommentTemplateSelect
            rows={4}
            placeholder={type === 'approve' ? '请输入同意理由（可选）' : '请输入拒绝理由'}
            maxLength={500}
            showCount
          />
        </Form.Item>
        {needSignature && (
          <Form.Item label="手写签名" required style={{ marginBottom: 8 }}>
            <SignaturePad ref={sigPadRef} width={480} height={160} />
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

const ApprovalActionBar: React.FC<ApprovalActionBarProps> = ({
  task,
  history = [],
  onApprove,
  onReject,
  onTransfer,
  onAddSign,
  onDelegate,
  loading = false
}) => {
  const [approveModalOpen, setApproveModalOpen] = useState(false)
  const [rejectModalOpen, setRejectModalOpen] = useState(false)
  const [transferModalOpen, setTransferModalOpen] = useState(false)
  const [addSignModalOpen, setAddSignModalOpen] = useState(false)
  const [delegateModalOpen, setDelegateModalOpen] = useState(false)
  const [customRejectOpen, setCustomRejectOpen] = useState(false)

  const rejectCount = task.rejectCount ?? 0
  const maxRejectCount = task.maxRejectCount ?? 5
  const reachMaxReject = rejectCount >= maxRejectCount

  const handleQuickApprove = () => {
    if (task.needComment || task.needSignature) {
      setApproveModalOpen(true)
      return
    }
    confirm({
      title: '确认同意',
      icon: <ExclamationCircleOutlined />,
      content: `确定同意该审批吗？（${task.taskNo}）`,
      okText: '确认同意',
      okButtonProps: { type: 'primary' },
      cancelText: '取消',
      onOk: () => onApprove?.({})
    })
  }

  const handleRejectClick = () => {
    if (task.canReject) {
      setCustomRejectOpen(true)
    } else {
      setRejectModalOpen(true)
    }
  }

  const rejectButton = task.canReject ? (
    <Tooltip
      title={
        reachMaxReject
          ? `已达到最大驳回次数（${maxRejectCount}次），无法继续驳回`
          : `支持驳回至之前任意节点（跨级驳回），当前已驳回 ${rejectCount}/${maxRejectCount} 次`
      }
    >
      <Button
        danger
        icon={<RollbackOutlined />}
        size="large"
        onClick={() => !reachMaxReject && setCustomRejectOpen(true)}
        loading={loading}
        disabled={reachMaxReject}
      >
        <Space size={4}>
          <span>驳回</span>
          {rejectCount > 0 && (
            <Tag color={reachMaxReject ? 'red' : 'orange'} style={{ margin: 0 }}>
              {rejectCount}/{maxRejectCount}
            </Tag>
          )}
        </Space>
      </Button>
    </Tooltip>
  ) : (
    <Button
      danger
      icon={<CloseOutlined />}
      size="large"
      onClick={handleRejectClick}
      loading={loading}
    >
      拒绝
    </Button>
  )

  return (
    <div
      style={{
        position: 'sticky',
        bottom: 0,
        background: '#fff',
        padding: '16px 24px',
        borderTop: '1px solid #f0f0f0',
        zIndex: 10
      }}
    >
      <Space size={12} wrap>
        <Button
          type="primary"
          icon={<CheckOutlined />}
          size="large"
          onClick={handleQuickApprove}
          loading={loading}
        >
          同意
        </Button>
        {!task.canReject && (
          <Button
            danger
            icon={<CloseOutlined />}
            size="large"
            onClick={handleRejectClick}
            loading={loading}
          >
            拒绝
          </Button>
        )}
        {rejectButton}
        {task.canTransfer && (
          <Button
            icon={<SwapOutlined />}
            size="large"
            onClick={() => setTransferModalOpen(true)}
            loading={loading}
          >
            转审
          </Button>
        )}
        {task.canAddSign && (
          <Button
            icon={<TeamOutlined />}
            size="large"
            onClick={() => setAddSignModalOpen(true)}
            loading={loading}
          >
            加签
          </Button>
        )}
        {task.canDelegate && (
          <Button
            icon={<UserOutlined />}
            size="large"
            onClick={() => setDelegateModalOpen(true)}
            loading={loading}
          >
            委派
          </Button>
        )}
      </Space>

      <BaseApprovalModal
        open={approveModalOpen}
        title="审批 - 同意"
        type="approve"
        needComment={task.needComment}
        needSignature={task.needSignature}
        onCancel={() => setApproveModalOpen(false)}
        onConfirm={async (data) => {
          await onApprove?.(data)
          setApproveModalOpen(false)
        }}
        loading={loading}
      />

      <BaseApprovalModal
        open={rejectModalOpen}
        title="审批 - 拒绝"
        type="reject"
        needComment={task.needComment || true}
        needSignature={task.needSignature}
        onCancel={() => setRejectModalOpen(false)}
        onConfirm={async (data) => {
          await onReject?.(data)
          setRejectModalOpen(false)
        }}
        loading={loading}
      />

      <TransferModal
        open={transferModalOpen}
        task={task}
        onCancel={() => setTransferModalOpen(false)}
        onOk={async (data) => {
          await onTransfer?.(data)
          setTransferModalOpen(false)
        }}
        loading={loading}
      />

      <AddSignModal
        open={addSignModalOpen}
        task={task}
        onCancel={() => setAddSignModalOpen(false)}
        onOk={async (data) => {
          await onAddSign?.(data)
          setAddSignModalOpen(false)
        }}
        loading={loading}
      />

      <DelegateModal
        open={delegateModalOpen}
        task={task}
        onCancel={() => setDelegateModalOpen(false)}
        onOk={async (data) => {
          await onDelegate?.(data)
          setDelegateModalOpen(false)
        }}
        loading={loading}
      />

      <RejectModal
        open={customRejectOpen}
        task={task}
        history={history}
        onCancel={() => setCustomRejectOpen(false)}
        onOk={async (data) => {
          await onReject?.(data)
          setCustomRejectOpen(false)
        }}
        loading={loading}
      />
    </div>
  )
}

export default ApprovalActionBar
