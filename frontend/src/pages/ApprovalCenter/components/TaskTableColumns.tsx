import React, { useState, useEffect } from 'react'
import { Avatar, Tag, Tooltip, Button, Space, Typography, message, Progress } from 'antd'
import { CopyOutlined, CheckCircleOutlined, ClockCircleOutlined, UserOutlined, BulbOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import type { ApprovalTaskVO, Priority } from '@/types/approval'
import type { AiRecommendationVO } from '@/types/ai'

const { Text } = Typography

const priorityColorMap: Record<Priority, { color: string; label: string }> = {
  1: { color: 'red', label: '高' },
  2: { color: 'orange', label: '中' },
  3: { color: 'green', label: '低' }
}

const approvalResultColorMap: Record<string, string> = {
  APPROVE: 'green',
  REJECT: 'red',
  TRANSFER: 'blue',
  ADD_SIGN: 'purple',
  DELEGATE: 'cyan',
  REJECT_TO_NODE: 'red',
  WITHDRAW: 'default'
}

const approvalResultLabelMap: Record<string, string> = {
  APPROVE: '同意',
  REJECT: '拒绝',
  TRANSFER: '转审',
  ADD_SIGN: '加签',
  DELEGATE: '委派',
  REJECT_TO_NODE: '驳回',
  WITHDRAW: '撤回'
}

const instanceStatusColorMap: Record<number, string> = {
  1: 'processing',
  2: 'success',
  3: 'error',
  4: 'default'
}

const instanceStatusLabelMap: Record<number, string> = {
  1: '审批中',
  2: '已完成',
  3: '已驳回',
  4: '已撤回'
}

export const formatDuration = (ms?: number): string => {
  if (!ms || ms <= 0) return '0分'
  const days = Math.floor(ms / (1000 * 60 * 60 * 24))
  const hours = Math.floor((ms % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((ms % (1000 * 60 * 60)) / (1000 * 60))
  let result = ''
  if (days > 0) result += `${days}天`
  if (hours > 0) result += `${hours}小时`
  if (days === 0 && minutes > 0) result += `${minutes}分`
  return result || '0分'
}

export const useCountdown = (dueTime?: string): { text: string; isUrgent: boolean; isTimeout: boolean } => {
  const [now, setNow] = useState(dayjs())

  useEffect(() => {
    const timer = setInterval(() => {
      setNow(dayjs())
    }, 60000)
    return () => clearInterval(timer)
  }, [])

  if (!dueTime) {
    return { text: '-', isUrgent: false, isTimeout: false }
  }

  const diff = dayjs(dueTime).valueOf() - now.valueOf()
  const isTimeout = diff <= 0
  const isUrgent = !isTimeout && diff < 1000 * 60 * 60 * 24

  if (isTimeout) {
    return { text: '已超时', isUrgent: false, isTimeout: true }
  }

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

  let text = ''
  if (days > 0) text += `${days}天`
  if (hours > 0) text += `${hours}小时`
  if (days === 0 && minutes > 0) text += `${minutes}分`
  if (!text) text = '即将超时'

  return { text, isUrgent, isTimeout }
}

export const getPriorityTag = (priority: Priority) => {
  const { color, label } = priorityColorMap[priority] || priorityColorMap[2]
  return <Tag color={color} style={{ margin: 0 }}>{label}</Tag>
}

export const getApprovalResultTag = (result?: string) => {
  if (!result) return '-'
  return (
    <Tag color={approvalResultColorMap[result] || 'default'}>
      {approvalResultLabelMap[result] || result}
    </Tag>
  )
}

export const getInstanceStatusTag = (status: number) => {
  return (
    <Tag color={instanceStatusColorMap[status]}>
      {instanceStatusLabelMap[status] || '未知'}
    </Tag>
  )
}

export const highlightText = (text: string, keyword?: string): React.ReactNode => {
  if (!keyword || !text) return text
  const index = text.toLowerCase().indexOf(keyword.toLowerCase())
  if (index === -1) return text
  return (
    <>
      {text.slice(0, index)}
      <span style={{ backgroundColor: '#fffbe6', color: '#d48806' }}>
        {text.slice(index, index + keyword.length)}
      </span>
      {text.slice(index + keyword.length)}
    </>
  )
}

export const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    message.success('复制成功')
  })
}

