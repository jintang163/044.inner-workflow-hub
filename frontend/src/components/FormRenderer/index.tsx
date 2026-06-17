import { useRef, useImperativeHandle, forwardRef, useMemo, useEffect } from 'react'
import { createForm } from '@formily/core'
import {
  createSchemaField,
  FormProvider,
  FormConsumer,
  RecursionField,
  useField,
  observer,
  Field as FormilyField
} from '@formily/react'
import * as AntdVRFC from '@formily/antd-v5'
import type { Field } from '@formily/core'
import { Space, Tag, Input as AntdInput } from 'antd'
import type { FormilySchema, FieldPermissionType } from '@/types/form'
import { UserSelect } from '@/components/business/UserSelect'
import { DeptSelect } from '@/components/business/DeptSelect'
import { buildReactions } from '@/components/FormDesigner/FormPreviewer'

export interface FormRendererProps {
  schema: FormilySchema
  formData?: Record<string, any>
  mode?: 'edit' | 'view'
  fieldPermission?: Record<string, FieldPermissionType>
  onSubmit?: (values: Record<string, any>) => void
  onChange?: (values: Record<string, any>) => void
}

export interface FormRendererRef {
  submit: () => Promise<Record<string, any>>
  getFormData: () => Record<string, any>
  validate: () => Promise<Record<string, any>>
  reset: () => void
  setFieldValue: (field: string, value: any) => void
  getFieldValue: (field: string) => any
  getFormInstance: () => ReturnType<typeof createForm> | null
}

const ViewText = observer((_props: any) => {
  const field = useField<Field>()
  const value = field.value
  const component = field.componentProps?.['x-view-component'] || field.componentType

  const renderValue = () => {
    if (value === null || value === undefined || value === '') {
      return <span style={{ color: '#bfbfbf' }}>-</span>
    }

    switch (component) {
      case 'Switch':
        return (
          <Tag color={value ? 'green' : 'default'}>
            {value ? '是' : '否'}
          </Tag>
        )
      case 'Rate':
        return (
          <span style={{ color: '#faad14' }}>
            {'★'.repeat(value)}
            {'☆'.repeat(Math.max(0, (field.componentProps?.count || 5) - value))}
          </span>
        )
      case 'NumberPicker': {
        const unit = field.componentProps?.addonAfter
        const precision = field.componentProps?.precision
        let numStr = String(value)
        if (precision !== undefined && typeof value === 'number') {
          numStr = value.toFixed(precision)
        }
        return (
          <span>
            {numStr}
            {unit ? ` ${unit}` : ''}
          </span>
        )
      }
      case 'Select':
      case 'Radio.Group':
      case 'Checkbox.Group': {
        const options = field.componentProps?.options || []
        if (Array.isArray(value)) {
          return (
            <Space size={4} wrap>
              {value.map((v: any) => {
                const opt = options.find((o: any) => String(o.value) === String(v))
                return <Tag key={v}>{opt?.label || v}</Tag>
              })}
            </Space>
          )
        }
        const selected = options.find((o: any) => String(o.value) === String(value))
        return <Tag>{selected?.label || value}</Tag>
      }
      case 'Upload': {
        const files = Array.isArray(value) ? value : []
        if (files.length === 0) {
          return <span style={{ color: '#bfbfbf' }}>-</span>
        }
        return (
          <Space direction="vertical" size={2}>
            {files.map((f: any, i: number) => (
              <a key={i} href={f.url || f.response} target="_blank" rel="noreferrer">
                📎 {f.name || `附件${i + 1}`}
              </a>
            ))}
          </Space>
        )
      }
      case 'Cascader': {
        return Array.isArray(value) ? value.join(' / ') : String(value)
      }
      case 'UserSelect': {
        const users = Array.isArray(value) ? value : value ? [value] : []
        if (users.length === 0) {
          return <span style={{ color: '#bfbfbf' }}>-</span>
        }
        return (
          <Space size={4} wrap>
            {users.map((u: any, i: number) => (
              <Tag key={i} color="blue">
                👤 {typeof u === 'object' ? (u.nickname || u.username) : `用户${u}`}
              </Tag>
            ))}
          </Space>
        )
      }
      case 'DeptSelect': {
        const depts = Array.isArray(value) ? value : value ? [value] : []
        if (depts.length === 0) {
          return <span style={{ color: '#bfbfbf' }}>-</span>
        }
        return (
          <Space size={4} wrap>
            {depts.map((d: any, i: number) => (
              <Tag key={i} color="purple">
                🏢 {typeof d === 'object' ? d.deptName : `部门${d}`}
              </Tag>
            ))}
          </Space>
        )
      }
      case 'ArrayTable':
      case 'SubForm': {
        const rows = Array.isArray(value) ? value : []
        if (rows.length === 0) {
          return <span style={{ color: '#bfbfbf' }}>-</span>
        }
        return (
          <div>
            {rows.map((row: any, i: number) => (
              <div
                key={i}
                style={{
                  padding: '4px 8px',
                  marginBottom: 4,
                  background: '#fafafa',
                  borderRadius: 4,
                  fontSize: 12
                }}
              >
                <b>行 {i + 1}：</b>
                {Object.entries(row)
                  .map(([k, v]) => `${k}: ${v ?? '-'}`)
                  .join(' | ')}
              </div>
            ))}
          </div>
        )
      }
      case 'DatePicker':
      case 'DateTimePicker':
      case 'TimePicker': {
        return typeof value === 'object' && value?.format
          ? value.format(field.componentProps?.format || 'YYYY-MM-DD')
          : String(value)
      }
      default:
        return String(value)
    }
  }

  return <AntdInput readOnly value={undefined} style={{ display: 'none' }} /> || (
    <div
      style={{
        minHeight: 32,
        padding: '5px 11px',
        border: '1px solid transparent',
        borderRadius: 6,
        background: 'transparent',
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap'
      }}
    >
      {renderValue()}
    </div>
  )
})

