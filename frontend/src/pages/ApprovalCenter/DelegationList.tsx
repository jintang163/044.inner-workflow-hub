import React, { useState, useCallback, useEffect } from 'react'
import {
  Card,
  Table,
  Space,
  Button,
  Tag,
  Modal,
  Form,
  DatePicker,
  Input,
  message,
  Popconfirm,
  Tabs
} from 'antd'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons'
import { approvalApi } from '@/api'
import type { DelegationVO, DelegationSaveDTO, DelegationStatus } from '@/types/approval'
import type { PageResult } from '@/types'
import dayjs from 'dayjs'
import UserSelect from '@/components/business/UserSelect'

const { RangePicker } = DatePicker
const { TextArea } = Input
const { TabPane } = Tabs

const statusColors: Record<DelegationStatus, string> = {
  0: 'default',
  1: 'success',
  2: 'warning',
  3: 'error'
}

const statusText: Record<DelegationStatus, string> = {
  0: '待生效',
  1: '生效中',
  2: '已过期',
  3: '已撤销'
}

const DelegationList: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<PageResult<DelegationVO>>({
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: 10
  })
  const [queryType, setQueryType] = useState<number>(0)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<DelegationVO | null>(null)
  const [form] = Form.useForm<DelegationSaveDTO>()

  const fetchData = useCallback(async (pageNum = 1, pageSize = 10, type = queryType) => {
    setLoading(true)
    try {
      const res = await approvalApi.delegationPage({
        pageNum,
        pageSize,
        queryType: type
      })
      setData({ ...res, pageNum, pageSize })
    } catch (e: any) {
      message.error(e.message || '获取委托列表失败')
    } finally {
      setLoading(false)
    }
  }, [queryType])

  useEffect(() => {
    fetchData(1, 10, queryType)
  }, [queryType, fetchData])

  const handleTableChange = (pagination: any) => {
    fetchData(pagination.current, pagination.pageSize, queryType)
  }

  const handleAdd = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: DelegationVO) => {
    setEditingItem(record)
    form.setFieldsValue({
      id: record.id,
      delegateeId: record.delegateeId,
      timeRange: [dayjs(record.startTime), dayjs(record.endTime)],
      delegationReason: record.delegationReason,
      processKeys: record.processKeyList,
      remark: record.remark
    })
    setModalVisible(true)
  }

  const handleRevoke = async (id: number) => {
    try {
      await approvalApi.delegationRevoke(id)
      message.success('撤销成功')
      fetchData(data.pageNum, data.pageSize, queryType)
    } catch (e: any) {
      message.error(e.message || '撤销失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      const dateRange = values.timeRange
      if (dateRange && dateRange.length === 2) {
        values.startTime = dayjs(dateRange[0]).format('YYYY-MM-DD HH:mm:ss')
        values.endTime = dayjs(dateRange[1]).format('YYYY-MM-DD HH:mm:ss')
        delete values.timeRange
      }

      if (editingItem) {
        await approvalApi.delegationUpdate(values as DelegationSaveDTO)
        message.success('更新成功')
      } else {
        await approvalApi.delegationSave(values as DelegationSaveDTO)
        message.success('创建成功')
      }

      setModalVisible(false)
      fetchData(data.pageNum, data.pageSize, queryType)
    } catch (e: any) {
      message.error(e.message || '操作失败')
    }
  }

  const columns = [
    {
      title: '委托人',
      dataIndex: 'delegatorName',
      key: 'delegatorName',
      width: 120
    },
    {
      title: '代理人',
      dataIndex: 'delegateeName',
      key: 'delegateeName',
      width: 120
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 180
    },
    {
      title: '委托原因',
      dataIndex: 'delegationReason',
      key: 'delegationReason',
      ellipsis: true
    },
    {
      title: '状态',
      dataIndex: 'delegationStatus',
      key: 'delegationStatus',
      width: 100,
      render: (status: DelegationStatus) => (
        <Tag color={statusColors[status]}>{statusText[status]}</Tag>
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_: any, record: DelegationVO) => (
        <Space size="small">
          {(record.delegationStatus === 0 || record.delegationStatus === 1) && (
            <>
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEdit(record)}
              >
                编辑
              </Button>
              <Popconfirm
                title="确定撤销此委托吗？"
                onConfirm={() => handleRevoke(record.id)}
                okText="确定"
                cancelText="取消"
              >
                <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                  撤销
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
      )
    }
  ]

  return (
    <div className="p-6">
      <Card
        title="委托代理管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建委托
          </Button>
        }
      >
        <Tabs
          activeKey={queryType.toString()}
          onChange={(key) => setQueryType(Number(key))}
          type="card"
        >
          <TabPane tab="全部" key="0" />
          <TabPane tab="我发起的" key="1" />
          <TabPane tab="代理我的" key="2" />
        </Tabs>

        <div className="mb-4 flex justify-end">
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchData(data.pageNum, data.pageSize, queryType)}
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
        title={editingItem ? '编辑委托' : '新建委托'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="delegateeId"
            label="代理人"
            rules={[{ required: true, message: '请选择代理人' }]}
          >
            <UserSelect placeholder="请选择代理人" multiple={false} />
          </Form.Item>

          <Form.Item
            name="timeRange"
            label="委托时间"
            rules={[{ required: true, message: '请选择委托时间' }]}
          >
            <RangePicker
              showTime
              style={{ width: '100%' }}
              format="YYYY-MM-DD HH:mm:ss"
              placeholder={['开始时间', '结束时间']}
            />
          </Form.Item>

          <Form.Item name="delegationReason" label="委托原因">
            <Input placeholder="请输入委托原因" maxLength={200} />
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <TextArea rows={3} placeholder="请输入备注" maxLength={500} />
          </Form.Item>

          <Form.Item label="说明">
            <div className="text-gray-500 text-sm">
              <p>• 委托期间，您的待办任务将自动转移给代理人处理</p>
              <p>• 到期后委托自动失效，任务将恢复到您的账户</p>
              <p>• 您可以随时撤销委托</p>
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DelegationList
