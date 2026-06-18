import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Table, Space, Button, message, Modal } from 'antd'
import { ReloadOutlined, PlusOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import type { TableRowSelection } from 'antd/es/table/interface'
import TaskFilterBar, { FilterValues } from './components/TaskFilterBar'
import { getTodoColumns } from './components/TaskTableColumns'
import BatchApprovalDrawer from './components/BatchApprovalDrawer'
import { approvalApi, aiApi } from '@/api'
import type { ApprovalTaskVO } from '@/types/approval'
import type { AiRecommendationVO } from '@/types/ai'
import type { PageResult } from '@/types'
import dayjs from 'dayjs'

const { confirm } = Modal

const mockAiRecommendation = (id: number, processName: string, amount: number): AiRecommendationVO => {
  const amountLevel = amount < 5000 ? 'low' : amount < 50000 ? 'medium' : 'high'
  const baseProbability = amountLevel === 'low' ? 0.92 : amountLevel === 'medium' ? 0.75 : 0.55
  const processBonus = processName.includes('请假') ? 0.1 : processName.includes('报销') ? 0.05 : 0
  const finalProbability = Math.min(0.98, Math.max(0.1, baseProbability + processBonus))
  const recommendedAction = finalProbability >= 0.7 ? 1 : 0

  const reasons = [
    '金额较低，历史审批记录良好，建议同意',
    '金额适中，该部门历史通过率较高，建议同意',
    '金额较大，需要谨慎评估，建议拒绝',
    '优先级高，业务紧急，建议同意',
    '发起人级别较高，历史审批记录优秀，建议同意'
  ]

  const amountText = amount < 5000 ? '低' : amount < 50000 ? '中' : '高'
  const deptRate = (0.6 + Math.random() * 0.35).toFixed(2)
  const approverRate = (0.7 + Math.random() * 0.25).toFixed(2)

  return {
    id: 100000 + id,
    taskId: id,
    instanceId: 50000 + id,
    approverId: 1,
    approveProbability: Number(finalProbability.toFixed(4)),
    recommendedAction,
    reason: recommendedAction === 1
      ? (amount < 5000 ? '金额低，历史审批通过率高，建议同意' : reasons[Math.floor(Math.random() * 2)])
      : reasons[2],
    factors: [
      { key: 'amount_level', value: amountText, weight: 0.35 },
      { key: 'department_rate', value: `${Math.round(Number(deptRate) * 100)}%`, weight: 0.25 },
      { key: 'approver_approval_rate', value: `${Math.round(Number(approverRate) * 100)}%`, weight: 0.2 },
      { key: 'process_type', value: processName, weight: 0.12 },
      { key: 'initiator_level_rate', value: '良好', weight: 0.08 }
    ],
    factorsJson: '',
    modelVersion: 'v1.0.0',
    inferenceMs: 45 + Math.floor(Math.random() * 30),
    adopted: Math.random() > 0.7 ? (Math.random() > 0.5 ? 1 : 2) : 0,
    createTime: dayjs().subtract(id, 'minute').format('YYYY-MM-DD HH:mm:ss')
  }
}

const mockTodoData = (pageNum: number, pageSize: number): PageResult<ApprovalTaskVO> => {
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

  const list: ApprovalTaskVO[] = []
  const total = 87
  const start = (pageNum - 1) * pageSize
  const end = Math.min(start + pageSize, total)

  for (let i = start; i < end; i++) {
    const priority = priorities[i % 3]
    const amount = [800, 2500, 8000, 35000, 120000, 500, 30000, 90000][i % 8]
    const processName = mockProcesses[i % mockProcesses.length]
    list.push({
      id: 10000 + i,
      taskNo: `TK${(202400000 + i).toString()}`,
      flowableTaskId: `task_${i}`,
      instanceId: 50000 + i,
      instanceNo: `AP${(2024060000 + i).toString()}`,
      processKey: `process_${i % 5}`,
      processName,
      title: titles[i % titles.length] + ` #${i + 1}`,
      nodeId: `node_${i % 4}`,
      nodeName: mockNodes[i % mockNodes.length],
      assigneeId: 1,
      assigneeName: '当前用户',
      assignTime: dayjs().subtract(i, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      dueTime: dayjs().add(priority === 1 ? 6 : priority === 2 ? 24 : 72, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      taskStatus: 'PENDING',
      priority,
      startUserId: (i % 10) + 1,
      startUserName: mockNames[i % mockNames.length],
      startDeptName: mockDepts[i % mockDepts.length],
      startTime: dayjs().subtract(i + 2, 'hour').format('YYYY-MM-DD HH:mm:ss'),
      formData: { amount },
      aiRecommendation: mockAiRecommendation(10000 + i, processName, amount),
      aiRecommendationId: 100000 + i,
      canAddSign: true,
      canTransfer: true,
      canDelegate: true,
      canReject: i > 2,
      needSignature: priority === 1,
      needComment: priority === 1 || i % 3 === 0
    })
  }

  return { list, total, pageNum, pageSize }
}

const TodoList: React.FC = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<PageResult<ApprovalTaskVO>>({ list: [], total: 0, pageNum: 1, pageSize: 10 })
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [selectedRows, setSelectedRows] = useState<ApprovalTaskVO[]>([])
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [filterValues, setFilterValues] = useState<FilterValues>({})
  const [keyword, setKeyword] = useState<string | undefined>(undefined)

  const fetchData = useCallback(async (pageNum = 1, pageSize = 10, filters?: FilterValues) => {
    setLoading(true)
    try {
      const params: any = { pageNum, pageSize }
      if (filters?.category?.length) params.categoryId = filters.category[filters.category.length - 1]
      if (filters?.processKey) params.processKey = filters.processKey
      if (filters?.taskStatus) params.taskStatus = filters.taskStatus
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

      // const res = await approvalApi.todoList(params)
      // setData(res)
      setData(mockTodoData(pageNum, pageSize))
    } catch (err: any) {
      message.error(err?.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(1, 10)
  }, [fetchData])

  const rowSelection: TableRowSelection<ApprovalTaskVO> = {
    selectedRowKeys,
    onChange: (keys, rows) => {
      setSelectedRowKeys(keys)
      setSelectedRows(rows)
    },
    getCheckboxProps: () => ({
      disabled: false
    })
  }

  const handleViewDetail = (record: ApprovalTaskVO) => {
    navigate(`/approval/detail/${record.instanceNo}`, { state: { task: record } })
  }

  const handleQuickApprove = async (record: ApprovalTaskVO) => {
    if (record.needComment || record.needSignature) {
      handleViewDetail(record)
      return
    }
    confirm({
      title: '快捷同意',
      icon: <ExclamationCircleOutlined />,
      content: `确定同意审批【${record.taskNo}】吗？`,
      okText: '确认同意',
      cancelText: '取消',
      onOk: async () => {
        try {
          setActionLoading(true)
          if (record.aiRecommendationId && record.aiRecommendation?.adopted === 0) {
            const isMatch = record.aiRecommendation?.recommendedAction === 1
            await aiApi.recordAdoption(record.aiRecommendationId, isMatch ? 1 : 2)
          }
          await approvalApi.approve({
            taskId: record.flowableTaskId,
            instanceId: record.instanceId
          })
          message.success('审批成功')
          fetchData(data.pageNum, data.pageSize, filterValues)
        } catch (err: any) {
          message.error(err?.message || '操作失败')
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

  const handleRemoveFromSelection = (task: ApprovalTaskVO) => {
    setSelectedRowKeys(prev => prev.filter(k => k !== task.id))
    setSelectedRows(prev => prev.filter(r => r.id !== task.id))
  }

  const handleBatchApprove = async (batchData: { comment?: string; signatureUrl?: string }) => {
    try {
      setActionLoading(true)
      await approvalApi.batchApprove({
        taskIds: selectedRows.map(r => r.flowableTaskId),
        comment: batchData.comment,
        signatureUrl: batchData.signatureUrl
      })
      message.success(`批量同意 ${selectedRows.length} 条任务成功`)
      setDrawerOpen(false)
      setSelectedRowKeys([])
      setSelectedRows([])
      fetchData(data.pageNum, data.pageSize, filterValues)
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleBatchReject = async (batchData: { comment: string }) => {
    try {
      setActionLoading(true)
      await approvalApi.batchReject({
        taskIds: selectedRows.map(r => r.flowableTaskId),
        comment: batchData.comment
      })
      message.success(`批量拒绝 ${selectedRows.length} 条任务成功`)
      setDrawerOpen(false)
      setSelectedRowKeys([])
      setSelectedRows([])
      fetchData(data.pageNum, data.pageSize, filterValues)
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const columns = getTodoColumns({
    keyword,
    onViewDetail: handleViewDetail,
    onQuickApprove: handleQuickApprove
  })

  return (
    <div style={{ padding: 16 }}>
      <TaskFilterBar
        onSearch={handleSearch}
        onReset={handleReset}
      />

      <Card
        style={{ borderRadius: 8 }}
        bodyStyle={{ padding: 0 }}
        title={
          <Space>
            <span>待办任务</span>
            <a onClick={() => navigate('/approval/apply')}>
              <Button type="primary" size="small" icon={<PlusOutlined />}>
                发起审批
              </Button>
            </a>
          </Space>
        }
        extra={
          <Space size={8}>
            <Button
              type={selectedRowKeys.length > 0 ? 'primary' : 'default'}
              disabled={selectedRowKeys.length === 0}
              onClick={() => setDrawerOpen(true)}
            >
              批量审批（{selectedRowKeys.length}）
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => fetchData(data.pageNum, data.pageSize, filterValues)}
            >
              刷新
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          loading={loading}
          dataSource={data.list}
          columns={columns}
          rowSelection={rowSelection}
          scroll={{ x: 1400 }}
          pagination={{
            current: data.pageNum,
            pageSize: data.pageSize,
            total: data.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条待办任务`,
            onChange: handlePageChange
          }}
        />
      </Card>

      <BatchApprovalDrawer
        open={drawerOpen}
        selectedTasks={selectedRows}
        onClose={() => setDrawerOpen(false)}
        onRemoveTask={handleRemoveFromSelection}
        onBatchApprove={handleBatchApprove}
        onBatchReject={handleBatchReject}
        loading={actionLoading}
      />
    </div>
  )
}

export default TodoList