interface RemainingTimeCellProps {
  dueTime?: string
}

const RemainingTimeCell: React.FC<RemainingTimeCellProps> = ({ dueTime }) => {
  const { text, isUrgent, isTimeout } = useCountdown(dueTime)
  const color = isTimeout ? '#ff4d4f' : isUrgent ? '#fa8c16' : undefined
  const icon = isTimeout ? undefined : isUrgent ? <ClockCircleOutlined /> : <ClockCircleOutlined />

  return (
    <Space size={4}>
      {!isTimeout && icon}
      <span style={{ color }}>{text}</span>
    </Space>
  )
}

interface TodoColumnsOptions {
  keyword?: string
  onViewDetail: (record: ApprovalTaskVO) => void
  onQuickApprove?: (record: ApprovalTaskVO) => void
  selectionType?: 'checkbox' | 'radio' | false
}

interface DoneColumnsOptions {
  keyword?: string
  onViewDetail: (record: ApprovalTaskVO) => void
}

interface MyApplyColumnsOptions {
  keyword?: string
  onViewDetail: (record: any) => void
  onWithdraw?: (record: any) => void
}

interface CcColumnsOptions {
  keyword?: string
  onViewDetail: (record: any) => void
}

export const getTodoColumns = (options: TodoColumnsOptions): ColumnsType<ApprovalTaskVO> => {
  const { keyword, onViewDetail, onQuickApprove } = options
  return [
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 70,
      align: 'center',
      render: (priority: Priority) => getPriorityTag(priority)
    },
    {
      title: 'AI推荐',
      dataIndex: 'aiRecommendation',
      width: 110,
      align: 'center',
      render: (rec: AiRecommendationVO, record) => {
        if (!rec || record.taskStatus !== 'PENDING') {
          return <Tag color="default">待计算</Tag>
        }
        const isApprove = rec.recommendedAction === 1
        const probability = Math.round((rec.approveProbability || 0) * 100)
        const isAdopted = rec.adopted === 1
        const isIgnored = rec.adopted === 2
        return (
          <Tooltip
            title={
              <div style={{ maxWidth: 240 }}>
                <div style={{ marginBottom: 4, fontWeight: 600 }}>
                  {isApprove ? '建议同意' : '建议拒绝'} · {probability}%
                </div>
                {rec.reason && <div style={{ color: '#d9d9d9' }}>{rec.reason}</div>}
                {isAdopted && <div style={{ color: '#52c41a', marginTop: 4 }}>✓ 已采纳</div>}
                {isIgnored && <div style={{ color: '#8c8c8c', marginTop: 4 }}>— 已忽略</div>}
              </div>
            }
          >
            <Space size={4} direction="vertical" style={{ width: '100%' }}>
              <Tag color={isApprove ? 'success' : 'error'} style={{ margin: 0 }}>
                <BulbOutlined style={{ marginRight: 2 }} />
                {isApprove ? '同意' : '拒绝'} {probability}%
              </Tag>
              {isAdopted && <Tag color="green" style={{ margin: 0 }}>已采纳</Tag>}
              {isIgnored && <Tag color="default" style={{ margin: 0 }}>已忽略</Tag>}
            </Space>
          </Tooltip>
        )
      }
    },
    {
      title: '审批单号',
      dataIndex: 'taskNo',
      width: 180,
      render: (taskNo: string, record) => (
        <Space size={4}>
          <a onClick={() => onViewDetail(record)}>
            {highlightText(taskNo, keyword)}
          </a>
          <Tooltip title="复制单号">
            <CopyOutlined
              style={{ color: '#999', cursor: 'pointer' }}
              onClick={() => copyToClipboard(taskNo)}
            />
          </Tooltip>
        </Space>
      )
    },
    {
      title: '标题',
      dataIndex: 'title',
      minWidth: 200,
      ellipsis: { showTitle: false },
      render: (title: string) => (
        <Tooltip title={title} placement="topLeft">
          <Text ellipsis={{ rows: 2 }} style={{ width: '100%', display: 'block' }}>
            {highlightText(title, keyword)}
          </Text>
        </Tooltip>
      )
    },
    {
      title: '流程名称',
      dataIndex: 'processName',
      width: 140,
      render: (processName: string) => (
        <Tag color="blue" style={{ margin: 0 }}>{processName}</Tag>
      )
    },
    {
      title: '当前节点',
      dataIndex: 'nodeName',
      width: 120,
      render: (nodeName: string) => <Tag color="purple">{nodeName}</Tag>
    },
    {
      title: '发起人',
      dataIndex: 'startUserName',
      width: 140,
      render: (name: string, record) => (
        <Space size={6}>
          <Avatar size={24} src={record.startUserAvatar} icon={<UserOutlined />}>
            {name?.charAt(0)}
          </Avatar>
          <Text>{highlightText(name, keyword)}</Text>
        </Space>
      )
    },
    {
      title: '发起部门',
      dataIndex: 'startDeptName',
      width: 140,
      ellipsis: true
    },
    {
      title: '发起时间',
      dataIndex: 'startTime',
      width: 160,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm')
    },
    {
      title: '剩余时间',
      dataIndex: 'dueTime',
      width: 120,
      render: (dueTime: string) => <RemainingTimeCell dueTime={dueTime} />
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 160,
      fixed: 'right',
      render: (_: any, record) => (
        <Space size={8}>
          <Button type="primary" size="small" onClick={() => onViewDetail(record)}>
            审批
          </Button>
          {onQuickApprove && (
            <Tooltip title="快捷同意（无需填意见）">
              <Button
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() => onQuickApprove(record)}
              >
                同意
              </Button>
            </Tooltip>
          )}
        </Space>
      )
    }
  ]
}

