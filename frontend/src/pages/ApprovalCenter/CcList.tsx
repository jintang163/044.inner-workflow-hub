import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Table, Space, Button, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import TaskFilterBar, { FilterValues } from './components/TaskFilterBar'
import { getCcColumns } from './components/TaskTableColumns'
import { approvalApi } from '@/api'
import type { CcTaskVO } from '@/types/approval'
import type { PageResult } from '@/types'
import dayjs from 'dayjs'

const mockCcData = (pageNum: number, pageSize: number): PageResult<CcTaskVO> => {
  const mockProcesses = ['请假审批', '报销审批', '采购申请', '合同审批', '用印申请']
  const mockNodes = ['部门主管审批', '财务审批', '总监审批', '总经理审批']
  const mockNames = ['张三', '李四', '王五', '赵六', '钱七', '孙八', '周九', '吴十']
  const mockDepts = ['技术部', '产品部', '市场部', '财务部', '人事部', '运营部']
  const priorities: [1, 2, 3] = [1, 2, 3]
  const titles = [
    '2024年6月年假申请（5天）',
    '差旅费报销单-北京出差',
    '办公用品采购申请-电脑设备',
    '客户合同审批-XX科技合作协议',
    '公章使用申请-投标文件',
    '团建活动经费申请',
    '项目立项审批-AI智能助手项目'
  ]

  const list: CcTaskVO[] = []
  const total = 48
  const start = (pageNum - 1) * pageSize
  const end = Math.min(start + pageSize, total)

  for (let i = start; i < end; i++) {
    const priority = priorities[i % 3]
    const readStatus = (i % 3 === 0 ? 0 : 1) as 0 | 1
    list.push({
      id: 40000 + i,
      taskNo: `TK${(202400000 + i).toString()}`,
      flowableTaskId: `task_cc_${i}`,
      instanceId: 70000 + i,
      instanceNo: `AP${(2024060000 + i).toString()}`,
      processKey: `process_${i % 5}`,
      processName: mockProcesses[i % mockProcesses.length],
      title: titles[i % titles.length] + ` #${i + 1}`,
      nodeId: `node_${i % 4}`,
      nodeName: mockNodes[i % mockNodes.length],
      assigneeId: 1,
      assigneeName: '当前用户',
      assignTime: dayjs().subtract(i * 8, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      dueTime: dayjs().add(72, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      taskStatus: 'DONE',
      priority,
      startUserId: (i % 10) + 1,
      startUserName: mockNames[i % mockNames.length],
      startDeptName: mockDepts[i % mockDepts.length],
      startTime: dayjs().subtract(i * 8 + 2, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      formData: {},
      canAddSign: false,
      canTransfer: false,
      canDelegate: false,
      canReject: false,
      needSignature: false,
      needComment: false,
      readStatus,
      ccTime: dayjs().subtract(i * 8, 'hour').format('YYYY-MM-DD HH:mm:ss')
    })
  }

  return { list, total, pageNum, pageSize }
}

const CcList: React.FC = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<PageResult<CcTaskVO>>({ list: [], total: 0, pageNum: 1, pageSize: 10 })
  const [filterValues, setFilterValues] = useState<FilterValues>({})
  const [keyword, setKeyword] = useState<string | undefined>(undefined)

  const fetchData = useCallback(async (pageNum = 1, pageSize = 10, filters?: FilterValues) => {
    setLoading(true)
    try {
      const params: any = { pageNum, pageSize }
      if (filters?.category?.length) params.categoryId = filters.category[filters.category.length - 1]
      if (filters?.processKey) params.processKey = filters.processKey
      if (filters?.readStatus !== undefined) params.readStatus = filters.readStatus
      if (filters?.timeRange) {
        params.ccTimeBegin = filters.timeRange[0]?.format('YYYY-MM-DD HH:mm:ss')
        params.ccTimeEnd = filters.timeRange[1]?.format('YYYY-MM-DD HH:mm:ss')
      }
      if (filters?.keyword) {
        params.keyword = filters.keyword
        setKeyword(filters.keyword)
      } else {
        setKeyword(undefined)
      }

      // const res = await approvalApi.ccList(params)
      // setData(res)
      setData(mockCcData(pageNum, pageSize))
    } catch (err: any) {
      message.error(err?.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(1, 10)
  }, [fetchData])

  const handleViewDetail = async (record: CcTaskVO) => {
    if (record.readStatus === 0) {
      try {
        await approvalApi.markCcRead(record.id)
        const newList = data.list.map(item =>
          item.id === record.id ? { ...item, readStatus: 1 as const } : item
        )
        setData({ ...data, list: newList })
      } catch (_) {}
    }
    navigate(`/approval/detail/${record.instanceNo}`)
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

  const columns = getCcColumns({
    keyword,
    onViewDetail: handleViewDetail
  })

  const unreadCount = data.list.filter(t => t.readStatus === 0).length

  return (
    <div style={{ padding: 16 }}>
      <TaskFilterBar
        showReadStatus
        onSearch={handleSearch}
        onReset={handleReset}
      />

      <Card
        style={{ borderRadius: 8 }}
        bodyStyle={{ padding: 0 }}
        title={
          <Space>
            <span>抄送我的</span>
            {unreadCount > 0 && (
              <Button type="primary" size="small" danger ghost>
                {unreadCount} 条未读
              </Button>
            )}
          </Space>
        }
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchData(data.pageNum, data.pageSize, filterValues)}
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
          scroll={{ x: 1500 }}
          pagination={{
            current: data.pageNum,
            pageSize: data.pageSize,
            total: data.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条抄送记录`,
            onChange: handlePageChange
          }}
          rowClassName={(record) => record.readStatus === 0 ? 'ant-table-row-unread' : ''}
        />
      </Card>
      <style>{`
        .ant-table-row-unread td {
          background-color: #fffbe6 !important;
        }
        .ant-table-row-unread:hover td {
          background-color: #fff7cc !important;
        }
      `}</style>
    </div>
  )
}

export default CcList
