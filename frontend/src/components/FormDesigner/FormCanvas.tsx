import { useRef, useMemo } from 'react'
import { Empty, Button, Space, message } from 'antd'
import {
  DeleteOutlined,
  CopyOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  DragOutlined
} from '@ant-design/icons'
import { createForm } from '@formily/core'
import { createSchemaField, FormProvider, Field as FormilyField } from '@formily/react'
import * as AntdVRFC from '@formily/antd-v5'
import type { FormilySchema, WidgetConfig } from '@/types/form'
import { UserSelect } from '@/components/business/UserSelect'
import { DeptSelect } from '@/components/business/DeptSelect'

interface FormCanvasProps {
  schema: FormilySchema
  selectedField: string | null
  onSelectField: (fieldName: string | null) => void
  onAddWidget: (widget: WidgetConfig, fieldName: string) => void
  onUpdateField: (fieldPath: string, updates: Record<string, any>) => void
  onRemoveField: (fieldPath: string) => void
  onDuplicateField: (fieldPath: string) => void
  onMoveField: (fromPath: string, toPath: string, position: 'before' | 'after') => void
}

const SchemaField = createSchemaField({
  components: {
    ...AntdVRFC,
    UserSelect,
    DeptSelect
  }
})

function FormCanvas(props: FormCanvasProps) {
  const {
    schema,
    selectedField,
    onSelectField,
    onAddWidget,
    onRemoveField,
    onDuplicateField,
    onMoveField
  } = props

  const dragOverFieldRef = useRef<string | null>(null)
  const dragPositionRef = useRef<'before' | 'after' | null>(null)

  const form = useMemo(
    () =>
      createForm({
        readPretty: false,
        designable: true
      }),
    []
  )

  const fieldNames = useMemo(() => {
    if (!schema?.properties) return []
    return Object.keys(schema.properties)
  }, [schema])

  const isEmpty = fieldNames.length === 0

  const handleDragOver = (e: React.DragEvent, fieldName?: string) => {
    e.preventDefault()
    e.stopPropagation()
    if (fieldName) {
      const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
      const midY = rect.top + rect.height / 2
      dragOverFieldRef.current = fieldName
      dragPositionRef.current = e.clientY < midY ? 'before' : 'after'
    } else {
      dragOverFieldRef.current = null
      dragPositionRef.current = null
    }
  }

  const handleDrop = (e: React.DragEvent, targetField?: string) => {
    e.preventDefault()
    e.stopPropagation()

    const data = e.dataTransfer.getData('application/json')
    if (!data) return

    try {
      const widget: WidgetConfig = JSON.parse(data)
      const baseName = widget.name
      let counter = 1
      let newFieldName = baseName

      if (schema?.properties) {
        while (schema.properties[newFieldName]) {
          newFieldName = `${baseName}_${counter}`
          counter++
        }
      }

      if (targetField) {
        const position = dragPositionRef.current || 'after'
        onAddWidget(widget, newFieldName)
        const names = Object.keys(schema.properties || {})
        const currentIdx = names.indexOf(newFieldName)
        const targetIdx = names.indexOf(targetField)
        const insertIdx = position === 'before' ? targetIdx : targetIdx + 1

        if (currentIdx !== insertIdx && currentIdx >= 0) {
          setTimeout(() => {
            const fromPath = newFieldName
            const allNames = Object.keys({ ...schema.properties, [newFieldName]: widget.defaultSchema })
            const fromIdx = allNames.indexOf(fromPath)
            const toIdx = insertIdx > fromIdx ? insertIdx - 1 : insertIdx
            const targetPath = allNames[toIdx]
            if (targetPath && targetPath !== fromPath) {
              onMoveField(fromPath, targetPath, position)
            }
          }, 0)
        }
      } else {
        onAddWidget(widget, newFieldName)
      }

      message.success(`已添加控件：${widget.label}`)
    } catch (_err) {
      message.error('添加控件失败')
    } finally {
      dragOverFieldRef.current = null
      dragPositionRef.current = null
    }
  }

  const handleFieldDragStart = (e: React.DragEvent, fieldName: string) => {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData(
      'application/x-field-move',
      JSON.stringify({ fieldName })
    )
    e.stopPropagation()
  }

  const handleFieldDrop = (e: React.DragEvent, targetField: string) => {
    e.preventDefault()
    e.stopPropagation()

    const moveData = e.dataTransfer.getData('application/x-field-move')
    if (moveData) {
      try {
        const { fieldName: fromField } = JSON.parse(moveData)
        if (fromField !== targetField) {
          const position = dragPositionRef.current || 'after'
          onMoveField(fromField, targetField, position)
          message.success('已移动控件')
        }
      } catch (_err) {
        // ignore
      }
      dragOverFieldRef.current = null
      dragPositionRef.current = null
      return
    }

    handleDrop(e, targetField)
  }

  const renderFieldActions = (fieldName: string) => {
    if (selectedField !== fieldName) return null

    const currentIndex = fieldNames.indexOf(fieldName)
    const isFirst = currentIndex === 0
    const isLast = currentIndex === fieldNames.length - 1

    return (
      <div
        style={{
          position: 'absolute',
          top: -32,
          right: 0,
          zIndex: 100,
          display: 'flex',
          gap: 4,
          padding: 4,
          background: '#1677ff',
          borderRadius: '6px 6px 0 0'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <Button
          type="text"
          size="small"
          icon={<ArrowUpOutlined />}
          disabled={isFirst}
          style={{ color: '#fff', padding: '0 4px' }}
          onClick={() => {
            if (!isFirst) {
              onMoveField(fieldName, fieldNames[currentIndex - 1], 'before')
            }
          }}
        />
        <Button
          type="text"
          size="small"
          icon={<ArrowDownOutlined />}
          disabled={isLast}
          style={{ color: '#fff', padding: '0 4px' }}
          onClick={() => {
            if (!isLast) {
              onMoveField(fieldName, fieldNames[currentIndex + 1], 'after')
            }
          }}
        />
        <Button
          type="text"
          size="small"
          icon={<CopyOutlined />}
          style={{ color: '#fff', padding: '0 4px' }}
          onClick={() => {
            onDuplicateField(fieldName)
            message.success('已复制控件')
          }}
        />
        <Button
          type="text"
          size="small"
          danger
          icon={<DeleteOutlined />}
          style={{ color: '#fff', padding: '0 4px' }}
          onClick={() => {
            onRemoveField(fieldName)
            message.success('已删除控件')
          }}
        />
      </div>
    )
  }

  const renderCustomSchema = () => {
    if (!schema?.properties) return null

    return Object.entries(schema.properties).map(([fieldName, fieldSchema]) => {
      const isSelected = selectedField === fieldName
      const isDragOver = dragOverFieldRef.current === fieldName
      const dragPos = dragPositionRef.current

      const wrapperStyle: React.CSSProperties = {
        position: 'relative',
        padding: isSelected ? '20px 12px 12px' : '8px',
        margin: isSelected ? '24px 0 12px' : '4px 0',
        border: isSelected ? '2px solid #1677ff' : '1px solid transparent',
        borderRadius: 6,
        background: isSelected ? 'rgba(22, 119, 255, 0.04)' : 'transparent',
        transition: 'all 0.2s',
        cursor: 'pointer'
      }

      if (isDragOver) {
        if (dragPos === 'before') {
          wrapperStyle.borderTop = '2px dashed #1677ff'
        } else {
          wrapperStyle.borderBottom = '2px dashed #1677ff'
        }
      }

      return (
        <div
          key={fieldName}
          style={wrapperStyle}
          onClick={(e) => {
            e.stopPropagation()
            onSelectField(fieldName)
          }}
          draggable
          onDragStart={(e) => handleFieldDragStart(e, fieldName)}
          onDragOver={(e) => handleDragOver(e, fieldName)}
          onDragLeave={() => {
            dragOverFieldRef.current = null
            dragPositionRef.current = null
          }}
          onDrop={(e) => handleFieldDrop(e, fieldName)}
          onMouseEnter={(e) => {
            if (!isSelected) {
              e.currentTarget.style.borderColor = '#91caff'
              e.currentTarget.style.background = 'rgba(22, 119, 255, 0.02)'
            }
          }}
          onMouseLeave={(e) => {
            if (!isSelected) {
              e.currentTarget.style.borderColor = 'transparent'
              e.currentTarget.style.background = 'transparent'
            }
          }}
        >
          {renderFieldActions(fieldName)}
          {isSelected && (
            <div
              style={{
                position: 'absolute',
                left: 12,
                top: 2,
                fontSize: 11,
                color: '#1677ff',
                padding: '2px 6px',
                background: '#e6f4ff',
                borderRadius: 4,
                zIndex: 10,
                display: 'flex',
                alignItems: 'center',
                gap: 4
              }}
            >
              <DragOutlined />
              <span>{fieldName}</span>
            </div>
          )}
          <FormProvider form={form}>
            <FormilyField name={fieldName} />
          </FormProvider>
        </div>
      )
    })
  }

  return (
    <div
      style={{
        flex: 1,
        height: '100%',
        overflow: 'auto',
        background: '#fff',
        display: 'flex',
        flexDirection: 'column'
      }}
      onClick={() => onSelectField(null)}
    >
      <div
        style={{
          padding: '16px 24px',
          borderBottom: '1px solid #f0f0f0',
          fontWeight: 500,
          fontSize: 14,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          position: 'sticky',
          top: 0,
          background: '#fff',
          zIndex: 10
        }}
      >
        <span>📋 表单画布</span>
        <Space size="small">
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>
            共 {fieldNames.length} 个字段
          </span>
        </Space>
      </div>

      <div
        style={{
          flex: 1,
          padding: 24,
          minHeight: 400,
          background: isEmpty ? 'repeating-linear-gradient(45deg, #fafafa, #fafafa 10px, #fff 10px, #fff 20px)' : '#fff'
        }}
        onDragOver={(e) => {
          if (isEmpty) {
            handleDragOver(e)
          }
        }}
        onDrop={(e) => {
          if (isEmpty) {
            handleDrop(e)
          }
        }}
      >
        {isEmpty ? (
          <div
            style={{
              height: 400,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Empty
              description={
                <span style={{ color: '#8c8c8c' }}>
                  👆 从左侧拖拽控件到此处开始设计表单
                </span>
              }
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          </div>
        ) : (
          <div style={{ maxWidth: 800, margin: '0 auto' }}>
            <FormProvider form={form}>
              <SchemaField schema={schema} />
            </FormProvider>
            {renderCustomSchema()}
            <FormProvider form={form}>
              <SchemaField schema={schema} />
            </FormProvider>
          </div>
        )}
      </div>
    </div>
  )
}

export { FormCanvas }
export default FormCanvas
