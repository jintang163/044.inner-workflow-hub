import React, { useEffect, useRef, useState, useCallback } from 'react'
import { Spin, Empty, Drawer, Descriptions, Tag, Avatar, Space, Typography, Tooltip, Button, Divider } from 'antd'
import {
  UserOutlined,
  ClockCircleOutlined,
  ThunderboltOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  FullscreenOutlined,
  HistoryOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import type { TrackingMapVO, TrackingNodeVO } from '@/types/approval'

const { Text, Paragraph } = Typography

interface ApprovalTrackingMapProps {
  trackingMap?: TrackingMapVO
  loading?: boolean
  height?: number
}

const formatDuration = (ms?: number | null): string => {
  if (ms == null || ms === 0) return '-'
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds}秒`
  const minutes = Math.floor(seconds / 60)
  const remainSeconds = seconds % 60
  if (minutes < 60) return `${minutes}分${remainSeconds > 0 ? remainSeconds + '秒' : ''}`
  const hours = Math.floor(minutes / 60)
  const remainMinutes = minutes % 60
  if (hours < 24) return `${hours}小时${remainMinutes > 0 ? remainMinutes + '分' : ''}`
  const days = Math.floor(hours / 24)
  const remainHours = hours % 24
  return `${days}天${remainHours > 0 ? remainHours + '小时' : ''}`
}

const getNodeColor = (node: TrackingNodeVO): string => {
  if (node.status === 'active') return '#1890ff'
  if (node.isBottleneck) return '#ff4d4f'
  if (node.durationDeviation != null) {
    if (node.durationDeviation < -0.3) return '#52c41a'
    if (node.durationDeviation > 0.3) return '#faad14'
  }
  return '#69c0ff'
}

const getNodeStatusLabel = (node: TrackingNodeVO) => {
  if (node.status === 'active') return <Tag color="processing">进行中</Tag>
  if (node.status === 'completed') return <Tag color="success">已完成</Tag>
  return <Tag>{node.statusName}</Tag>
}

const getActionColor = (actionName?: string) => {
  if (!actionName) return 'default'
  if (actionName.includes('同意') || actionName.includes('通过')) return 'green'
  if (actionName.includes('拒绝') || actionName.includes('驳回')) return 'red'
  if (actionName.includes('转审')) return 'blue'
  if (actionName.includes('加签')) return 'purple'
  if (actionName.includes('委派')) return 'cyan'
  return 'default'
}

const ApprovalTrackingMap: React.FC<ApprovalTrackingMapProps> = ({
  trackingMap,
  loading = false,
  height = 520
}) => {
  const containerRef = useRef<HTMLDivElement>(null)
  const graphRef = useRef<any>(null)
  const [selectedNode, setSelectedNode] = useState<TrackingNodeVO | null>(null)
  const [drawerVisible, setDrawerVisible] = useState(false)

  const handleNodeClick = useCallback((node: TrackingNodeVO) => {
    setSelectedNode(node)
    setDrawerVisible(true)
  }, [])

  useEffect(() => {
    if (!containerRef.current || !trackingMap || trackingMap.nodes.length === 0) return

    let cancelled = false

    const initGraph = async () => {
      try {
        const G6 = (await import('@antv/g6')).default

        if (cancelled || !containerRef.current) return

        if (graphRef.current) {
          graphRef.current.destroy()
          graphRef.current = null
        }

        const container = containerRef.current
        const width = container.offsetWidth
        const graphHeight = typeof height === 'number' ? height : 520

        const nodes = trackingMap.nodes.map((node) => {
          const color = getNodeColor(node)
          const isUserTask = node.nodeCategory === 1
          const operatorNames = node.operators
            .filter(o => o.userName)
            .map(o => o.userName)
            .join(', ')

          const actionNameStr = node.actionName || ''
          const startTimeStr = node.startTime
            ? dayjs(node.startTime).format('MM-DD HH:mm')
            : ''

          let labelText = node.nodeName || node.nodeId
          if (isUserTask && operatorNames) {
            labelText += `\n${operatorNames}`
          }
          if (actionNameStr) {
            labelText += `\n${actionNameStr}`
          }
          if (startTimeStr) {
            labelText += `\n${startTimeStr}`
          }
          if (node.duration != null) {
            labelText += ` · ${formatDuration(node.duration)}`
          }

          return {
            id: node.nodeId,
            type: isUserTask ? 'rect' : 'circle',
            size: isUserTask ? [200, 80] : 48,
            label: labelText,
            labelCfg: {
              style: {
                fill: '#333',
                fontSize: 11,
                textAlign: 'center' as const,
                textBaseline: 'middle' as const
              },
              position: 'bottom' as const,
              offset: isUserTask ? 0 : 36
            },
            style: {
              fill: node.status === 'active' ? '#e6f7ff' : (node.isBottleneck ? '#fff1f0' : '#f6ffed'),
              stroke: color,
              lineWidth: node.status === 'active' ? 3 : 2,
              radius: isUserTask ? 8 : 24
            },
            stateStyles: {
              hover: {
                lineWidth: 3,
                shadowColor: color,
                shadowBlur: 12
              },
              selected: {
                lineWidth: 3,
                shadowColor: color,
                shadowBlur: 16
              }
            },
            meta: node
          }
        })

        const edgeSet = new Set<string>()
        const edges = trackingMap.edges
          .filter(edge => {
            const key = `${edge.sourceId}-${edge.targetId}`
            if (edgeSet.has(key)) return false
            edgeSet.add(key)
            return true
          })
          .map(edge => ({
            source: edge.sourceId,
            target: edge.targetId,
            type: 'cubic-horizontal',
            style: {
              stroke: '#1890ff',
              lineWidth: 2,
              endArrow: { path: 'M 0,0 L 8,4 L 8,-4 Z', fill: '#1890ff' }
            },
            label: edge.label || '',
            labelCfg: {
              style: {
                fill: '#1890ff',
                fontSize: 10,
                background: {
                  fill: '#fff',
                  padding: [2, 4, 2, 4],
                  radius: 2
                }
              }
            }
          }))

        const graph = new G6.Graph({
          container,
          width,
          height: graphHeight,
          modes: {
            default: ['drag-canvas', 'zoom-canvas', 'drag-node', 'click-select']
          },
          layout: {
            type: 'dagre',
            rankdir: 'LR',
            nodesep: 40,
            ranksep: 100,
            workerEnabled: true
          },
          defaultNode: {
            style: { radius: 8 }
          },
          defaultEdge: {
            type: 'cubic-horizontal'
          },
          nodeStateStyles: {
            hover: { lineWidth: 3 },
            selected: { lineWidth: 3 }
          },
          fitView: true,
          fitViewPadding: 40,
          animate: true,
          animateCfg: { duration: 200 }
        })

        graph.data({ nodes, edges })
        graph.render()

        graph.on('node:click', (evt: any) => {
          const model = evt.item?.getModel()
          if (model?.meta) {
            handleNodeClick(model.meta)
          }
        })

        graph.on('node:mouseenter', (evt: any) => {
          graph.setItemState(evt.item, 'hover', true)
        })

        graph.on('node:mouseleave', (evt: any) => {
          graph.setItemState(evt.item, 'hover', false)
        })

        graphRef.current = graph

        const resizeObserver = new ResizeObserver(() => {
          if (graphRef.current && !graphRef.current.get('destroyed')) {
            const newWidth = container.offsetWidth
            graphRef.current.changeSize(newWidth, graphHeight)
            graphRef.current.fitView(40)
          }
        })
        resizeObserver.observe(container)

      } catch (err) {
        console.error('G6 初始化失败:', err)
      }
    }

    initGraph()

    return () => {
      cancelled = true
      if (graphRef.current) {
        try { graphRef.current.destroy() } catch (_) {}
        graphRef.current = null
      }
    }
  }, [trackingMap, height, handleNodeClick])

  const handleZoomIn = () => {
    if (graphRef.current && !graphRef.current.get('destroyed')) {
      const zoom = graphRef.current.getZoom()
      graphRef.current.zoomTo(Math.min(zoom * 1.2, 3))
    }
  }

  const handleZoomOut = () => {
    if (graphRef.current && !graphRef.current.get('destroyed')) {
      const zoom = graphRef.current.getZoom()
      graphRef.current.zoomTo(Math.max(zoom / 1.2, 0.2))
    }
  }

  const handleFitView = () => {
    if (graphRef.current && !graphRef.current.get('destroyed')) {
      graphRef.current.fitView(40)
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height }}>
        <Spin size="large" tip="加载跟踪地图..." />
      </div>
    )
  }

  if (!trackingMap || trackingMap.nodes.length === 0) {
    return <Empty description="暂无跟踪数据" />
  }

  return (
    <div style={{ position: 'relative' }}>
      <div style={{
        display: 'flex',
        gap: 8,
        padding: '8px 12px',
        background: '#fafafa',
        borderRadius: '6px 6px 0 0',
        borderBottom: '1px solid #f0f0f0',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <Space size={16}>
          <Text type="secondary">
            <HistoryOutlined /> 历史实例：
            <Text strong>{trackingMap.historicalInstanceCount ?? '-'}</Text>
          </Text>
          <Text type="secondary">
            本流程平均耗时：<Text strong>{formatDuration(trackingMap.averageDuration)}</Text>
          </Text>
          <Text type="secondary">
            节点数：<Text strong>{trackingMap.nodes.length}</Text>
          </Text>
          <Space size={4}>
            <Tooltip title="快于历史平均"><Tag color="green">快</Tag></Tooltip>
            <Tooltip title="接近历史平均"><Tag color="blue">正常</Tag></Tooltip>
            <Tooltip title="慢于历史平均"><Tag color="orange">慢</Tag></Tooltip>
            <Tooltip title="瓶颈节点(>1.5倍历史平均)"><Tag color="red">瓶颈</Tag></Tooltip>
          </Space>
        </Space>
        <Space size={4}>
          <Button size="small" icon={<ZoomInOutlined />} onClick={handleZoomIn} />
          <Button size="small" icon={<ZoomOutOutlined />} onClick={handleZoomOut} />
          <Button size="small" icon={<FullscreenOutlined />} onClick={handleFitView} />
        </Space>
      </div>

      <div ref={containerRef} style={{ height, background: '#fff', border: '1px solid #f0f0f0', borderTop: 0, borderRadius: '0 0 6px 6px' }} />

      <Drawer
        title={selectedNode?.nodeName || '节点详情'}
        placement="right"
        width={440}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
      >
        {selectedNode && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="节点ID">{selectedNode.nodeId}</Descriptions.Item>
              <Descriptions.Item label="节点名称">{selectedNode.nodeName}</Descriptions.Item>
              <Descriptions.Item label="节点类型">{selectedNode.nodeType}</Descriptions.Item>
              <Descriptions.Item label="状态">{getNodeStatusLabel(selectedNode)}</Descriptions.Item>
              {selectedNode.actionName && (
                <Descriptions.Item label="操作">
                  <Tag color={getActionColor(selectedNode.actionName)}>{selectedNode.actionName}</Tag>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="开始时间">
                {selectedNode.startTime ? dayjs(selectedNode.startTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="结束时间">
                {selectedNode.endTime ? dayjs(selectedNode.endTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="消耗时长">
                <Space>
                  <Text strong>{formatDuration(selectedNode.duration)}</Text>
                  {selectedNode.historicalAvgDuration != null && selectedNode.historicalAvgDuration > 0 && (
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      (历史平均 {formatDuration(selectedNode.historicalAvgDuration)})
                    </Text>
                  )}
                  {selectedNode.durationDeviation != null && (
                    <Tag color={
                      selectedNode.durationDeviation < -0.3 ? 'green'
                        : selectedNode.durationDeviation > 0.5 ? 'red'
                          : selectedNode.durationDeviation > 0.3 ? 'orange' : 'blue'
                    }>
                      vs历史 {selectedNode.durationDeviation > 0 ? '+' : ''}
                      {(selectedNode.durationDeviation * 100).toFixed(0)}%
                    </Tag>
                  )}
                  {selectedNode.isBottleneck && (
                    <Tag color="red" icon={<ThunderboltOutlined />}>瓶颈</Tag>
                  )}
                </Space>
              </Descriptions.Item>
            </Descriptions>

            {selectedNode.actionRemark && (
              <>
                <Divider style={{ margin: 0 }}>操作意见</Divider>
                <Paragraph style={{ background: '#f6f8fa', padding: 12, borderRadius: 6, whiteSpace: 'pre-wrap' }}>
                  {selectedNode.actionRemark}
                </Paragraph>
              </>
            )}

            {selectedNode.signatureUrl && (
              <>
                <Divider style={{ margin: 0 }}>手写签名</Divider>
                <img
                  src={selectedNode.signatureUrl}
                  alt="签名"
                  style={{ width: 140, height: 56, objectFit: 'contain', border: '1px dashed #d9d9d9', borderRadius: 4, background: '#fff' }}
                />
              </>
            )}

            {selectedNode.operators.length > 0 && (
              <>
                <Divider style={{ margin: 0 }}>处理人 ({selectedNode.operators.length})</Divider>
                {selectedNode.operators.map((op, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: 12,
                      padding: '10px 12px',
                      background: '#fafafa',
                      borderRadius: 6,
                      border: '1px solid #f0f0f0',
                      marginBottom: 8
                    }}
                  >
                    <Avatar size={36} src={op.userAvatar} icon={<UserOutlined />}>
                      {op.userName?.charAt(0)}
                    </Avatar>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 4 }}>
                        <Space size={6}>
                          <Text strong>{op.userName}</Text>
                          {op.actionName && (
                            <Tag color={getActionColor(op.actionName)} style={{ fontSize: 11 }}>{op.actionName}</Tag>
                          )}
                        </Space>
                        {op.deptName && <Text type="secondary" style={{ fontSize: 12 }}>{op.deptName}</Text>}
                      </div>
                      {op.operateTime && (
                        <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 2 }}>
                          <ClockCircleOutlined /> {dayjs(op.operateTime).format('YYYY-MM-DD HH:mm')}
                          {op.duration != null && ` · 用时 ${formatDuration(op.duration)}`}
                        </Text>
                      )}
                      {op.actionRemark && (
                        <Paragraph style={{ margin: '4px 0 0', fontSize: 13, color: '#666', whiteSpace: 'pre-wrap' }}>
                          {op.actionRemark}
                        </Paragraph>
                      )}
                    </div>
                  </div>
                ))}
              </>
            )}
          </Space>
        )}
      </Drawer>
    </div>
  )
}

export default ApprovalTrackingMap
