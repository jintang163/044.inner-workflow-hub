import { Card, Tag, Progress, Button, Space, Tooltip, List, Alert } from 'antd'
import {
  BulbOutlined,
  CheckOutlined,
  CloseOutlined,
  ThunderboltOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import { AiRecommendationVO, AiFactorVO } from '@/types/ai'
import { message } from 'antd'
import { aiApi } from '@/api'
import { useState } from 'react'

interface Props {
  recommendation: AiRecommendationVO
  onAdopted?: (adopted: number) => void
  onIgnore?: () => void
}

const factorKeyMap: Record<string, { label: string; icon?: string }> = {
  amount_level: { label: '金额级别', icon: '💰' },
  department_rate: { label: '部门通过率', icon: '🏢' },
  initiator_level_rate: { label: '发起人级别', icon: '👤' },
  approver_approval_rate: { label: '审批人历史通过率', icon: '✅' },
  process_type: { label: '流程类型', icon: '📋' },
  priority: { label: '优先级', icon: '⚡' },
  business_line: { label: '业务线', icon: '🏷️' }
}

const getFactorColor = (weight: number) => {
  if (weight >= 0.25) return '#f5222d'
  if (weight >= 0.15) return '#fa8c16'
  if (weight >= 0.08) return '#1890ff'
  return '#52c41a'
}

export default function AiRecommendationCard({ recommendation, onAdopted, onIgnore }: Props) {
  const [processing, setProcessing] = useState(false)

  if (!recommendation) return null

  const isRecommendApprove = recommendation.recommendedAction === 1
  const probability = Math.round((recommendation.approveProbability || 0) * 100)
  const isAdopted = recommendation.adopted === 1
  const isIgnored = recommendation.adopted === 2

  const handleAdopt = async () => {
    if (isAdopted || isIgnored || processing) return
    setProcessing(true)
    try {
      await aiApi.recordAdoption(recommendation.id, 1)
      message.success('已采纳 AI 推荐')
      onAdopted?.(1)
    } catch {
      // ignore
    } finally {
      setProcessing(false)
    }
  }

  const handleIgnore = async () => {
    if (isAdopted || isIgnored || processing) return
    setProcessing(true)
    try {
      await aiApi.recordAdoption(recommendation.id, 2)
      message.info('已忽略 AI 推荐')
      onIgnore?.()
    } catch {
      // ignore
    } finally {
      setProcessing(false)
    }
  }

  return (
    <Card
      size="small"
      style={{
        background: isRecommendApprove
          ? 'linear-gradient(135deg, #f6ffed 0%, #fffbe6 100%)'
          : 'linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%)',
        borderColor: isRecommendApprove ? '#b7eb8f' : '#ffa39e',
        marginBottom: 16
      }}
      bodyStyle={{ padding: '12px 16px' }}
    >
      <Space direction="vertical" size={8} style={{ width: '100%' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Space size={8}>
            <BulbOutlined
              style={{ fontSize: 18, color: isRecommendApprove ? '#52c41a' : '#f5222d' }}
            />
            <span style={{ fontWeight: 600, fontSize: 15 }}>
              AI 智能推荐
              <Tag
                color={isRecommendApprove ? 'success' : 'error'}
                style={{ marginLeft: 8 }}
              >
                {isRecommendApprove ? '建议同意' : '建议拒绝'}
              </Tag>
            </span>
            {(isAdopted || isIgnored) && (
              <Tag color={isAdopted ? 'green' : 'default'}>
                {isAdopted ? '已采纳' : '已忽略'}
              </Tag>
            )}
          </Space>
          <Space size={4}>
            <Tooltip title={`模型版本: ${recommendation.modelVersion} | 推理耗时: ${recommendation.inferenceMs}ms`}>
              <InfoCircleOutlined style={{ color: '#999' }} />
            </Tooltip>
          </Space>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span style={{ color: '#666' }}>同意概率</span>
              <span style={{ fontWeight: 600, color: isRecommendApprove ? '#52c41a' : '#f5222d' }}>
                {probability}%
              </span>
            </div>
            <Progress
              percent={probability}
              showInfo={false}
              strokeColor={isRecommendApprove ? '#52c41a' : '#f5222d'}
              size="small"
            />
          </div>

          {!(isAdopted || isIgnored) && (
            <Space>
              <Button
                type="primary"
                size="small"
                icon={isRecommendApprove ? <CheckOutlined /> : <CloseOutlined />}
                onClick={handleAdopt}
                loading={processing}
              >
                一键采纳
              </Button>
              <Button size="small" onClick={handleIgnore} loading={processing}>
                忽略
              </Button>
            </Space>
          )}
        </div>

        {recommendation.reason && (
          <Alert
            type="info"
            showIcon
            icon={<ThunderboltOutlined />}
            message={recommendation.reason}
            style={{ marginTop: 4 }}
          />
        )}

        {recommendation.factors && recommendation.factors.length > 0 && (
          <div>
            <div style={{ fontSize: 13, color: '#666', marginBottom: 8, marginTop: 4 }}>
              影响因子分析：
            </div>
            <List
              size="small"
              dataSource={recommendation.factors}
              renderItem={(item: AiFactorVO) => (
                <List.Item
                  style={{
                    padding: '4px 0',
                    borderBottom: '1px dashed #e8e8e8',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                >
                  <Space size={8}>
                    <span style={{ fontSize: 14 }}>
                      {factorKeyMap[item.key]?.icon || '📊'}
                    </span>
                    <span style={{ color: '#333' }}>
                      {factorKeyMap[item.key]?.label || item.key}
                    </span>
                    <Tag color="blue" style={{ margin: 0 }}>
                      {item.value}
                    </Tag>
                  </Space>
                  <Space size={8}>
                    <Progress
                      percent={Math.round(item.weight * 100)}
                      size="small"
                      showInfo={false}
                      style={{ width: 60 }}
                      strokeColor={getFactorColor(item.weight)}
                    />
                    <span
                      style={{
                        color: getFactorColor(item.weight),
                        fontWeight: 600,
                        minWidth: 45,
                        textAlign: 'right'
                      }}
                    >
                      {Math.round(item.weight * 100)}%
                    </span>
                  </Space>
                </List.Item>
              )}
            />
          </div>
        )}
      </Space>
    </Card>
  )
}
