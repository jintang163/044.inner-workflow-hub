import { useState, useMemo, useCallback } from 'react'
import {
  Input,
  InputNumber,
  Switch,
  Select,
  Collapse,
  Form,
  Button,
  Space,
  Tag,
  Divider,
  List,
  Modal,
  message,
  Row,
  Col,
  Tooltip,
  Popconfirm,
  DatePicker,
  Radio,
  Dropdown,
  Empty,
  Segmented
} from 'antd'
import {
  PlusOutlined,
  DeleteOutlined,
  SettingOutlined,
  MinusCircleOutlined,
  QuestionCircleOutlined,
  EditOutlined,
  DownOutlined,
  UpOutlined,
  ExperimentOutlined,
  ThunderboltOutlined,
  BranchesOutlined
} from '@ant-design/icons'
import type {
  FormilySchema,
  ValidatorRule,
  ReactionRule,
  ReactionRuleV2,
  ConditionNode,
  SimpleCondition,
  GroupCondition,
  ConditionOperator,
  LogicalOperator,
  ReactionAction,
  SelectOption,
  SubFormColumnConfig,
  DisplayType
} from '@/types/form'
import { WIDGET_PRESETS } from './widgetPresets'

interface WidgetPropertiesPanelProps {
  schema: FormilySchema
  selectedField: string | null
  onUpdateField: (fieldPath: string, updates: Record<string, any>) => void
}

const OPERATORS: Array<{ label: string; value: ConditionOperator; needValue?: boolean }> = [
  { label: '等于 (==)', value: '==', needValue: true },
  { label: '不等于 (!=)', value: '!=', needValue: true },
  { label: '大于 (>)', value: '>', needValue: true },
  { label: '小于 (<)', value: '<', needValue: true },
  { label: '大于等于 (>=)', value: '>=', needValue: true },
  { label: '小于等于 (<=)', value: '<=', needValue: true },
  { label: '包含', value: 'contains', needValue: true },
  { label: '不包含', value: 'notContains', needValue: true },
  { label: '开头是', value: 'startsWith', needValue: true },
  { label: '结尾是', value: 'endsWith', needValue: true },
  { label: '为空', value: 'empty', needValue: false },
  { label: '不为空', value: 'notEmpty', needValue: false },
  { label: '在列表中', value: 'in', needValue: true },
  { label: '不在列表中', value: 'notIn', needValue: true }
]

const ACTIONS: Array<{ label: string; value: ReactionAction }> = [
  { label: '显示/隐藏', value: 'visible' },
  { label: '必填/非必填', value: 'required' },
  { label: '只读/可编辑', value: 'readonly' },
  { label: '禁用/启用', value: 'disabled' },
  { label: '设置默认值', value: 'setValue' }
]

const generateId = () => `node_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

const createSimpleCondition = (fieldName?: string): SimpleCondition => ({
  type: 'simple',
  id: generateId(),
  dependencies: fieldName ? [fieldName] : [],
  operator: '==',
  value: ''
})

const createGroupCondition = (logical: LogicalOperator = 'AND'): GroupCondition => ({
  type: 'group',
  id: generateId(),
  logicalOperator: logical,
  children: [createSimpleCondition()]
})

const createRuleV2 = (firstFieldName?: string): ReactionRuleV2 => ({
  id: `rule_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
  name: `规则`,
  enabled: true,
  condition: {
    type: 'group',
    id: generateId(),
    logicalOperator: 'AND',
    children: [createSimpleCondition(firstFieldName)]
  },
  action: 'visible',
  actionValue: true
})