export const getDoneColumns = (options: DoneColumnsOptions): ColumnsType<ApprovalTaskVO> => {
  const { keyword, onViewDetail } = options
  const baseColumns = getTodoColumns({ keyword, onViewDetail })
  return baseColumns
    .filter(col => (col as any).dataIndex !== 'dueTime' && (col as any).dataIndex !== 'action')
    .concat([
      {
        title: '审批结果',
        dataIndex: 'approvalResult',
        width: 100,
        render: (result?: string) => getApprovalResultTag(result)
      },
      {
        title: '处理时间',
        dataIndex: 'handleTime',
        width: 160,
        render: (time: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
      },
      {
        title: '我的意见',
        dataIndex: 'myComment',
        width: 160,
        ellipsis: true,
        render: (comment: string) => (
          <Tooltip title={comment} placement="topLeft">
            <Text ellipsis>{comment || '-'}</Text>
          </Tooltip>
        )
      },
      {
        title: '操作',
        dataIndex: 'action',
        width: 100,
        fixed: 'right',
        render: (_: any, record) => (
          <Button type="link" size="small" onClick={() => onViewDetail(record)}>
            查看详情
          </Button>
        )
      }
    ]) as ColumnsType<ApprovalTaskVO>
}

export const getMyApplyColumns = (options: MyApplyColumnsOptions): ColumnsType<any> => {
  const { keyword, onViewDetail, onWithdraw } = options
  return [
    {
      title: '审批单号',
      dataIndex: 'instanceNo',
      width: 180,
      render: (instanceNo: string, record) => (
        <Space size={4}>
          <a onClick={() => onViewDetail(record)}>
            {highlightText(instanceNo, keyword)}
          </a>
          <Tooltip title="复制单号">
            <CopyOutlined
              style={{ color: '#999', cursor: 'pointer' }}
              onClick={() => copyToClipboard(instanceNo)}
            />
          </Tooltip>
        </Space>
      )
    },
    {
      title: '标题',
      dataIndex: 'title',
      minWidth: 200,
      ellipsis: { showTitle: false },
      render: (title: string) => (
        <Tooltip title={title} placement="topLeft">
          <Text ellipsis={{ rows: 2 }} style={{ width: '100%', display: 'block' }}>
            {highlightText(title, keyword)}
          </Text>
        </Tooltip>
      )
    },
    {
      title: '流程名称',
      dataIndex: 'processName',
      width: 140,
      render: (processName: string) => <Tag color="blue">{processName}</Tag>
    },
    {
      title: '流程状态',
      dataIndex: 'instanceStatus',
      width: 100,
      render: (status: number) => getInstanceStatusTag(status)
    },
    {
      title: '当前节点',
      dataIndex: 'currentNodeNames',
      width: 140,
      render: (names?: string[]) => names?.length ? names.join('、') : '-'
    },
    {
      title: '发起时间',
      dataIndex: 'startTime',
      width: 160,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm')
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      width: 160,
      render: (time?: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 160,
      fixed: 'right',
      render: (_: any, record) => (
        <Space size={8}>
          {onWithdraw && record.canWithdraw && (
            <Button
              size="small"
              danger
              onClick={() => onWithdraw(record)}
            >
              撤回
            </Button>
          )}
          <Button type="link" size="small" onClick={() => onViewDetail(record)}>
            查看详情
          </Button>
        </Space>
      )
    }
  ]
}

export const getCcColumns = (options: CcColumnsOptions): ColumnsType<any> => {
  const { keyword, onViewDetail } = options
  return [
    {
      title: '',
      dataIndex: 'readStatus',
      width: 12,
      render: (status: number) => (
        <div
          style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            backgroundColor: status === 0 ? '#ff4d4f' : 'transparent',
            display: 'inline-block'
          }}
        />
      )
    },
    {
      title: '审批单号',
      dataIndex: 'instanceNo',
      width: 180,
      render: (instanceNo: string, record) => (
        <Space size={4}>
          <a onClick={() => onViewDetail(record)}>
            {highlightText(instanceNo, keyword)}
          </a>
          <Tooltip title="复制单号">
            <CopyOutlined
              style={{ color: '#999', cursor: 'pointer' }}
              onClick={() => copyToClipboard(instanceNo)}
            />
          </Tooltip>
        </Space>
      )
    },
    {
      title: '标题',
      dataIndex: 'title',
      minWidth: 200,
      ellipsis: { showTitle: false },
      render: (title: string) => (
        <Tooltip title={title} placement="topLeft">
          <Text
            ellipsis={{ rows: 2 }}
            style={{
              width: '100%',
              display: 'block',
              fontWeight: (keyword: any) => undefined
            }}
          >
            {highlightText(title, keyword)}
          </Text>
        </Tooltip>
      )
    },
    {
      title: '流程名称',
      dataIndex: 'processName',
      width: 140,
      render: (processName: string) => <Tag color="blue">{processName}</Tag>
    },
    {
      title: '发起人',
      dataIndex: 'startUserName',
      width: 140,
      render: (name: string, record) => (
        <Space size={6}>
          <Avatar size={24} src={record.startUserAvatar} icon={<UserOutlined />}>
            {name?.charAt(0)}
          </Avatar>
          <Text>{highlightText(name, keyword)}</Text>
        </Space>
      )
    },
    {
      title: '发起部门',
      dataIndex: 'startDeptName',
      width: 140,
      ellipsis: true
    },
    {
      title: '抄送时间',
      dataIndex: 'ccTime',
      width: 160,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm')
    },
    {
      title: '阅读状态',
      dataIndex: 'readStatus',
      width: 100,
      render: (status: number) => (
        <Tag color={status === 0 ? 'red' : 'default'}>
          {status === 0 ? '未读' : '已读'}
        </Tag>
      )
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 100,
      fixed: 'right',
      render: (_: any, record) => (
        <Button type="link" size="small" onClick={() => onViewDetail(record)}>
          查看详情
        </Button>
      )
    }
  ]
}
