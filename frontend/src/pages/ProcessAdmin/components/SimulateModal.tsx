import { useState, useEffect } from 'react'
import {
  Modal,
  Button,
  Space,
  Steps,
  Card,
  Tag,
  Input,
  InputNumber,
  Select,
  Form,
  Row,
  Col,
  Typography,
  Divider,
  Avatar,
  App,
  Empty,
  Spin,
  Timeline
} from 'antd'
import {
  PlayCircleOutlined,
  UserOutlined,
  RightOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons'
import { processApi } from '@/api'
import type { SimulateStepVO, FormFieldVO } from '@/types'

const { Text, Title } = Typography
const { Option } = Select
const { Step } = Steps

interface SimulateModalProps {
  open: boolean
  onCancel: () => void
  processDefinitionId: number | null
}

export default function SimulateModal({ open, onCancel, processDefinitionId }: SimulateModalProps) {
  const { message } = App.useApp()
  const [tab, setTab] = useState<'form' | 'result'>('form')
  const [loading, setLoading] = useState(false)
  const [running, setRunning] = useState(false)
  const [form] = Form.useForm()
  const [formFields, setFormFields] = useState<FormFieldVO[]>([])
  const [processInfo, setProcessInfo] = useState<any>(null)
  const [simulateSteps, setSimulateSteps] = useState<SimulateStepVO[]>([])
  const [currentStepIdx, setCurrentStepIdx] = useState(-1)

  useEffect(() => {
    if (open && processDefinitionId) {
      initForm()
      setSimulateSteps([])
      setCurrentStepIdx(-1)
      setTab('form')
    }
  }, [open, processDefinitionId])

  const initForm = async () => {
    setLoading(true)
    try {
      const info = (await processApi.definitionGet(processDefinitionId!)) as any
      setProcessInfo(info)
      if (info?.formId) {
        const fields = await processApi.formFields(info.formId)
        setFormFields(fields || [])
      }
    } catch (e) {
      // ignore
    } finally {
      setLoading(false)
    }
  }

  const handleRun = async () => {
    try {
      let formData: any = {}
      if (formFields.length > 0) {
        formData = await form.validateFields()
      }

      setRunning(true)
      setSimulateSteps([])
      setCurrentStepIdx(-1)

      const steps = (await processApi.simulate({
        processDefinitionId,
        formData
      })) as unknown as SimulateStepVO[]

      setSimulateSteps(steps || [])
      setTab('result')

      for (let i = 0; i < (steps?.length || 0); i++) {
        await new Promise((resolve) => setTimeout(resolve, 500))
        setCurrentStepIdx(i)
      }
    } catch (e) {
      // error handled
    } finally {
      setRunning(false)
    }
  }

  const handleReset = () => {
    form.resetFields()
  }

  const handleReRun = () => {
    setTab('form')
    setSimulateSteps([])
    setCurrentStepIdx(-1)
  }

  const renderFormField = (field: FormFieldVO) => {
    const commonProps = {
      style: { width: '100%' }
    }

    switch (field.fieldType?.toLowerCase()) {
      case 'number':
      case 'integer':
      case 'decimal':
        return (
          <Form.Item key={field.fieldKey} label={field.fieldName} name={field.fieldKey}>
            <InputNumber {...commonProps} placeholder={`请输入${field.fieldName}`} />
          </Form.Item>
        )
      case 'select':
      case 'enum':
        return (
          <Form.Item key={field.fieldKey} label={field.fieldName} name={field.fieldKey}>
            <Select {...commonProps} placeholder={`请选择${field.fieldName}`} allowClear>
              <Option value="option1">选项1</Option>
              <Option value="option2">选项2</Option>
            </Select>
          </Form.Item>
        )
      case 'textarea':
        return (
          <Form.Item key={field.fieldKey} label={field.fieldName} name={field.fieldKey}>
            <Input.TextArea rows={3} placeholder={`请输入${field.fieldName}`} />
          </Form.Item>
        )
      case 'date':
      case 'datetime':
        return (
          <Form.Item key={field.fieldKey} label={field.fieldName} name={field.fieldKey}>
            <Input {...commonProps} placeholder="YYYY-MM-DD" />
          </Form.Item>
        )
      case 'boolean':
        return (
          <Form.Item
            key={field.fieldKey}
            label={field.fieldName}
            name={field.fieldKey}
            valuePropName="checked"
          >
            <Select {...commonProps} placeholder={`请选择${field.fieldName}`} allowClear>
              <Option value={true}>是</Option>
              <Option value={false}>否</Option>
            </Select>
          </Form.Item>
        )
      default:
        return (
          <Form.Item key={field.fieldKey} label={field.fieldName} name={field.fieldKey}>
            <Input {...commonProps} placeholder={`请输入${field.fieldName}`} />
          </Form.Item>
        )
    }
  }

  const renderFormTab = () => (
    <Spin spinning={loading}>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }} size={4}>
          <Row>
            <Col span={12}>
              <Text type="secondary">流程名称：</Text>
              <Text strong>{processInfo?.processName || '-'}</Text>
            </Col>
            <Col span={12}>
              <Text type="secondary">流程标识：</Text>
              <Text code>{processInfo?.processKey || '-'}</Text>
            </Col>
          </Row>
        </Space>
      </Card>

      {formFields.length === 0 ? (
        <Empty description="当前流程未配置表单字段，将使用空数据模拟" />
      ) : (
        <Card size="small" title="填写表单数据">
          <Form form={form} layout="vertical">
            <Row gutter={16}>
              {formFields.map((field) => (
                <Col span={12} key={field.fieldKey}>
                  {renderFormField(field)}
                </Col>
              ))}
            </Row>
          </Form>
        </Card>
      )}
    </Spin>
  )

  const renderResultTab = () => {
    if (simulateSteps.length === 0) {
      return <Empty description="暂无模拟结果" />
    }

    return (
      <div>
        <Steps
          direction="vertical"
          current={currentStepIdx + 1}
          status={currentStepIdx >= simulateSteps.length - 1 ? 'finish' : 'process'}
        >
          {simulateSteps.map((step, idx) => (
            <Step
              key={idx}
              title={
                <Space>
                  <Text strong>{step.nodeName}</Text>
                  <Tag color="blue">第{step.step}步</Tag>
                  {step.action && <Tag color="cyan">{step.action}</Tag>}
                </Space>
              }
              description={
                <div style={{ marginTop: 8 }}>
                  <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
                    节点ID: <Text code>{step.nodeId}</Text>
                  </Text>
                  {step.assigneeNames && step.assigneeNames.length > 0 && (
                    <Space wrap>
                      <Text type="secondary">审批人：</Text>
                      {step.assigneeNames.map((name, i) => (
                        <Tag key={i} color="geekblue" icon={<UserOutlined />}>
                          {name}
                        </Tag>
                      ))}
                    </Space>
                  )}
                </div>
              }
              icon={
                idx <= currentStepIdx ? (
                  <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 20 }} />
                ) : idx === currentStepIdx + 1 && running ? (
                  <ClockCircleOutlined style={{ color: '#1890ff', fontSize: 20 }} />
                ) : (
                  <RightOutlined style={{ color: '#bfbfbf' }} />
                )
              }
            />
          ))}
        </Steps>

        {currentStepIdx >= simulateSteps.length - 1 && !running && (
          <div
            style={{
              marginTop: 24,
              padding: 16,
              background: '#f6ffed',
              borderRadius: 8,
              border: '1px solid #b7eb8f',
              textAlign: 'center'
            }}
          >
            <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 32 }} />
            <Title level={4} style={{ color: '#52c41a', margin: '12px 0 4px 0' }}>
              模拟运行完成
            </Title>
            <Text type="secondary">流程共经过 {simulateSteps.length} 个节点</Text>
          </div>
        )}
      </div>
    )
  }

  return (
    <Modal
      title={
        <Space>
          <PlayCircleOutlined style={{ color: '#1890ff' }} />
          <span>模拟运行</span>
          {processInfo && (
            <Tag color="blue" style={{ marginLeft: 8 }}>
              {processInfo.processName}
            </Tag>
          )}
        </Space>
      }
      open={open}
      onCancel={onCancel}
      width={760}
      destroyOnClose
      footer={
        tab === 'form'
          ? [
              <Button key="reset" onClick={handleReset}>
                重置
              </Button>,
              <Button key="run" type="primary" loading={running} icon={<PlayCircleOutlined />} onClick={handleRun}>
                开始模拟
              </Button>
            ]
          : [
              <Button key="reRun" onClick={handleReRun}>
                重新模拟
              </Button>,
              <Button key="close" type="primary" onClick={onCancel}>
                关闭
              </Button>
            ]
      }
    >
      {tab === 'form' ? renderFormTab() : renderResultTab()}
    </Modal>
  )
}