function WidgetPropertiesPanel(props: WidgetPropertiesPanelProps) {
  const { schema, selectedField, onUpdateField } = props

  const [activeKeys, setActiveKeys] = useState<string[]>(['base', 'component', 'validator', 'reaction'])
  const [optionModalOpen, setOptionModalOpen] = useState(false)
  const [tempOptions, setTempOptions] = useState<SelectOption[]>([])
  const [editingOptionIndex, setEditingOptionIndex] = useState<number | null>(null)
  const [newOptionLabel, setNewOptionLabel] = useState('')
  const [newOptionValue, setNewOptionValue] = useState('')
  const [reactionMode, setReactionMode] = useState<'v1' | 'v2'>('v2')
  const [expandedConditionIds, setExpandedConditionIds] = useState<Set<string>>(new Set())

  const fieldSchema = useMemo(() => {
    if (!selectedField || !schema?.properties) return null
    return schema.properties[selectedField]
  }, [selectedField, schema])

  const componentType = useMemo(() => {
    return fieldSchema?.['x-component'] || ''
  }, [fieldSchema])

  const hasOptions = useMemo(() => {
    const preset = WIDGET_PRESETS.find(w => w.component === componentType)
    return preset?.hasOptions || componentType === 'Cascader'
  }, [componentType])

  const componentProps = useMemo(() => {
    return fieldSchema?.['x-component-props'] || {}
  }, [fieldSchema])

  const validators = useMemo((): ValidatorRule[] => {
    return fieldSchema?.['x-validator'] || []
  }, [fieldSchema])

  const reactions = useMemo((): ReactionRule[] => {
    return fieldSchema?.['x-reactions-config'] || []
  }, [fieldSchema])

  const v2Reactions = useMemo((): ReactionRuleV2[] => {
    return fieldSchema?.['x-reactions-v2'] || []
  }, [fieldSchema])

  const allFieldNames = useMemo(() => {
    if (!schema?.properties) return []
    return Object.keys(schema.properties).filter(n => n !== selectedField)
  }, [schema, selectedField])

  if (!selectedField || !fieldSchema) {
    return (
      <div
        style={{
          width: 300,
          height: '100%',
          borderLeft: '1px solid #f0f0f0',
          background: '#fafafa',
          display: 'flex',
          flexDirection: 'column'
        }}
      >
        <div
          style={{
            padding: '16px',
            borderBottom: '1px solid #f0f0f0',
            fontWeight: 600,
            fontSize: 14,
            background: '#fff'
          }}
        >
          <SettingOutlined style={{ marginRight: 8 }} />
          属性配置
        </div>
        <div
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#8c8c8c',
            padding: 24,
            textAlign: 'center'
          }}
        >
          <div>
            <div style={{ fontSize: 48, marginBottom: 16, opacity: 0.3 }}>👈</div>
            <div>请在画布中选择一个控件<br />以配置其属性</div>
          </div>
        </div>
      </div>
    )
  }

  const updateValue = (key: string, value: any) => {
    onUpdateField(selectedField, { [key]: value })
  }

  const updateComponentProp = (key: string, value: any) => {
    const newProps = { ...componentProps, [key]: value }
    onUpdateField(selectedField, { 'x-component-props': newProps })
  }

  const handleValidatorAdd = () => {
    const newValidator: ValidatorRule = {
      type: 'required',
      message: '此字段必填'
    }
    updateValue('x-validator', [...validators, newValidator])
  }

  const handleValidatorUpdate = (index: number, key: string, value: any) => {
    const newValidators = [...validators]
    newValidators[index] = { ...newValidators[index], [key]: value }
    updateValue('x-validator', newValidators)
  }

  const handleValidatorRemove = (index: number) => {
    const newValidators = validators.filter((_, i) => i !== index)
    updateValue('x-validator', newValidators)
  }

  const handleReactionAdd = () => {
    const newReaction: ReactionRule = {
      id: `reaction_${Date.now()}`,
      dependencies: [],
      operator: '==',
      value: '',
      action: 'visible',
      actionValue: true
    }
    updateValue('x-reactions-config', [...reactions, newReaction])
  }

  const handleReactionUpdate = (index: number, key: string, value: any) => {
    const newReactions = [...reactions]
    newReactions[index] = { ...newReactions[index], [key]: value }
    updateValue('x-reactions-config', newReactions)
  }

  const handleReactionRemove = (index: number) => {
    const newReactions = reactions.filter((_, i) => i !== index)
    updateValue('x-reactions-config', newReactions)
  }

  const handleRuleV2Add = () => {
    const firstField = allFieldNames[0]
    const newRule = createRuleV2(firstField)
    updateValue('x-reactions-v2', [...v2Reactions, newRule])
  }

  const handleRuleV2Update = (index: number, updates: Partial<ReactionRuleV2>) => {
    const newRules = [...v2Reactions]
    newRules[index] = { ...newRules[index], ...updates }
    updateValue('x-reactions-v2', newRules)
  }

  const handleRuleV2Remove = (index: number) => {
    const newRules = v2Reactions.filter((_, i) => i !== index)
    updateValue('x-reactions-v2', newRules)
  }

  const updateConditionNodeInTree = useCallback(
    (root: ConditionNode, targetId: string, updater: (n: ConditionNode) => ConditionNode): ConditionNode => {
      if (root.id === targetId) {
        return updater(root)
      }
      if (root.type === 'group') {
        return {
          ...root,
          children: root.children.map(c => updateConditionNodeInTree(c, targetId, updater))
        }
      }
      return root
    },
    []
  )

  const deleteConditionNodeFromTree = useCallback(
    (root: ConditionNode, targetId: string): ConditionNode | null => {
      if (root.id === targetId) return null
      if (root.type === 'group') {
        const newChildren: ConditionNode[] = []
        for (const child of root.children) {
          const r = deleteConditionNodeFromTree(child, targetId)
          if (r) newChildren.push(r)
        }
        return { ...root, children: newChildren }
      }
      return root
    },
    []
  )

  const addChildToGroup = useCallback(
    (root: ConditionNode, groupId: string, newNode: ConditionNode): ConditionNode => {
      if (root.id === groupId && root.type === 'group') {
        return { ...root, children: [...root.children, newNode] }
      }
      if (root.type === 'group') {
        return {
          ...root,
          children: root.children.map(c => addChildToGroup(c, groupId, newNode))
        }
      }
      return root
    },
    []
  )

  const handleConditionNodeUpdate = (
    ruleIndex: number,
    nodeId: string,
    updater: (n: ConditionNode) => ConditionNode
  ) => {
    const rule = v2Reactions[ruleIndex]
    if (!rule) return
    const newCondition = updateConditionNodeInTree(rule.condition, nodeId, updater)
    handleRuleV2Update(ruleIndex, { condition: newCondition })
  }

  const handleConditionNodeDelete = (ruleIndex: number, nodeId: string) => {
    const rule = v2Reactions[ruleIndex]
    if (!rule) return
    const newCondition = deleteConditionNodeFromTree(rule.condition, nodeId)
    if (!newCondition) {
      message.warning('至少需要保留一个条件组')
      return
    }
    if (newCondition.type === 'group' && newCondition.children.length === 0) {
      message.warning('条件组至少需要一个子条件')
      return
    }
    handleRuleV2Update(ruleIndex, { condition: newCondition })
  }

  const handleAddSimpleChild = (ruleIndex: number, groupId: string) => {
    const firstField = allFieldNames[0]
    handleConditionNodeUpdate(ruleIndex, groupId, (node) => {
      if (node.type !== 'group') return node
      return {
        ...node,
        children: [...node.children, createSimpleCondition(firstField)]
      }
    })
  }

  const handleAddGroupChild = (ruleIndex: number, groupId: string) => {
    handleConditionNodeUpdate(ruleIndex, groupId, (node) => {
      if (node.type !== 'group') return node
      return {
        ...node,
        children: [...node.children, createGroupCondition('AND')]
      }
    })
  }

  const toggleNodeExpand = (nodeId: string) => {
    setExpandedConditionIds(prev => {
      const next = new Set(prev)
      if (next.has(nodeId)) next.delete(nodeId)
      else next.add(nodeId)
      return next
    })
  }

  const convertSimpleToGroup = (ruleIndex: number, nodeId: string, logical: LogicalOperator) => {
    handleConditionNodeUpdate(ruleIndex, nodeId, (node) => {
      if (node.type !== 'simple') return node
      return {
        type: 'group',
        id: generateId(),
        logicalOperator: logical,
        children: [node]
      }
    })
  }

  const renderConditionNode = (
    ruleIndex: number,
    node: ConditionNode,
    depth: number,
    _rule: ReactionRuleV2
  ): React.ReactNode => {
    const indent = depth * 12
    const isExpanded = expandedConditionIds.size === 0 || expandedConditionIds.has(node.id)

    if (node.type === 'simple') {
      const opDef = OPERATORS.find(o => o.value === node.operator)
      return (
        <div
          style={{
            padding: 6,
            marginLeft: indent,
            border: '1px dashed #d9d9d9',
            borderRadius: 4,
            marginBottom: 6,
            background: '#fafafa'
          }}
        >
          <div style={{ display: 'flex', gap: 4, alignItems: 'center', marginBottom: 6, flexWrap: 'wrap' }}>
            <Tag color="green" style={{ margin: 0, fontSize: 11 }}>条件</Tag>
            <Dropdown
              menu={{
                items: [
                  { key: 'to-and', label: '升级为【且】组', onClick: () => convertSimpleToGroup(ruleIndex, node.id, 'AND') },
                  { key: 'to-or', label: '升级为【或】组', onClick: () => convertSimpleToGroup(ruleIndex, node.id, 'OR') }
                ]
              }}
              trigger={['click']}
            >
              <Button type="text" size="small" icon={<BranchesOutlined />} />
            </Dropdown>
            <div style={{ flex: 1 }} />
            <Popconfirm title="删除此条件？" onConfirm={() => handleConditionNodeDelete(ruleIndex, node.id)}>
              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          </div>

          <Row gutter={4} style={{ rowGap: 4 }}>
            <Col xs={24} sm={depth === 0 ? 10 : 24}>
              <Select
                size="small"
                placeholder="依赖字段"
                value={node.dependencies}
                mode="multiple"
                maxTagCount={2}
                options={allFieldNames.map(name => ({ label: name, value: name }))}
                onChange={(val) => handleConditionNodeUpdate(ruleIndex, node.id, (n) =>
                  n.type === 'simple' ? { ...n, dependencies: val as string[] } : n
                )}
              />
            </Col>
            <Col xs={24} sm={depth === 0 ? 8 : 24}>
              <Select
                size="small"
                value={node.operator}
                options={OPERATORS.map(o => ({ label: o.label, value: o.value }))}
                onChange={(val) => handleConditionNodeUpdate(ruleIndex, node.id, (n) =>
                  n.type === 'simple' ? { ...n, operator: val as ConditionOperator } : n
                )}
              />
            </Col>
            {opDef?.needValue !== false && (
              <Col xs={24} sm={depth === 0 ? 6 : 24}>
                <Input
                  size="small"
                  placeholder="比较值"
                  value={String(node.value ?? '')}
                  onChange={(e) => {
                    const v = e.target.value
                    const num = Number(v)
                    const final = !isNaN(num) && v !== '' && v !== null ? num : v
                    handleConditionNodeUpdate(ruleIndex, node.id, (n) =>
                      n.type === 'simple' ? { ...n, value: final } : n
                    )
                  }}
                />
              </Col>
            )}
          </Row>
        </div>
      )
    }

    const group: GroupCondition = node
    const childCount = group.children.length

    return (
      <div
        style={{
          marginLeft: indent,
          marginBottom: 6,
          border: `1px solid ${group.logicalOperator === 'AND' ? '#91caff' : '#ffd591'}`,
          borderRadius: 6,
          background: group.logicalOperator === 'AND' ? '#f0f7ff' : '#fffbe6',
          overflow: 'hidden'
        }}
      >
        <div
          style={{
            padding: '4px 6px',
            background: group.logicalOperator === 'AND' ? '#e6f4ff' : '#fff7e6',
            borderBottom: `1px solid ${group.logicalOperator === 'AND' ? '#bae0ff' : '#ffd591'}`,
            display: 'flex',
            alignItems: 'center',
            gap: 6
          }}
        >
          <Tag
            color={group.logicalOperator === 'AND' ? 'blue' : 'orange'}
            style={{ margin: 0, fontSize: 11, fontWeight: 600 }}
          >
            {group.logicalOperator === 'AND' ? 'ALL 且' : 'ANY 或'}
          </Tag>
          <Radio.Group
            size="small"
            optionType="button"
            buttonStyle="solid"
            value={group.logicalOperator}
            onChange={(e) => handleConditionNodeUpdate(ruleIndex, node.id, (n) =>
              n.type === 'group' ? { ...n, logicalOperator: e.target.value as LogicalOperator } : n
            )}
          >
            <Radio.Button value="AND">且（AND）</Radio.Button>
            <Radio.Button value="OR">或（OR）</Radio.Button>
          </Radio.Group>

          <span style={{ color: '#8c8c8c', fontSize: 11, marginLeft: 4 }}>
            {childCount} 项
          </span>

          <div style={{ flex: 1 }} />

          <Button
            type="text"
            size="small"
            onClick={() => toggleNodeExpand(node.id)}
            icon={isExpanded ? <UpOutlined /> : <DownOutlined />}
          />
          <Popconfirm title="删除整个条件组？" onConfirm={() => handleConditionNodeDelete(ruleIndex, node.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </div>

        {isExpanded && (
          <div style={{ padding: 6 }}>
            {group.children.map(child => renderConditionNode(ruleIndex, child, depth + 1, _rule))}

            <Space size={4} style={{ marginTop: 4 }} wrap>
              <Button
                type="dashed"
                size="small"
                icon={<PlusOutlined />}
                onClick={() => handleAddSimpleChild(ruleIndex, group.id)}
              >
                + 条件
              </Button>
              <Button
                type="dashed"
                size="small"
                icon={<BranchesOutlined />}
                onClick={() => handleAddGroupChild(ruleIndex, group.id)}
              >
                + 子组
              </Button>
              {group.children.length === 1 && (
                <Tooltip title="组只剩一个条件时可降级为简单条件">
                  <Button
                    type="text"
                    size="small"
                    disabled={group.children[0].type !== 'simple'}
                    onClick={() => {
                      const onlyChild = group.children[0]
                      if (onlyChild.type !== 'simple') return
                      handleConditionNodeUpdate(ruleIndex, node.id, () => onlyChild)
                    }}
                  >
                    <ExperimentOutlined /> 降级
                  </Button>
                </Tooltip>
              )}
            </Space>
          </div>
        )}
      </div>
    )
  }

  const openOptionModal = () => {
    setTempOptions(JSON.parse(JSON.stringify(componentProps.options || [])))
    setOptionModalOpen(true)
    setEditingOptionIndex(null)
    setNewOptionLabel('')
    setNewOptionValue('')
  }

  const handleAddOption = () => {
    if (!newOptionLabel || !newOptionValue) {
      message.warning('请填写标签和值')
      return
    }
    if (tempOptions.some(o => String(o.value) === String(newOptionValue))) {
      message.warning('值已存在')
      return
    }
    setTempOptions([...tempOptions, { label: newOptionLabel, value: newOptionValue }])
    setNewOptionLabel('')
    setNewOptionValue('')
  }

  const handleRemoveOption = (index: number) => {
    setTempOptions(tempOptions.filter((_, i) => i !== index))
  }

  const handleSaveOptions = () => {
    updateComponentProp('options', tempOptions)
    setOptionModalOpen(false)
    message.success('选项已保存')
  }

  const handleColumnAdd = () => {
    const items = fieldSchema?.items || {}
    const properties = items?.properties || {}
    const colCount = Object.keys(properties).length
    const newColName = `col${colCount + 1}`
    const newCol: SubFormColumnConfig = {
      fieldName: newColName,
      title: `列${colCount + 1}`,
      component: 'Input',
      componentProps: { placeholder: '请输入' }
    }
    const newProperties = {
      ...properties,
      [newColName]: {
        type: 'string',
        title: newCol.title,
        'x-decorator': 'FormItem',
        'x-component': newCol.component,
        'x-component-props': newCol.componentProps
      }
    }
    onUpdateField(selectedField, {
      items: {
        ...items,
        properties: newProperties
      }
    })
  }

  const handleColumnRemove = (colName: string) => {
    const items = fieldSchema?.items || {}
    const properties = { ...(items?.properties || {}) }
    delete properties[colName]
    onUpdateField(selectedField, {
      items: {
        ...items,
        properties
      }
    })
  }

  const handleColumnUpdate = (colName: string, key: string, value: any) => {
    const items = fieldSchema?.items || {}
    const properties = { ...(items?.properties || {}) }
    if (properties[colName]) {
      if (key === 'title') {
        properties[colName] = { ...properties[colName], title: value }
      } else if (key === 'component') {
        properties[colName] = {
          ...properties[colName],
          'x-component': value
        }
      }
    }
    onUpdateField(selectedField, {
      items: {
        ...items,
        properties
      }
    })
  }

  const columns = useMemo(() => {
    const items = fieldSchema?.items || {}
    const props = items?.properties || {}
    return Object.entries(props).map(([name, config]: [string, any]) => ({
      name,
      title: config?.title || name,
      component: config?.['x-component'] || 'Input'
    }))
  }, [fieldSchema])

  return (
    <div
      style={{
        width: 300,
        height: '100%',
        borderLeft: '1px solid #f0f0f0',
        background: '#fafafa',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}
    >
      <div
        style={{
          padding: '16px',
          borderBottom: '1px solid #f0f0f0',
          fontWeight: 600,
          fontSize: 14,
          background: '#fff',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <div>
          <SettingOutlined style={{ marginRight: 8 }} />
          属性配置
        </div>
        <Tag color="blue" style={{ margin: 0, fontSize: 11 }}>{selectedField}</Tag>
      </div>

      <div style={{ flex: 1, overflow: 'auto' }}>
        <Collapse
          activeKey={activeKeys}
          onChange={(keys) => setActiveKeys(keys as string[])}
          ghost
          size="small"
          style={{ background: '#fff' }}
          items={[
            {
              key: 'base',
              label: '⚙️ 基础属性',
              children: (
                <div style={{ padding: '8px 4px' }}>
                  <Form layout="vertical" size="small">
                    <Form.Item label="字段标识 (field name)">
                      <Tooltip title="字段的唯一标识，英文，如 amount">
                        <Input
                          value={selectedField}
                          onChange={(e) => {
                            const newName = e.target.value
                            if (!newName || !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(newName)) {
                              message.warning('请输入合法的字段名（字母开头，英文、数字、下划线）')
                              return
                            }
                            if (schema?.properties && schema.properties[newName] && newName !== selectedField) {
                              message.warning('字段名已存在')
                              return
                            }
                            const newProps = { ...schema.properties }
                            const value = newProps[selectedField]
                            delete newProps[selectedField]
                            const keys = Object.keys(newProps)
                            const idx = keys.indexOf(selectedField)
                            const before = keys.slice(0, idx)
                            const after = keys.slice(idx)
                            const orderedProps: Record<string, any> = {}
                            before.forEach(k => { orderedProps[k] = newProps[k] })
                            orderedProps[newName] = value
                            after.forEach(k => { orderedProps[k] = newProps[k] })
                            onUpdateField('', { __renameField: { from: selectedField, to: newName } })
                          }}
                        />
                      </Tooltip>
                    </Form.Item>

                    <Form.Item label="字段标题 (title)">
                      <Input
                        value={fieldSchema?.title || ''}
                        onChange={(e) => updateValue('title', e.target.value)}
                        placeholder="如：报销金额"
                      />
                    </Form.Item>

                    <Form.Item label="默认值">
                      {componentType === 'Switch' ? (
                        <Switch
                          checked={!!fieldSchema?.default}
                          onChange={(checked) => updateValue('default', checked)}
                        />
                      ) : componentType === 'NumberPicker' || componentType === 'Rate' || componentType === 'Slider' ? (
                        <InputNumber
                          style={{ width: '100%' }}
                          value={fieldSchema?.default}
                          onChange={(val) => updateValue('default', val)}
                        />
                      ) : (
                        <Input
                          value={fieldSchema?.default || ''}
                          onChange={(e) => updateValue('default', e.target.value)}
                          placeholder="默认值"
                        />
                      )}
                    </Form.Item>

                    {componentType !== 'Switch' && componentType !== 'Divider' && componentType !== 'Alert' && (
                      <Form.Item label="占位符 (placeholder)">
                        <Input
                          value={componentProps.placeholder || ''}
                          onChange={(e) => updateComponentProp('placeholder', e.target.value)}
                          placeholder="请输入..."
                        />
                      </Form.Item>
                    )}

                    <Form.Item label="描述 (description)">
                      <Input.TextArea
                        value={fieldSchema?.description || ''}
                        onChange={(e) => updateValue('description', e.target.value)}
                        rows={2}
                        placeholder="提示文字，显示在字段下方"
                      />
                    </Form.Item>

                    <Divider style={{ margin: '12px 0' }} />

                    <Row gutter={8}>
                      <Col span={12}>
                        <Form.Item label="是否必填">
                          <Switch
                            checked={!!fieldSchema?.required}
                            onChange={(checked) => updateValue('required', checked)}
                          />
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item label="是否只读">
                          <Switch
                            checked={!!componentProps.readOnly || !!fieldSchema?.readOnly}
                            onChange={(checked) => updateComponentProp('readOnly', checked)}
                          />
                        </Form.Item>
                      </Col>
                    </Row>

                    <Row gutter={8}>
                      <Col span={12}>
                        <Form.Item label="是否隐藏">
                          <Switch
                            checked={fieldSchema?.['x-display'] === 'none' || !!fieldSchema?.hidden}
                            onChange={(checked) =>
                              updateValue('x-display', checked ? 'none' : undefined as any)
                            }
                          />
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item label="是否禁用">
                          <Switch
                            checked={!!componentProps.disabled || !!fieldSchema?.disabled}
                            onChange={(checked) => updateComponentProp('disabled', checked)}
                          />
                        </Form.Item>
                      </Col>
                    </Row>

                    <Form.Item label="显示模式">
                      <Select
                        value={fieldSchema?.['x-display'] || 'visible'}
                        onChange={(val: DisplayType) => updateValue('x-display', val)}
                        options={[
                          { label: '正常显示', value: 'visible' },
                          { label: '内嵌显示', value: 'embedded' },
                          { label: '隐藏', value: 'none' }
                        ]}
                      />
                    </Form.Item>

                    <Form.Item label="宽度 (栅格 1-24)">
                      <InputNumber
                        min={1}
                        max={24}
                        style={{ width: '100%' }}
                        value={componentProps.colSpan || 24}
                        onChange={(val) => updateComponentProp('colSpan', val)}
                      />
                    </Form.Item>
                  </Form>
                </div>
              )
            },
            {
              key: 'component',
              label: '🎨 控件属性',
              children: (
                <div style={{ padding: '8px 4px' }}>
                  <Form layout="vertical" size="small">
                    {hasOptions && (
                      <Form.Item label="选项配置">
                        <Button
                          size="small"
                          icon={<EditOutlined />}
                          onClick={openOptionModal}
                          block
                        >
                          编辑选项 ({(componentProps.options || []).length})
                        </Button>
                      </Form.Item>
                    )}

                    {(componentType === 'Upload') && (
                      <>
                        <Form.Item label="文件数量上限">
                          <InputNumber
                            min={1}
                            style={{ width: '100%' }}
                            value={componentProps.maxCount || 5}
                            onChange={(val) => updateComponentProp('maxCount', val)}
                          />
                        </Form.Item>
                        <Form.Item label="文件大小限制 (MB)">
                          <InputNumber
                            min={0.1}
                            step={0.5}
                            style={{ width: '100%' }}
                            value={componentProps.maxSize || 10}
                            onChange={(val) => updateComponentProp('maxSize', val)}
                          />
                        </Form.Item>
                        <Form.Item label="允许的文件类型">
                          <Select
                            mode="tags"
                            style={{ width: '100%' }}
                            value={componentProps.acceptTypes || []}
                            onChange={(val) => updateComponentProp('acceptTypes', val)}
                            placeholder="输入如 .pdf,.doc 回车添加"
                          />
                        </Form.Item>
                      </>
                    )}

                    {(componentType === 'NumberPicker') && (
                      <>
                        <Row gutter={8}>
                          <Col span={12}>
                            <Form.Item label="最小值">
                              <InputNumber
                                style={{ width: '100%' }}
                                value={componentProps.min}
                                onChange={(val) => updateComponentProp('min', val)}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item label="最大值">
                              <InputNumber
                                style={{ width: '100%' }}
                                value={componentProps.max}
                                onChange={(val) => updateComponentProp('max', val)}
                              />
                            </Form.Item>
                          </Col>
                        </Row>
                        <Row gutter={8}>
                          <Col span={12}>
                            <Form.Item label="步长">
                              <InputNumber
                                style={{ width: '100%' }}
                                step={0.1}
                                value={componentProps.step || 1}
                                onChange={(val) => updateComponentProp('step', val)}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item label="小数精度">
                              <InputNumber
                                min={0}
                                max={10}
                                style={{ width: '100%' }}
                                value={componentProps.precision}
                                onChange={(val) => updateComponentProp('precision', val)}
                              />
                            </Form.Item>
                          </Col>
                        </Row>
                        <Form.Item label="单位（后缀）">
                          <Input
                            value={componentProps.addonAfter || ''}
                            onChange={(e) => updateComponentProp('addonAfter', e.target.value)}
                            placeholder="如：元、%"
                          />
                        </Form.Item>
                      </>
                    )}

                    {(componentType === 'DatePicker' || componentType === 'DateTimePicker' || componentType === 'TimePicker') && (
                      <>
                        <Form.Item label="格式">
                          <Input
                            value={componentProps.format || ''}
                            onChange={(e) => updateComponentProp('format', e.target.value)}
                            placeholder="如：YYYY-MM-DD"
                          />
                        </Form.Item>
                        <Row gutter={8}>
                          <Col span={12}>
                            <Form.Item label="禁用过去">
                              <Switch
                                checked={!!componentProps.disabledPast}
                                onChange={(checked) => updateComponentProp('disabledPast', checked)}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item label="禁用未来">
                              <Switch
                                checked={!!componentProps.disabledFuture}
                                onChange={(checked) => updateComponentProp('disabledFuture', checked)}
                              />
                            </Form.Item>
                          </Col>
                        </Row>
                        {(componentType === 'DatePicker' || componentType === 'DateTimePicker') && (
                          <Form.Item label="范围选择">
                            <Switch
                              checked={!!componentProps.range}
                              onChange={(checked) => updateComponentProp('range', checked)}
                            />
                          </Form.Item>
                        )}
                      </>
                    )}

                    {(componentType === 'Rate') && (
                      <Form.Item label="最高分">
                        <InputNumber
                          min={1}
                          max={10}
                          style={{ width: '100%' }}
                          value={componentProps.count || 5}
                          onChange={(val) => updateComponentProp('count', val)}
                        />
                      </Form.Item>
                    )}

                    {(componentType === 'Slider') && (
                      <>
                        <Row gutter={8}>
                          <Col span={12}>
                            <Form.Item label="最小值">
                              <InputNumber
                                style={{ width: '100%' }}
                                value={componentProps.min || 0}
                                onChange={(val) => updateComponentProp('min', val)}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item label="最大值">
                              <InputNumber
                                style={{ width: '100%' }}
                                value={componentProps.max || 100}
                                onChange={(val) => updateComponentProp('max', val)}
                              />
                            </Form.Item>
                          </Col>
                        </Row>
                        <Form.Item label="步长">
                          <InputNumber
                            min={1}
                            style={{ width: '100%' }}
                            value={componentProps.step || 1}
                            onChange={(val) => updateComponentProp('step', val)}
                          />
                        </Form.Item>
                      </>
                    )}

                    {(componentType === 'TextArea') && (
                      <Form.Item label="行数">
                        <InputNumber
                          min={1}
                          max={20}
                          style={{ width: '100%' }}
                          value={componentProps.rows || 3}
                          onChange={(val) => updateComponentProp('rows', val)}
                        />
                      </Form.Item>
                    )}

                    {(componentType === 'Divider') && (
                      <>
                        <Form.Item label="方向">
                          <Radio.Group
                            value={componentProps.type || 'horizontal'}
                            onChange={(e) => updateComponentProp('type', e.target.value)}
                          >
                            <Radio value="horizontal">水平</Radio>
                            <Radio value="vertical">垂直</Radio>
                          </Radio.Group>
                        </Form.Item>
                        <Form.Item label="虚线">
                          <Switch
                            checked={!!componentProps.dashed}
                            onChange={(checked) => updateComponentProp('dashed', checked)}
                          />
                        </Form.Item>
                      </>
                    )}

                    {(componentType === 'Alert') && (
                      <>
                        <Form.Item label="提示类型">
                          <Select
                            value={componentProps.type || 'info'}
                            onChange={(val) => updateComponentProp('type', val)}
                            options={[
                              { label: '信息', value: 'info' },
                              { label: '成功', value: 'success' },
                              { label: '警告', value: 'warning' },
                              { label: '错误', value: 'error' }
                            ]}
                          />
                        </Form.Item>
                        <Form.Item label="提示内容">
                          <Input.TextArea
                            value={componentProps.message || ''}
                            onChange={(e) => updateComponentProp('message', e.target.value)}
                            rows={3}
                          />
                        </Form.Item>
                        <Form.Item label="显示图标">
                          <Switch
                            checked={!!componentProps.showIcon}
                            onChange={(checked) => updateComponentProp('showIcon', checked)}
                          />
                        </Form.Item>
                      </>
                    )}

                    {(componentType === 'UserSelect') && (
                      <>
                        <Form.Item label="是否多选">
                          <Switch
                            checked={!!componentProps.multiple}
                            onChange={(checked) => {
                              updateComponentProp('multiple', checked)
                              updateValue('type', checked ? 'array' : 'number')
                            }}
                          />
                        </Form.Item>
                        {componentProps.multiple && (
                          <Form.Item label="最大选择数">
                            <InputNumber
                              min={1}
                              style={{ width: '100%' }}
                              value={componentProps.maxCount}
                              onChange={(val) => updateComponentProp('maxCount', val)}
                            />
                          </Form.Item>
                        )}
                      </>
                    )}

                    {(componentType === 'DeptSelect') && (
                      <Form.Item label="是否多选">
                        <Switch
                          checked={!!componentProps.multiple}
                          onChange={(checked) => {
                            updateComponentProp('multiple', checked)
                            updateValue('type', checked ? 'array' : 'number')
                          }}
                        />
                      </Form.Item>
                    )}

                    {(componentType === 'ArrayTable') && (
                      <>
                        <Divider style={{ margin: '8px 0' }} orientation="left">
                          <span style={{ fontSize: 12 }}>列配置</span>
                        </Divider>
                        <List
                          size="small"
                          dataSource={columns}
                          locale={{ emptyText: '暂无列，请添加' }}
                          renderItem={(col, idx) => (
                            <List.Item
                              key={col.name}
                              style={{ padding: '8px 0', flexWrap: 'wrap', gap: 8 }}
                            >
                              <Space.Compact style={{ width: '100%' }}>
                                <Input
                                  size="small"
                                  placeholder="列标题"
                                  value={col.title}
                                  style={{ width: 100 }}
                                  onChange={(e) => handleColumnUpdate(col.name, 'title', e.target.value)}
                                />
                                <Select
                                  size="small"
                                  value={col.component}
                                  style={{ width: 110 }}
                                  onChange={(val) => handleColumnUpdate(col.name, 'component', val)}
                                  options={[
                                    { label: '输入框', value: 'Input' },
                                    { label: '数字', value: 'NumberPicker' },
                                    { label: '选择器', value: 'Select' },
                                    { label: '日期', value: 'DatePicker' }
                                  ]}
                                />
                              </Space.Compact>
                              <Space>
                                {idx > 0 && (
                                  <Button
                                    size="small"
                                    type="text"
                                    icon={<UpOutlined />}
                                    onClick={() => {
                                      const items = fieldSchema?.items || {}
                                      const props = { ...(items?.properties || {}) }
                                      const keys = Object.keys(props)
                                      const i = keys.indexOf(col.name)
                                      if (i > 0) {
                                        const temp = keys[i - 1]
                                        keys[i - 1] = keys[i]
                                        keys[i] = temp
                                        const newProps: Record<string, any> = {}
                                        keys.forEach(k => { newProps[k] = props[k] })
                                        onUpdateField(selectedField, {
                                          items: { ...items, properties: newProps }
                                        })
                                      }
                                    }}
                                  />
                                )}
                                {idx < columns.length - 1 && (
                                  <Button
                                    size="small"
                                    type="text"
                                    icon={<DownOutlined />}
                                    onClick={() => {
                                      const items = fieldSchema?.items || {}
                                      const props = { ...(items?.properties || {}) }
                                      const keys = Object.keys(props)
                                      const i = keys.indexOf(col.name)
                                      if (i < keys.length - 1) {
                                        const temp = keys[i + 1]
                                        keys[i + 1] = keys[i]
                                        keys[i] = temp
                                        const newProps: Record<string, any> = {}
                                        keys.forEach(k => { newProps[k] = props[k] })
                                        onUpdateField(selectedField, {
                                          items: { ...items, properties: newProps }
                                        })
                                      }
                                    }}
                                  />
                                )}
                                <Popconfirm
                                  title="确认删除该列？"
                                  onConfirm={() => handleColumnRemove(col.name)}
                                >
                                  <Button
                                    size="small"
                                    type="text"
                                    danger
                                    icon={<DeleteOutlined />}
                                  />
                                </Popconfirm>
                              </Space>
                            </List.Item>
                          )}
                        />
                        <Button
                          type="dashed"
                          size="small"
                          icon={<PlusOutlined />}
                          block
                          style={{ marginTop: 8 }}
                          onClick={handleColumnAdd}
                        >
                          添加列
                        </Button>
                      </>
                    )}
                  </Form>
                </div>
              )
            },
            {
              key: 'validator',
              label: '✅ 校验规则',
              children: (
                <div style={{ padding: '8px 4px' }}>
                  <List
                    size="small"
                    dataSource={validators}
                    locale={{ emptyText: '暂无校验规则' }}
                    renderItem={(rule, index) => (
                      <List.Item
                        key={index}
                        style={{
                          padding: '8px 4px',
                          border: '1px solid #f0f0f0',
                          borderRadius: 4,
                          marginBottom: 8,
                          flexDirection: 'column',
                          alignItems: 'stretch',
                          gap: 8
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Tag color="blue" style={{ margin: 0 }}>
                            规则 {index + 1}
                          </Tag>
                          <Button
                            type="text"
                            size="small"
                            danger
                            icon={<MinusCircleOutlined />}
                            onClick={() => handleValidatorRemove(index)}
                          />
                        </div>
                        <Select
                          size="small"
                          value={rule.type}
                          onChange={(val) => handleValidatorUpdate(index, 'type', val)}
                          options={[
                            { label: '必填', value: 'required' },
                            { label: '正则表达式', value: 'pattern' },
                            { label: '最小长度', value: 'minLength' },
                            { label: '最大长度', value: 'maxLength' },
                            { label: '最小值', value: 'min' },
                            { label: '最大值', value: 'max' },
                            { label: '自定义校验', value: 'custom' }
                          ]}
                        />
                        {(rule.type === 'pattern' || rule.type === 'minLength' || rule.type === 'maxLength' ||
                          rule.type === 'min' || rule.type === 'max') && (
                          <Input
                            size="small"
                            placeholder={
                              rule.type === 'pattern' ? '正则表达式' :
                              rule.type === 'minLength' || rule.type === 'maxLength' ? '长度值' :
                              '数值'
                            }
                            value={
                              rule.type === 'pattern' ? (rule.pattern as string) || '' :
                              rule.type === 'minLength' || rule.type === 'maxLength' ? String((rule as any)[rule.type] || '') :
                              String((rule as any)[rule.type] || '')
                            }
                            onChange={(e) => {
                              const val = ['min', 'max', 'minLength', 'maxLength'].includes(rule.type!)
                                ? Number(e.target.value)
                                : e.target.value
                              handleValidatorUpdate(index, rule.type!, val)
                            }}
                          />
                        )}
                        <Input
                          size="small"
                          placeholder="错误提示信息"
                          value={rule.message || ''}
                          onChange={(e) => handleValidatorUpdate(index, 'message', e.target.value)}
                        />
                      </List.Item>
                    )}
                  />
                  <Button
                    type="dashed"
                    size="small"
                    icon={<PlusOutlined />}
                    block
                    style={{ marginTop: 8 }}
                    onClick={handleValidatorAdd}
                  >
                    添加校验规则
                  </Button>
                </div>
              )
            },
            {
              key: 'reaction',
              label: '🔗 联动规则',
              children: (
                <div style={{ padding: '8px 4px' }}>
                  <div
                    style={{
                      padding: '8px',
                      background: '#f0f5ff',
                      borderRadius: 4,
                      marginBottom: 8,
                      fontSize: 12,
                      color: '#597ef7',
                      display: 'flex',
                      gap: 4,
                      flexDirection: 'column'
                    }}
                  >
                    <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                      <QuestionCircleOutlined />
                      <span>当依赖字段满足条件时，对当前字段执行联动动作</span>
                    </div>
                    <Segmented
                      size="small"
                      value={reactionMode}
                      onChange={(v) => setReactionMode(v as 'v1' | 'v2')}
                      options={[
                        { label: (
                          <Space size={4}>
                            <ThunderboltOutlined />简单模式
                          </Space>
                        ), value: 'v1' },
                        { label: (
                          <Space size={4}>
                            <BranchesOutlined />高级模式（AND/OR）
                          </Space>
                        ), value: 'v2' }
                      ]}
                      style={{ marginTop: 6, alignSelf: 'flex-start' }}
                    />
                  </div>

                  {reactionMode === 'v1' && (
                    <>
                      <List
                        size="small"
                        dataSource={reactions}
                        locale={{ emptyText: '暂无联动规则' }}
                        renderItem={(reaction, index) => (
                          <List.Item
                            key={reaction.id}
                            style={{
                              padding: '8px',
                              border: '1px solid #f0f0f0',
                              borderRadius: 4,
                              marginBottom: 8,
                              flexDirection: 'column',
                              alignItems: 'stretch',
                              gap: 8
                            }}
                          >
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <Tag color="purple" style={{ margin: 0 }}>
                                规则 {index + 1}
                              </Tag>
                              <Button
                                type="text"
                                size="small"
                                danger
                                icon={<MinusCircleOutlined />}
                                onClick={() => handleReactionRemove(index)}
                              />
                            </div>

                            <Select
                              size="small"
                              mode={reaction.operator === 'empty' || reaction.operator === 'notEmpty' ? undefined : 'multiple'}
                              placeholder="选择依赖字段"
                              value={reaction.dependencies}
                              onChange={(val) => handleReactionUpdate(index, 'dependencies', val)}
                              options={allFieldNames.map(name => ({
                                label: name,
                                value: name
                              }))}
                              style={{ maxTagCount: 3 }}
                            />

                            <Select
                              size="small"
                              value={reaction.operator}
                              onChange={(val) => handleReactionUpdate(index, 'operator', val)}
                              options={OPERATORS.map(o => ({ label: o.label, value: o.value }))}
                            />

                            {(() => {
                              const op = OPERATORS.find(o => o.value === reaction.operator)
                              return op?.needValue !== false && reaction.operator !== 'empty' && reaction.operator !== 'notEmpty' ? (
                                <Input
                                  size="small"
                                  placeholder="触发值（支持字符串/数字）"
                                  value={String(reaction.value ?? '')}
                                  onChange={(e) => {
                                    const v = e.target.value
                                    const numVal = Number(v)
                                    const finalVal = !isNaN(numVal) && v !== '' && v !== null ? numVal : v
                                    handleReactionUpdate(index, 'value', finalVal)
                                  }}
                                />
                              ) : null
                            })()}

                            <Select
                              size="small"
                              value={reaction.action}
                              onChange={(val) => handleReactionUpdate(index, 'action', val)}
                              options={ACTIONS.map(a => ({ label: a.label, value: a.value }))}
                            />

                            {(reaction.action === 'visible' || reaction.action === 'required' ||
                              reaction.action === 'readonly' || reaction.action === 'disabled') && (
                              <Radio.Group
                                size="small"
                                value={reaction.actionValue ?? true}
                                onChange={(e) => handleReactionUpdate(index, 'actionValue', e.target.value)}
                              >
                                <Radio value={true}>满足条件</Radio>
                                <Radio value={false}>不满足条件</Radio>
                              </Radio.Group>
                            )}

                            {reaction.action === 'setValue' && (
                              <Input
                                size="small"
                                placeholder="设置的默认值"
                                value={String(reaction.actionValue ?? '')}
                                onChange={(e) => handleReactionUpdate(index, 'actionValue', e.target.value)}
                              />
                            )}
                          </List.Item>
                        )}
                      />
                      <Button
                        type="dashed"
                        size="small"
                        icon={<PlusOutlined />}
                        block
                        style={{ marginTop: 8 }}
                        onClick={handleReactionAdd}
                      >
                        添加简单规则
                      </Button>
                    </>
                  )}

                  {reactionMode === 'v2' && (
                    <>
                      {v2Reactions.length === 0 && (
                        <Empty
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                          description="暂无高级联动规则"
                          style={{ padding: '16px 0' }}
                        />
                      )}
                      {v2Reactions.map((rule, ruleIdx) => (
                        <div
                          key={rule.id}
                          style={{
                            border: '1px solid #e8e8e8',
                            borderRadius: 6,
                            marginBottom: 10,
                            overflow: 'hidden',
                            background: '#fff'
                          }}
                        >
                          <div
                            style={{
                              padding: '6px 8px',
                              background: rule.enabled !== false ? '#f6ffed' : '#fafafa',
                              borderBottom: '1px solid #f0f0f0',
                              display: 'flex',
                              alignItems: 'center',
                              gap: 6
                            }}
                          >
                            <Switch
                              size="small"
                              checked={rule.enabled !== false}
                              onChange={(v) => handleRuleV2Update(ruleIdx, { enabled: v })}
                            />
                            <Input
                              size="small"
                              value={rule.name || ''}
                              placeholder="规则名称"
                              style={{ flex: 1, minWidth: 0 }}
                              onChange={(e) => handleRuleV2Update(ruleIdx, { name: e.target.value })}
                            />
                            <Tag color={rule.action === 'visible' ? 'blue' : 'orange'} style={{ margin: 0 }}>
                              {ACTIONS.find(a => a.value === rule.action)?.label || rule.action}
                            </Tag>
                            <Popconfirm
                              title="删除该联动规则？"
                              onConfirm={() => handleRuleV2Remove(ruleIdx)}
                            >
                              <Button
                                type="text"
                                size="small"
                                danger
                                icon={<DeleteOutlined />}
                              />
                            </Popconfirm>
                          </div>

                          <div style={{ padding: 8 }}>
                            <div
                              style={{
                                fontSize: 11,
                                color: '#8c8c8c',
                                marginBottom: 6,
                                fontWeight: 500
                              }}
                            >
                              📋 条件树
                            </div>
                            {renderConditionNode(ruleIdx, rule.condition, 0, rule)}

                            <Divider style={{ margin: '10px 0' }} />

                            <Form layout="vertical" size="small">
                              <Row gutter={8}>
                                <Col span={12}>
                                  <Form.Item label="执行动作">
                                    <Select
                                      size="small"
                                      value={rule.action}
                                      onChange={(val) => handleRuleV2Update(ruleIdx, { action: val as ReactionAction })}
                                      options={ACTIONS.map(a => ({ label: a.label, value: a.value }))}
                                    />
                                  </Form.Item>
                                </Col>
                                <Col span={12}>
                                  {(['visible', 'required', 'readonly', 'disabled'] as ReactionAction[]).includes(rule.action) && (
                                    <Form.Item label="触发方向">
                                      <Radio.Group
                                        size="small"
                                        value={rule.actionValue ?? true}
                                        onChange={(e) => handleRuleV2Update(ruleIdx, { actionValue: e.target.value })}
                                      >
                                        <Radio value={true}>满足→生效</Radio>
                                        <Radio value={false}>不满足→生效</Radio>
                                      </Radio.Group>
                                    </Form.Item>
                                  )}
                                  {rule.action === 'setValue' && (
                                    <Form.Item label="设置默认值">
                                      <Input
                                        size="small"
                                        value={String(rule.actionValue ?? '')}
                                        onChange={(e) => handleRuleV2Update(ruleIdx, { actionValue: e.target.value })}
                                        placeholder="条件满足时设置的默认值"
                                      />
                                    </Form.Item>
                                  )}
                                </Col>
                              </Row>
                            </Form>
                          </div>
                        </div>
                      ))}
                      <Button
                        type="dashed"
                        size="small"
                        icon={<BranchesOutlined />}
                        block
                        style={{ marginTop: 8 }}
                        onClick={handleRuleV2Add}
                      >
                        添加高级联动规则（支持 AND/OR 组合）
                      </Button>
                    </>
                  )}
                </div>
              )
            }
          ]}
        />
      </div>

      <Modal
        title="编辑选项"
        open={optionModalOpen}
        onCancel={() => setOptionModalOpen(false)}
        onOk={handleSaveOptions}
        okText="保存"
        cancelText="取消"
      >
        <Form layout="vertical" size="small">
          <Form.Item label="选项列表">
            <List
              size="small"
              bordered
              dataSource={tempOptions}
              locale={{ emptyText: '暂无选项' }}
              renderItem={(opt, idx) => (
                <List.Item
                  key={idx}
                  actions={[
                    <Button
                      key="delete"
                      type="text"
                      danger
                      size="small"
                      icon={<DeleteOutlined />}
                      onClick={() => handleRemoveOption(idx)}
                    />
                  ]}
                >
                  <Row gutter={8} style={{ width: '100%' }}>
                    <Col span={12}>
                      <Input
                        size="small"
                        value={opt.label}
                        placeholder="标签"
                        onChange={(e) => {
                          const newOpts = [...tempOptions]
                          newOpts[idx] = { ...newOpts[idx], label: e.target.value }
                          setTempOptions(newOpts)
                        }}
                      />
                    </Col>
                    <Col span={12}>
                      <Input
                        size="small"
                        value={String(opt.value)}
                        placeholder="值"
                        onChange={(e) => {
                          const newOpts = [...tempOptions]
                          newOpts[idx] = { ...newOpts[idx], value: e.target.value }
                          setTempOptions(newOpts)
                        }}
                      />
                    </Col>
                  </Row>
                </List.Item>
              )}
            />
          </Form.Item>
          <Divider style={{ margin: '8px 0' }} />
          <Form.Item label="添加新选项">
            <Row gutter={8} align="middle">
              <Col span={9}>
                <Input
                  size="small"
                  placeholder="标签"
                  value={newOptionLabel}
                  onChange={(e) => setNewOptionLabel(e.target.value)}
                />
              </Col>
              <Col span={9}>
                <Input
                  size="small"
                  placeholder="值"
                  value={newOptionValue}
                  onChange={(e) => setNewOptionValue(e.target.value)}
                />
              </Col>
              <Col span={6}>
                <Button size="small" type="primary" icon={<PlusOutlined />} onClick={handleAddOption}>
                  添加
                </Button>
              </Col>
            </Row>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export { WidgetPropertiesPanel }
export default WidgetPropertiesPanel
