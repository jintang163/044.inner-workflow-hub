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
  Tabs,
  Select,
  Switch
} from 'antd'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  SyncOutlined,
  SwapOutlined,
  StopOutlined
} from '@ant-design/icons'
import { approvalApi } from '@/api'
import type { PageResult } from '@/types'
import dayjs from 'dayjs'
import UserSelect from '@/components/business/UserSelect'

const { RangePicker } = DatePicker
const { TextArea } = Input
const { TabPane } = Tabs

type VacationType = 1 | 2 | 3 | 4 | 5 | 6
type SourceType = 1 | 2 | 3 | 4 | 5
type VacationStatus = 0 | 1

interface VacationVO {
  id: number
  userId: number
  userName: string
  vacationType: VacationType
  vacationTitle: string
  startTime: string
  endTime: string
  fullDay: boolean
  sourceType: SourceType
  autoDelegate: boolean
  agentUserId: number
  agentUserName: string
  vacationStatus: VacationStatus
  remark: string
  createTime: string
}

interface VacationSaveDTO {
  id?: number
  userId: number
  vacationType: number
  vacationTitle: string
  startTime: string
  endTime: string
  fullDay: boolean
  autoDelegate: boolean
  agentUserId?: number
  remark?: string
}

const vacationTypeText: Record<VacationType, string> = {
  1: '年假',
  2: '事假',
  3: '病假',
  4: '出差',
  5: '调休',
  6: '其他'
}

const vacationTypeColor: Record<VacationType, string> = {
  1: 'green',
  2: 'orange',
  3: 'red',
  4: 'blue',
  5: 'cyan',
  6: 'default'
}

const sourceTypeText: Record<SourceType, string> = {
  1: '手动设置',
  2: '钉钉',
  3: '飞书',
  4: 'Outlook',
  5: '企业微信'
}

const sourceTypeColor: Record<SourceType, string> = {
  1: 'default',
  2: 'blue',
  3: 'geekblue',
  4: 'purple',
  5: 'green'
}

const vacationStatusText: Record<VacationStatus, string> = {
  0: '已取消',
  1: '有效'
}

const vacationStatusColor: Record<VacationStatus, string> = {
  0: 'default',
  1: 'success'
}

