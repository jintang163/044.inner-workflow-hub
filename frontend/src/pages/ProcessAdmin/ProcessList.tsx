import { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Input,
  Select,
  Tag,
  Modal,
  Form,
  InputNumber,
  App,
  Popconfirm,
  Card,
  Row,
  Col
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  DeploymentUnitOutlined,
  HistoryOutlined,
  RocketOutlined,
  SearchOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { processApi, formApi } from '@/api'
import type { ProcessDefinitionVO, PageResult } from '@/types'
import ProcessVersionModal from './components/ProcessVersionModal'
import SimulateModal from './components/SimulateModal'

const { Option } = Select

interface SearchParams {
  processName?: string
  businessLineId?: number
  categoryId?: number
  status?: number
}

export default function ProcessList() {
  const navigate = useNavigate()
  const { message, modal } = App.useApp()
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<ProcessDefinitionVO[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [searchParams, setSearchParams] = useState<SearchParams>({})

  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form] = Form.useForm()

  const [categoryList, setCategoryList] = useState<any[]>([])
  const [businessLineList, setBusinessLineList] = useState<any[]>([])
  const [formList, setFormList] = useState<any[]>([])

  const [versionModalVisible, setVersionModalVisible] = useState(false)
  const [currentProcessId, setCurrentProcessId] = useState<number | null>(null)
  const [simulateVisible, setSimulateVisible] = useState(false)

  const loadData = async () => {
    setLoading(true)
    try {
      const res = (await processApi.definitionList({
        pageNum,
        pageSize,
        ...searchParams
      })) as unknown as PageResult<ProcessDefinitionVO>
      setList(res.list || [])
      setTotal(res.total || 0)
    } catch (e) {
      // error handled
    } finally {
      setLoading(false)
    }
  }

  const loadSelectData = async () => {
    try {
      const [categories, businessLines, forms] = await Promise.all([
        processApi.categoryTree(),
        processApi.businessLineList({ pageNum: 1, pageSize: 1000 }),
        formApi.definitionList({ pageNum: 1, pageSize: 1000 })
      ])
      setCategoryList(categories || [])
      setBusinessLineList((businessLines as any).list || [])
      setFormList((forms as any).list || [])
    } catch (e) {
      // error handled
    }
  }

  useEffect(() => {
    loadData()
  }, [pageNum, pageSize])

  useEffect(() => {
    loadSelectData()
  }, [])

  const handleSearch = () => {
    setPageNum(1)
    loadData()
  }

  const handleReset = () => {
    setSearchParams({})
    setPageNum(1)
    loadData()
  }

  const handleAdd = () => {
    setEditingId(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: ProcessDefinitionVO) => {
    setEditingId(record.id)
    form.setFieldsValue({
      processName: record.processName,
      processKey: record.processKey,
      businessLineId: record.businessLineId,
      categoryId: record.categoryId,
      formId: record.formId,
      description: record.description,
      icon: ''
    })
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await processApi.definitionUpdate({ id: editingId, ...values })
        message.success('更新成功')
      } else {
        await processApi.definitionSave(values)
        message.success('创建成功')
      }
      setModalVisible(false)
      loadData()
    } catch (e) {
      // validation error
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await processApi.definitionRemove(id)
      message.success('删除成功')
      loadData()
    } catch (e) {
      // error handled
    }
  }

  const handleDesign = (id: number) => {
    navigate(`/process/design/${id}`)
  }

  const handleDeploy = async (record: ProcessDefinitionVO) => {
    modal.confirm({
      title: '确认发布',
      content: `确定要发布流程「${record.processName}」吗？`,
      onOk: async () => {
        try {
          await processApi.definitionDeploy({ id: record.id })
          message.success('发布成功')
          loadData()
        } catch (e) {
          // error handled
        }
      }
    })
  }

  const handleVersion = (record: ProcessDefinitionVO) => {
    setCurrentProcessId(record.id)
    setVersionModalVisible(true)
  }

  const handleSimulate = (record: ProcessDefinitionVO) => {
    setCurrentProcessId(record.id)
    setSimulateVisible(true)
  }

  const renderCategoryTree = (data: any[]): any[] => {
    return data.map((item) => ({
      value: item.id,
      label: item.name,
      children: item.children ? renderCategoryTree(item.children) : undefined
    }))
  }

  const columns = [
    {
      title: '流程名称',
      dataIndex: 'processName',
      key: 'processName',
      width: 180
    },
    {
      title: '流程标识',
      dataIndex: 'processKey',
      key: 'processKey',
      width: 150
    },
    {
      title: '业务线',
      dataIndex: 'businessLineName',
      key: 'businessLineName',
      width: 120
    },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 120
    },
    {
      title: '当前版本',
      dataIndex: 'version',
      key: 'version',
      width: 100,
      render: (v: number) => <Tag color="blue">v{v}</Tag>
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: number) =>
        v === 1 ? <Tag color="green">已发布</Tag> : <Tag color="default">草稿</Tag>
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180
    },
    {
      title: '操作',
      key: 'action',
      width: 400,
      fixed: 'right' as const,
      render: (_: any, record: ProcessDefinitionVO) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<DeploymentUnitOutlined />}
            onClick={() => handleDesign(record.id)}
          >
            设计
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            icon={<RocketOutlined />}
            onClick={() => handleDeploy(record)}
          >
            发布
          </Button>
          <Button
            type="link"
            size="small"
            icon={<HistoryOutlined />}
            onClick={() => handleVersion(record)}
          >
            版本
          </Button>
          <Button
            type="link"
            size="small"
            icon={<PlayCircleOutlined />}
            onClick={() => handleSimulate(record)}
          >
            模拟
          </Button>
          <Popconfirm title="确定删除此流程？" onConfirm={() => handleDelete(record.id)}>
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
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col span={6}>
            <Input
              placeholder="流程名称"
              allowClear
              prefix={<SearchOutlined />}
              value={searchParams.processName}
              onChange={(e) => setSearchParams({ ...searchParams, processName: e.target.value })}
              onPressEnter={handleSearch}
            />
          </Col>
          <Col span={5}>
            <Select
              placeholder="业务线"
              allowClear
              style={{ width: '100%' }}
              value={searchParams.businessLineId}
              onChange={(v) => setSearchParams({ ...searchParams, businessLineId: v })}
            >
              {businessLineList.map((item) => (
                <Option key={item.id} value={item.id}>
                  {item.lineName || item.name}
                </Option>
              ))}
            </Select>
          </Col>
          <Col span={5}>
            <Select
              placeholder="分类"
              allowClear
              style={{ width: '100%' }}
              value={searchParams.categoryId}
              onChange={(v) => setSearchParams({ ...searchParams, categoryId: v })}
              treeData={renderCategoryTree(categoryList)}
              showSearch
              treeDefaultExpandAll
            />
          </Col>
          <Col span={4}>
            <Select
              placeholder="状态"
              allowClear
              style={{ width: '100%' }}
              value={searchParams.status}
              onChange={(v) => setSearchParams({ ...searchParams, status: v })}
            >
              <Option value={0}>草稿</Option>
              <Option value={1}>已发布</Option>
            </Select>
          </Col>
          <Col span={4}>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增流程
            </Button>
          </Space>
        </div>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={list}
          loading={loading}
          scroll={{ x: 1300 }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, ps) => {
              setPageNum(p)
              setPageSize(ps)
            }
          }}
        />
      </Card>

      <Modal
        title={editingId ? '编辑流程' : '新增流程'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="流程名称"
                name="processName"
                rules={[{ required: true, message: '请输入流程名称' }]}
              >
                <Input placeholder="请输入流程名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="流程标识"
                name="processKey"
                rules={[{ required: true, message: '请输入流程标识' }]}
              >
                <Input placeholder="英文标识，如 leave_apply" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="业务线"
                name="businessLineId"
                rules={[{ required: true, message: '请选择业务线' }]}
              >
                <Select placeholder="请选择业务线">
                  {businessLineList.map((item) => (
                    <Option key={item.id} value={item.id}>
                      {item.lineName || item.name}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="流程分类"
                name="categoryId"
                rules={[{ required: true, message: '请选择分类' }]}
              >
                <Select
                  placeholder="请选择分类"
                  treeData={renderCategoryTree(categoryList)}
                  showSearch
                  treeDefaultExpandAll
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="关联表单"
                name="formId"
                rules={[{ required: true, message: '请选择关联表单' }]}
              >
                <Select placeholder="请选择关联表单" showSearch optionFilterProp="children">
                  {formList.map((item) => (
                    <Option key={item.id} value={item.id}>
                      {item.formName}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="图标" name="icon">
                <Input placeholder="图标名称（可选）" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} placeholder="请输入流程描述" />
          </Form.Item>
        </Form>
      </Modal>

      {currentProcessId && (
        <ProcessVersionModal
          open={versionModalVisible}
          onCancel={() => setVersionModalVisible(false)}
          processDefinitionId={currentProcessId}
          onSuccess={() => loadData()}
        />
      )}

      {currentProcessId && (
        <SimulateModal
          open={simulateVisible}
          onCancel={() => setSimulateVisible(false)}
          processDefinitionId={currentProcessId}
        />
      )}
    </div>
  )
}
