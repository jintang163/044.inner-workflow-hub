import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Table, Space, Button, message, Modal, Form, Input, Alert, List, Tag } from 'antd'
import {
  ReloadOutlined,
  PlusOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  SwapOutlined,
  BellOutlined,
  CloseCircleOutlined
} from '@ant-design/icons'
import type { TableRowSelection } from 'antd/es/table/interface'
import TaskFilterBar, { FilterValues } from './components/TaskFilterBar'
import { getTodoColumns } from './components/TaskTableColumns'
import BatchApprovalDrawer from './components/BatchApprovalDrawer'
import { approvalApi, aiApi } from '@/api'
import type { ApprovalTaskVO, BatchTransferDTO, BatchRemindResultVO } from '@/types/approval'
import type { PageResult } from '@/types'
import UserSelect from '@/components/business/UserSelect'

const { confirm } = Modal
const { TextArea } = Input

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
  const [transferModalVisible, setTransferModalVisible] = useState(false)
  const [transferAll, setTransferAll] = useState(false)
  const [transferForm] = Form.useForm()
  const [remindModalVisible, setRemindModalVisible] = useState(false)
  const [remindResult, setRemindResult] = useState<BatchRemindResultVO | null>(null)
  const [remindForm] = Form.useForm()

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

      const res = await approvalApi.todoList(params)
      setData(res)
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

  const handleTransferAll = () => {
    setTransferAll(true)
    transferForm.resetFields()
    setTransferModalVisible(true)
  }

  const handleTransferSelected = () => {
    setTransferAll(false)
    transferForm.resetFields()
    setTransferModalVisible(true)
  }

  const handleBatchTransfer = async () => {
    try {
      const values = await transferForm.validateFields()
      setActionLoading(true)

      const transferData: BatchTransferDTO = {
        taskIds: transferAll ? undefined : selectedRows.map(r => r.flowableTaskId),
        transferAll: transferAll,
        targetUserId: values.targetUserId,
        targetUserName: '',
        actionRemark: values.actionRemark
      }

      await approvalApi.batchTransfer(transferData)
      const successMsg = transferAll ? '一键转全部待办成功' : `批量转审 ${selectedRows.length} 条任务成功`
      message.success(successMsg)
      setTransferModalVisible(false)
      setSelectedRowKeys([])
      setSelectedRows([])
      fetchData(data.pageNum, data.pageSize, filterValues)
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleBatchRemind = () => {
    setRemindResult(null)
    remindForm.resetFields()
    setRemindModalVisible(true)
  }

  const handleRemindSubmit = async () => {
    try {
      const values = await remindForm.validateFields()
      setActionLoading(true)

      const result = await approvalApi.batchRemind({
        taskIds: selectedRows.map(r => r.id),
        remark: values.remark
      })

      setRemindResult(result)

      if (result.successCount > 0 && result.failCount === 0) {
        message.success(`批量催办成功，共发送 ${result.remindMessageCount} 条通知`)
        setRemindModalVisible(false)
        setSelectedRowKeys([])
        setSelectedRows([])
        fetchData(data.pageNum, data.pageSize, filterValues)
      } else if (result.successCount > 0) {
        message.warning(`部分催办成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
      } else {
        message.error(`批量催办全部失败`)
      }
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
              disabled={selectedRowKeys.length === 0}
              icon={<BellOutlined />}
              onClick={handleBatchRemind}
            >
              批量催办
            </Button>
            <Button
              disabled={selectedRowKeys.length === 0}
              icon={<SwapOutlined />}
              onClick={handleTransferSelected}
            >
              批量转审
            </Button>
            <Button
              icon={<SwapOutlined />}
              onClick={handleTransferAll}
            >
              一键转全部待办
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

      <Modal
        title={transferAll ? '一键转全部待办' : `批量转审（${selectedRows.length} 条任务）`}
        open={transferModalVisible}
        onOk={handleBatchTransfer}
        onCancel={() => setTransferModalVisible(false)}
        confirmLoading={actionLoading}
        okText="确认转审"
        cancelText="取消"
        width={480}
      >
        <Form form={transferForm} layout="vertical">
          <Form.Item
            name="targetUserId"
            label="转审给"
            rules={[{ required: true, message: '请选择转审对象' }]}
          >
            <UserSelect placeholder="请选择转审对象" multiple={false} />
          </Form.Item>

          <Form.Item name="actionRemark" label="转审原因">
            <TextArea rows={3} placeholder="请输入转审原因（可选）" maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          <Space>
            <BellOutlined style={{ color: '#faad14' }} />
            <span>批量催办（{selectedRows.length} 条任务）</span>
          </Space>
        }
        open={remindModalVisible}
        onOk={handleRemindSubmit}
        onCancel={() => {
          setRemindModalVisible(false)
          setRemindResult(null)
        }}
        confirmLoading={actionLoading}
        okText={remindResult ? '关闭' : '确认催办'}
        cancelText="取消"
        okButtonProps={remindResult ? { onClick: () => setRemindModalVisible(false) } : undefined}
        width={520}
      >
        {remindResult ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              message={
                <Space>
                  {remindResult.failCount === 0 ? (
                    <><CheckCircleOutlined style={{ color: '#52c41a' }} /> 全部催办成功</>
                  ) : remindResult.successCount === 0 ? (
                    <><CloseCircleOutlined style={{ color: '#ff4d4f' }} /> 全部催办失败</>
                  ) : (
                    <><ExclamationCircleOutlined style={{ color: '#faad14' }} /> 部分催办成功</>
                  )}
                </Space>
              }
              description={`成功 ${remindResult.successCount} 条，失败 ${remindResult.failCount} 条，发送通知 ${remindResult.remindMessageCount} 条`}
              type={remindResult.failCount === 0 ? 'success' : remindResult.successCount === 0 ? 'error' : 'warning'}
              showIcon={false}
            />
            {remindResult.failItems.length > 0 && (
              <div>
                <div style={{ fontWeight: 500, marginBottom: 8 }}>失败详情：</div>
                <List
                  size="small"
                  dataSource={remindResult.failItems}
                  style={{ maxHeight: 200, overflowY: 'auto' }}
                  renderItem={(item) => (
                    <List.Item>
                      <Space>
                        <span style={{ color: '#999' }}>{item.taskNo}</span>
                        <span style={{ color: '#ff4d4f' }}>{item.reason}</span>
                      </Space>
                    </List.Item>
                  )}
                />
              </div>
            )}
          </Space>
        ) : (
          <Form form={remindForm} layout="vertical">
            <Alert
              message="催办说明"
              description={
                <div>
                  <div>• 同一审批人多个待办将合并发送一条通知，避免刷屏</div>
                  <div>• 同一任务 5 分钟内只能催办一次</div>
                  <div>• 1 小时内最多催办 10 次</div>
                  <div>• 只有发起人或管理员可以催办</div>
                </div>
              }
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <Form.Item
              name="remark"
              label="催办留言"
            >
              <TextArea
                rows={3}
                placeholder="请输入催办留言（可选），如：请尽快处理此审批"
                maxLength={200}
                showCount
              />
            </Form.Item>
            <Alert
              message={
                <Space>
                  <span>已选择</span>
                  <Tag color="blue">{selectedRows.length} 条</Tag>
                  <span>待办任务进行催办</span>
                </Space>
              }
              type="info"
              showIcon={false}
            />
          </Form>
        )}
      </Modal>
    </div>
  )
}

export default TodoList
