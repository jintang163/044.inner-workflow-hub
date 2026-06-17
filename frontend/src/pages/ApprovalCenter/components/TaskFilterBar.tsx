import { useState } from 'react'
import { Form, Select, DatePicker, Input, Button, Space, Cascader, Row, Col, Tag } from 'antd'
import { SearchOutlined, ReloadOutlined, FilterOutlined } from '@ant-design/icons'
import type { CascaderOption } from 'antd/es/cascader'
import dayjs, { Dayjs } from 'dayjs'

const { RangePicker } = DatePicker

export interface FilterValues {
  category?: string[]
  processKey?: string
  taskStatus?: 'URGENT' | 'NEARLY_TIMEOUT' | 'NORMAL'
  approvalResult?: string
  instanceStatus?: number
  timeRange?: [Dayjs, Dayjs]
  keyword?: string
  readStatus?: 0 | 1
}

interface TaskFilterBarProps {
  showApprovalResult?: boolean
  showInstanceStatus?: boolean
  showReadStatus?: boolean
  onSearch: (values: FilterValues) => void
  onReset: () => void
  processOptions?: { label: string; value: string }[]
  categoryTree?: CascaderOption[]
}

const TaskFilterBar: React.FC<TaskFilterBarProps> = ({
  showApprovalResult = false,
  showInstanceStatus = false,
  showReadStatus = false,
  onSearch,
  onReset,
  processOptions = [],
  categoryTree = []
}) => {
  const [form] = Form.useForm()
  const [expanded, setExpanded] = useState(false)

  const handleSearch = () => {
    const values = form.getFieldsValue()
    onSearch(values)
  }

  const handleReset = () => {
    form.resetFields()
    onReset()
  }

  const taskStatusOptions = [
    { label: <Tag color="red">紧急</Tag>, value: 'URGENT' },
    { label: <Tag color="orange">即将超时</Tag>, value: 'NEARLY_TIMEOUT' },
    { label: <Tag color="blue">普通</Tag>, value: 'NORMAL' }
  ]

  const approvalResultOptions = [
    { label: '同意', value: 'APPROVE' },
    { label: '拒绝', value: 'REJECT' },
    { label: '转审', value: 'TRANSFER' },
    { label: '加签', value: 'ADD_SIGN' },
    { label: '委派', value: 'DELEGATE' },
    { label: '驳回', value: 'REJECT_TO_NODE' },
    { label: '撤回', value: 'WITHDRAW' }
  ]

  const instanceStatusOptions = [
    { label: '审批中', value: 1 },
    { label: '已完成', value: 2 },
    { label: '已驳回', value: 3 },
    { label: '已撤回', value: 4 }
  ]

  const readStatusOptions = [
    { label: '未读', value: 0 },
    { label: '已读', value: 1 }
  ]

  return (
    <div className="task-filter-bar" style={{ background: '#fff', padding: 16, borderRadius: 8, marginBottom: 16 }}>
      <Form form={form} layout="horizontal" onFinish={handleSearch}>
        <Row gutter={16}>
          <Col xs={24} sm={12} md={8} lg={6}>
            <Form.Item name="category" label="流程分类" style={{ marginBottom: 12 }}>
              <Cascader
                options={categoryTree}
                placeholder="请选择流程分类"
                changeOnSelect
                allowClear
                style={{ width: '100%' }}
              />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12} md={8} lg={6}>
            <Form.Item name="processKey" label="流程名称" style={{ marginBottom: 12 }}>
              <Select
                options={processOptions}
                placeholder="请选择流程"
                allowClear
                showSearch
                optionFilterProp="label"
              />
            </Form.Item>
          </Col>
          {!showApprovalResult && !showInstanceStatus && (
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="taskStatus" label="任务状态" style={{ marginBottom: 12 }}>
                <Select options={taskStatusOptions} placeholder="请选择状态" allowClear />
              </Form.Item>
            </Col>
          )}
          {showApprovalResult && (
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="approvalResult" label="审批结果" style={{ marginBottom: 12 }}>
                <Select options={approvalResultOptions} placeholder="请选择审批结果" allowClear />
              </Form.Item>
            </Col>
          )}
          {showInstanceStatus && (
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="instanceStatus" label="流程状态" style={{ marginBottom: 12 }}>
                <Select options={instanceStatusOptions} placeholder="请选择流程状态" allowClear />
              </Form.Item>
            </Col>
          )}
          {showReadStatus && (
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="readStatus" label="阅读状态" style={{ marginBottom: 12 }}>
                <Select options={readStatusOptions} placeholder="请选择阅读状态" allowClear />
              </Form.Item>
            </Col>
          )}
          <Col xs={24} sm={12} md={8} lg={6}>
            <Form.Item name="timeRange" label="发起时间" style={{ marginBottom: 12 }}>
              <RangePicker
                style={{ width: '100%' }}
                format="YYYY-MM-DD"
                ranges={{
                  今天: [dayjs().startOf('day'), dayjs().endOf('day')],
                  本周: [dayjs().startOf('week'), dayjs().endOf('week')],
                  本月: [dayjs().startOf('month'), dayjs().endOf('month')],
                  近三个月: [dayjs().subtract(3, 'month'), dayjs()]
                }}
              />
            </Form.Item>
          </Col>
          {expanded && (
            <Col xs={24} sm={24} md={16} lg={12}>
              <Form.Item name="keyword" label="关键字" style={{ marginBottom: 12 }}>
                <Input
                  prefix={<SearchOutlined />}
                  placeholder="搜索标题/单号/发起人"
                  allowClear
                />
              </Form.Item>
            </Col>
          )}
        </Row>
        <Row>
          <Col span={24} style={{ textAlign: 'right' }}>
            <Space>
              <Button
                type="text"
                icon={<FilterOutlined />}
                onClick={() => setExpanded(!expanded)}
              >
                {expanded ? '收起' : '展开'}
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
            </Space>
          </Col>
        </Row>
      </Form>
    </div>
  )
}

export default TaskFilterBar
