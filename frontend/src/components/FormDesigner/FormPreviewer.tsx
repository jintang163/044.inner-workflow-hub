import { useMemo } from 'react'
import { Modal, Button, Space, message, Tabs } from 'antd'
import { EyeOutlined, ReloadOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { createForm } from '@formily/core'
import { createSchemaField, FormProvider, FormConsumer } from '@formily/react'
import * as AntdVRFC from '@formily/antd-v5'
import { FormItem } from '@formily/antd-v5'
import type { FormilySchema } from '@/types/form'
import { UserSelect } from '@/components/business/UserSelect'
import { DeptSelect } from '@/components/business/DeptSelect'

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
    FormItem
  }
})

function buildReactions(schema: FormilySchema): FormilySchema {
  if (!schema?.properties) return schema

  const newSchema = JSON.parse(JSON.stringify(schema))

  Object.keys(newSchema.properties).forEach(fieldName => {
    const field = newSchema.properties[fieldName]
    const configs = field?.['x-reactions-config'] || []

    if (configs.length > 0) {
      field['x-reactions'] = configs.map((config: any) => {
        const { dependencies, operator, value, action, actionValue } = config
        let fulfillState: Record<string, any> = {}

        const buildCondition = () => {
          if (!dependencies || dependencies.length === 0) return 'true'
          const deps = dependencies.map((d: string, i: number) => `$deps[${i}]`)

          switch (operator) {
            case '==':
              return deps.map((d: string) => `${d} === ${JSON.stringify(value)}`).join(' && ')
            case '!=':
              return deps.map((d: string) => `${d} !== ${JSON.stringify(value)}`).join(' && ')
            case '>':
              return deps.map((d: string) => `${d} > ${JSON.stringify(value)}`).join(' && ')
            case '<':
              return deps.map((d: string) => `${d} < ${JSON.stringify(value)}`).join(' && ')
            case 'contains':
              return deps.map((d: string) => `${d} && String(${d}).includes(${JSON.stringify(String(value))})`).join(' && ')
            case 'empty':
              return deps.map((d: string) => `!${d} || ${d} === '' || (Array.isArray(${d}) && ${d}.length === 0)`).join(' && ')
            case 'notEmpty':
              return deps.map((d: string) => `${d} && (${d} !== '') && (!Array.isArray(${d}) || ${d}.length > 0)`).join(' && ')
            default:
              return 'true'
          }
        }

        const condition = buildCondition()
        const conditionExpr = actionValue ? condition : `!(${condition})`

        switch (action) {
          case 'visible':
            fulfillState = {
              visible: `{{ ${conditionExpr} }}`,
              display: `{{ ${conditionExpr} ? 'visible' : 'none' }}`
            }
            break
          case 'required':
            fulfillState = {
              required: `{{ ${conditionExpr} }}`
            }
            break
          case 'readonly':
            fulfillState = {
              readOnly: `{{ ${conditionExpr} }}`
            }
            break
          case 'disabled':
            fulfillState = {
              disabled: `{{ ${conditionExpr} }}`
            }
            break
          case 'setValue':
            fulfillState = {
              value: `{{ ${condition} ? ${JSON.stringify(actionValue)} : undefined }}`
            }
            break
          default:
            break
        }

        return {
          dependencies,
          fulfill: {
            state: fulfillState
          }
        }
      })
    }
  })

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
