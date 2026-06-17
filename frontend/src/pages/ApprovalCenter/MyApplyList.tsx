import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Table, Space, Button, message, Modal } from 'antd'
import { ReloadOutlined, PlusOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import TaskFilterBar, { FilterValues } from './components/TaskFilterBar'
import { getMyApplyColumns } from './components/TaskTableColumns'
import { approvalApi } from '@/api'
import type { ProcessInstanceVO } from '@/types/approval'
import type { PageResult } from '@/types'
import dayjs from 'dayjs'

const { confirm } = Modal

const mockMyApplyData = (pageNum: number, pageSize: number): PageResult<ProcessInstanceVO> => {
  const mockProcesses = ['请假审批', '报销审批', '采购申请', '合同审批', '用印申请']
  const mockNodes = ['部门主管审批', '财务审批', '总监审批', '总经理审批']
  const statuses: [1, 2, 3, 4] = [1, 2, 3, 4]
  const titles = [
    '2024年6月年假申请（5天）',
    '差旅费报销单-北京出差',
    '办公用品采购申请-电脑设备',
    '客户合同审批-XX科技合作协议',
    '公章使用申请-投标文件',
    '团建活动经费申请',
    '项目立项审批-AI智能助手项目'
  ]

  const list: ProcessInstanceVO[] = []
  const total = 92
  const start = (pageNum - 1) * pageSize
  const end = Math.min(start + pageSize, total)

  for (let i = start; i < end; i++) {
    const instanceStatus = statuses[i % 4]
    list.push({
      id: 30000 + i,
      instanceNo: `AP${(2024060000 + i).toString()}`,
      processKey: `process_${i % 5}`,
      processName: mockProcesses[i % mockProcesses.length],
      title: titles[i % titles.length] + ` #${i + 1}`,
      formId: 1000 + i,
      formVersion: 1,
      formData: {},
      instanceStatus,
      instanceStatusDesc: ['审批中', '已完成', '已驳回', '已撤回'][instanceStatus - 1],
      startUserId: 1,
      startUserName: '当前用户',
      startDeptName: '技术部',
      startTime: dayjs().subtract(i * 5, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      endTime: instanceStatus !== 1 ? dayjs().subtract(i * 5 - 2, 'hour').format('YYYY-MM-DD HH:mm:ss') : undefined,
      currentNodeIds: instanceStatus === 1 ? [`node_${i % 4 + 1}`] : [],
      currentNodeNames: instanceStatus === 1 ? [mockNodes[i % mockNodes.length]] : [],
      canWithdraw: instanceStatus === 1 && i % 3 === 0
    })
  }

  return { list, total, pageNum, pageSize }
}

const MyApplyList: React.FC = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [data, setData] = useState<PageResult<ProcessInstanceVO>>({ list: [], total: 0, pageNum: 1, pageSize: 10 })
  const [filterValues, setFilterValues] = useState<FilterValues>({})
  const [keyword, setKeyword] = useState<string | undefined>(undefined)

  const fetchData = useCallback(async (pageNum = 1, pageSize = 10, filters?: FilterValues) => {
    setLoading(true)
    try {
      const params: any = { pageNum, pageSize }
      if (filters?.category?.length) params.categoryId = filters.category[filters.category.length - 1]
      if (filters?.processKey) params.processKey = filters.processKey
      if (filters?.instanceStatus) params.instanceStatus = filters.instanceStatus
      if (filters?.timeRange) {
        params.startTimeBegin = filters.timeRange[0]?.format('YYYY-MM-DD HH:mm:ss')
        params.startTimeEnd = filters.timeRange[1]?.format('YYYY-MM-DD HH:mm:ss')
      }
      if (filters?.keyword) {
        params.keyword = filters.keyword
        setKeyword(filters.keyword)
      } else {
        setKeyword(undefined)
      }

      // const res = await approvalApi.myApplyList(params)
      // setData(res)
      setData(mockMyApplyData(pageNum, pageSize))
    } catch (err: any) {
      message.error(err?.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(1, 10)
  }, [fetchData])

  const handleViewDetail = (record: ProcessInstanceVO) => {
    navigate(`/approval/detail/${record.instanceNo}`)
  }

  const handleWithdraw = (record: ProcessInstanceVO) => {
    confirm({
      title: '撤回申请',
      icon: <ExclamationCircleOutlined />,
      content: `确定撤回【${record.instanceNo}】的审批申请吗？撤回后可修改重新提交。`,
      okText: '确认撤回',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          setActionLoading(true)
          await approvalApi.withdraw({
            instanceId: record.id
          })
          message.success('撤回成功')
          fetchData(data.pageNum, data.pageSize, filterValues)
        } catch (err: any) {
          message.error(err?.message || '撤回失败')
        } finally {
          setActionLoading(false)
        }
      }
    })
  }

  const handleSearch = (values: FilterValues) => {
    setFilterValues(values)
    fetchData(1, data.pageSize, values)
  }

  const handleReset = () => {
    setFilterValues({})
    fetchData(1, data.pageSize)
  }

  const handlePageChange = (page: number, pageSize: number) => {
    fetchData(page, pageSize, filterValues)
  }

  const columns = getMyApplyColumns({
    keyword,
    onViewDetail: handleViewDetail,
    onWithdraw: handleWithdraw
  })

  return (
    <div style={{ padding: 16 }}>
      <TaskFilterBar
        showInstanceStatus
        onSearch={handleSearch}
        onReset={handleReset}
      />

      <Card
        style={{ borderRadius: 8 }}
        bodyStyle={{ padding: 0 }}
        title={
          <Space>
            <span>我发起的</span>
            <a onClick={() => navigate('/approval/apply')}>
              <Button type="primary" size="small" icon={<PlusOutlined />}>
                发起审批
              </Button>
            </a>
          </Space>
        }
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchData(data.pageNum, data.pageSize, filterValues)}
            loading={actionLoading}
          >
            刷新
          </Button>
        }
      >
        <Table
          rowKey="id"
          loading={loading}
          dataSource={data.list}
          columns={columns}
          scroll={{ x: 1400 }}
          pagination={{
            current: data.pageNum,
            pageSize: data.pageSize,
            total: data.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条申请记录`,
            onChange: handlePageChange
          }}
        />
      </Card>
    </div>
  )
}

export default MyApplyList
