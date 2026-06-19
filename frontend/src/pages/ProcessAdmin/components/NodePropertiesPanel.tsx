import { useState, useEffect } from 'react'
import {
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Checkbox,
  Radio,
  Tabs,
  Card,
  Divider,
  Row,
  Col,
  Typography,
  Empty,
  Space,
  Tag,
  Button,
  Table,
  App
} from 'antd'
import {
  MinusCircleOutlined,
  PlusOutlined,
  UserOutlined,
  TeamOutlined,
  CodeOutlined
} from '@ant-design/icons'
import type { NodeConfig, SequenceFlowConfig, FormFieldVO } from '@/types'
import { userApi, roleApi, processApi } from '@/api'

const { Text } = Typography
const { Option } = Select
const { TabPane } = Tabs

interface NodePropertiesPanelProps {
  selectedElement: any
  nodeConfig: NodeConfig | null
  flowConfig: SequenceFlowConfig | null
  formId: number | null
  onNodeConfigChange: (config: Partial<NodeConfig>) => void
  onFlowConfigChange: (config: Partial<SequenceFlowConfig>) => void
  onElementNameChange: (name: string) => void
}

const APPROVE_TYPES = [
  { label: '或签（任一通过）', value: 1 },
  { label: '会签（全部通过）', value: 2 },
  { label: '依次审批', value: 3 }
]

const ASSIGNEE_TYPES = [
  { label: '指定人员', value: 'FIXED_USER', icon: <UserOutlined /> },
  { label: '部门主管', value: 'DEPT_LEADER', icon: <TeamOutlined /> },
  { label: '指定角色', value: 'ROLE', icon: <TeamOutlined /> },
  { label: '发起人', value: 'START_USER', icon: <UserOutlined /> },
  { label: '发起人主管', value: 'START_USER_LEADER', icon: <TeamOutlined /> },
  { label: '脚本配置', value: 'SCRIPT', icon: <CodeOutlined /> }
]

const TIMEOUT_STRATEGIES = [
  { label: '自动通过', value: 1 },
  { label: '自动拒绝', value: 2 },
  { label: '升级上级', value: 3 },
  { label: '仅提醒', value: 4 }
]

const REFUSE_STRATEGIES = [
  { label: '驳回发起人', value: 1 },
  { label: '驳回到指定节点', value: 2 }
]

const MULTI_INSTANCE_COMPLETION_TYPES = [
  { label: '全部通过（会签）', value: 1 },
  { label: '任一通过（或签）', value: 2 },
  { label: '百分比通过', value: 3 }
]

const NOTIFY_CHANNELS = [
  { label: '钉钉', value: 'DINGTALK' },
  { label: '企微', value: 'WECOM' },
  { label: '邮件', value: 'EMAIL' },
  { label: '短信', value: 'SMS' }
]

const OPERATORS = [
  { label: '>', value: '>' },
  { label: '>=', value: '>=' },
  { label: '<', value: '<' },
  { label: '<=', value: '<=' },
  { label: '==', value: '==' },
  { label: '!=', value: '!=' },
  { label: '包含', value: 'contains' }
]

const DEFAULT_NODE_CONFIG: Partial<NodeConfig> = {
  approveType: 1,
  assigneeType: 1,
  assigneeValue: [],
  assigneeDeptLevel: 1,
  assigneeScript: '',
  formPermission: {},
  timeoutStrategy: 4,
  timeoutHours: 0,
  timeoutEscalateLevels: 1,
  passRate: 100,
  notifyConfig: {
    channels: [],
    ccUserIds: []
  },
  emptyAssigneeStrategy: 1,
  refuseStrategy: 1,
  refuseTargetNodeId: '',
  canAddSign: 0,
  canTransfer: 0,
  canDelegate: 0,
  needSignature: 0,
  needComment: 0,
  multiInstance: 0,
  multiInstanceCompletionType: 1,
  passPercentage: 100,
  vetoEnabled: 0
}

