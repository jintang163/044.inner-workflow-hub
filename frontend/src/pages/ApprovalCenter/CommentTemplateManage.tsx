import React, { useState, useCallback, useEffect } from 'react'
import {
  Card,
  Table,
  Space,
  Button,
  Tag,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  message,
  Popconfirm,
  Row,
  Col,
  List,
  Avatar,
  Tooltip,
  Divider
} from 'antd'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  TeamOutlined,
  GlobalOutlined,
  FireOutlined,
  FormOutlined,
  FolderOutlined,
  FolderOpenOutlined
} from '@ant-design/icons'
import { approvalApi } from '@/api'
import type { PageResult } from '@/types'
import type {
  CommentTemplateVO,
  CommentTemplateCategoryVO,
  CommentTemplateSaveDTO,
  CommentTemplateCategorySaveDTO,
  CommentTemplateScopeType
} from '@/types/approval'

const scopeTypeOptions = [
  { value: 0, label: '个人模板', icon: <UserOutlined />, color: 'blue' },
  { value: 1, label: '部门公共模板', icon: <TeamOutlined />, color: 'green' },
  { value: 2, label: '全局模板', icon: <GlobalOutlined />, color: 'gold' }
]

const getScopeInfo = (scopeType: CommentTemplateScopeType) => {
  return scopeTypeOptions.find(item => item.value === scopeType) || scopeTypeOptions[0]
}

