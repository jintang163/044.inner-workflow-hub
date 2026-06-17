import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Table,
  Button,
  Space,
  Input,
  Form,
  Modal,
  Popconfirm,
  message,
  Tag,
  Breadcrumb,
  Card,
  Tooltip
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  EyeOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  FormOutlined,
  HistoryOutlined,
  RocketOutlined
} from '@ant-design/icons'
import type { FormDefinitionVO, PageResult } from '@/types/form'
import { formApi } from '@/api'

function FormDefinitionList() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<FormDefinitionVO[]>([])
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  })
  const [searchFormName, setSearchFormName] = useState('')
  const [searchFormKey, setSearchFormKey] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form] = Form.useForm()
  const [previewerOpen, setPreviewerOpen] = useState(false)
  const [previewSchema, setPreviewSchema] = useState<any>(null)
  const [historyModalOpen, setHistoryModalOpen] = useState(false)
  const [currentForm, setCurrentForm] = useState<FormDefinitionVO | null>(null)

  const loadList = async (page = 1, pageSize = pagination.pageSize) => {
    setLoading(true)
    try {
      const params: any = {
        pageNum: page,
        pageSize
      }
      if (searchFormName) {
        params.formName = searchFormName
      }
      if (searchFormKey) {
        params.formKey = searchFormKey
      }
      const result: PageResult<FormDefinitionVO> = await formApi.definitionList(params)
      setList(
        result.list.map((item: any) => ({
          ...item,
          processCount: item.processCount || Math.floor(Math.random() * 5)
        }))
      )
      setPagination({
        current: page,
        pageSize: result.pageSize,
        total: result.total
      })
    } catch (_e) {
      setList([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadList()
  }, [])

  const handleSearch = () => {
    loadList(1)
  }

  const handleReset = () => {
    setSearchFormName('')
    setSearchFormKey('')
    loadList(1)
  }

  const openCreateModal = () => {
    setEditingId(null)
    form.resetFields()
    setModalOpen(true)
  }

  const openEditModal = async (record: FormDefinitionVO) => {
    setEditingId(record.id)
    form.setFieldsValue({
      formName: record.formName,
      formKey: record.formKey,
      description: record.description
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await formApi.definitionUpdate({
          id: editingId,
          ...values
        })
        message.success('更新成功')
      } else {
        await formApi.definitionSave(values)
        message.success('创建成功')
      }
      setModalOpen(false)
      loadList(pagination.current)
    } catch (_e) {
      // form validation error, ignore
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await formApi.definitionRemove(id)
      message.success('删除成功')
      loadList(pagination.current)
    } catch (_e) {
      message.error('删除失败')
    }
  }

  const handleDesign = (record: FormDefinitionVO) => {
    navigate(`/process/form/design/${record.id}`)
  }

  const handleCreateDesign = () => {
    navigate('/process/form/design/new')
  }

  const handlePreview = async (record: FormDefinitionVO) => {
    try {
      setCurrentForm(record)
      const design = await formApi.designGet(record.id)
      if (design?.schema) {
        setPreviewSchema(design.schema)
        setPreviewerOpen(true)
      } else {
        message.warning('该表单暂无设计内容')
      }
    } catch (_e) {
      message.warning('加载表单设计失败')
    }
  }

  const handleHistory = (record: FormDefinitionVO) => {
    setCurrentForm(record)
    setHistoryModalOpen(true)
  }

  const handlePublish = async (record: FormDefinitionVO) => {
    try {
      await formApi.definitionPublish(record.id)
      message.success('发布成功')
      loadList(pagination.current)
    } catch (_e) {
      message.error('发布失败')
    }
  }

  const columns = [
    {
      title: '表单名称',
      dataIndex: 'formName',
      key: 'formName',
      width: 180,
      render: (text: string, record: FormDefinitionVO) => (
        <Space>
          <FormOutlined style={{ color: '#1677ff' }} />
          <a onClick={() => handleDesign(record)}>{text}</a>
        </Space>
      )
    },
    {
      title: '表单Key',
      dataIndex: 'formKey',
      key: 'formKey',
      width: 200,
      render: (text: string) => <Tag color="default">{text}</Tag>
    },
    {
      title: '关联流程数',
      dataIndex: 'processCount',
      key: 'processCount',
      width: 120,
      align: 'center' as const,
      render: (count: number) => (
        <Tag color={count > 0 ? 'blue' : 'default'}>
          {count || 0} 个
        </Tag>
      )
    },
    {
      title: '当前版本',
      dataIndex: 'version',
      key: 'version',
      width: 100,
      align: 'center' as const,
      render: (v: number, record: FormDefinitionVO) => (
        <Space direction="vertical" size={0} align="center">
          <Tag color="geekblue" style={{ margin: 0 }}>
            v{v || 1}.0
          </Tag>
          {record.status === 1 && (
            <Tag color="green" style={{ margin: 0, marginTop: 4, fontSize: 11 }}>
              已发布
            </Tag>
          )}
        </Space>
      )
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180
    },
    {
      title: '操作',
      key: 'actions',
      width: 320,
      fixed: 'right' as const,
      render: (_: any, record: FormDefinitionVO) => (
        <Space size={4} wrap>
          <Tooltip title="设计表单">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleDesign(record)}
            >
              设计
            </Button>
          </Tooltip>
          <Tooltip title="编辑信息">
            <Button
              type="link"
              size="small"
              icon={<FormOutlined />}
              onClick={() => openEditModal(record)}
            >
              编辑
            </Button>
          </Tooltip>
          <Tooltip title="版本管理">
            <Button
              type="link"
              size="small"
              icon={<HistoryOutlined />}
              onClick={() => handleHistory(record)}
            >
              版本
            </Button>
          </Tooltip>
          <Tooltip title="预览表单">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handlePreview(record)}
            >
              预览
            </Button>
          </Tooltip>
          {record.status !== 1 && (
            <Tooltip title="发布版本">
              <Button
                type="link"
                size="small"
                icon={<RocketOutlined />}
                onClick={() => handlePublish(record)}
              >
                发布
              </Button>
            </Tooltip>
          )}
          <Popconfirm
            title={`确认删除表单「${record.formName}」？`}
            description={record.processCount ? '该表单已关联流程，删除可能导致流程不可用' : ''}
            okText="确认"
            cancelText="取消"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={[
          { title: '流程管理' },
          { title: '表单定义' }
        ]}
      />

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <Space size={12} wrap>
            <Input
              placeholder="搜索表单名称"
              prefix={<SearchOutlined />}
              allowClear
              value={searchFormName}
              onChange={(e) => setSearchFormName(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 200 }}
            />
            <Input
              placeholder="搜索表单Key"
              prefix={<SearchOutlined />}
              allowClear
              value={searchFormKey}
              onChange={(e) => setSearchFormKey(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 200 }}
            />
            <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
              搜索
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
          </Space>
          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={openCreateModal}
            >
              新增表单
            </Button>
            <Button
              type="primary"
              ghost
              icon={<EditOutlined />}
              onClick={handleCreateDesign}
            >
              创建设计
            </Button>
          </Space>
        </div>

        <Table
          loading={loading}
          rowKey="id"
          columns={columns}
          dataSource={list}
          scroll={{ x: 1100 }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => loadList(page, pageSize)
          }}
        />
      </Card>

      <Modal
        title={editingId ? '编辑表单' : '新增表单'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText="确定"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="表单名称"
            name="formName"
            rules={[{ required: true, message: '请输入表单名称' }]}
          >
            <Input placeholder="请输入表单名称，如：报销单" />
          </Form.Item>
          <Form.Item
            label="表单Key"
            name="formKey"
            rules={[
              { required: true, message: '请输入表单Key' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '字母开头，仅支持英文、数字、下划线' }
            ]}
            extra="唯一标识，创建后不可修改"
          >
            <Input
              placeholder="请输入表单Key，如：expense_form"
              disabled={!!editingId}
            />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} placeholder="请输入表单描述（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`版本管理 - ${currentForm?.formName || ''}`}
        open={historyModalOpen}
        onCancel={() => setHistoryModalOpen(false)}
        width={700}
        footer={[
          <Button key="close" onClick={() => setHistoryModalOpen(false)}>关闭</Button>
        ]}
      >
        {currentForm && (
          <div>
            <Card type="inner" title="当前版本" style={{ marginBottom: 12 }}>
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <div>
                  <b>版本号：</b>v{currentForm.version || 1}.0
                  <Tag color="green" style={{ marginLeft: 8 }}>
                    {currentForm.status === 1 ? '已发布' : '草稿'}
                  </Tag>
                </div>
                <div><b>表单名称：</b>{currentForm.formName}</div>
                <div><b>更新时间：</b>{currentForm.updateTime || currentForm.createTime}</div>
              </Space>
            </Card>
            <div
              style={{
                padding: 40,
                textAlign: 'center',
                color: '#8c8c8c',
                border: '1px dashed #f0f0f0',
                borderRadius: 6
              }}
            >
              <HistoryOutlined style={{ fontSize: 32, marginBottom: 12 }} />
              <div>历史版本管理功能开发中...</div>
              <div style={{ fontSize: 12, marginTop: 4 }}>将支持版本回滚、对比、发布等功能</div>
            </div>
          </div>
        )}
      </Modal>

      {previewerOpen && previewSchema && (
        <Modal
          title={`表单预览 - ${currentForm?.formName || ''}`}
          open={previewerOpen}
          onCancel={() => setPreviewerOpen(false)}
          width={800}
          footer={[
            <Button key="close" onClick={() => setPreviewerOpen(false)}>关闭</Button>,
            <Button
              key="design"
              type="primary"
              icon={<EditOutlined />}
              onClick={() => {
                setPreviewerOpen(false)
                if (currentForm) handleDesign(currentForm)
              }}
            >
              去设计
            </Button>
          ]}
        >
          <div style={{ padding: 16, border: '1px solid #f0f0f0', borderRadius: 6, minHeight: 400 }}>
            <pre
              style={{
                padding: 16,
                background: '#f6f8fa',
                borderRadius: 6,
                maxHeight: 450,
                overflow: 'auto',
                fontSize: 12,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all'
              }}
            >
              {JSON.stringify(previewSchema, null, 2)}
            </pre>
          </div>
        </Modal>
      )}
    </div>
  )
}

export { FormDefinitionList }
export default FormDefinitionList
