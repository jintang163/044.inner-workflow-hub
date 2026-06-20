import React, { useState, useRef } from 'react'
import {
  Drawer,
  List,
  Avatar,
  Tag,
  Button,
  Space,
  Form,
  Input,
  Typography,
  Divider,
  Alert,
  message,
  Empty
} from 'antd'
import {
  UserOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import SignaturePad, { SignaturePadRef } from './SignaturePad'
import CommentTemplateSelect from '@/components/business/CommentTemplateSelect'
import type { ApprovalTaskVO } from '@/types/approval'

const { Text, Paragraph } = Typography
const { TextArea } = Input

interface BatchApprovalDrawerProps {
  open: boolean
  selectedTasks: ApprovalTaskVO[]
  onClose: () => void
  onRemoveTask: (task: ApprovalTaskVO) => void
  onBatchApprove: (data: { comment?: string; signatureUrl?: string }) => Promise<void> | void
  onBatchReject: (data: { comment: string }) => Promise<void> | void
  loading?: boolean
}

const BatchApprovalDrawer: React.FC<BatchApprovalDrawerProps> = ({
  open,
  selectedTasks = [],
  onClose,
  onRemoveTask,
  onBatchApprove,
  onBatchReject,
  loading = false
}) => {
  const [form] = Form.useForm()
  const sigPadRef = useRef<SignaturePadRef>(null)
  const [activeAction, setActiveAction] = useState<'approve' | 'reject' | null>(null)

  const needSignature = selectedTasks.some(t => t.needSignature)
  const needCommentApprove = selectedTasks.some(t => t.needComment)

  const handleBatchApprove = async () => {
    setActiveAction('approve')
    try {
      const values = await form.validateFields()
      let signatureUrl: string | undefined
      if (needSignature && sigPadRef.current) {
        signatureUrl = sigPadRef.current.save() || undefined
        if (!signatureUrl) {
          message.warning('请先签名')
          return
        }
      }
      await onBatchApprove({ comment: values.comment, signatureUrl })
      form.resetFields()
      sigPadRef.current?.clear()
    } catch (_) {}
  }

  const handleBatchReject = async () => {
    setActiveAction('reject')
    try {
      const values = await form.validateFields()
      if (!values.comment?.trim()) {
        message.warning('请输入驳回理由')
        return
      }
      await onBatchReject({ comment: values.comment })
      form.resetFields()
      sigPadRef.current?.clear()
    } catch (_) {}
  }

  return (
    <Drawer
      title={
        <Space>
          <span>批量审批</span>
          <Tag color="blue">已选 {selectedTasks.length} 项</Tag>
        </Space>
      }
      width={560}
      open={open}
      onClose={onClose}
      destroyOnClose
      extra={
        <Button onClick={onClose}>取消</Button>
      }
    >
      {selectedTasks.length === 0 ? (
        <Empty description="请先在列表中选择需要审批的任务" style={{ marginTop: 100 }} />
      ) : (
        <>
          <Alert
            message={`您已选择 ${selectedTasks.length} 条待办任务进行批量处理`}
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />

          <div style={{ marginBottom: 16 }}>
            <Text strong style={{ fontSize: 14 }}>待处理任务列表</Text>
            <List
              size="small"
              style={{ marginTop: 8, maxHeight: 240, overflowY: 'auto' }}
              dataSource={selectedTasks}
              renderItem={(task) => (
                <List.Item
                  key={task.id}
                  actions={[
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => onRemoveTask(task)}
                    />
                  ]}
                >
                  <List.Item.Meta
                    avatar={
                      <Avatar size={32} src={task.startUserAvatar} icon={<UserOutlined />}>
                        {task.startUserName?.charAt(0)}
                      </Avatar>
                    }
                    title={
                      <Space size={8}>
                        <Text strong style={{ fontSize: 13 }}>{task.taskNo}</Text>
                        <Tag color="blue" style={{ margin: 0 }}>{task.processName}</Tag>
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size={0}>
                        <Paragraph ellipsis style={{ margin: 0, fontSize: 13 }}>
                          {task.title}
                        </Paragraph>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          发起人：{task.startUserName} · 节点：{task.nodeName}
                        </Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </div>

          <Divider orientation="left" plain>统一审批意见</Divider>

          <Form form={form} layout="vertical">
            <Form.Item
              name="comment"
              label={
                <Space>
                  <span>审批意见</span>
                  {(activeAction === 'reject' || needCommentApprove) && (
                    <span style={{ color: '#ff4d4f' }}>*</span>
                  )}
                </Space>
              }
              rules={
                activeAction === 'reject'
                  ? [{ required: true, message: '批量驳回必须填写理由' }]
                  : needCommentApprove
                  ? [{ required: true, message: '部分任务要求必须填写意见' }]
                  : []
              }
              style={{ marginBottom: needSignature ? 8 : 16 }}
            >
              <CommentTemplateSelect
                rows={4}
                placeholder={
                  activeAction === 'reject'
                    ? '请输入统一驳回理由（必填）'
                    : '请输入统一审批意见' + (needCommentApprove ? '（必填）' : '（可选）')
                }
                showCount
                maxLength={500}
                onManageClick={() => message.info('请到\"意见模板管理\"页面进行模板管理')}
              />
            </Form.Item>

            {needSignature && (
              <>
                <Form.Item label="手写签名" required style={{ marginBottom: 16 }}>
                  <Alert
                    message="选中的任务中有需要签名的审批，请完成签名"
                    type="warning"
                    showIcon
                    style={{ marginBottom: 8 }}
                  />
                  <SignaturePad ref={sigPadRef} width={480} height={160} />
                </Form.Item>
              </>
            )}

            <Space size={12} style={{ display: 'flex', justifyContent: 'center' }}>
              <Button
                type="primary"
                size="large"
                icon={<CheckCircleOutlined />}
                onClick={handleBatchApprove}
                loading={loading && activeAction === 'approve'}
              >
                批量同意（{selectedTasks.length}）
              </Button>
              <Button
                danger
                size="large"
                icon={<CloseCircleOutlined />}
                onClick={handleBatchReject}
                loading={loading && activeAction === 'reject'}
              >
                批量拒绝（{selectedTasks.length}）
              </Button>
            </Space>
          </Form>
        </>
      )}
    </Drawer>
  )
}

export default BatchApprovalDrawer
