import React, { useEffect, useState } from 'react'
import {
  Modal, Form, Select, Input, InputNumber, Switch, Button, Space,
  Card, Tag, Row, Col, message, Spin, Empty
} from 'antd'
import { FileTextOutlined, FileSearchOutlined } from '@ant-design/icons'
import type { WfRedocTemplateVO, WfRedocGenerateDTO, WfSealConfigVO } from '@/types/redoc'
import { redocApi, sealApi } from '@/api'

interface RedocGenerateModalProps {
  open: boolean
  instanceNo: string
  processKey?: string
  defaultTitle?: string
  onCancel: () => void
  onSuccess?: (generated: any) => void
}

const { TextArea } = Input

const RedocGenerateModal: React.FC<RedocGenerateModalProps> = ({
  open, instanceNo, processKey, defaultTitle, onCancel, onSuccess
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [templates, setTemplates] = useState<WfRedocTemplateVO[]>([])
  const [seals, setSeals] = useState<WfSealConfigVO[]>([])
  const [selectedTemplate, setSelectedTemplate] = useState<WfRedocTemplateVO | null>(null)
  const [placeholders, setPlaceholders] = useState<string[]>([])
  const [placeholderLoading, setPlaceholderLoading] = useState(false)

  useEffect(() => {
    if (open) {
      setLoading(true)
      Promise.all([
        redocApi.templateList(undefined, processKey),
        sealApi.list()
      ]).then(([tpls, sls]) => {
        setTemplates(tpls || [])
        setSeals(sls || [])
        if (tpls && tpls.length > 0 && !form.getFieldValue('templateId')) {
          form.setFieldsValue({ templateId: tpls[0].id, fileTitle: defaultTitle })
          handleTemplateChange(tpls[0].id)
        }
      }).finally(() => setLoading(false))
      form.setFieldsValue({
        instanceNo,
        fileTitle: defaultTitle,
        outputFormat: 2,
        sealEnabled: 1
      })
    }
  }, [open, instanceNo, processKey, defaultTitle])

  const handleTemplateChange = (templateId: number) => {
    const tpl = templates.find(t => t.id === templateId)
    setSelectedTemplate(tpl || null)
    if (tpl) {
      form.setFieldsValue({
        outputFormat: tpl.outputFormat || 2,
        sealEnabled: tpl.sealEnabled || 0,
        sealId: tpl.sealId,
        signatureEnabled: tpl.signatureEnabled || 0
      })
      setPlaceholderLoading(true)
      redocApi.templatePlaceholders(templateId)
        .then(p => setPlaceholders(p || []))
        .catch(() => setPlaceholders([]))
        .finally(() => setPlaceholderLoading(false))
    }
  }

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      const dto: WfRedocGenerateDTO = {
        instanceNo,
        templateId: values.templateId,
        fileTitle: values.fileTitle,
        approvalNo: values.approvalNo,
        fileNo: values.fileNo,
        outputFormat: values.outputFormat,
        sealId: values.sealId,
        sealEnabled: values.sealEnabled ? 1 : 0,
        signatureEnabled: values.signatureEnabled ? 1 : 0,
        placeholderValues: values.placeholderValues ? parsePlaceholderText(values.placeholderValues) : undefined
      }
      const generated = await redocApi.generate(dto)
      message.success('红头文件生成成功')
      onSuccess?.(generated)
      onCancel()
      form.resetFields()
    } catch (e: any) {
      if (e?.errorFields) return
      message.error(e?.message || '生成失败')
    } finally {
      setLoading(false)
    }
  }

  const parsePlaceholderText = (text: string): Record<string, string> => {
    const map: Record<string, string> = {}
    text.split('\n').forEach(line => {
      const idx = line.indexOf('=')
      if (idx > 0) {
        const k = line.substring(0, idx).trim()
        const v = line.substring(idx + 1).trim()
        if (k) map[k] = v
      }
    })
    return map
  }

  const categoryMap: Record<string, string> = {
    general: '通用',
    finance: '财务',
    personnel: '人事',
    official: '行政'
  }

  return (
    <Modal
      title="生成红头文件"
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={loading}
      width={760}
      destroyOnClose
      maskClosable={false}
      okText="生成"
    >
      <Spin spinning={loading}>
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="instanceNo" hidden><Input /></Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="红头模板" name="templateId" rules={[{ required: true, message: '请选择模板' }]}>
                <Select
                  placeholder="请选择红头模板"
                  showSearch
                  optionFilterProp="label"
                  onChange={handleTemplateChange}
                  options={templates.map(t => ({
                    value: t.id,
                    label: (
                      <Space>
                        <FileTextOutlined />
                        <span>{t.templateName}</span>
                        {t.category && <Tag color="blue">{categoryMap[t.category] || t.category}</Tag>}
                      </Space>
                    )
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="文件标题" name="fileTitle" rules={[{ required: true, message: '请输入文件标题' }]}>
                <Input placeholder="如：关于XXX的批复" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="审批编号/文号" name="approvalNo">
                <Input placeholder="如：XX发〔2025〕1号" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="发文号" name="fileNo">
                <Input placeholder="可选" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="输出格式" name="outputFormat">
                <Select options={[
                  { value: 1, label: '仅WORD' },
                  { value: 2, label: '仅PDF（推荐）' },
                  { value: 3, label: 'WORD + PDF' }
                ]} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="印章" name="sealId">
                <Select
                  placeholder="选择印章（默认用模板印章）"
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={seals.map(s => ({
                    value: s.id,
                    label: s.sealName
                  }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item label="加盖印章" name="sealEnabled" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label="国密数字签名" name="signatureEnabled" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>

          {selectedTemplate && (
            <Card
              size="small"
              title={<span><FileSearchOutlined /> 模板占位符（已检测{placeholders.length}个）</span>}
              style={{ marginBottom: 16 }}
              extra={<Tag color="blue">{selectedTemplate.templateCode}</Tag>}
            >
              <Spin spinning={placeholderLoading}>
                {placeholders.length === 0 ? (
                  <Empty description="未检测到占位符" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ margin: 0 }} />
                ) : (
                  <Space wrap>
                    {placeholders.map(p => (
                      <Tag key={p} color="purple">{'{' + p + '}'}</Tag>
                    ))}
                  </Space>
                )}
              </Spin>
              <Form.Item label="自定义占位符值" name="placeholderValues" style={{ marginTop: 12, marginBottom: 0 }}>
                <TextArea
                  rows={3}
                  placeholder={'每行一个，格式：占位符名=值\n例如：\ndeptName=技术部\napplicant=张三'}
                />
              </Form.Item>
            </Card>
          )}
        </Form>
      </Spin>
    </Modal>
  )
}

export default RedocGenerateModal
