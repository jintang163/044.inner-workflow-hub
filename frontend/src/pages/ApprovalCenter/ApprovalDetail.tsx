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
  List
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
import { approvalApi } from '@/api'
import type { ProcessInstanceVO, ApprovalHistoryVO, ApprovalTaskVO } from '@/types/approval'
import dayjs from 'dayjs'

const { Text, Title, Paragraph } = Typography
const { TextArea } = Input
const { TabPane } = Tabs
const { confirm } = Modal

const mockInstance = (instanceNo: string): ProcessInstanceVO => ({
  id: 50001,
  instanceNo,
  processKey: 'leave_annual',
  processName: '年假申请',
  title: '2024年6月年假申请（5天）',
  formId: 1,
  formVersion: 1,
  formData: {
    title: '2024年6月年假申请（5天）',
    leaveType: '年假',
    startDate: '2024-06-20',
    endDate: '2024-06-24',
    leaveDays: 5,
    reason: '家庭出游计划，陪同家人出行',
    handover: '已与李四确认工作交接事宜',
    contact: '138xxxx8888'
  },
  formSchema: null,
  instanceStatus: 1,
  instanceStatusDesc: '审批中',
  startUserId: 101,
  startUserName: '张三',
  startUserAvatar: '',
  startDeptName: '技术研发部',
  startTime: dayjs().subtract(2, 'day').format('YYYY-MM-DD HH:mm:ss'),
  currentNodeIds: ['node_approve_2'],
  currentNodeNames: ['财务总监审批'],
  canWithdraw: false,
  bpmnXml: `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <process id="Process_1" isExecutable="false">
    <startEvent id="start" name="发起申请">
      <outgoing>Flow_1</outgoing>
    </startEvent>
    <userTask id="node_approve_1" name="部门主管审批">
      <incoming>Flow_1</incoming>
      <outgoing>Flow_2</outgoing>
    </userTask>
    <userTask id="node_approve_2" name="财务总监审批">
      <incoming>Flow_2</incoming>
      <outgoing>Flow_3</outgoing>
    </userTask>
    <userTask id="node_approve_3" name="总经理审批">
      <incoming>Flow_3</incoming>
      <outgoing>Flow_4</outgoing>
    </userTask>
    <endEvent id="end" name="审批完成">
      <incoming>Flow_4</incoming>
    </endEvent>
    <sequenceFlow id="Flow_1" sourceRef="start" targetRef="node_approve_1" />
    <sequenceFlow id="Flow_2" sourceRef="node_approve_1" targetRef="node_approve_2" />
    <sequenceFlow id="Flow_3" sourceRef="node_approve_2" targetRef="node_approve_3" />
    <sequenceFlow id="Flow_4" sourceRef="node_approve_3" targetRef="end" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane bpmnElement="Process_1" id="BPMNPlane_1">
      <bpmndi:BPMNShape bpmnElement="start" id="BPMNShape_start">
        <omgdc:Bounds x="150" y="100" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <omgdc:Bounds x="138" y="142" width="60" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="node_approve_1" id="BPMNShape_node_approve_1">
        <omgdc:Bounds x="270" y="78" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="node_approve_2" id="BPMNShape_node_approve_2">
        <omgdc:Bounds x="470" y="78" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="node_approve_3" id="BPMNShape_node_approve_3">
        <omgdc:Bounds x="670" y="78" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="end" id="BPMNShape_end">
        <omgdc:Bounds x="870" y="100" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <omgdc:Bounds x="860" y="142" width="56" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge bpmnElement="Flow_1" id="BPMNEdge_Flow_1">
        <omgdi:waypoint x="186" y="118" />
        <omgdi:waypoint x="270" y="118" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="Flow_2" id="BPMNEdge_Flow_2">
        <omgdi:waypoint x="370" y="118" />
        <omgdi:waypoint x="470" y="118" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="Flow_3" id="BPMNEdge_Flow_3">
        <omgdi:waypoint x="570" y="118" />
        <omgdi:waypoint x="670" y="118" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="Flow_4" id="BPMNEdge_Flow_4">
        <omgdi:waypoint x="770" y="118" />
        <omgdi:waypoint x="870" y="118" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`
})

