import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import {
  Card,
  Space,
  Button,
  Tabs,
  Row,
  Col,
  Descriptions,
  Avatar,
  Tag,
  Typography,
  message,
  Spin,
  Empty,
  Breadcrumb,
  Modal,
  Form,
  Input,
  List,
  Divider,
  Alert
} from 'antd'
import {
  ArrowLeftOutlined,
  RollbackOutlined,
  UserOutlined,
  PrinterOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import ApprovalTimeline from './components/ApprovalTimeline'
import FlowDiagram from './components/FlowDiagram'
import ApprovalActionBar from './components/ApprovalActionBar'
import MultiInstanceSignCard from './components/MultiInstanceSignCard'
import ApprovalTrackingMap from './components/ApprovalTrackingMap'
import AiRecommendationCard from '@/components/business/AiRecommendationCard'
import { approvalApi, formApi, aiApi } from '@/api'
import type { ProcessInstanceVO, ApprovalHistoryVO, ApprovalTaskVO, MultiInstanceSignVO, TrackingMapVO } from '@/types/approval'
import type { FormilySchema } from '@/types/form'
import type { AiRecommendationVO } from '@/types/ai'
import FormRenderer from '@/components/FormRenderer'
import dayjs from 'dayjs'

const { Text, Title, Paragraph } = Typography
const { TextArea } = Input

const buildDefaultViewSchema = (formData: Record<string, any>): FormilySchema => {
  const props: Record<string, any> = {}
  const fieldLabels: Record<string, string> = {
    title: '申请标题',
    leaveType: '请假类型',
    startDate: '开始日期',
    endDate: '结束日期',
    leaveDays: '请假天数',
    contact: '联系电话',
    reason: '申请理由',
    handover: '工作交接',
    amount: '金额',
    totalAmount: '总金额',
    city: '城市',
    remark: '备注'
  }
  Object.keys(formData).forEach(key => {
    props[key] = {
      type: typeof formData[key] === 'number' ? 'number' : 'string',
      title: fieldLabels[key] || key,
      default: formData[key],
      'x-decorator': 'FormItem',
      'x-component': 'Input'
    }
  })
  return { type: 'object', properties: props }
}
const { TabPane } = Tabs
const { confirm } = Modal

const mockCcList = [
  { id: 1, userId: 201, userName: 'HR小刘', deptName: '人力资源部' },
  { id: 2, userId: 202, userName: '行政小王', deptName: '行政管理部' }
]

const ApprovalDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const location = useLocation() as any
  const passedTask = location.state?.task as ApprovalTaskVO | undefined

  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [instance, setInstance] = useState<ProcessInstanceVO | null>(null)
  const [history, setHistory] = useState<ApprovalHistoryVO[]>([])
  const [currentTask, setCurrentTask] = useState<ApprovalTaskVO | null>(null)
  const [activeTab, setActiveTab] = useState('form')
  const [withdrawForm] = Form.useForm()
  const [withdrawOpen, setWithdrawOpen] = useState(false)
  const [aiRecommendation, setAiRecommendation] = useState<AiRecommendationVO | null>(null)
  const [formSchema, setFormSchema] = useState<FormilySchema | null>(null)
  const [schemaLoading, setSchemaLoading] = useState(false)
  const [multiInstanceSignList, setMultiInstanceSignList] = useState<MultiInstanceSignVO[]>([])
  const [trackingMap, setTrackingMap] = useState<TrackingMapVO | null>(null)

  const instanceNo = id || ''

  const isStarter = instance?.startUserId === 1
  const hasPendingTask = currentTask?.taskStatus === 'PENDING'

  const rejectCount = history.filter(h => h.status === 'rejected').length
  const maxRejectCount = instance?.maxRejectCount ?? currentTask?.maxRejectCount ?? 5

  const mergedTask = currentTask ? {
    ...currentTask,
    rejectCount: currentTask.rejectCount ?? rejectCount,
    maxRejectCount: currentTask.maxRejectCount ?? maxRejectCount
  } : null

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [instRes, histRes] = await Promise.all([
        approvalApi.instanceDetail(instanceNo),
        approvalApi.approvalHistory(instanceNo)
      ])
      setInstance(instRes)
      setHistory(histRes)
      setMultiInstanceSignList(instRes?.multiInstanceSignList || [])
      setTrackingMap(instRes?.trackingMap || null)

      if (passedTask) {
        setCurrentTask(passedTask)
        setAiRecommendation(passedTask.aiRecommendation || null)
        if (!passedTask.aiRecommendation && passedTask.id) {
          try {
            const rec = await aiApi.getRecommendation(passedTask.id)
            setAiRecommendation(rec)
          } catch {
            // ignore
          }
        }
      } else {
        const todoListRes = await approvalApi.todoList({ pageNum: 1, pageSize: 100 })
        const matchedTask = todoListRes?.list?.find((t: ApprovalTaskVO) => t.instanceNo === instanceNo)
        if (matchedTask) {
          setCurrentTask(matchedTask)
          setAiRecommendation(matchedTask.aiRecommendation || null)
          if (!matchedTask.aiRecommendation && matchedTask.id) {
            try {
              const rec = await aiApi.getRecommendation(matchedTask.id)
              setAiRecommendation(rec)
            } catch {
              // ignore
            }
          }
        }
      }

      if (instRes && instRes.formId) {
        setSchemaLoading(true)
        try {
          const schema = await formApi.schemaGet(instRes.formId, instRes.formVersion)
          if (schema && schema.properties) {
            setFormSchema(schema)
          } else if (instRes.formSchema && instRes.formSchema.properties) {
            setFormSchema(instRes.formSchema)
          } else {
            setFormSchema(buildDefaultViewSchema(instRes.formData || {}))
          }
        } catch (_) {
          if (instRes.formSchema && instRes.formSchema.properties) {
            setFormSchema(instRes.formSchema)
          } else {
            setFormSchema(buildDefaultViewSchema(instRes.formData || {}))
          }
        } finally {
          setSchemaLoading(false)
        }
      } else if (instRes) {
        setFormSchema(buildDefaultViewSchema(instRes.formData || {}))
      }
    } catch (err: any) {
      message.error(err?.message || '加载详情失败')
    } finally {
      setLoading(false)
    }
  }, [instanceNo, passedTask])

  const handleAiAdopted = (adopted: number) => {
    setAiRecommendation(prev => prev ? { ...prev, adopted } : null)
    setCurrentTask(prev => prev && prev.aiRecommendation ? {
      ...prev,
      aiRecommendation: { ...prev.aiRecommendation, adopted }
    } : prev)
  }

  useEffect(() => {
    if (instanceNo) {
      loadData()
    }
  }, [instanceNo, loadData])

  const handleApprove = async (data: any) => {
    if (!currentTask) return
    try {
      setActionLoading(true)
      if (currentTask.aiRecommendationId && currentTask.aiRecommendation?.adopted === 0) {
        const isMatch = currentTask.aiRecommendation?.recommendedAction === 1
        await aiApi.recordAdoption(currentTask.aiRecommendationId, isMatch ? 1 : 2)
      }
      await approvalApi.approve({
        taskId: currentTask.flowableTaskId,
        instanceId: currentTask.instanceId,
        ...data
      })
      message.success('审批成功')
      loadData()
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleReject = async (data: any) => {
    if (!currentTask) return
    try {
      setActionLoading(true)
      if (currentTask.aiRecommendationId && currentTask.aiRecommendation?.adopted === 0) {
        const isMatch = currentTask.aiRecommendation?.recommendedAction === 0
        await aiApi.recordAdoption(currentTask.aiRecommendationId, isMatch ? 1 : 2)
      }
      await approvalApi.reject({
        taskId: currentTask.flowableTaskId,
        instanceId: currentTask.instanceId,
        ...data
      })
      message.success('已驳回')
      loadData()
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleTransfer = async (data: any) => {
    if (!currentTask) return
    try {
      setActionLoading(true)
      await approvalApi.transfer({
        taskId: currentTask.flowableTaskId,
        instanceId: currentTask.instanceId,
        ...data
      })
      message.success('转审成功')
      loadData()
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleAddSign = async (data: any) => {
    if (!currentTask) return
    try {
      setActionLoading(true)
      await approvalApi.addSign({
        taskId: currentTask.flowableTaskId,
        instanceId: currentTask.instanceId,
        ...data
      })
      message.success('加签成功')
      loadData()
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleDelegate = async (data: any) => {
    if (!currentTask) return
    try {
      setActionLoading(true)
      await approvalApi.delegate({
        taskId: currentTask.flowableTaskId,
        instanceId: currentTask.instanceId,
        ...data
      })
      message.success('委派成功')
      loadData()
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleWithdraw = async () => {
    if (!instance) return
    try {
      const values = await withdrawForm.validateFields()
      setActionLoading(true)
      await approvalApi.withdraw({
        instanceId: instance.id,
        comment: values.reason
      })
      message.success('撤回成功')
      setWithdrawOpen(false)
      loadData()
    } catch (err: any) {
      if (err?.errorFields) return
      message.error(err?.message || '撤回失败')
    } finally {
      setActionLoading(false)
    }
  }

  const historyNodeIds = history
    .filter(h => h.status === 'approved' && h.nodeId && h.nodeId !== 'start' && h.nodeId !== 'end')
    .map(h => h.nodeId)

  const statusTagColor: Record<number, string> = {
    1: 'processing',
    2: 'success',
    3: 'error',
    4: 'default'
  }

  const statusTagText: Record<number, string> = {
    1: '审批中',
    2: '已完成',
    3: '已驳回',
    4: '已撤回'
  }

  const renderFormContent = () => {
    if (!instance) return null
    const { formData, processName } = instance

    return (
      <Card
        type="inner"
        title={
          <Space>
            <span>📋</span>
            <span>{processName} - 申请信息</span>
          </Space>
        }
        style={{ borderRadius: 8 }}
      >
        <Spin spinning={schemaLoading} tip="加载表单中...">
          {formSchema ? (
            <div style={{ border: '1px solid #f5f5f5', padding: 16, borderRadius: 6, background: '#fafafa' }}>
              <FormRenderer
                schema={formSchema}
                formData={formData || {}}
                mode="view"
              />
            </div>
          ) : (
            <Descriptions
              bordered
              column={{ xs: 1, sm: 2, md: 2 }}
              size="middle"
              labelStyle={{ width: 140, fontWeight: 500, background: '#fafafa' }}
            >
              <Descriptions.Item label="申请标题">
                <Text strong style={{ fontSize: 15 }}>{formData.title}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="请假类型">
                <Tag color="blue">{formData.leaveType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="开始日期">
                <Text>{formData.startDate}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="结束日期">
                <Text>{formData.endDate}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="请假天数">
                <Text strong style={{ color: '#1890ff', fontSize: 16 }}>{formData.leaveDays} 天</Text>
              </Descriptions.Item>
              <Descriptions.Item label="联系电话">
                <Text>{formData.contact || '-'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="申请理由" span={2}>
                <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                  {formData.reason || '-'}
                </Paragraph>
              </Descriptions.Item>
              <Descriptions.Item label="工作交接" span={2}>
                <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                  {formData.handover || '-'}
                </Paragraph>
              </Descriptions.Item>
            </Descriptions>
          )}
        </Spin>
      </Card>
    )
  }

  return (
    <Spin spinning={loading} tip="加载中...">
      <div style={{ padding: 16 }}>
        <Card style={{ borderRadius: 8, marginBottom: 16 }} bodyStyle={{ padding: '16px 24px' }}>
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Breadcrumb
              items={[
                { title: <a onClick={() => navigate('/approval/todo')}>审批中心</a> },
                { title: instance?.processName || '审批详情' }
              ]}
            />

            <Space style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap' }}>
              <Space size={16} wrap>
                <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
                  返回
                </Button>
                <Title level={4} style={{ margin: 0 }}>
                  {instance?.title}
                </Title>
                {instance && (
                  <Tag color={statusTagColor[instance.instanceStatus]} style={{ fontSize: 14, padding: '4px 12px' }}>
                    {statusTagText[instance.instanceStatus]}
                  </Tag>
                )}
                {rejectCount > 0 && (
                  <Tag color={rejectCount >= maxRejectCount ? 'red' : 'orange'} style={{ fontSize: 14, padding: '4px 12px' }}>
                    已驳回 {rejectCount}/{maxRejectCount} 次
                  </Tag>
                )}
              </Space>

              <Space size={8} wrap>
                {isStarter && instance?.canWithdraw && (
                  <Button
                    icon={<RollbackOutlined />}
                    danger
                    onClick={() => setWithdrawOpen(true)}
                  >
                    撤回申请
                  </Button>
                )}
                <Button icon={<ReloadOutlined />} onClick={loadData}>
                  刷新
                </Button>
                <Button icon={<PrinterOutlined />}>
                  打印
                </Button>
              </Space>
            </Space>

            {instance && (
              <Descriptions column={{ xs: 2, sm: 3, md: 4, lg: 5 }} size="small">
                <Descriptions.Item label="审批单号">
                  <Text copyable>{instance.instanceNo}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="流程名称">
                  <Tag color="blue">{instance.processName}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="发起人">
                  <Space size={4}>
                    <Avatar size={20} src={instance.startUserAvatar} icon={<UserOutlined />}>
                      {instance.startUserName?.charAt(0)}
                    </Avatar>
                    <Text>{instance.startUserName}</Text>
                  </Space>
                </Descriptions.Item>
                <Descriptions.Item label="发起部门">
                  {instance.startDeptName}
                </Descriptions.Item>
                <Descriptions.Item label="发起时间">
                  {dayjs(instance.startTime).format('YYYY-MM-DD HH:mm')}
                </Descriptions.Item>
              </Descriptions>
            )}
          </Space>
        </Card>

        <Row gutter={16}>
          <Col xs={24} lg={7} xl={6}>
            <Card
              style={{ borderRadius: 8, position: 'sticky', top: 16 }}
              title={
                <Space>
                  <span>📌</span>
                  <span>审批流转</span>
                </Space>
              }
              bodyStyle={{ padding: 0 }}
            >
              <div style={{ maxHeight: 'calc(100vh - 300px)', overflowY: 'auto', padding: '4px 12px' }}>
                {history.length > 0 ? (
                  <ApprovalTimeline history={history} loading={loading} />
                ) : (
                  <Empty description="暂无流转记录" style={{ padding: 40 }} />
                )}
              </div>

              {mockCcList.length > 0 && (
                <div style={{ padding: '0 16px 16px' }}>
                  <Divider orientation="left" plain style={{ fontSize: 12 }}>
                    抄送人
                  </Divider>
                  <List
                    size="small"
                    dataSource={mockCcList}
                    renderItem={(user) => (
                      <List.Item style={{ padding: '4px 0' }}>
                        <Space size={6}>
                          <Avatar size={20} icon={<UserOutlined />}>
                            {user.userName?.charAt(0)}
                          </Avatar>
                          <Text style={{ fontSize: 12 }}>{user.userName}</Text>
                          <Text type="secondary" style={{ fontSize: 12 }}>· {user.deptName}</Text>
                        </Space>
                      </List.Item>
                    )}
                  />
                </div>
              )}
            </Card>
          </Col>

          <Col xs={24} lg={17} xl={18}>
            {hasPendingTask && aiRecommendation && (
              <AiRecommendationCard
                recommendation={aiRecommendation}
                onAdopted={handleAiAdopted}
                onIgnore={() => handleAiAdopted(2)}
              />
            )}

            <Card
              style={{ borderRadius: 8 }}
              bodyStyle={{ padding: 0 }}
            >
              <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                style={{ padding: '0 24px' }}
                tabBarStyle={{ marginBottom: 16 }}
                items={[
                  {
                    key: 'form',
                    label: (
                      <Space size={4}>
                        <span>📋</span>
                        <span>申请表单</span>
                      </Space>
                    )
                  },
                  ...(multiInstanceSignList.length > 0 ? [{
                    key: 'sign',
                    label: (
                      <Space size={4}>
                        <span>👥</span>
                        <span>会签/或签</span>
                      </Space>
                    )
                  }] : []),
                  {
                    key: 'tracking',
                    label: (
                      <Space size={4}>
                        <span>🗺️</span>
                        <span>跟踪地图</span>
                      </Space>
                    )
                  },
                  {
                    key: 'flow',
                    label: (
                      <Space size={4}>
                        <span>🔀</span>
                        <span>流程图</span>
                      </Space>
                    )
                  }
                ]}
              />

              <div style={{ padding: '0 24px 24px' }}>
                {activeTab === 'form' && (
                  instance ? renderFormContent() : <Empty description="暂无表单数据" />
                )}
                {activeTab === 'sign' && multiInstanceSignList.length > 0 && (
                  <Space direction="vertical" size={0} style={{ width: '100%' }}>
                    {multiInstanceSignList.map((signData, index) => (
                      <MultiInstanceSignCard key={signData.nodeId || index} signData={signData} />
                    ))}
                  </Space>
                )}
                {activeTab === 'tracking' && (
                  <ApprovalTrackingMap
                    trackingMap={trackingMap || undefined}
                    loading={loading}
                    height={520}
                  />
                )}
                {activeTab === 'flow' && (
                  <FlowDiagram
                    bpmnXml={instance?.bpmnXml}
                    currentNodeIds={instance?.currentNodeIds || []}
                    historyNodeIds={historyNodeIds}
                    height={480}
                    loading={loading}
                  />
                )}
              </div>
            </Card>

            <div style={{ height: 16 }} />
          </Col>
        </Row>

        {hasPendingTask && mergedTask && (
          <div style={{ marginTop: -16, marginLeft: -16, marginRight: -16, marginBottom: -16 }}>
            <ApprovalActionBar
              task={mergedTask}
              history={history}
              onApprove={handleApprove}
              onReject={handleReject}
              onTransfer={handleTransfer}
              onAddSign={handleAddSign}
              onDelegate={handleDelegate}
              loading={actionLoading}
            />
          </div>
        )}

        <Modal
          title="撤回申请"
          open={withdrawOpen}
          onOk={handleWithdraw}
          onCancel={() => setWithdrawOpen(false)}
          okText="确认撤回"
          okButtonProps={{ danger: true }}
          confirmLoading={actionLoading}
          destroyOnClose
        >
          <Alert
            message="撤回后可修改申请内容并重新提交审批"
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Form form={withdrawForm} layout="vertical">
            <Form.Item
              name="reason"
              label="撤回原因"
              rules={[{ required: true, message: '请输入撤回原因' }]}
            >
              <TextArea
                rows={4}
                placeholder="请说明撤回原因"
                showCount
                maxLength={300}
              />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </Spin>
  )
}

export default ApprovalDetail