const CommentTemplateManage: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [categoryLoading, setCategoryLoading] = useState(false)
  const [categories, setCategories] = useState<CommentTemplateCategoryVO[]>([])
  const [activeCategoryId, setActiveCategoryId] = useState<number | null>(null)
  const [templateData, setTemplateData] = useState<PageResult<CommentTemplateVO>>({
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: 10
  })

  const [categoryModalVisible, setCategoryModalVisible] = useState(false)
  const [editingCategory, setEditingCategory] = useState<CommentTemplateCategoryVO | null>(null)
  const [categoryForm] = Form.useForm<CommentTemplateCategorySaveDTO>()

  const [templateModalVisible, setTemplateModalVisible] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState<CommentTemplateVO | null>(null)
  const [templateForm] = Form.useForm<CommentTemplateSaveDTO>()

  const fetchCategories = useCallback(async () => {
    setCategoryLoading(true)
    try {
      const res = await approvalApi.commentTemplateCategoryPage({
        pageNum: 1,
        pageSize: 999
      })
      setCategories(res.data?.list || [])
      if (!activeCategoryId && res.data?.list && res.data.list.length > 0) {
        setActiveCategoryId(res.data.list[0].id)
      }
    } catch (e: any) {
      message.error(e.message || '获取分类列表失败')
    } finally {
      setCategoryLoading(false)
    }
  }, [activeCategoryId])

  const fetchTemplates = useCallback(async (pageNum = 1, pageSize = 10) => {
    setLoading(true)
    try {
      const params: any = { pageNum, pageSize }
      if (activeCategoryId) {
        params.categoryId = activeCategoryId
      }
      const res = await approvalApi.commentTemplatePage(params)
      setTemplateData({ ...res.data, pageNum, pageSize })
    } catch (e: any) {
      message.error(e.message || '获取模板列表失败')
    } finally {
      setLoading(false)
    }
  }, [activeCategoryId])

  useEffect(() => {
    fetchCategories()
  }, [fetchCategories])

  useEffect(() => {
    if (activeCategoryId) {
      fetchTemplates(1, templateData.pageSize)
    }
  }, [activeCategoryId])

  const handleTableChange = (pagination: any) => {
    fetchTemplates(pagination.current, pagination.pageSize)
  }

  const handleAddCategory = () => {
    setEditingCategory(null)
    categoryForm.resetFields()
    categoryForm.setFieldsValue({ scopeType: 0, sortOrder: 0, status: 1 })
    setCategoryModalVisible(true)
  }

  const handleEditCategory = (record: CommentTemplateCategoryVO) => {
    setEditingCategory(record)
    categoryForm.setFieldsValue({
      id: record.id,
      categoryName: record.categoryName,
      categoryCode: record.categoryCode,
      scopeType: record.scopeType,
      deptId: record.deptId,
      sortOrder: record.sortOrder,
      status: record.status,
      remark: record.remark
    })
    setCategoryModalVisible(true)
  }

  const handleDeleteCategory = async (id: number) => {
    try {
      await approvalApi.commentTemplateCategoryDelete(id)
      message.success('删除成功')
      fetchCategories()
      if (activeCategoryId === id) {
        setActiveCategoryId(null)
      }
    } catch (e: any) {
      message.error(e.message || '删除失败')
    }
  }

  const handleCategorySubmit = async () => {
    try {
      const values = await categoryForm.validateFields()
      if (editingCategory) {
        await approvalApi.commentTemplateCategoryUpdate(values as CommentTemplateCategorySaveDTO)
        message.success('更新成功')
      } else {
        await approvalApi.commentTemplateCategorySave(values as CommentTemplateCategorySaveDTO)
        message.success('创建成功')
      }
      setCategoryModalVisible(false)
      fetchCategories()
    } catch (e: any) {
      message.error(e.message || '操作失败')
    }
  }

  const handleAddTemplate = () => {
    if (!activeCategoryId) {
      message.warning('请先选择一个分类')
      return
    }
    setEditingTemplate(null)
    templateForm.resetFields()
    templateForm.setFieldsValue({
      categoryId: activeCategoryId,
      scopeType: 0,
      sortOrder: 0,
      status: 1
    })
    setTemplateModalVisible(true)
  }

  const handleEditTemplate = (record: CommentTemplateVO) => {
    setEditingTemplate(record)
    templateForm.setFieldsValue({
      id: record.id,
      categoryId: record.categoryId,
      templateName: record.templateName,
      templateContent: record.templateContent,
      scopeType: record.scopeType,
      deptId: record.deptId,
      sortOrder: record.sortOrder,
      status: record.status,
      remark: record.remark
    })
    setTemplateModalVisible(true)
  }

  const handleDeleteTemplate = async (id: number) => {
    try {
      await approvalApi.commentTemplateDelete(id)
      message.success('删除成功')
      fetchTemplates(templateData.pageNum, templateData.pageSize)
    } catch (e: any) {
      message.error(e.message || '删除失败')
    }
  }

  const handleTemplateSubmit = async () => {
    try {
      const values = await templateForm.validateFields()
      if (editingTemplate) {
        await approvalApi.commentTemplateUpdate(values as CommentTemplateSaveDTO)
        message.success('更新成功')
      } else {
        await approvalApi.commentTemplateSave(values as CommentTemplateSaveDTO)
        message.success('创建成功')
      }
      setTemplateModalVisible(false)
      fetchTemplates(templateData.pageNum, templateData.pageSize)
    } catch (e: any) {
      message.error(e.message || '操作失败')
    }
  }

  const templateColumns = [
    {
      title: '模板名称',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 180,
      render: (name: string, record: CommentTemplateVO) => (
        <Space>
          <FormOutlined style={{ color: '#1890ff' }} />
          <span>{name}</span>
        </Space>
      )
    },
    {
      title: '模板内容',
      dataIndex: 'templateContent',
      key: 'templateContent',
      ellipsis: true
    },
    {
      title: '适用范围',
      dataIndex: 'scopeType',
      key: 'scopeType',
      width: 140,
      render: (scopeType: CommentTemplateScopeType) => {
        const info = getScopeInfo(scopeType)
        return (
          <Tag color={info.color}>
            {info.icon}
            <span style={{ marginLeft: 4 }}>{info.label}</span>
          </Tag>
        )
      }
    },
    {
      title: '使用次数',
      dataIndex: 'useCount',
      key: 'useCount',
      width: 100,
      render: (count: number) => (
        <Space size={4}>
          <FireOutlined style={{ color: count > 0 ? '#ff4d4f' : '#d9d9d9' }} />
          <span>{count}</span>
        </Space>
      )
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 80
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) => (
        <Tag color={status === 1 ? 'success' : 'default'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      )
    },
    {
      title: '创建人',
      dataIndex: 'createByName',
      key: 'createByName',
      width: 100
    },
    {
      title: '操作',
      key: 'actions',
      width: 160,
      fixed: 'right' as const,
      render: (_: any, record: CommentTemplateVO) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditTemplate(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除此模板吗？"
            onConfirm={() => handleDeleteTemplate(record.id)}
            okText="确定"
            cancelText="取消"
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
    <div className="p-6">
      <Row gutter={16}>
        <Col span={6}>
          <Card
            title="模板分类"
            size="small"
            extra={
              <Button type="primary" size="small" icon={<PlusOutlined />} onClick={handleAddCategory}>
                新增
              </Button>
            }
            style={{ height: '100%' }}
            bodyStyle={{ padding: 0 }}
          >
            <List
              loading={categoryLoading}
              dataSource={categories}
              renderItem={(item) => {
                const scopeInfo = getScopeInfo(item.scopeType)
                return (
                  <List.Item
                    key={item.id}
                    onClick={() => setActiveCategoryId(item.id)}
                    style={{
                      cursor: 'pointer',
                      padding: '12px 16px',
                      backgroundColor: activeCategoryId === item.id ? '#e6f7ff' : 'transparent',
                      borderLeft: activeCategoryId === item.id ? '3px solid #1890ff' : '3px solid transparent'
                    }}
                  >
                    <List.Item.Meta
                      avatar={
                        <Avatar
                          size="small"
                          style={{ backgroundColor: scopeInfo.color + '20', color: scopeInfo.color }}
                          icon={activeCategoryId === item.id ? <FolderOpenOutlined /> : <FolderOutlined />}
                        />
                      }
                      title={
                        <Space size={8}>
                          <span style={{ fontSize: 14, fontWeight: activeCategoryId === item.id ? 500 : 400 }}>
                            {item.categoryName}
                          </span>
                          <Tag color={scopeInfo.color} style={{ margin: 0, fontSize: 11, padding: '0 6px' }}>
                            {scopeInfo.label}
                          </Tag>
                        </Space>
                      }
                      description={
                        <Space size={8} style={{ fontSize: 12, color: '#999' }}>
                          <span>编码：{item.categoryCode}</span>
                          {item.remark && <span> | {item.remark}</span>}
                        </Space>
                      }
                    />
                    <div style={{ display: 'flex', gap: 4 }}>
                      <Button
                        type="text"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={(e) => {
                          e.stopPropagation()
                          handleEditCategory(item)
                        }}
                      />
                      <Popconfirm
                        title="确定删除此分类吗？"
                        onConfirm={(e) => {
                          e?.stopPropagation()
                          handleDeleteCategory(item.id)
                        }}
                        okText="确定"
                        cancelText="取消"
                      >
                        <Button
                          type="text"
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={(e) => e.stopPropagation()}
                        />
                      </Popconfirm>
                    </div>
                  </List.Item>
                )
              }}
            />
            {categories.length === 0 && !categoryLoading && (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                暂无分类，请先创建
              </div>
            )}
          </Card>
        </Col>

        <Col span={18}>
          <Card
            title="意见模板"
            extra={
              <Space>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={() => {
                    fetchCategories()
                    fetchTemplates(1, templateData.pageSize)
                  }}
                >
                  刷新
                </Button>
                <Button type="primary" icon={<PlusOutlined />} onClick={handleAddTemplate}>
                  新增模板
                </Button>
              </Space>
            }
          >
            <Table
              rowKey="id"
              loading={loading}
              columns={templateColumns}
              dataSource={templateData.list}
              pagination={{
                current: templateData.pageNum,
                pageSize: templateData.pageSize,
                total: templateData.total,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条记录`
              }}
              onChange={handleTableChange}
              scroll={{ x: 1000 }}
            />
          </Card>
        </Col>
      </Row>

      <Modal
        title={editingCategory ? '编辑分类' : '新增分类'}
        open={categoryModalVisible}
        onOk={handleCategorySubmit}
        onCancel={() => setCategoryModalVisible(false)}
        width={520}
        destroyOnClose
      >
        <Form form={categoryForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="categoryName"
                label="分类名称"
                rules={[{ required: true, message: '请输入分类名称' }]}
              >
                <Input placeholder="请输入分类名称" maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="categoryCode"
                label="分类编码"
                rules={[{ required: true, message: '请输入分类编码' }]}
              >
                <Input placeholder="请输入分类编码" maxLength={50} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="scopeType"
            label="适用范围"
            rules={[{ required: true, message: '请选择适用范围' }]}
          >
            <Select placeholder="请选择适用范围">
              {scopeTypeOptions.map(item => (
                <Select.Option key={item.value} value={item.value}>
                  <Space size={6}>
                    {item.icon}
                    <span>{item.label}</span>
                  </Space>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="sortOrder" label="排序">
                <InputNumber min={0} style={{ width: '100%' }} placeholder="数字越小越靠前" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态">
                <Select>
                  <Select.Option value={1}>启用</Select.Option>
                  <Select.Option value={0}>禁用</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入备注" maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingTemplate ? '编辑模板' : '新增模板'}
        open={templateModalVisible}
        onOk={handleTemplateSubmit}
        onCancel={() => setTemplateModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={templateForm} layout="vertical">
          <Form.Item
            name="categoryId"
            label="所属分类"
            rules={[{ required: true, message: '请选择分类' }]}
          >
            <Select placeholder="请选择分类">
              {categories.map(cat => (
                <Select.Option key={cat.id} value={cat.id}>
                  {cat.categoryName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="templateName"
            label="模板名称"
            rules={[{ required: true, message: '请输入模板名称' }]}
          >
            <Input placeholder="请输入模板名称，如：同意、已核实" maxLength={50} />
          </Form.Item>

          <Form.Item
            name="templateContent"
            label="模板内容"
            rules={[{ required: true, message: '请输入模板内容' }]}
          >
            <Input.TextArea
              rows={4}
              placeholder="请输入审批意见模板内容"
              maxLength={500}
              showCount
            />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="scopeType"
                label="适用范围"
                rules={[{ required: true, message: '请选择适用范围' }]}
              >
                <Select placeholder="请选择适用范围">
                  {scopeTypeOptions.map(item => (
                    <Select.Option key={item.value} value={item.value}>
                      <Space size={6}>
                        {item.icon}
                        <span>{item.label}</span>
                      </Space>
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="sortOrder" label="排序">
                <InputNumber min={0} style={{ width: '100%' }} placeholder="数字越小越靠前" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="status" label="状态">
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="请输入备注" maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default CommentTemplateManage
