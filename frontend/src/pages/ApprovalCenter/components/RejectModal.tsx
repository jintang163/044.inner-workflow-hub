import React, { useState, useRef, useMemo } from 'react'
import { Modal, Form, Input, Select, Alert, Upload, Button, Radio, Space, Typography, Tag, Tooltip } from 'antd'
import { ExclamationCircleOutlined, WarningOutlined, InfoCircleOutlined } from '@ant-design/icons'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import SignaturePad, { SignaturePadRef } from './SignaturePad'
import type { ApprovalTaskVO, ApprovalHistoryVO } from '@/types/approval'

const { TextArea } = Input
const { Text } = Typography

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

  const rejectCount = task.rejectCount ?? 0
  const maxRejectCount = task.maxRejectCount ?? 5
  const rejectableNodeIds = task.rejectableNodeIds

  const reachMaxReject = rejectCount >= maxRejectCount

  const nodeOptions = useMemo(() => {
    const seenNodeIds = new Set<string>()
    const options: Array<{ value: string; label: string; nodeName: string; operatorName: string; operateTime?: string }> = [
      { value: 'start', label: '驳回到发起人（重新提交）', nodeName: '发起节点', operatorName: task.startUserName }
    ]
    seenNodeIds.add('start')

    const sortedHistory = [...history]
      .filter(h => {
        if (!h.nodeId) return false
        if (h.nodeId === task.nodeId) return false
        if (h.activityType === 1) return false
        if (h.status && h.status !== 'approved' && h.status !== 'rejected') return false
        if (rejectableNodeIds && rejectableNodeIds.length > 0 && !rejectableNodeIds.includes(h.nodeId)) return false
        return true
      })
      .sort((a, b) => new Date(b.operateTime).getTime() - new Date(a.operateTime).getTime())

    for (const h of sortedHistory) {
      if (!h.nodeId || seenNodeIds.has(h.nodeId)) continue
      seenNodeIds.add(h.nodeId)
      options.push({
        value: h.nodeId,
        label: `驳回到 ${h.nodeName}（${h.operatorName}）`,
        nodeName: h.nodeName,
        operatorName: h.operatorName,
        operateTime: h.operateTime
      })
    }

    return options
  }, [history, task.nodeId, task.startUserName, rejectableNodeIds])

  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    setFileList(prev => [...prev, file as UploadFile])
    return false
  }

  const handleOk = async () => {
    try {
      if (reachMaxReject) return

      const values = await form.validateFields()
      let signatureUrl: string | undefined
      if (task.needSignature && sigPadRef.current) {
        signatureUrl = sigPadRef.current.save() || undefined
        if (!signatureUrl) return
      }
      const targetNode = nodeOptions.find(n => n.value === values.targetNodeId)
      await onOk({
        targetNodeId: values.targetNodeId,
        targetNodeName: targetNode?.nodeName,
        actionRemark: values.actionRemark,
        resetFormData: values.resetFormData,
        signatureUrl,
        attachmentIds: fileList.map(f => f.uid as number).filter(Boolean)
      })
      form.resetFields()
      sigPadRef.current?.clear()
      setFileList([])
    } catch (_) {}
  }

  const handleCancel = () => {
    form.resetFields()
    sigPadRef.current?.clear()
    setFileList([])
    onCancel()
  }

  return (
    <Modal
      open={open}
      title={
        <Space>
          <span>驳回审批</span>
          {rejectCount > 0 && (
            <Tag color={reachMaxReject ? 'red' : 'orange'}>
              已驳回 {rejectCount}/{maxRejectCount} 次
            </Tag>
          )}
        </Space>
      }
      onCancel={handleCancel}
      onOk={handleOk}
      okText={reachMaxReject ? '已达驳回上限' : '确认驳回'}
      okButtonProps={{ danger: true, disabled: reachMaxReject }}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        {reachMaxReject ? (
          <Alert
            message={
              <Space>
                <WarningOutlined style={{ color: '#ff4d4f' }} />
                <span>已达到最大驳回次数（{maxRejectCount}次），无法继续驳回</span>
              </Space>
            }
            type="error"
            showIcon={false}
            style={{ marginBottom: 0 }}
          />
        ) : (
          <Alert
            message={
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Space>
                  <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                  <Text strong>驳回将使审批流程回退，被驳回人可修改后重新提交审批</Text>
                </Space>
                {rejectCount > 0 && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    当前流程已驳回 <Text strong type="warning">{rejectCount}</Text> 次，
                    最多允许驳回 <Text strong>{maxRejectCount}</Text> 次，
                    超过上限将无法驳回
                  </Text>
                )}
              </Space>
            }
            type="warning"
            showIcon={false}
            style={{ marginBottom: 0 }}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          initialValues={{ targetNodeId: 'start', resetFormData: false }}
          disabled={reachMaxReject}
        >
          <Form.Item
            name="targetNodeId"
            label={
              <Space>
                <span>驳回目标节点</span>
                <Tooltip title="支持跨级驳回至之前任意已完成的审批节点">
                  <InfoCircleOutlined style={{ color: '#8c8c8c', fontSize: 12 }} />
                </Tooltip>
              </Space>
            }
            rules={[{ required: true, message: '请选择驳回节点' }]}
          >
            <Select
              placeholder="请选择驳回到哪个节点"
              options={nodeOptions}
              allowClear
              disabled={reachMaxReject}
              optionLabelProp="label"
            />
          </Form.Item>

          <Form.Item
            name="resetFormData"
            label={
              <Space>
                <span>回退时表单数据处理</span>
                <Tooltip title="选择保留则驳回后的节点可看到原始填写的数据，选择重置则需要重新填写">
                  <InfoCircleOutlined style={{ color: '#8c8c8c', fontSize: 12 }} />
                </Tooltip>
              </Space>
            }
            rules={[{ required: true, message: '请选择表单数据处理方式' }]}
          >
            <Radio.Group disabled={reachMaxReject}>
              <Radio value={false}>
                <Space size={4}>
                  <span>保留表单数据</span>
                  <Tag color="blue" style={{ margin: 0 }}>推荐</Tag>
                </Space>
                <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4, paddingLeft: 22 }}>
                  被驳回人可在原有数据基础上修改后重新提交
                </div>
              </Radio>
              <Radio value={true}>
                <Space size={4}>
                  <span>重置表单数据</span>
                </Space>
                <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4, paddingLeft: 22 }}>
                  清空所有已填写内容，被驳回人需重新填写全部表单
                </div>
              </Radio>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            name="actionRemark"
            label={
              <Space>
                <span style={{ color: '#ff4d4f' }}>*</span>
                <span>驳回理由</span>
              </Space>
            }
            rules={[
              { required: true, message: '请输入驳回理由' },
              { min: 5, message: '驳回理由至少5个字，请详细说明驳回原因' },
              { max: 500, message: '驳回理由不能超过500个字' }
            ]}
            style={{ marginBottom: task.needSignature ? 8 : 0 }}
          >
            <TextArea
              rows={4}
              placeholder="请详细说明驳回原因（不少于5个字），便于申请人了解问题并进行修改"
              showCount
              maxLength={500}
              disabled={reachMaxReject}
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
              disabled={reachMaxReject}
            >
              <Button icon={<ExclamationCircleOutlined />} disabled={reachMaxReject}>
                上传附件
              </Button>
            </Upload>
          </Form.Item>
        </Form>
      </Space>
    </Modal>
  )
}

export default RejectModal
