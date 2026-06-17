import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button,
  Space,
  message,
  Input,
  Popconfirm,
  Breadcrumb,
  Card,
  Tag,
  Modal,
  Form,
  Tooltip
} from 'antd'
import {
  ArrowLeftOutlined,
  SaveOutlined,
  EyeOutlined,
  ClearOutlined,
  CodeOutlined,
  CopyOutlined,
  HistoryOutlined,
  UploadOutlined,
  DownloadOutlined,
  RollbackOutlined
} from '@ant-design/icons'
import type { FormilySchema, WidgetConfig } from '@/types/form'
import { formApi } from '@/api'
import WidgetPalette from '@/components/FormDesigner/WidgetPalette'
import FormCanvas from '@/components/FormDesigner/FormCanvas'
import WidgetPropertiesPanel from '@/components/FormDesigner/WidgetPropertiesPanel'
import FormPreviewer from '@/components/FormDesigner/FormPreviewer'

const EMPTY_SCHEMA: FormilySchema = {
  type: 'object',
  properties: {}
}

function FormDesign() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isEdit = !!id && id !== 'new'

  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [schema, setSchema] = useState<FormilySchema>(EMPTY_SCHEMA)
  const [selectedField, setSelectedField] = useState<string | null>(null)
  const [previewerOpen, setPreviewerOpen] = useState(false)
  const [jsonModalOpen, setJsonModalOpen] = useState(false)
  const [historyModalOpen, setHistoryModalOpen] = useState(false)
  const [formName, setFormName] = useState('')
  const [formKey, setFormKey] = useState('')
  const [description, setDescription] = useState('')
  const [metaModalOpen, setMetaModalOpen] = useState(false)

  useEffect(() => {
    if (isEdit) {
      loadFormData()
    } else {
      setMetaModalOpen(true)
    }
  }, [id])

  const loadFormData = async () => {
    setLoading(true)
    try {
      const def = await formApi.definitionGet(Number(id))
      setFormName(def.formName)
      setFormKey(def.formKey)
      setDescription(def.description || '')

      try {
        const design = await formApi.designGet(Number(id))
        if (design?.schema) {
          setSchema(design.schema)
        }
      } catch (_e) {
        // ignore
      }
    } catch (_e) {
      message.error('加载表单数据失败')
    } finally {
      setLoading(false)
    }
  }

  const handleAddWidget = useCallback(
    (widget: WidgetConfig, fieldName: string) => {
      setSchema(prev => {
        const newProperties = { ...(prev.properties || {}) }
        newProperties[fieldName] = {
          ...JSON.parse(JSON.stringify(widget.defaultSchema)),
          'x-design-name': widget.name
        }
        return {
          ...prev,
          properties: newProperties
        }
      })
      setSelectedField(fieldName)
    },
    []
  )

  const handleUpdateField = useCallback(
    (fieldPath: string, updates: Record<string, any>) => {
      if (updates.__renameField) {
        const { from, to } = updates.__renameField
        setSchema(prev => {
          const props = { ...(prev.properties || {}) }
          const keys = Object.keys(props)
          const idx = keys.indexOf(from)
          const value = props[from]
          delete props[from]

          const before = keys.slice(0, idx)
          const after = keys.slice(idx + 1)
          const ordered: Record<string, any> = {}
          before.forEach(k => { ordered[k] = props[k] })
          ordered[to] = value
          after.forEach(k => { ordered[k] = props[k] })

          return { ...prev, properties: ordered }
        })
        setSelectedField(to)
        return
      }

      if (!fieldPath) return

      setSchema(prev => {
        const props = { ...(prev.properties || {}) }
        if (!props[fieldPath]) return prev

        const newField = { ...props[fieldPath] }
        Object.keys(updates).forEach(key => {
          if (updates[key] === undefined || updates[key] === null) {
            delete newField[key]
          } else {
            newField[key] = updates[key]
          }
        })

        return {
          ...prev,
          properties: {
            ...props,
            [fieldPath]: newField
          }
        }
      })
    },
    []
  )

  const handleRemoveField = useCallback(
    (fieldPath: string) => {
      setSchema(prev => {
        const props = { ...(prev.properties || {}) }
        delete props[fieldPath]
        return { ...prev, properties: props }
      })
      if (selectedField === fieldPath) {
        setSelectedField(null)
      }
    },
    [selectedField]
  )

  const handleDuplicateField = useCallback(
    (fieldPath: string) => {
      setSchema(prev => {
        const props = { ...(prev.properties || {}) }
        const sourceField = props[fieldPath]
        if (!sourceField) return prev

        let counter = 1
        let newFieldName = `${fieldPath}_copy`
        while (props[newFieldName]) {
          newFieldName = `${fieldPath}_copy_${counter}`
          counter++
        }

        const keys = Object.keys(props)
        const idx = keys.indexOf(fieldPath)

        const newField = JSON.parse(JSON.stringify(sourceField))
        if (newField.title) {
          newField.title = `${newField.title} (副本)`
        }

        const ordered: Record<string, any> = {}
        keys.slice(0, idx + 1).forEach(k => { ordered[k] = props[k] })
        ordered[newFieldName] = newField
        keys.slice(idx + 1).forEach(k => { ordered[k] = props[k] })

        return { ...prev, properties: ordered }
      })
    },
    []
  )

  const handleMoveField = useCallback(
    (fromPath: string, toPath: string, position: 'before' | 'after') => {
      setSchema(prev => {
        const props = { ...(prev.properties || {}) }
        const keys = Object.keys(props)
        const fromIdx = keys.indexOf(fromPath)
        const toIdx = keys.indexOf(toPath)

        if (fromIdx === -1 || toIdx === -1) return prev

        const value = props[fromPath]
        const newKeys = [...keys]
        newKeys.splice(fromIdx, 1)

        let insertIdx = newKeys.indexOf(toPath)
        if (position === 'after') insertIdx += 1

        newKeys.splice(insertIdx, 0, fromPath)

        const ordered: Record<string, any> = {}
        newKeys.forEach(k => { ordered[k] = props[k] })
        return { ...prev, properties: ordered }
      })
    },
    []
  )

  const handleClearAll = () => {
    setSchema(EMPTY_SCHEMA)
    setSelectedField(null)
    message.success('已清空所有控件')
  }

  const handleSave = async (publish = false) => {
    if (!formName || !formKey) {
      message.warning('请先设置表单名称和Key')
      setMetaModalOpen(true)
      return
    }

    setSaving(true)
    try {
      if (isEdit) {
        await formApi.definitionUpdate({
          id: Number(id),
          formName,
          formKey,
          description
        })
      } else {
        const saved = await formApi.definitionSave({
          formName,
          formKey,
          description
        })
        if (saved?.id) {
          navigate(`/process/form/design/${saved.id}`, { replace: true })
        }
      }

      await formApi.designSave({
        formId: isEdit ? Number(id) : undefined,
        formKey,
        schema
      })

      if (publish && id) {
        await formApi.definitionPublish(Number(id))
        message.success('表单已发布')
      } else {
        message.success('保存成功')
      }
    } catch (_e) {
      message.error('保存失败')
    } finally {
      setSaving(false)
    }
  }

  const handleExportSchema = () => {
    const jsonStr = JSON.stringify(schema, null, 2)
    const blob = new Blob([jsonStr], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${formKey || 'form'}_schema.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    message.success('已导出 Schema')
  }

  const handleImportSchema = () => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.json'
    input.onchange = (e) => {
      const file = (e.target as any).files?.[0]
      if (!file) return
      const reader = new FileReader()
      reader.onload = (ev) => {
        try {
          const data = JSON.parse(ev.target?.result as string)
          if (data && data.properties) {
            setSchema(data)
            message.success('Schema 导入成功')
          } else {
            message.error('无效的 Schema 文件')
          }
        } catch (_err) {
          message.error('文件解析失败')
        }
      }
      reader.readAsText(file)
    }
    input.click()
  }

  const handleCopySchema = async () => {
    try {
      await navigator.clipboard.writeText(JSON.stringify(schema, null, 2))
      message.success('Schema 已复制到剪贴板')
    } catch (_e) {
      const textarea = document.createElement('textarea')
      textarea.value = JSON.stringify(schema, null, 2)
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      message.success('Schema 已复制')
    }
  }

  return (
    <div style={{ height: 'calc(100vh - 64px - 24px - 24px - 48px)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/process/form/list')}>
            返回列表
          </Button>
          <Breadcrumb
            items={[
              { title: '流程管理' },
              { title: (
                <span style={{ cursor: 'pointer' }} onClick={() => navigate('/process/form/list')}>
                  表单定义
                </span>
              )},
              { title: (
                <Space size={4}>
                  {isEdit ? '编辑表单' : '新建表单'}
                  {formName && <Tag color="blue">{formName}</Tag>}
                  {formKey && <Tag color="default">{formKey}</Tag>}
                </Space>
              )}
            ]}
          />
        </Space>
        <Space>
          <Tooltip title="导入 Schema">
            <Button icon={<UploadOutlined />} onClick={handleImportSchema}>导入</Button>
          </Tooltip>
          <Tooltip title="导出 Schema">
            <Button icon={<DownloadOutlined />} onClick={handleExportSchema}>导出</Button>
          </Tooltip>
          <Tooltip title="查看 JSON">
            <Button icon={<CodeOutlined />} onClick={() => setJsonModalOpen(true)}>JSON</Button>
          </Tooltip>
          <Tooltip title="版本历史">
            <Button icon={<HistoryOutlined />} onClick={() => setHistoryModalOpen(true)}>历史</Button>
          </Tooltip>
          <Tooltip title="清空画布">
            <Popconfirm title="确认清空所有控件？" onConfirm={handleClearAll}>
              <Button icon={<ClearOutlined />} danger>清空</Button>
            </Popconfirm>
          </Tooltip>
          <Tooltip title="基本信息">
            <Button icon={<RollbackOutlined />} onClick={() => setMetaModalOpen(true)}>
              设置
            </Button>
          </Tooltip>
          <Button
            icon={<EyeOutlined />}
            type="default"
            onClick={() => setPreviewerOpen(true)}
            disabled={Object.keys(schema.properties || {}).length === 0}
          >
            预览
          </Button>
          <Button
            icon={<SaveOutlined />}
            type="primary"
            loading={saving}
            onClick={() => handleSave(false)}
          >
            保存
          </Button>
          {isEdit && (
            <Button
              type="primary"
              ghost
              loading={saving}
              onClick={() => handleSave(true)}
            >
              保存并发布
            </Button>
          )}
        </Space>
      </div>

      <Card
        bodyStyle={{ padding: 0, height: '100%' }}
        style={{ flex: 1, overflow: 'hidden' }}
        loading={loading}
      >
        <div style={{ height: '100%', display: 'flex' }}>
          <WidgetPalette />
          <FormCanvas
            schema={schema}
            selectedField={selectedField}
            onSelectField={setSelectedField}
            onAddWidget={handleAddWidget}
            onUpdateField={handleUpdateField}
            onRemoveField={handleRemoveField}
            onDuplicateField={handleDuplicateField}
            onMoveField={handleMoveField}
          />
          <WidgetPropertiesPanel
            schema={schema}
            selectedField={selectedField}
            onUpdateField={handleUpdateField}
          />
        </div>
      </Card>

      <FormPreviewer
        open={previewerOpen}
        onCancel={() => setPreviewerOpen(false)}
        schema={schema}
      />

      <Modal
        title="Schema JSON 预览"
        open={jsonModalOpen}
        onCancel={() => setJsonModalOpen(false)}
        width={800}
        footer={[
          <Button key="copy" icon={<CopyOutlined />} onClick={handleCopySchema}>复制</Button>,
          <Button key="close" onClick={() => setJsonModalOpen(false)}>关闭</Button>
        ]}
      >
        <pre
          style={{
            padding: 16,
            background: '#f6f8fa',
            borderRadius: 6,
            maxHeight: 500,
            overflow: 'auto',
            fontSize: 12,
            lineHeight: 1.6
          }}
        >
          {JSON.stringify(schema, null, 2)}
        </pre>
      </Modal>

      <Modal
        title="版本历史"
        open={historyModalOpen}
        onCancel={() => setHistoryModalOpen(false)}
        width={700}
        footer={[
          <Button key="close" onClick={() => setHistoryModalOpen(false)}>关闭</Button>
        ]}
      >
        <Card type="inner" title="当前版本">
          <Space direction="vertical" size={8} style={{ width: '100%' }}>
            <div><b>表单名称：</b>{formName || '-'}</div>
            <div><b>表单Key：</b>{formKey || '-'}</div>
            <div><b>字段数量：</b>{Object.keys(schema.properties || {}).length} 个</div>
            <div><b>描述：</b>{description || '-'}</div>
          </Space>
        </Card>
        <div style={{ marginTop: 16, textAlign: 'center', color: '#8c8c8c', padding: 20 }}>
          历史版本功能正在开发中...
        </div>
      </Modal>

      <Modal
        title={isEdit ? '表单基本信息' : '新建表单'}
        open={metaModalOpen}
        onCancel={() => {
          if (isEdit) setMetaModalOpen(false)
        }}
        onOk={() => {
          if (!formName || !formKey) {
            message.warning('请填写表单名称和Key')
            return
          }
          setMetaModalOpen(false)
        }}
        maskClosable={isEdit}
        closable={isEdit}
      >
        <Form layout="vertical">
          <Form.Item label="表单名称" required>
            <Input
              placeholder="请输入表单名称，如：报销单"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              disabled={isEdit}
            />
          </Form.Item>
          <Form.Item label="表单Key" required>
            <Input
              placeholder="请输入唯一标识，英文，如：expense_form"
              value={formKey}
              onChange={(e) => setFormKey(e.target.value)}
              disabled={isEdit}
            />
          </Form.Item>
          <Form.Item label="描述">
            <Input.TextArea
              placeholder="请输入表单描述"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export { FormDesign }
export default FormDesign