const mockHistory = (): ApprovalHistoryVO[] => [
  {
    id: 1,
    nodeId: 'start',
    nodeName: '发起申请',
    activityType: 1,
    activityTypeDesc: '发起',
    operatorId: 101,
    operatorName: '张三',
    operatorDeptName: '技术研发部',
    operateTime: dayjs().subtract(2, 'day').format('YYYY-MM-DD HH:mm:ss'),
    actionRemark: '提交年假申请，计划5天，陪同家人出游',
    duration: undefined,
    status: 'approved'
  },
  {
    id: 2,
    nodeId: 'node_approve_1',
    nodeName: '部门主管审批',
    activityType: 2,
    activityTypeDesc: '同意',
    operatorId: 102,
    operatorName: '李主管',
    operatorDeptName: '技术研发部',
    operateTime: dayjs().subtract(2, 'day').add(2, 'hour').format('YYYY-MM-DD HH:mm:ss'),
    actionRemark: '工作已安排妥当，同意休假',
    duration: 2 * 60 * 60 * 1000,
    status: 'approved',
    signatureUrl: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNTAiIGhlaWdodD0iNjAiPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjMzMzIiBmb250LWZhbWlseT0iY2FybGlncmFwaHkiIGZvbnQtc2l6ZT0iMjAiPuW8oOS4nOWFiDwvdGV4dD48L3N2Zz4='
  },
  {
    id: 3,
    nodeId: 'node_approve_2',
    nodeName: '财务总监审批',
    activityType: 1,
    activityTypeDesc: '审批中',
    operatorId: 103,
    operatorName: '王总监',
    operatorDeptName: '财务部',
    operateTime: dayjs().subtract(1, 'day').add(8, 'hour').format('YYYY-MM-DD HH:mm:ss'),
    duration: undefined,
    status: 'current'
  },
  {
    id: 4,
    nodeId: 'node_approve_3',
    nodeName: '总经理审批',
    activityType: 1,
    operatorId: 0,
    operatorName: '待审批',
    operatorDeptName: '-',
    operateTime: '-',
    status: 'pending'
  }
]

const mockCurrentTask: ApprovalTaskVO = {
  id: 10001,
  taskNo: 'TK202406001',
  flowableTaskId: 'task_2',
  instanceId: 50001,
  instanceNo: 'AP20240600001',
  processKey: 'leave_annual',
  processName: '年假申请',
  title: '2024年6月年假申请（5天）',
  nodeId: 'node_approve_2',
  nodeName: '财务总监审批',
  assigneeId: 1,
  assigneeName: '当前用户',
  assignTime: dayjs().subtract(1, 'day').add(8, 'hour').format('YYYY-MM-DD HH:mm:ss'),
  dueTime: dayjs().add(24, 'hour').format('YYYY-MM-DD HH:mm:ss'),
  taskStatus: 'PENDING',
  priority: 2,
  startUserId: 101,
  startUserName: '张三',
  startDeptName: '技术研发部',
  startTime: dayjs().subtract(2, 'day').format('YYYY-MM-DD HH:mm:ss'),
  formData: {},
  canAddSign: true,
  canTransfer: true,
  canDelegate: true,
  canReject: true,
  needSignature: false,
  needComment: true
}

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

  const instanceNo = id || ''

  const isStarter = instance?.startUserId === 1
  const hasPendingTask = currentTask?.taskStatus === 'PENDING'

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      // const [instRes, histRes] = await Promise.all([
      //   approvalApi.instanceDetail(instanceNo),
      //   approvalApi.approvalHistory(instanceNo)
      // ])
      // setInstance(instRes)
      // setHistory(histRes)
      await new Promise(resolve => setTimeout(resolve, 500))
      const inst = mockInstance(instanceNo)
      setInstance(inst)
      setHistory(mockHistory())
      if (passedTask) {
        setCurrentTask(passedTask)
      } else if (hasPendingTask) {
        setCurrentTask(mockCurrentTask)
      } else {
        setCurrentTask(mockCurrentTask)
      }
    } catch (err: any) {
      message.error(err?.message || '加载详情失败')
    } finally {
      setLoading(false)
    }
  }, [instanceNo, passedTask])

  useEffect(() => {
    if (instanceNo) {
      loadData()
    }
  }, [instanceNo, loadData])

  const handleApprove = async (data: any) => {
    if (!currentTask) return
    try {
      setActionLoading(true)
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

        {hasPendingTask && currentTask && (
          <div style={{ marginTop: -16, marginLeft: -16, marginRight: -16, marginBottom: -16 }}>
            <ApprovalActionBar
              task={currentTask}
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
