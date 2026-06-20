import { useMemo } from 'react'
import { Modal, Button, Space, message, Tabs } from 'antd'
import { EyeOutlined, ReloadOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { createForm } from '@formily/core'
import { createSchemaField, FormProvider, FormConsumer } from '@formily/react'
import * as AntdVRFC from '@formily/antd-v5'
import { FormItem } from '@formily/antd-v5'
import type {
  FormilySchema,
  ReactionRule,
  ReactionRuleV2,
  ConditionNode,
  SimpleCondition,
  GroupCondition
} from '@/types/form'
import { UserSelect } from '@/components/business/UserSelect'
import { DeptSelect } from '@/components/business/DeptSelect'
import { DictSelect } from '@/components/business/DictSelect'
import {
  DataSourceSelectWrap,
  DataSourceCascaderWrap,
  DataSourceRadioGroupWrap,
  DataSourceCheckboxGroupWrap
} from '@/components/business/DataSourceWraps'

interface FormPreviewerProps {
  open: boolean
  onCancel: () => void
  schema: FormilySchema
}

const SchemaField = createSchemaField({
  components: {
    ...AntdVRFC,
    UserSelect,
    DeptSelect,
    DictSelect,
    Select: DataSourceSelectWrap,
    Cascader: DataSourceCascaderWrap,
    'Radio.Group': DataSourceRadioGroupWrap,
    'Checkbox.Group': DataSourceCheckboxGroupWrap,
    FormItem
  }
})

function collectDependencies(node: ConditionNode): string[] {
  if (node.type === 'simple') {
    return node.dependencies || []
  }
  const deps: string[] = []
  for (const child of node.children) {
    deps.push(...collectDependencies(child))
  }
  return Array.from(new Set(deps))
}

function getDepAccessor(depPath: string, index: number): string {
  if (!depPath.includes('.')) {
    return `$deps[${index}]`
  }
  const parts = depPath.split('.')
  let result = `$deps[${index}]`
  for (let i = 1; i < parts.length; i++) {
    result = `(${result}?.${parts[i]})`
  }
  return result
}

function buildSimpleConditionExpr(
  cond: SimpleCondition,
  depMap: Map<string, number>
): string {
  const parts: string[] = []
  for (const dep of cond.dependencies || []) {
    let idx = depMap.get(dep)
    if (idx === undefined) {
      idx = depMap.size
      depMap.set(dep, idx)
    }
    const depRef = getDepAccessor(dep, idx)
    const v = JSON.stringify(cond.value)

    switch (cond.operator) {
      case '==':
        parts.push(`${depRef} === ${v}`)
        break
      case '!=':
        parts.push(`${depRef} !== ${v}`)
        break
      case '>':
        parts.push(`${depRef} > ${v}`)
        break
      case '<':
        parts.push(`${depRef} < ${v}`)
        break
      case '>=':
        parts.push(`${depRef} >= ${v}`)
        break
      case '<=':
        parts.push(`${depRef} <= ${v}`)
        break
      case 'contains':
        parts.push(
          `${depRef} && typeof ${depRef} === 'string' && ${depRef}.includes(${JSON.stringify(String(cond.value))})`
        )
        break
      case 'notContains':
        parts.push(
          `!${depRef} || typeof ${depRef} !== 'string' || !${depRef}.includes(${JSON.stringify(String(cond.value))})`
        )
        break
      case 'startsWith':
        parts.push(
          `${depRef} && typeof ${depRef} === 'string' && ${depRef}.startsWith(${JSON.stringify(String(cond.value))})`
        )
        break
      case 'endsWith':
        parts.push(
          `${depRef} && typeof ${depRef} === 'string' && ${depRef}.endsWith(${JSON.stringify(String(cond.value))})`
        )
        break
      case 'empty':
        parts.push(
          `!${depRef} || ${depRef} === '' || (Array.isArray(${depRef}) && ${depRef}.length === 0) || (typeof ${depRef} === 'object' && ${depRef} !== null && Object.keys(${depRef}).length === 0)`
        )
        break
      case 'notEmpty':
        parts.push(
          `${depRef} && ${depRef} !== '' && (!Array.isArray(${depRef}) || ${depRef}.length > 0) && (!(typeof ${depRef} === 'object' && ${depRef} !== null) || Object.keys(${depRef}).length > 0)`
        )
        break
      case 'in': {
        const arr = Array.isArray(cond.value) ? cond.value : [cond.value]
        parts.push(
          `${depRef} != null && ${JSON.stringify(arr)}.indexOf(${depRef}) > -1`
        )
        break
      }
      case 'notIn': {
        const arr = Array.isArray(cond.value) ? cond.value : [cond.value]
        parts.push(
          `${depRef} == null || ${JSON.stringify(arr)}.indexOf(${depRef}) === -1`
        )
        break
      }
      default:
        parts.push('true')
    }
  }
  return parts.length > 0 ? parts.join(' || ') : 'true'
}

function buildGroupConditionExpr(
  group: GroupCondition,
  depMap: Map<string, number>
): string {
  if (!group.children || group.children.length === 0) return 'true'
  const childExprs = group.children.map(child =>
    buildConditionExpr(child, depMap)
  )
  const joiner = group.logicalOperator === 'OR' ? ' || ' : ' && '
  return `(${childExprs.join(joiner)})`
}

function buildConditionExpr(
  node: ConditionNode,
  depMap: Map<string, number>
): string {
  if (node.type === 'simple') {
    return buildSimpleConditionExpr(node, depMap)
  }
  return buildGroupConditionExpr(node, depMap)
}

function buildActionState(
  action: string,
  actionValue: any,
  conditionExpr: string,
  _field: any
): Record<string, any> {
  const fulfillState: Record<string, any> = {}
  const isPositive = actionValue !== false

  switch (action) {
    case 'visible': {
      const visibleExpr = isPositive ? conditionExpr : `!(${conditionExpr})`
      fulfillState.visible = `{{ ${visibleExpr} }}`
      fulfillState.display = `{{ ${visibleExpr} ? 'visible' : 'none' }}`
      fulfillState.required = `{{ ${visibleExpr} ? (self.required !== undefined ? self.required : (Array.isArray(self.data) && self.data.length > 0)) : false }}`
      break
    }
    case 'required':
      fulfillState.required = `{{ ${isPositive ? conditionExpr : `!(${conditionExpr})`} }}`
      break
    case 'readonly':
      fulfillState.readOnly = `{{ ${isPositive ? conditionExpr : `!(${conditionExpr})`} }}`
      break
    case 'disabled':
      fulfillState.disabled = `{{ ${isPositive ? conditionExpr : `!(${conditionExpr})`} }}`
      break
    case 'setValue':
      fulfillState.value = `{{ ${conditionExpr} ? ${JSON.stringify(actionValue)} : undefined }}`
      break
    default:
      break
  }
  return fulfillState
}

function buildV1Reactions(field: any): any[] {
  const configs: ReactionRule[] = field?.['x-reactions-config'] || []
  return configs.map((config: ReactionRule) => {
    const { dependencies, operator, value, action, actionValue } = config
    const depMap = new Map<string, number>()
    const fakeCond: SimpleCondition = {
      type: 'simple',
      id: config.id,
      dependencies,
      operator: operator as any,
      value
    }
    const conditionExpr = buildSimpleConditionExpr(fakeCond, depMap)
    const sortedDeps: string[] = new Array(depMap.size)
    depMap.forEach((idx, name) => { sortedDeps[idx] = name })
    const orderedDeps = sortedDeps.filter(Boolean)

    const fulfillState = buildActionState(action, actionValue, conditionExpr, field)
    return {
      dependencies: orderedDeps,
      fulfill: { state: fulfillState }
    }
  })
}

function buildV2Reactions(field: any): any[] {
  const rules: ReactionRuleV2[] = field?.['x-reactions-v2'] || []
  const result: any[] = []

  for (const rule of rules) {
    if (rule.enabled === false) continue
    const depMap = new Map<string, number>()
    const conditionExpr = buildConditionExpr(rule.condition, depMap)
    const depsArray: string[] = new Array(depMap.size)
    depMap.forEach((idx, name) => { depsArray[idx] = name })
    const dependencies = depsArray.filter(Boolean)

    const fulfillState = buildActionState(
      rule.action,
      rule.actionValue,
      conditionExpr,
      field
    )
    result.push({
      dependencies,
      fulfill: { state: fulfillState }
    })
  }
  return result
}

function processSchemaReactions(schema: any): void {
  if (!schema) return

  if (schema.properties) {
    Object.keys(schema.properties).forEach(fieldName => {
      const field = schema.properties[fieldName]

      const v2Rules = field?.['x-reactions-v2'] || []
      if (v2Rules.length > 0) {
        field['x-reactions'] = buildV2Reactions(field)
      } else {
        const v1Configs = field?.['x-reactions-config'] || []
        if (v1Configs.length > 0) {
          field['x-reactions'] = buildV1Reactions(field)
        }
      }

      if (field.properties) {
        processSchemaReactions(field)
      }

      if (field.items) {
        processSchemaReactions(field.items)
      }
    })
  }

  if (schema.items) {
    processSchemaReactions(schema.items)
  }
}

function buildReactions(schema: FormilySchema): FormilySchema {
  if (!schema) return schema
  const newSchema = JSON.parse(JSON.stringify(schema))
  processSchemaReactions(newSchema)
  return newSchema
}

function FormPreviewer(props: FormPreviewerProps) {
  const { open, onCancel, schema } = props

  const effectiveSchema = useMemo(() => buildReactions(schema), [schema])

  const editForm = useMemo(() => createForm(), [])
  const viewForm = useMemo(() => createForm({ readPretty: true }), [])

  const handleReset = () => {
    editForm.reset()
    viewForm.reset()
    message.success('表单已重置')
  }

  const handleSubmit = async () => {
    try {
      const values = await editForm.submit()
      message.success('校验通过，当前表单数据：')
      console.log('Form Values:', values)
    } catch (_e) {
      message.warning('表单校验未通过')
    }
  }

  const fieldCount = useMemo(() => {
    return schema?.properties ? Object.keys(schema.properties).length : 0
  }, [schema])

  return (
    <Modal
      title={
        <Space>
          <EyeOutlined />
          <span>表单预览</span>
          <span style={{ fontSize: 12, color: '#8c8c8c', fontWeight: 'normal' }}>
            共 {fieldCount} 个字段
          </span>
        </Space>
      }
      open={open}
      onCancel={onCancel}
      width={800}
      footer={null}
      destroyOnClose
    >
      <Tabs
        size="small"
        defaultActiveKey="edit"
        items={[
          {
            key: 'edit',
            label: (
              <span>
                ✏️ 编辑模式
              </span>
            ),
            children: (
              <div
                style={{
                  padding: 16,
                  border: '1px solid #f0f0f0',
                  borderRadius: 6,
                  minHeight: 400,
                  maxHeight: 600,
                  overflow: 'auto'
                }}
              >
                <FormProvider form={editForm}>
                  <SchemaField schema={effectiveSchema} />
                  <FormConsumer>
                    {() => null}
                  </FormConsumer>
                </FormProvider>

                <div
                  style={{
                    marginTop: 24,
                    paddingTop: 16,
                    borderTop: '1px dashed #f0f0f0',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                >
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={handleReset}
                  >
                    重置表单
                  </Button>
                  <Space>
                    <Button onClick={onCancel}>取消预览</Button>
                    <Button
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      onClick={handleSubmit}
                    >
                      模拟提交
                    </Button>
                  </Space>
                </div>
              </div>
            )
          },
          {
            key: 'view',
            label: (
              <span>
                👁️ 只读模式
              </span>
            ),
            children: (
              <div
                style={{
                  padding: 16,
                  border: '1px solid #f0f0f0',
                  borderRadius: 6,
                  minHeight: 400,
                  maxHeight: 600,
                  overflow: 'auto'
                }}
              >
                <FormProvider form={viewForm}>
                  <SchemaField schema={effectiveSchema} />
                </FormProvider>
              </div>
            )
          }
        ]}
      />
    </Modal>
  )
}

export { FormPreviewer, buildReactions }
export default FormPreviewer
