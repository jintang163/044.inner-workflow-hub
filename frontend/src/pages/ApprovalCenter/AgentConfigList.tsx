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
  Switch,
  Select,
  message,
  Popconfirm
} from 'antd'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons'
import { approvalApi } from '@/api'
import type { PageResult } from '@/types'
import UserSelect from '@/components/business/UserSelect'

interface AgentConfigVO {
  id: number
  userName: string
  agentUserName: string
  agentUserId: number
  configType: number
  processKeys: string
  priority: number
  enabled: boolean
  remark: string
  createTime: string
}

interface AgentConfigSaveDTO {
  id?: number
  agentUserId: number
  configType: number
  processKeys: string
  priority: number
  enabled: boolean
  remark: string
}

const configTypeMap: Record<number, string> = {
  1: '常驻代理人',
  2: '休假默认代理人'
}

const AgentConfigList: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<PageResult<AgentConfigVO>>({
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: 10
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<AgentConfigVO | null>(null)
  const [form] = Form.useForm<AgentConfigSaveDTO>()

  const fetchData = useCallback(async (pageNum = 1, pageSize = 10) => {
    setLoading(true)
    try {
      const res = await approvalApi.agentConfigPage({
        pageNum,
        pageSize
      })
      setData({ ...res, pageNum, pageSize })
    } catch (e: any) {
      message.error(e.message || '获取代理配置列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(1, 10)
  }, [fetchData])

  const handleTableChange = (pagination: any) => {
    fetchData(pagination.current, pagination.pageSize)
  }

  const handleAdd = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: AgentConfigVO) => {
    setEditingItem(record)
    form.setFieldsValue({
      id: record.id,
      agentUserId: record.agentUserId,
      configType: record.configType,
      processKeys: record.processKeys,
      priority: record.priority,
      enabled: record.enabled,
      remark: record.remark
    })
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await approvalApi.agentConfigDelete(id)
      message.success('删除成功')
      fetchData(data.pageNum, data.pageSize)
    } catch (e: any) {
      message.error(e.message || '删除失败')
    }
  }

  const handleToggleEnabled = async (record: AgentConfigVO) => {
    try {
      await approvalApi.agentConfigUpdate({
        id: record.id,
        agentUserId: record.agentUserId,
        configType: record.configType,
        processKeys: record.processKeys,
        priority: record.priority,
        enabled: !record.enabled,
        remark: record.remark
      })
      message.success(record.enabled ? '已禁用' : '已启用')
      fetchData(data.pageNum, data.pageSize)
    } catch (e: any) {
      message.error(e.message || '操作失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      if (editingItem) {
        values.id = editingItem.id
        await approvalApi.agentConfigUpdate(values as AgentConfigSaveDTO)
        message.success('更新成功')
      } else {
        await approvalApi.agentConfigSave(values as AgentConfigSaveDTO)
        message.success('创建成功')
      }

      setModalVisible(false)
      fetchData(data.pageNum, data.pageSize)
    } catch (e: any) {
      message.error(e.message || '操作失败')
    }
  }

  const columns = [
    {
      title: '用户',
      dataIndex: 'userName',
      key: 'userName',
      width: 120
    },
    {
      title: '代理人',
      dataIndex: 'agentUserName',
      key: 'agentUserName',
      width: 120
    },
    {
      title: '配置类型',
      dataIndex: 'configType',
      key: 'configType',
      width: 140,
      render: (type: number) => <Tag color="blue">{configTypeMap[type]}</Tag>
    },
    {
      title: '流程标识',
      dataIndex: 'processKeys',
      key: 'processKeys',
      ellipsis: true
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'success' : 'default'}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      )
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      ellipsis: true
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_: any, record: AgentConfigVO) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => handleToggleEnabled(record)}
          >
            {record.enabled ? '禁用' : '启用'}
          </Button>
          <Popconfirm
            title="确定删除此代理配置吗？"
            onConfirm={() => handleDelete(record.id)}
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
      <Card
        title="代理配置管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建配置
          </Button>
        }
      >
        <div className="mb-4 flex justify-end">
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchData(data.pageNum, data.pageSize)}
          >
            刷新
          </Button>
        </div>

        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={data.list}
          pagination={{
            current: data.pageNum,
            pageSize: data.pageSize,
            total: data.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`
          }}
          onChange={handleTableChange}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑代理配置' : '新建代理配置'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical" initialValues={{ configType: 1, priority: 0, enabled: true }}>
          <Form.Item
            name="agentUserId"
            label="代理人"
            rules={[{ required: true, message: '请选择代理人' }]}
          >
            <UserSelect placeholder="请选择代理人" multiple={false} />
          </Form.Item>

          <Form.Item
            name="configType"
            label="配置类型"
            rules={[{ required: true, message: '请选择配置类型' }]}
          >
            <Select placeholder="请选择配置类型">
              {Object.entries(configTypeMap).map(([value, label]) => (
                <Select.Option key={value} value={Number(value)}>
                  {label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="processKeys" label="流程标识">
            <Input placeholder="多个以逗号分隔" maxLength={500} />
          </Form.Item>

          <Form.Item name="priority" label="优先级">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="请输入优先级" />
          </Form.Item>

          <Form.Item name="enabled" label="是否启用" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入备注" maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AgentConfigList