const VacationList: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<PageResult<VacationVO>>({
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: 10
  })
  const [queryType, setQueryType] = useState<number>(0)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<VacationVO | null>(null)
  const [autoDelegate, setAutoDelegate] = useState(false)
  const [syncLoading, setSyncLoading] = useState<Record<number, boolean>>({})
  const [transferLoading, setTransferLoading] = useState(false)
  const [form] = Form.useForm<VacationSaveDTO>()

  const fetchData = useCallback(async (pageNum = 1, pageSize = 10, type = queryType) => {
    setLoading(true)
    try {
      const res = await approvalApi.vacationPage({
        pageNum,
        pageSize,
        queryType: type
      })
      setData({ ...res, pageNum, pageSize })
    } catch (e: any) {
      message.error(e.message || '获取休假列表失败')
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
    setAutoDelegate(false)
    form.resetFields()
    form.setFieldsValue({ fullDay: true, autoDelegate: false })
    setModalVisible(true)
  }

  const handleEdit = (record: VacationVO) => {
    setEditingItem(record)
    setAutoDelegate(record.autoDelegate)
    form.setFieldsValue({
      id: record.id,
      userId: record.userId,
      vacationType: record.vacationType,
      vacationTitle: record.vacationTitle,
      timeRange: [dayjs(record.startTime), dayjs(record.endTime)],
      fullDay: record.fullDay,
      autoDelegate: record.autoDelegate,
      agentUserId: record.agentUserId,
      remark: record.remark
    })
    setModalVisible(true)
  }

  const handleCancel = async (id: number) => {
    try {
      await approvalApi.vacationCancel(id)
      message.success('取消休假成功')
      fetchData(data.pageNum, data.pageSize, queryType)
    } catch (e: any) {
      message.error(e.message || '取消休假失败')
    }
  }

  const handleSync = async (sourceType: number) => {
    setSyncLoading((prev) => ({ ...prev, [sourceType]: true }))
    try {
      await approvalApi.vacationSync(sourceType)
      message.success('同步成功')
      fetchData(data.pageNum, data.pageSize, queryType)
    } catch (e: any) {
      message.error(e.message || '同步失败')
    } finally {
      setSyncLoading((prev) => ({ ...prev, [sourceType]: false }))
    }
  }

  const handleBatchTransfer = async () => {
    setTransferLoading(true)
    try {
      await approvalApi.vacationBatchTransfer()
      message.success('批量转交成功')
      fetchData(data.pageNum, data.pageSize, queryType)
    } catch (e: any) {
      message.error(e.message || '批量转交失败')
    } finally {
      setTransferLoading(false)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      const dateRange = values.timeRange
      if (dateRange && dateRange.length === 2) {
        values.startTime = dayjs(dateRange[0]).format('YYYY-MM-DD HH:mm:ss')
        values.endTime = dayjs(dateRange[1]).format('YYYY-MM-DD HH:mm:ss')
        delete (values as any).timeRange
      }

      if (!values.autoDelegate) {
        delete values.agentUserId
      }

      if (editingItem) {
        await approvalApi.vacationUpdate(values as VacationSaveDTO)
        message.success('更新成功')
      } else {
        await approvalApi.vacationSave(values as VacationSaveDTO)
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
      title: '用户',
      dataIndex: 'userName',
      key: 'userName',
      width: 120
    },
    {
      title: '休假类型',
      dataIndex: 'vacationType',
      key: 'vacationType',
      width: 100,
      render: (type: VacationType) => (
        <Tag color={vacationTypeColor[type]}>{vacationTypeText[type]}</Tag>
      )
    },
    {
      title: '休假标题',
      dataIndex: 'vacationTitle',
      key: 'vacationTitle',
      ellipsis: true
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
      title: '来源',
      dataIndex: 'sourceType',
      key: 'sourceType',
      width: 100,
      render: (type: SourceType) => (
        <Tag color={sourceTypeColor[type]}>{sourceTypeText[type]}</Tag>
      )
    },
    {
      title: '自动委托',
      dataIndex: 'autoDelegate',
      key: 'autoDelegate',
      width: 100,
      render: (val: boolean) => (
        <Tag color={val ? 'green' : 'default'}>{val ? '是' : '否'}</Tag>
      )
    },
    {
      title: '代理人',
      dataIndex: 'agentUserName',
      key: 'agentUserName',
      width: 120,
      render: (val: string) => val || '-'
    },
    {
      title: '状态',
      dataIndex: 'vacationStatus',
      key: 'vacationStatus',
      width: 100,
      render: (status: VacationStatus) => (
        <Tag color={vacationStatusColor[status]}>{vacationStatusText[status]}</Tag>
      )
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_: any, record: VacationVO) => (
        <Space size="small">
          {record.vacationStatus === 1 && record.sourceType === 1 && (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            >
              编辑
            </Button>
          )}
          {record.vacationStatus === 1 && (
            <Popconfirm
              title="确定取消此休假吗？"
              onConfirm={() => handleCancel(record.id)}
              okText="确定"
              cancelText="取消"
            >
              <Button type="link" size="small" danger icon={<StopOutlined />}>
                取消
              </Button>
            </Popconfirm>
          )}
        </Space>
      )
    }
  ]

  return (
    <div className="p-6">
      <Card
        title="休假管理"
        extra={
          <Space>
            <Button
              icon={<SwapOutlined />}
              loading={transferLoading}
              onClick={handleBatchTransfer}
            >
              批量转交
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新建休假
            </Button>
          </Space>
        }
      >
        <Tabs
          activeKey={queryType.toString()}
          onChange={(key) => setQueryType(Number(key))}
          type="card"
        >
          <TabPane tab="我的休假" key="0" />
          <TabPane tab="全部休假" key="1" />
        </Tabs>

        <div className="mb-4 flex justify-between">
          <Space>
            <Button
              icon={<SyncOutlined spin={syncLoading[2]} />}
              loading={syncLoading[2]}
              onClick={() => handleSync(2)}
            >
              同步钉钉
            </Button>
            <Button
              icon={<SyncOutlined spin={syncLoading[3]} />}
              loading={syncLoading[3]}
              onClick={() => handleSync(3)}
            >
              同步飞书
            </Button>
            <Button
              icon={<SyncOutlined spin={syncLoading[4]} />}
              loading={syncLoading[4]}
              onClick={() => handleSync(4)}
            >
              同步Outlook
            </Button>
          </Space>
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
        title={editingItem ? '编辑休假' : '新建休假'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="userId"
            label="休假用户"
            rules={[{ required: true, message: '请选择休假用户' }]}
          >
            <UserSelect placeholder="请选择休假用户" multiple={false} />
          </Form.Item>

          <Form.Item
            name="vacationType"
            label="休假类型"
            rules={[{ required: true, message: '请选择休假类型' }]}
          >
            <Select placeholder="请选择休假类型">
              {Object.entries(vacationTypeText).map(([value, label]) => (
                <Select.Option key={value} value={Number(value)}>
                  {label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="vacationTitle"
            label="休假标题"
            rules={[{ required: true, message: '请输入休假标题' }]}
          >
            <Input placeholder="请输入休假标题" maxLength={200} />
          </Form.Item>

          <Form.Item
            name="timeRange"
            label="休假时间"
            rules={[{ required: true, message: '请选择休假时间' }]}
          >
            <RangePicker
              showTime
              style={{ width: '100%' }}
              format="YYYY-MM-DD HH:mm:ss"
              placeholder={['开始时间', '结束时间']}
            />
          </Form.Item>

          <Form.Item name="fullDay" label="全天" valuePropName="checked">
            <Switch />
          </Form.Item>

          <Form.Item name="autoDelegate" label="自动委托" valuePropName="checked">
            <Switch onChange={(val) => setAutoDelegate(val)} />
          </Form.Item>

          {autoDelegate && (
            <Form.Item
              name="agentUserId"
              label="代理人"
              rules={[{ required: true, message: '请选择代理人' }]}
            >
              <UserSelect placeholder="请选择代理人" multiple={false} />
            </Form.Item>
          )}

          <Form.Item name="remark" label="备注">
            <TextArea rows={3} placeholder="请输入备注" maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default VacationList