const components: Record<string, any> = {
  ...AntdVRFC,
  UserSelect,
  DeptSelect,
  ViewText,
  FormItem: AntdVRFC.FormItem,
  ArrayTable: AntdVRFC.ArrayTable
}

const SchemaField = createSchemaField({
  components
})

function useFormRenderer() {
  const formRef = useRef<FormRendererRef | null>(null)

  const getFormData = () => {
    return formRef.current?.getFormData() || {}
  }

  const submit = async () => {
    return formRef.current?.submit() || Promise.resolve({})
  }

  const validate = async () => {
    return formRef.current?.validate() || Promise.resolve({})
  }

  const reset = () => {
    formRef.current?.reset()
  }

  const setFieldValue = (field: string, value: any) => {
    formRef.current?.setFieldValue(field, value)
  }

  const getFieldValue = (field: string) => {
    return formRef.current?.getFieldValue(field)
  }

  return {
    formRef,
    getFormData,
    submit,
    validate,
    reset,
    setFieldValue,
    getFieldValue
  }
}

const FormRenderer = forwardRef<FormRendererRef, FormRendererProps>(function FormRenderer(props, ref) {
  const {
    schema,
    formData = {},
    mode = 'edit',
    fieldPermission = {},
    onSubmit,
    onChange
  } = props

  const effectiveSchema = useMemo(() => {
    const built = buildReactions(schema)
    const cloned = JSON.parse(JSON.stringify(built))

    if (cloned?.properties) {
      Object.keys(cloned.properties).forEach(fieldName => {
        const field = cloned.properties[fieldName]
        const permission = fieldPermission[fieldName]

        if (permission === 'hidden' || field?.['x-display'] === 'none') {
          field['x-display'] = 'none'
          field.hidden = true
          return
        }

        const componentType = field?.['x-component'] || ''
        if (componentType === 'Divider' || componentType === 'Alert') {
          return
        }

        if (mode === 'view' || permission === 'readonly') {
          field.readOnly = true
          field['x-read-pretty'] = true

          if (!field['x-view-component']) {
            field['x-view-component'] = componentType
          }
        } else if (permission === 'edit') {
          field.readOnly = false
          field['x-read-pretty'] = false
        }

        const componentProps = field['x-component-props'] || {}
        if (mode === 'view' || permission === 'readonly') {
          field['x-component-props'] = {
            ...componentProps,
            readOnly: true,
            disabled: true
          }
        }
      })
    }

    return cloned
  }, [schema, mode, fieldPermission])

  const form = useMemo(() => {
    return createForm({
      readPretty: mode === 'view',
      values: { ...formData },
      effects() {}
    })
  }, [effectiveSchema])

  useEffect(() => {
    form.setValues(formData)
  }, [JSON.stringify(formData)])

  useImperativeHandle(ref, (): FormRendererRef => ({
    submit: async () => {
      try {
        const values = await form.submit()
        onSubmit?.(values)
        return values
      } catch (errors) {
        throw errors
      }
    },
    getFormData: () => {
      return form.values
    },
    validate: async () => {
      try {
        await form.validate()
        return form.values
      } catch (errors) {
        throw errors
      }
    },
    reset: () => {
      form.reset()
      form.setValues(formData)
    },
    setFieldValue: (field: string, value: any) => {
      form.setValuesIn(field, value)
    },
    getFieldValue: (field: string) => {
      return form.getValue(field)
    },
    getFormInstance: () => form
  }))

  return (
    <FormProvider form={form}>
      <FormConsumer>
        {() => {
          if (onChange) {
            Promise.resolve().then(() => onChange(form.values))
          }
          return (
            <div>
              <SchemaField schema={effectiveSchema} />
              <RecursionField schema={effectiveSchema} name={undefined} />
              {Object.entries(effectiveSchema.properties || {}).map(([name, fieldSchema]) => (
                <FormilyField
                  key={name}
                  name={name}
                  basePath={undefined}
                >
                  {(field) => {
                    if (field.hidden || field.display === 'none') return null
                    return <RecursionField schema={fieldSchema as any} name={name} />
                  }}
                </FormilyField>
              ))}
            </div>
          )
        }}
      </FormConsumer>
    </FormProvider>
  )
})

export { FormRenderer, useFormRenderer, SchemaField, createForm, FormProvider, FormConsumer }
export default FormRenderer