export default function NodePropertiesPanel({
  selectedElement,
  nodeConfig,
  flowConfig,
  formId,
  onNodeConfigChange,
  onFlowConfigChange,
  onElementNameChange
}: NodePropertiesPanelProps) {
  const { message } = App.useApp()
  const [form] = Form.useForm()
  const [conditionMode, setConditionMode] = useState<'visual' | 'expression'>('visual')
  const [formFields, setFormFields] = useState<FormFieldVO[]>([])
  const [userList, setUserList] = useState<any[]>([])
  const [roleList, setRoleList] = useState<any[]>([])

  const elementType = selectedElement?.type
  const elementName = selectedElement?.name || ''
  const elementId = selectedElement?.id || ''
  const isUserTask = elementType === 'node' && selectedElement?.nodeType === 'USER_TASK'
  const isSequenceFlow = elementType === 'flow'
  const isStartEvent = elementType === 'node' && selectedElement?.nodeType === 'START_EVENT'
  const isEndEvent = elementType === 'node' && selectedElement?.nodeType === 'END_EVENT'
  const isGateway = elementType === 'node' && (selectedElement?.nodeType === 'EXCLUSIVE_GATEWAY' || selectedElement?.nodeType === 'PARALLEL_GATEWAY')

  useEffect(() => {
    if (formId) {
      loadFormFields()
    }
    loadSelectData()
  }, [formId])

  useEffect(() => {
    if (isUserTask) {
      const merged = { ...DEFAULT_NODE_CONFIG, ...nodeConfig }
      form.setFieldsValue({
        nodeName: elementName,
        approveType: merged.approveType,
        assigneeType: Object.keys(ASSIGNEE_TYPES)[(merged.assigneeType || 1) - 1] || 'FIXED_USER',
        assigneeValue: merged.assigneeValue,
        assigneeDeptLevel: merged.assigneeDeptLevel,
        assigneeScript: merged.assigneeScript,
        timeoutStrategy: merged.timeoutStrategy,
        timeoutHours: merged.timeoutHours,
        timeoutEscalateLevels: merged.timeoutEscalateLevels,
        passRate: merged.passRate,
        notifyChannels: merged.notifyConfig?.channels || [],
        ccUserIds: merged.notifyConfig?.ccUserIds || [],
        refuseStrategy: merged.refuseStrategy,
        refuseTargetNodeId: merged.refuseTargetNodeId,
        canAddSign: merged.canAddSign === 1,
        canTransfer: merged.canTransfer === 1,
        canDelegate: merged.canDelegate === 1,
        needSignature: merged.needSignature === 1,
        needComment: merged.needComment === 1,
        multiInstance: merged.multiInstance === 1,
        multiInstanceCompletionType: merged.multiInstanceCompletionType || 1,
        passPercentage: merged.passPercentage || 100,
        vetoEnabled: merged.vetoEnabled === 1
      })
    } else if (isSequenceFlow) {
      form.setFieldsValue({
        flowName: elementName,
        isDefault: flowConfig?.isDefault === 1,
        conditionExpression: flowConfig?.conditionExpression || ''
      })
    } else {
      form.setFieldsValue({
        nodeName: elementName
      })
    }
  }, [selectedElement, nodeConfig, flowConfig])

  const loadFormFields = async () => {
    try {
      const fields = await processApi.formFields(formId!)
      setFormFields(fields || [])
    } catch (e) {
      // ignore
    }
  }

  const loadSelectData = async () => {
    try {
      const [users, roles] = await Promise.all([
        userApi.list({ pageNum: 1, pageSize: 1000 }),
        roleApi.all()
      ])
      setUserList((users as any).list || [])
      setRoleList(roles || [])
    } catch (e) {
      // ignore
    }
  }

  const handleFormChange = (changedValues: any) => {
    if (changedValues.nodeName !== undefined) {
      onElementNameChange(changedValues.nodeName)
    }

    if (isUserTask) {
      const updates: Partial<NodeConfig> = {}
      if (changedValues.nodeName !== undefined) updates.nodeName = changedValues.nodeName
      if (changedValues.approveType !== undefined) updates.approveType = changedValues.approveType
      if (changedValues.assigneeType !== undefined) {
        const idx = ASSIGNEE_TYPES.findIndex((t) => t.value === changedValues.assigneeType)
        updates.assigneeType = idx + 1
      }
      if (changedValues.assigneeValue !== undefined) updates.assigneeValue = changedValues.assigneeValue
      if (changedValues.assigneeDeptLevel !== undefined) updates.assigneeDeptLevel = changedValues.assigneeDeptLevel
      if (changedValues.assigneeScript !== undefined) updates.assigneeScript = changedValues.assigneeScript
      if (changedValues.timeoutStrategy !== undefined) updates.timeoutStrategy = changedValues.timeoutStrategy
      if (changedValues.timeoutHours !== undefined) updates.timeoutHours = changedValues.timeoutHours
      if (changedValues.timeoutEscalateLevels !== undefined) updates.timeoutEscalateLevels = changedValues.timeoutEscalateLevels
      if (changedValues.passRate !== undefined) updates.passRate = changedValues.passRate
      if (changedValues.notifyChannels !== undefined || changedValues.ccUserIds !== undefined) {
        const current = nodeConfig?.notifyConfig || { channels: [], ccUserIds: [] }
        updates.notifyConfig = {
          channels: changedValues.notifyChannels ?? current.channels,
          ccUserIds: changedValues.ccUserIds ?? current.ccUserIds
        }
      }
      if (changedValues.refuseStrategy !== undefined) updates.refuseStrategy = changedValues.refuseStrategy
      if (changedValues.refuseTargetNodeId !== undefined) updates.refuseTargetNodeId = changedValues.refuseTargetNodeId
      if (changedValues.canAddSign !== undefined) updates.canAddSign = changedValues.canAddSign ? 1 : 0
      if (changedValues.canTransfer !== undefined) updates.canTransfer = changedValues.canTransfer ? 1 : 0
      if (changedValues.canDelegate !== undefined) updates.canDelegate = changedValues.canDelegate ? 1 : 0
      if (changedValues.needSignature !== undefined) updates.needSignature = changedValues.needSignature ? 1 : 0
      if (changedValues.needComment !== undefined) updates.needComment = changedValues.needComment ? 1 : 0
      if (changedValues.multiInstance !== undefined) {
        updates.multiInstance = changedValues.multiInstance ? 1 : 0
      }
      if (changedValues.multiInstanceCompletionType !== undefined) {
        updates.multiInstanceCompletionType = changedValues.multiInstanceCompletionType
      }
      if (changedValues.passPercentage !== undefined) updates.passPercentage = changedValues.passPercentage
      if (changedValues.vetoEnabled !== undefined) updates.vetoEnabled = changedValues.vetoEnabled ? 1 : 0

      if (Object.keys(updates).length > 0) {
        onNodeConfigChange(updates)
      }
    }

    if (isSequenceFlow) {
      const updates: Partial<SequenceFlowConfig> = {}
      if (changedValues.flowName !== undefined) {
        onElementNameChange(changedValues.flowName)
      }
      if (changedValues.isDefault !== undefined) updates.isDefault = changedValues.isDefault ? 1 : 0
      if (changedValues.conditionExpression !== undefined) {
        updates.conditionExpression = changedValues.conditionExpression
      }
      if (Object.keys(updates).length > 0) {
        onFlowConfigChange(updates)
      }
    }
  }

  const handleFormPermissionChange = (fieldKey: string, permission: 'edit' | 'readonly' | 'hidden') => {
    const current = nodeConfig?.formPermission || {}
    onNodeConfigChange({
      formPermission: { ...current, [fieldKey]: permission }
    })
  }

  const handleVisualConditionChange = (conditions: any[]) => {
    if (conditions.length === 0) {
      form.setFieldsValue({ conditionExpression: '' })
      onFlowConfigChange({ conditionExpression: '' })
      return
    }
    const parts = conditions.map((c) => {
      if (!c.field || !c.operator) return ''
      const val = typeof c.value === 'string' ? `'${c.value}'` : c.value
      if (c.operator === 'contains') {
        return `${form.getFieldValue('assigneeType') || ''}.${c.field}.indexOf(${val}) != -1`
      }
      return `formData.${c.field} ${c.operator} ${val}`
    }).filter(Boolean)
    const expr = parts.length > 0 ? `\${${parts.join(' && ')}}` : ''
    form.setFieldsValue({ conditionExpression: expr })
    onFlowConfigChange({ conditionExpression: expr })
  }

  const renderEmpty = () => (
    <div style={{ padding: 40, textAlign: 'center' }}>
      <Empty description="请选择节点或连线查看属性" />
    </div>
  )

  const renderBasicInfo = () => (
    <Card size="small" title="基础信息" style={{ marginBottom: 12 }}>
      <Form.Item label="节点ID" style={{ marginBottom: 12 }}>
        <Input value={elementId} disabled />
      </Form.Item>
      <Form.Item
        label="节点名称"
        name="nodeName"
        rules={[{ required: true, message: '请输入节点名称' }]}
        style={{ marginBottom: 0 }}
      >
        <Input placeholder="请输入节点名称" />
      </Form.Item>
    </Card>
  )

  const renderUserTask = () => {
    const assigneeType = form.getFieldValue('assigneeType') || 'FIXED_USER'
    const approveType = form.getFieldValue('approveType') || 1
    const timeoutStrategy = form.getFieldValue('timeoutStrategy') || 4
    const refuseStrategy = form.getFieldValue('refuseStrategy') || 1
    const multiInstance = form.getFieldValue('multiInstance')
    const completionType = form.getFieldValue('multiInstanceCompletionType') || 1

    const useMultiInstanceConfig = multiInstance || approveType === 2 || approveType === 1

    return (
      <>
        {renderBasicInfo()}

        <Card size="small" title="审批配置" style={{ marginBottom: 12 }}>
          <Form.Item label="审批类型" name="approveType" style={{ marginBottom: 12 }}>
            <Radio.Group>
              {APPROVE_TYPES.map((t) => (
                <Radio key={t.value} value={t.value}>
                  {t.label}
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>

          {(approveType === 2 || approveType === 3) && (
            <Form.Item
              label="通过比例(%)"
              name="passRate"
              extra="会签时多少比例通过才算通过（兼容旧配置）"
              style={{ marginBottom: 12 }}
            >
              <InputNumber min={1} max={100} style={{ width: 120 }} />
            </Form.Item>
          )}

          <Divider style={{ margin: '12px 0' }} />
          <Text strong style={{ display: 'block', marginBottom: 12 }}>
            多实例/会签设置
          </Text>

          <Form.Item
            label="启用多实例"
            name="multiInstance"
            valuePropName="checked"
            extra="开启后将创建多人并行审批任务（会签/或签），审批类型选择或签/会签时自动启用"
            style={{ marginBottom: 12 }}
          >
            <Switch />
          </Form.Item>

          {useMultiInstanceConfig && (
            <>
              <Form.Item
                label="完成条件"
                name="multiInstanceCompletionType"
                style={{ marginBottom: 12 }}
              >
                <Radio.Group>
                  {MULTI_INSTANCE_COMPLETION_TYPES.map((t) => (
                    <Radio key={t.value} value={t.value}>
                      {t.label}
                    </Radio>
                  ))}
                </Radio.Group>
              </Form.Item>

              {completionType === 3 && (
                <Form.Item
                  label="通过阈值(%)"
                  name="passPercentage"
                  extra="同意人数达到此百分比即自动通过，如80表示80%同意即通过"
                  style={{ marginBottom: 12 }}
                >
                  <InputNumber min={1} max={100} style={{ width: 120 }} />
                </Form.Item>
              )}

              <Form.Item
                label="一票否决"
                name="vetoEnabled"
                valuePropName="checked"
                extra="开启后，任一人拒绝即整体拒绝，其他审批人无需再审批"
                style={{ marginBottom: 0 }}
              >
                <Switch />
              </Form.Item>
            </>
          )}

          <Divider style={{ margin: '12px 0' }} />
          <Text strong style={{ display: 'block', marginBottom: 12 }}>
            审批人设置
          </Text>
          <Form.Item name="assigneeType" style={{ marginBottom: 12 }}>
            <Tabs size="small">
              {ASSIGNEE_TYPES.map((t) => (
                <TabPane
                  key={t.value}
                  tab={
                    <span>
                      {t.icon} {t.label}
                    </span>
                  }
                />
              ))}
            </Tabs>
          </Form.Item>

          {assigneeType === 'FIXED_USER' && (
            <Form.Item label="选择人员" name="assigneeValue" style={{ marginBottom: 12 }}>
              <Select
                mode="multiple"
                placeholder="请选择审批人"
                showSearch
                optionFilterProp="children"
                style={{ width: '100%' }}
              >
                {userList.map((u) => (
                  <Option key={u.id} value={u.id}>
                    {u.nickname || u.username}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          )}

          {assigneeType === 'DEPT_LEADER' && (
            <Form.Item
              label="部门层级"
              name="assigneeDeptLevel"
              extra="1=直属主管，2=上二级，以此类推"
              style={{ marginBottom: 12 }}
            >
              <InputNumber min={1} max={10} />
            </Form.Item>
          )}

          {assigneeType === 'ROLE' && (
            <Form.Item label="选择角色" name="assigneeValue" style={{ marginBottom: 12 }}>
              <Select
                mode="multiple"
                placeholder="请选择角色"
                showSearch
                optionFilterProp="children"
                style={{ width: '100%' }}
              >
                {roleList.map((r) => (
                  <Option key={r.id} value={r.id}>
                    {r.roleName}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          )}

          {(assigneeType === 'START_USER' || assigneeType === 'START_USER_LEADER') && (
            <div style={{ padding: 12, background: '#f5f5f5', borderRadius: 6, marginBottom: 12 }}>
              <Tag color="blue">说明</Tag>
              <Text type="secondary">
                {assigneeType === 'START_USER' ? '审批人为流程发起人' : '审批人为发起人的直属主管'}
              </Text>
            </div>
          )}

          {assigneeType === 'SCRIPT' && (
            <Form.Item
              label="审批人脚本"
              name="assigneeScript"
              extra="返回用户ID数组，如: return [1001, 1002];"
              style={{ marginBottom: 0 }}
            >
              <Input.TextArea
                rows={4}
                placeholder="// JavaScript 脚本，返回用户ID数组&#10;const userId = 1001;&#10;return [userId];"
                style={{ fontFamily: 'Consolas, Monaco, monospace' }}
              />
            </Form.Item>
          )}
        </Card>

        <Card size="small" title="表单权限" style={{ marginBottom: 12 }}>
          {formFields.length === 0 ? (
            <Empty description="请先关联表单" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          ) : (
            <Table
              size="small"
              pagination={false}
              dataSource={formFields}
              rowKey="fieldKey"
              columns={[
                { title: '字段名', dataIndex: 'fieldName', key: 'fieldName' },
                {
                  title: '权限',
                  dataIndex: 'fieldKey',
                  key: 'permission',
                  render: (fieldKey: string) => (
                    <Radio.Group
                      size="small"
                      value={nodeConfig?.formPermission?.[fieldKey] || 'edit'}
                      onChange={(e) => handleFormPermissionChange(fieldKey, e.target.value)}
                    >
                      <Radio value="edit">编辑</Radio>
                      <Radio value="readonly">只读</Radio>
                      <Radio value="hidden">隐藏</Radio>
                    </Radio.Group>
                  )
                }
              ]}
            />
          )}
        </Card>

        <Card size="small" title="超时处理" style={{ marginBottom: 12 }}>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="超时时间(小时)" name="timeoutHours" style={{ marginBottom: 12 }}>
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="超时策略" name="timeoutStrategy" style={{ marginBottom: 12 }}>
                <Select>
                  {TIMEOUT_STRATEGIES.map((s) => (
                    <Option key={s.value} value={s.value}>
                      {s.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          {timeoutStrategy === 3 && (
            <Form.Item
              label="升级层级"
              name="timeoutEscalateLevels"
              extra="最多向上升级几级"
              style={{ marginBottom: 0 }}
            >
              <InputNumber min={1} max={10} />
            </Form.Item>
          )}
        </Card>

        <Card size="small" title="通知配置" style={{ marginBottom: 12 }}>
          <Form.Item label="通知渠道" name="notifyChannels" style={{ marginBottom: 12 }}>
            <Checkbox.Group
              options={NOTIFY_CHANNELS.map((c) => ({ label: c.label, value: c.value }))}
            />
          </Form.Item>
          <Form.Item label="抄送人员" name="ccUserIds" style={{ marginBottom: 0 }}>
            <Select
              mode="multiple"
              placeholder="选择抄送人"
              showSearch
              optionFilterProp="children"
              style={{ width: '100%' }}
            >
              {userList.map((u) => (
                <Option key={u.id} value={u.id}>
                  {u.nickname || u.username}
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Card>

        <Card size="small" title="操作权限" style={{ marginBottom: 12 }}>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="允许加签" name="canAddSign" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="允许转审" name="canTransfer" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="允许委派" name="canDelegate" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="手写签名" name="needSignature" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="必须填意见" name="needComment" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        <Card size="small" title="驳回策略">
          <Form.Item label="驳回方式" name="refuseStrategy" style={{ marginBottom: 12 }}>
            <Radio.Group>
              {REFUSE_STRATEGIES.map((s) => (
                <Radio key={s.value} value={s.value}>
                  {s.label}
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>
          {refuseStrategy === 2 && (
            <Form.Item
              label="驳回目标节点"
              name="refuseTargetNodeId"
              rules={[{ required: true, message: '请选择驳回节点' }]}
              style={{ marginBottom: 0 }}
            >
              <Select placeholder="选择要驳回的节点">
                <Option value="start">开始节点</Option>
              </Select>
            </Form.Item>
          )}
        </Card>
      </>
    )
  }

  const renderSequenceFlow = () => {
    const isDefault = form.getFieldValue('isDefault')
    return (
      <>
        <Card size="small" title="基础信息" style={{ marginBottom: 12 }}>
          <Form.Item label="连线ID" style={{ marginBottom: 12 }}>
            <Input value={elementId} disabled />
          </Form.Item>
          <Form.Item label="连线名称" name="flowName" style={{ marginBottom: 0 }}>
            <Input placeholder="请输入连线名称（可选）" />
          </Form.Item>
        </Card>

        <Card
          size="small"
          title={
            <Space>
              条件表达式
              <Tag color={isDefault ? 'green' : 'blue'}>
                {isDefault ? '默认分支' : '条件分支'}
              </Tag>
            </Space>
          }
          extra={
            <Form.Item name="isDefault" valuePropName="checked" style={{ margin: 0 }}>
              <Switch
                checkedChildren="默认"
                unCheckedChildren="条件"
                disabled={false}
                onChange={(v) => {
                  if (v) {
                    form.setFieldsValue({ conditionExpression: '' })
                    onFlowConfigChange({ conditionExpression: '' })
                  }
                }}
              />
            </Form.Item>
          }
          style={{ marginBottom: 12 }}
        >
          {isDefault ? (
            <div style={{ padding: 12, background: '#f6ffed', borderRadius: 6 }}>
              <Tag color="green">默认分支</Tag>
              <Text type="secondary">当其他条件都不满足时，走此分支</Text>
            </div>
          ) : (
            <>
              <Tabs
                activeKey={conditionMode}
                onChange={setConditionMode as any}
                size="small"
                style={{ marginBottom: 12 }}
              >
                <TabPane tab="可视化配置" key="visual" />
                <TabPane tab="直接写表达式" key="expression" />
              </Tabs>

              {conditionMode === 'visual' ? (
                <VisualConditionBuilder
                  formFields={formFields}
                  onChange={handleVisualConditionChange}
                />
              ) : (
                <Form.Item
                  name="conditionExpression"
                  rules={[{ required: true, message: '请输入条件表达式' }]}
                  style={{ marginBottom: 0 }}
                >
                  <Input.TextArea
                    rows={4}
                    placeholder="${formData.amount > 50000}"
                    style={{ fontFamily: 'Consolas, Monaco, monospace' }}
                  />
                </Form.Item>
              )}
            </>
          )}
        </Card>
      </>
    )
  }

  const renderGateway = () => renderBasicInfo()
  const renderStartEvent = () => renderBasicInfo()
  const renderEndEvent = () => renderBasicInfo()

  return (
    <div
      style={{
        height: '100%',
        overflowY: 'auto',
        background: '#fff',
        borderLeft: '1px solid #f0f0f0',
        padding: 16
      }}
    >
      <Typography.Title level={5} style={{ margin: '0 0 16px 0' }}>
        属性配置
      </Typography.Title>

      {!selectedElement ? (
        renderEmpty()
      ) : (
        <Form
          form={form}
          layout="vertical"
          onValuesChange={handleFormChange}
          preserve={false}
        >
          {isUserTask && renderUserTask()}
          {isSequenceFlow && renderSequenceFlow()}
          {(isStartEvent || isEndEvent || isGateway) && renderBasicInfo()}
          {!isUserTask && !isSequenceFlow && !isStartEvent && !isEndEvent && !isGateway && renderEmpty()}
        </Form>
      )}
    </div>
  )
}

function VisualConditionBuilder({
  formFields,
  onChange
}: {
  formFields: FormFieldVO[]
  onChange: (conditions: any[]) => void
}) {
  const [conditions, setConditions] = useState<any[]>([{ field: '', operator: '', value: '' }])

  const updateCondition = (idx: number, key: string, value: any) => {
    const newConditions = [...conditions]
    newConditions[idx] = { ...newConditions[idx], [key]: value }
    setConditions(newConditions)
    onChange(newConditions)
  }

  const addCondition = () => {
    const newConditions = [...conditions, { field: '', operator: '', value: '' }]
    setConditions(newConditions)
    onChange(newConditions)
  }

  const removeCondition = (idx: number) => {
    if (conditions.length === 1) {
      setConditions([{ field: '', operator: '', value: '' }])
      onChange([{ field: '', operator: '', value: '' }])
      return
    }
    const newConditions = conditions.filter((_, i) => i !== idx)
    setConditions(newConditions)
    onChange(newConditions)
  }

  return (
    <div>
      {conditions.map((cond, idx) => (
        <div key={idx} style={{ marginBottom: 8 }}>
          {idx > 0 && (
            <Tag color="blue" style={{ marginBottom: 8 }}>
              AND
            </Tag>
          )}
          <Row gutter={8} align="middle">
            <Col span={8}>
              <Select
                placeholder="选择字段"
                value={cond.field}
                onChange={(v) => updateCondition(idx, 'field', v)}
                style={{ width: '100%' }}
                showSearch
                optionFilterProp="children"
              >
                {formFields.map((f) => (
                  <Option key={f.fieldKey} value={f.fieldKey}>
                    {f.fieldName}
                  </Option>
                ))}
              </Select>
            </Col>
            <Col span={5}>
              <Select
                placeholder="操作符"
                value={cond.operator}
                onChange={(v) => updateCondition(idx, 'operator', v)}
                style={{ width: '100%' }}
              >
                {OPERATORS.map((o) => (
                  <Option key={o.value} value={o.value}>
                    {o.label}
                  </Option>
                ))}
              </Select>
            </Col>
            <Col span={9}>
              <Input
                placeholder="值"
                value={cond.value}
                onChange={(e) => updateCondition(idx, 'value', e.target.value)}
              />
            </Col>
            <Col span={2}>
              <Button
                type="text"
                danger
                icon={<MinusCircleOutlined />}
                onClick={() => removeCondition(idx)}
              />
            </Col>
          </Row>
        </div>
      ))}
      <Button type="dashed" block icon={<PlusOutlined />} onClick={addCondition}>
        添加条件
      </Button>
    </div>
  )
}
