import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card,
  Steps,
  Button,
  Space,
  Tree,
  Input,
  Upload,
  Select,
  message,
  Empty,
  Tag,
  Descriptions,
  List,
  Avatar,
  Typography,
  Tooltip,
  Divider,
  Modal,
  Row,
  Col,
  Spin
} from 'antd'
import {
  ArrowLeftOutlined,
  FileTextOutlined,
  FolderOutlined,
  PlusOutlined,
  SaveOutlined,
  SendOutlined,
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  ClockCircleOutlined,
  UserOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import type { DataNode } from 'antd/es/tree'
import { approvalApi, formApi } from '@/api'
import type { StartableProcessVO, ProcessCategoryVO, DraftVO } from '@/types/approval'
import type { FormilySchema } from '@/types/form'
import FormRenderer, { type FormRendererRef } from '@/components/FormRenderer'
import dayjs from 'dayjs'

const { Step } = Steps
const { Text, Paragraph } = Typography
const { TextArea } = Input

const buildFallbackSchema = (): FormilySchema => ({
  type: 'object',
  properties: {
    title: {
      type: 'string',
      title: '申请标题',
      required: true,
      default: '',
      'x-decorator': 'FormItem',
      'x-component': 'Input',
      'x-component-props': { placeholder: '请输入申请标题', maxLength: 200, showCount: true }
    },
    reason: {
      type: 'string',
      title: '申请事由',
      required: true,
      default: '',
      'x-decorator': 'FormItem',
      'x-component': 'TextArea',
      'x-component-props': { placeholder: '请详细说明申请理由', rows: 4, maxLength: 1000, showCount: true }
    },
    amount: {
      type: 'number',
      title: '金额（元）',
      default: undefined,
      'x-decorator': 'FormItem',
      'x-component': 'NumberPicker',
      'x-component-props': { placeholder: '请输入金额', min: 0, precision: 2, style: { width: '100%' } }
    },
    startDate: {
      type: 'string',
      title: '开始日期',
      default: '',
      'x-decorator': 'FormItem',
      'x-component': 'DatePicker',
      'x-component-props': { style: { width: '100%' } }
    },
    endDate: {
      type: 'string',
      title: '结束日期',
      default: '',
      'x-decorator': 'FormItem',
      'x-component': 'DatePicker',
      'x-component-props': { style: { width: '100%' } }
    },
    remark: {
      type: 'string',
      title: '备注',
      default: '',
      'x-decorator': 'FormItem',
      'x-component': 'TextArea',
      'x-component-props': { placeholder: '其他需要说明的事项', rows: 3, maxLength: 500 }
    }
  }
})

const mockCategoryTree: ProcessCategoryVO[] = [
  {
    id: 1,
    parentId: 0,
    categoryName: '行政办公',
    children: [
      { id: 11, parentId: 1, categoryName: '请假审批' },
      { id: 12, parentId: 1, categoryName: '加班申请' },
      { id: 13, parentId: 1, categoryName: '用印申请' }
    ]
  },
  {
    id: 2,
    parentId: 0,
    categoryName: '财务报销',
    children: [
      { id: 21, parentId: 2, categoryName: '日常报销' },
      { id: 22, parentId: 2, categoryName: '差旅费报销' },
      { id: 23, parentId: 2, categoryName: '借款申请' }
    ]
  },
  {
    id: 3,
    parentId: 0,
    categoryName: '采购合同',
    children: [
      { id: 31, parentId: 3, categoryName: '采购申请' },
      { id: 32, parentId: 3, categoryName: '合同审批' }
    ]
  },
  {
    id: 4,
    parentId: 0,
    categoryName: '人事管理',
    children: [
      { id: 41, parentId: 4, categoryName: '入职办理' },
      { id: 42, parentId: 4, categoryName: '离职申请' },
      { id: 43, parentId: 4, categoryName: '调岗申请' }
    ]
  }
]

const icons = ['📝', '💰', '📋', '📄', '🖋️', '🏢', '🚗', '🎯', '⭐', '📊']

const mockProcesses: StartableProcessVO[] = [
  {
    id: 101, processName: '年假申请', processKey: 'leave_annual',
    categoryId: 11, categoryName: '请假审批',
    icon: '📝', description: '员工年度休假申请流程',
    formId: 1, formVersion: 1
  },
  {
    id: 102, processName: '事假申请', processKey: 'leave_personal',
    categoryId: 11, categoryName: '请假审批',
    icon: '📝', description: '员工因私请假申请',
    formId: 2, formVersion: 1
  },
  {
    id: 103, processName: '病假申请', processKey: 'leave_sick',
    categoryId: 11, categoryName: '请假审批',
    icon: '📝', description: '员工病假申请（需附诊断证明）',
    formId: 3, formVersion: 1
  },
  {
    id: 201, processName: '日常费用报销', processKey: 'expense_daily',
    categoryId: 21, categoryName: '日常报销',
    icon: '💰', description: '日常办公费用报销',
    formId: 4, formVersion: 1
  },
  {
    id: 202, processName: '差旅报销', processKey: 'expense_travel',
    categoryId: 22, categoryName: '差旅费报销',
    icon: '🚗', description: '出差差旅费用报销',
    formId: 5, formVersion: 1
  },
  {
    id: 301, processName: '办公用品采购', processKey: 'purchase_supplies',
    categoryId: 31, categoryName: '采购申请',
    icon: '📋', description: '办公用品采购申请',
    formId: 6, formVersion: 1
  },
  {
    id: 302, processName: '设备采购申请', processKey: 'purchase_equipment',
    categoryId: 31, categoryName: '采购申请',
    icon: '📊', description: 'IT设备及固定资产采购',
    formId: 7, formVersion: 1
  },
  {
    id: 303, processName: '采购合同审批', processKey: 'contract_purchase',
    categoryId: 32, categoryName: '合同审批',
    icon: '📄', description: '采购类合同审批流程',
    formId: 8, formVersion: 1
  },
  {
    id: 401, processName: '员工入职审批', processKey: 'hr_onboard',
    categoryId: 41, categoryName: '入职办理',
    icon: '⭐', description: '新员工入职流程审批',
    formId: 9, formVersion: 1
  },
  {
    id: 402, processName: '离职申请', processKey: 'hr_resign',
    categoryId: 42, categoryName: '离职申请',
    icon: '🖋️', description: '员工离职申请及交接审批',
    formId: 10, formVersion: 1
  }
]

const mockDrafts: DraftVO[] = [
  {
    id: 9001, processKey: 'leave_annual', processName: '年假申请',
    title: '2024年7月年假申请',
    formData: { leaveDays: 5, startDate: '2024-07-01', reason: '家庭出游' },
    updateTime: dayjs().subtract(2, 'hour').format('YYYY-MM-DD HH:mm:ss')
  },
  {
    id: 9002, processKey: 'expense_travel', processName: '差旅报销',
    title: '上海出差费用报销',
    formData: { totalAmount: 3500, city: '上海' },
    updateTime: dayjs().subtract(1, 'day').format('YYYY-MM-DD HH:mm:ss')
  }
]

const categoryToTreeData = (categories: ProcessCategoryVO[]): DataNode[] => {
  return categories.map(cat => ({
    key: String(cat.id),
    title: cat.categoryName,
    icon: <FolderOutlined />,
    children: cat.children ? categoryToTreeData(cat.children) : undefined
  }))
}

const mockCcUsers = [
  { value: 1, label: '张三 (技术部)' },
  { value: 2, label: '李四 (产品部)' },
  { value: 3, label: '王五 (市场部)' },
  { value: 4, label: '赵六 (财务部)' },
  { value: 5, label: '钱七 (人事部)' },
  { value: 6, label: '孙八 (运营部)' },
  { value: 7, label: '周九 (总监办)' }
]

const ApplyForm: React.FC = () => {
  const navigate = useNavigate()
  const [currentStep, setCurrentStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [schemaLoading, setSchemaLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)

  const [categoryTree] = useState<DataNode[]>(categoryToTreeData(mockCategoryTree))
  const [selectedCategory, setSelectedCategory] = useState<string[]>([])
  const [processList, setProcessList] = useState<StartableProcessVO[]>(mockProcesses)
  const [selectedProcess, setSelectedProcess] = useState<StartableProcessVO | null>(null)

  const [formSchema, setFormSchema] = useState<FormilySchema | null>(null)
  const formRendererRef = useRef<FormRendererRef>(null)

  const [formValues, setFormValues] = useState<Record<string, any>>({})
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [ccUserIds, setCcUserIds] = useState<number[]>([])

  const [drafts, setDrafts] = useState<DraftVO[]>(mockDrafts)
  const [showDrafts, setShowDrafts] = useState(true)
  const [draftId, setDraftId] = useState<number | null>(null)
  const [savedState, setSavedState] = useState<{ saved: boolean; time?: string }>({ saved: false })

  const autoSaveTimer = useRef<number | null>(null)
  const debounceTimer = useRef<number | null>(null)

  const loadSchema = useCallback(async (formId: number, version?: number) => {
    setSchemaLoading(true)
    try {
      let schema: FormilySchema | null = null
      try {
        schema = await formApi.schemaGet(formId, version)
      } catch (_) {
        schema = null
      }
      if (!schema || !schema.properties) {
        schema = buildFallbackSchema()
      }
      setFormSchema(schema)
    } catch (_) {
      setFormSchema(buildFallbackSchema())
    } finally {
      setSchemaLoading(false)
    }
  }, [])

  const loadProcesses = useCallback(async () => {
    setLoading(true)
    try {
      let list = mockProcesses
      if (selectedCategory.length > 0) {
        const catId = Number(selectedCategory[selectedCategory.length - 1])
        list = list.filter(p => p.categoryId === catId || p.categoryId.toString().startsWith(selectedCategory[0]))
      }
      setProcessList(list)
    } catch (err: any) {
      message.error(err?.message || '加载流程列表失败')
    } finally {
      setLoading(false)
    }
  }, [selectedCategory])

  useEffect(() => {
    loadProcesses()
  }, [loadProcesses])

  const autoSaveDraft = useCallback(async () => {
    if (!selectedProcess || Object.keys(formValues).length === 0) return
    try {
      const data = {
        processKey: selectedProcess.processKey,
        processName: selectedProcess.processName,
        title: formValues.title || selectedProcess.processName,
        formData: formValues
      }
      // const res = await formApi.draftSave(data)
      const newDraft: DraftVO = {
        id: draftId || Date.now(),
        ...data,
        updateTime: dayjs().format('YYYY-MM-DD HH:mm:ss')
      }
      setDraftId(newDraft.id)
      setSavedState({ saved: true, time: dayjs().format('HH:mm:ss') })
      setTimeout(() => setSavedState(prev => ({ ...prev, saved: false })), 2000)
    } catch (_) {}
  }, [selectedProcess, formValues, draftId])

  useEffect(() => {
    const interval = setInterval(() => {
      autoSaveDraft()
    }, 30000)
    return () => clearInterval(interval)
  }, [autoSaveDraft])

  const triggerDebouncedSave = () => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current)
    debounceTimer.current = window.setTimeout(() => {
      autoSaveDraft()
    }, 2000)
  }

  useEffect(() => {
    return () => {
      if (autoSaveTimer.current) clearInterval(autoSaveTimer.current)
      if (debounceTimer.current) clearTimeout(debounceTimer.current)
    }
  }, [])

  const handleProcessSelect = async (process: StartableProcessVO) => {
    setSelectedProcess(process)
    setFormValues({})
    setFormSchema(null)
    setFileList([])
    setCcUserIds([])
    setDraftId(null)
    await loadSchema(process.formId, process.formVersion)
  }

  const handleNextToForm = () => {
    if (!selectedProcess) {
      message.warning('请先选择一个流程')
      return
    }
    setCurrentStep(1)
    setTimeout(() => {
      if (formRendererRef.current && formSchema) {
        const defaultTitle = selectedProcess.processName + '-' + dayjs().format('YYYY-MM-DD')
        formRendererRef.current.setFieldValue('title', defaultTitle)
        setFormValues(prev => ({ ...prev, title: defaultTitle }))
      }
    }, 50)
  }

  const handleBackToProcess = () => {
    setCurrentStep(0)
  }

  const handleNextToConfirm = async () => {
    if (!formRendererRef.current) return
    try {
      const values = await formRendererRef.current.validate()
      if (values && !values.title && selectedProcess) {
        values.title = selectedProcess.processName + '-' + dayjs().format('YYYY-MM-DD')
      }
      setFormValues(values || {})
      triggerDebouncedSave()
      setCurrentStep(2)
    } catch (_) {}
  }

  const handleBackToForm = () => {
    setCurrentStep(1)
  }

  const handleFormValuesChange = (values: Record<string, any>) => {
    setFormValues(values)
    triggerDebouncedSave()
  }

  const handleSubmit = async () => {
    if (!selectedProcess || !formRendererRef.current) return
    try {
      setSubmitLoading(true)
      const values = await formRendererRef.current.submit()
      const submitData = {
        processKey: selectedProcess.processKey,
        title: values.title || selectedProcess.processName,
        formData: values,
        ccUserIds,
        attachmentIds: fileList.map(f => f.uid as number).filter(Boolean),
        draftId: draftId || undefined
      }
      // const res = await approvalApi.start(submitData)
      message.success('审批发起成功！')
      navigate('/approval/my-apply')
    } catch (err: any) {
      message.error(err?.message || '提交失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  const handleSaveDraft = async () => {
    if (!selectedProcess) return
    try {
      setSubmitLoading(true)
      autoSaveDraft()
      message.success('草稿已保存')
    } catch (err: any) {
      message.error(err?.message || '保存失败')
    } finally {
      setSubmitLoading(false)
    }
  }

  const handleContinueDraft = async (draft: DraftVO) => {
    const process = mockProcesses.find(p => p.processKey === draft.processKey)
    if (process) {
      await handleProcessSelect(process)
      setTimeout(() => {
        if (formRendererRef.current) {
          Object.entries(draft.formData || {}).forEach(([k, v]) => {
            formRendererRef.current?.setFieldValue(k, v)
          })
        }
        setFormValues({ ...draft.formData })
      }, 80)
      setDraftId(draft.id)
      setCurrentStep(1)
      setShowDrafts(false)
    }
  }

  const handleDeleteDraft = (draft: DraftVO) => {
    Modal.confirm({
      title: '删除草稿',
      icon: <ExclamationCircleOutlined />,
      content: `确定删除草稿【${draft.title}】吗？`,
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          // await formApi.draftRemove(draft.id)
          setDrafts(prev => prev.filter(d => d.id !== draft.id))
          message.success('已删除')
        } catch (err: any) {
          message.error(err?.message || '删除失败')
        }
      }
    })
  }

  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    setFileList(prev => [...prev, file as UploadFile])
    return false
  }

  const steps = [
    { title: '选择流程', icon: <FileTextOutlined /> },
    { title: '填写表单', icon: <EditOutlined /> },
    { title: '确认提交', icon: <SendOutlined /> }
  ]

  return (
    <div style={{ padding: 16 }}>
      <Card style={{ borderRadius: 8, marginBottom: 16 }}>
        <Space style={{ marginBottom: 24 }}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
            返回
          </Button>
          <h2 style={{ margin: 0 }}>发起审批</h2>
          {savedState.saved && (
            <Tag color="success" icon={<ClockCircleOutlined />}>
              已自动保存于 {savedState.time}
            </Tag>
          )}
        </Space>
        <Steps current={currentStep} items={steps} />
      </Card>

      {currentStep === 0 && (
        <Row gutter={16}>
          <Col xs={24} md={6}>
            <Card
              title={
                <Space>
                  <FolderOutlined />
                  <span>流程分类</span>
                </Space>
              }
              style={{ borderRadius: 8, height: 'calc(100vh - 280px)', overflow: 'auto' }}
              bodyStyle={{ padding: 0 }}
            >
              <Tree
                showLine
                showIcon
                treeData={[{ key: 'all', title: '全部流程', icon: <SafetyCertificateOutlined /> }, ...categoryTree]}
                selectedKeys={selectedCategory}
                onSelect={(keys) => {
                  if (keys[0] === 'all') {
                    setSelectedCategory([])
                  } else {
                    setSelectedCategory(keys as string[])
                  }
                }}
                defaultExpandAll
              />
            </Card>
          </Col>

          <Col xs={24} md={18}>
            {showDrafts && drafts.length > 0 && (
              <Card
                style={{ borderRadius: 8, marginBottom: 16 }}
                title={
                  <Space>
                    <SaveOutlined />
                    <span>草稿箱（{drafts.length}）</span>
                    <Button type="text" size="small" onClick={() => setShowDrafts(false)}>
                      收起
                    </Button>
                  </Space>
                }
              >
                <List
                  grid={{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4 }}
                  dataSource={drafts}
                  renderItem={(draft) => (
                    <List.Item>
                      <Card
                        size="small"
                        hoverable
                        actions={[
                          <Button
                            type="link"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => handleContinueDraft(draft)}
                          >
                            继续编辑
                          </Button>,
                          <Button
                            type="link"
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={() => handleDeleteDraft(draft)}
                          >
                            删除
                          </Button>
                        ]}
                      >
                        <Card.Meta
                          title={<Text strong ellipsis>{draft.title}</Text>}
                          description={
                            <Space direction="vertical" size={0} style={{ marginTop: 4 }}>
                              <Tag color="blue" style={{ margin: 0 }}>{draft.processName}</Tag>
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                {dayjs(draft.updateTime).fromNow()}
                              </Text>
                            </Space>
                          }
                        />
                      </Card>
                    </List.Item>
                  )}
                />
              </Card>
            )}

            <Card
              style={{ borderRadius: 8 }}
              title={
                <Space>
                  <span>可选流程</span>
                  <Tag color="blue">{processList.length} 个</Tag>
                </Space>
              }
              bodyStyle={{ padding: 16 }}
              loading={loading}
            >
              {processList.length === 0 ? (
                <Empty description="该分类下暂无可用流程" />
              ) : (
                <Row gutter={[16, 16]}>
                  {processList.map((process, index) => (
                    <Col xs={24} sm={12} md={8} lg={8} xl={6} key={process.id}>
                      <Card
                        hoverable
                        style={{
                          borderRadius: 8,
                          cursor: 'pointer',
                          border: selectedProcess?.id === process.id
                            ? '2px solid #1890ff'
                            : '1px solid #f0f0f0',
                          transition: 'all 0.2s'
                        }}
                        bodyStyle={{ padding: 16, minHeight: 120 }}
                        onClick={() => handleProcessSelect(process)}
                      >
                        <Space direction="vertical" size={8} style={{ width: '100%' }}>
                          <Space size={8}>
                            <span style={{ fontSize: 28 }}>
                              {process.icon || icons[index % icons.length]}
                            </span>
                            <Text strong style={{ fontSize: 15 }}>
                              {process.processName}
                            </Text>
                          </Space>
                          <Text type="secondary" style={{ fontSize: 12, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                            {process.description || '点击发起该审批流程'}
                          </Text>
                          <Space size={4} wrap>
                            <Tag color="blue" style={{ margin: 0, fontSize: 11 }}>
                              {process.categoryName}
                            </Tag>
                          </Space>
                        </Space>
                      </Card>
                    </Col>
                  ))}
                </Row>
              )}

              <Divider />
              <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  type="primary"
                  size="large"
                  icon={<PlusOutlined />}
                  onClick={handleNextToForm}
                  disabled={!selectedProcess}
                >
                  下一步：填写表单
                </Button>
              </Space>
            </Card>
          </Col>
        </Row>
      )}

      {currentStep === 1 && selectedProcess && (
        <Card
          style={{ borderRadius: 8 }}
          title={
            <Space>
              <span style={{ fontSize: 22 }}>{selectedProcess.icon}</span>
              <Space direction="vertical" size={0}>
                <Text strong style={{ fontSize: 16 }}>{selectedProcess.processName}</Text>
                <Tag color="blue" style={{ margin: 0 }}>{selectedProcess.categoryName}</Tag>
              </Space>
            </Space>
          }
          extra={
            <Space>
              <Button icon={<ArrowLeftOutlined />} onClick={handleBackToProcess}>
                上一步
              </Button>
              <Button
                icon={<SaveOutlined />}
                onClick={handleSaveDraft}
                loading={submitLoading}
              >
                保存草稿
              </Button>
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleNextToConfirm}
              >
                下一步：确认提交
              </Button>
            </Space>
          }
        >
          <Row gutter={24}>
            <Col xs={24} md={16}>
              <Card
                type="inner"
                title="填写申请信息"
                style={{ borderRadius: 8 }}
              >
                <Spin spinning={schemaLoading} tip="加载表单中...">
                  {formSchema ? (
                    <>
                      <FormRenderer
                        ref={formRendererRef}
                        schema={formSchema}
                        formData={formValues}
                        mode="edit"
                        onChange={handleFormValuesChange}
                        onSubmit={(values) => setFormValues(values)}
                      />
                      <div style={{ marginTop: 16 }}>
                        <Divider plain style={{ fontSize: 12, margin: '8px 0 12px' }}>附件上传（可选）</Divider>
                        <Upload
                          fileList={fileList}
                          beforeUpload={beforeUpload}
                          onRemove={(file) => setFileList(prev => prev.filter(f => f.uid !== file.uid))}
                          multiple
                        >
                          <Button icon={<PlusOutlined />}>点击上传附件</Button>
                        </Upload>
                      </div>
                    </>
                  ) : (
                    <Empty description="未加载到表单定义" />
                  )}
                </Spin>
              </Card>
            </Col>

            <Col xs={24} md={8}>
              <Card
                type="inner"
                title="流程说明"
                style={{ borderRadius: 8, marginBottom: 16, position: 'sticky', top: 16 }}
              >
                <Paragraph type="secondary" style={{ marginBottom: 16 }}>
                  {selectedProcess.description || '请填写完整的申请信息后提交审批。'}
                </Paragraph>
                <Descriptions column={1} size="small" bordered>
                  <Descriptions.Item label="表单版本">
                    V{selectedProcess.formVersion}.0
                  </Descriptions.Item>
                  <Descriptions.Item label="所属分类">
                    {selectedProcess.categoryName}
                  </Descriptions.Item>
                  <Descriptions.Item label="抄送人员">
                    <Form.Item style={{ margin: 0, marginTop: 8 }}>
                      <Select
                        mode="multiple"
                        placeholder="请选择抄送人（可选）"
                        options={mockCcUsers}
                        value={ccUserIds}
                        onChange={setCcUserIds}
                        allowClear
                        size="small"
                        maxTagCount="responsive"
                      />
                    </Form.Item>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
          </Row>
        </Card>
      )}

      {currentStep === 2 && selectedProcess && (
        <Card
          style={{ borderRadius: 8 }}
          title={
            <Space>
              <span style={{ fontSize: 22 }}>{selectedProcess.icon}</span>
              <Space direction="vertical" size={0}>
                <Text strong style={{ fontSize: 16 }}>
                  确认提交 - {selectedProcess.processName}
                </Text>
                <Tag color="blue" style={{ margin: 0 }}>{selectedProcess.categoryName}</Tag>
              </Space>
            </Space>
          }
          extra={
            <Space>
              <Button icon={<ArrowLeftOutlined />} onClick={handleBackToForm}>
                上一步
              </Button>
              <Button
                icon={<SaveOutlined />}
                onClick={handleSaveDraft}
              >
                保存草稿
              </Button>
              <Button
                type="primary"
                size="large"
                icon={<SendOutlined />}
                onClick={handleSubmit}
                loading={submitLoading}
              >
                提交审批
              </Button>
            </Space>
          }
        >
          <Row gutter={24}>
            <Col xs={24} md={16}>
              <Card
                type="inner"
                title="申请信息预览"
                style={{ borderRadius: 8, marginBottom: 16 }}
              >
                {formSchema ? (
                  <>
                    <div style={{ border: '1px solid #f5f5f5', padding: 16, borderRadius: 6, background: '#fafafa' }}>
                      <FormRenderer
                        schema={formSchema}
                        formData={formValues}
                        mode="view"
                      />
                    </div>
                    <Divider plain style={{ fontSize: 12, margin: '16px 0 12px' }}>附件</Divider>
                    {fileList.length > 0 ? (
                      <Space direction="vertical">
                        {fileList.map(f => (
                          <span key={f.uid}>📎 {f.name}</span>
                        ))}
                      </Space>
                    ) : (
                      <Text type="secondary">无附件</Text>
                    )}
                  </>
                ) : (
                  <Descriptions column={1} bordered size="middle">
                    <Descriptions.Item label="申请标题">
                      {formValues.title || <Text type="secondary">未填写</Text>}
                    </Descriptions.Item>
                  </Descriptions>
                )}
              </Card>

              <Card
                type="inner"
                title="审批流程预览"
                style={{ borderRadius: 8 }}
              >
                <Steps
                  direction="vertical"
                  size="small"
                  current={-1}
                  items={[
                    { title: '发起申请', description: formValues.title || selectedProcess.processName, status: 'finish' },
                    { title: selectedProcess.categoryName.includes('请假') ? '部门主管审批' : '直属领导审批', status: 'process' },
                    { title: selectedProcess.processName.includes('报销') || selectedProcess.processName.includes('采购') ? '财务审核' : 'HR审核', status: 'wait' },
                    { title: '审批完成', status: 'wait' }
                  ]}
                />
              </Card>
            </Col>

            <Col xs={24} md={8}>
              <Card
                type="inner"
                title="抄送人"
                style={{ borderRadius: 8, marginBottom: 16, position: 'sticky', top: 16 }}
              >
                {ccUserIds.length > 0 ? (
                  <List
                    size="small"
                    dataSource={mockCcUsers.filter(u => ccUserIds.includes(u.value))}
                    renderItem={(user) => (
                      <List.Item>
                        <Space>
                          <Avatar size={24} icon={<UserOutlined />} />
                          <Text>{user.label}</Text>
                        </Space>
                      </List.Item>
                    )}
                  />
                ) : (
                  <Text type="secondary">未选择抄送人</Text>
                )}
              </Card>

              <Tooltip title="提交后流程将自动发送给审批人处理">
                <Alert
                  message="确认提交后，流程将无法修改"
                  type="warning"
                  showIcon
                  style={{ borderRadius: 8 }}
                />
              </Tooltip>
            </Col>
          </Row>
        </Card>
      )}
    </div>
  )
}

export default ApplyForm
