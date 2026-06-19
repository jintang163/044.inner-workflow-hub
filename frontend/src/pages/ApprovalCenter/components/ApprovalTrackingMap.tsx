import React, { useEffect, useRef, useState, useCallback } from 'react'
import { Spin, Empty, Drawer, Descriptions, Tag, Avatar, Space, Typography, Tooltip, Button } from 'antd'
import {
  UserOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ThunderboltOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  FullscreenOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import type { TrackingMapVO, TrackingNodeVO, TrackingEdgeVO } from '@/types/approval'

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

const getNodeTypeIcon = (category: number): string => {
  switch (category) {
    case 2: return '●'
    case 3: return '◉'
    case 4: return '◇'
    case 5: return '▦'
    default: return '■'
  }
}

const getNodeStatusLabel = (node: TrackingNodeVO) => {
  if (node.status === 'active') return <Tag color="processing">进行中</Tag>
  if (node.status === 'completed') return <Tag color="success">已完成</Tag>
  return <Tag>{node.statusName}</Tag>
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

        const nodes = trackingMap.nodes.map((node, index) => {
          const color = getNodeColor(node)
          const isUserTask = node.nodeCategory === 1
          const operatorNames = node.operators
            .filter(o => o.userName)
            .map(o => o.userName)
            .join(', ')

          let labelText = node.nodeName || node.nodeId
          if (operatorNames && isUserTask) {
            labelText += `\n${operatorNames}`
          }
          if (node.duration != null) {
            labelText += `\n${formatDuration(node.duration)}`
          }

          return {
            id: node.nodeId,
            type: isUserTask ? 'rect' : 'circle',
            size: isUserTask ? [180, 72] : 48,
            label: labelText,
            labelCfg: {
              style: {
                fill: '#333',
                fontSize: 11,
                textAlign: 'center',
                textBaseline: 'middle'
              },
              position: 'bottom' as const,
              offset: isUserTask ? 0 : 30
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
              stroke: edge.isActualPath ? '#1890ff' : '#d9d9d9',
              lineWidth: edge.isActualPath ? 2.5 : 1,
              lineDash: edge.isActualPath ? [] : [4, 4],
              endArrow: edge.isActualPath
                ? { path: 'M 0,0 L 8,4 L 8,-4 Z', fill: '#1890ff' }
                : { path: 'M 0,0 L 6,3 L 6,-3 Z', fill: '#d9d9d9' }
            },
            label: edge.label || '',
            labelCfg: {
              style: {
                fill: edge.isActualPath ? '#1890ff' : '#999',
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
            nodesep: 30,
            ranksep: 80,
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
            平均耗时：<Text strong>{formatDuration(trackingMap.averageDuration)}</Text>
          </Text>
          <Text type="secondary">
            节点数：<Text strong>{trackingMap.nodes.length}</Text>
          </Text>
          <Space size={4}>
            <Tooltip title="快于平均"><Tag color="green">快</Tag></Tooltip>
            <Tooltip title="接近平均"><Tag color="blue">正常</Tag></Tooltip>
            <Tooltip title="慢于平均"><Tag color="orange">慢</Tag></Tooltip>
            <Tooltip title="瓶颈节点(>1.5倍平均)"><Tag color="red">瓶颈</Tag></Tooltip>
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
        width={420}
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
              <Descriptions.Item label="开始时间">
                {selectedNode.startTime ? dayjs(selectedNode.startTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="结束时间">
                {selectedNode.endTime ? dayjs(selectedNode.endTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="消耗时长">
                <Space>
                  <Text strong>{formatDuration(selectedNode.duration)}</Text>
                  {selectedNode.durationDeviation != null && (
                    <Tag color={
                      selectedNode.durationDeviation < -0.3 ? 'green'
                        : selectedNode.durationDeviation > 0.5 ? 'red'
                          : selectedNode.durationDeviation > 0.3 ? 'orange' : 'blue'
                    }>
                      {selectedNode.durationDeviation > 0 ? '+' : ''}
                      {(selectedNode.durationDeviation * 100).toFixed(0)}%
                    </Tag>
                  )}
                  {selectedNode.isBottleneck && (
                    <Tag color="red" icon={<ThunderboltOutlined />}>瓶颈</Tag>
                  )}
                </Space>
              </Descriptions.Item>
            </Descriptions>

            {selectedNode.operators.length > 0 && (
              <>
                <Text strong>处理人</Text>
                {selectedNode.operators.map((op, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: 12,
                      padding: '8px 12px',
                      background: '#fafafa',
                      borderRadius: 6,
                      border: '1px solid #f0f0f0'
                    }}
                  >
                    <Avatar size={32} src={op.userAvatar} icon={<UserOutlined />}>
                      {op.userName?.charAt(0)}
                    </Avatar>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text strong>{op.userName}</Text>
                        {op.deptName && <Text type="secondary" style={{ fontSize: 12 }}>{op.deptName}</Text>}
                      </div>
                      {op.operateTime && (
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          <ClockCircleOutlined /> {dayjs(op.operateTime).format('MM-DD HH:mm')}
                          {op.duration != null && ` · ${formatDuration(op.duration)}`}
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

            {selectedNode.actionRemark && (
              <>
                <Text strong>操作意见</Text>
                <Paragraph style={{ background: '#f6f8fa', padding: 12, borderRadius: 6 }}>
                  {selectedNode.actionRemark}
                </Paragraph>
              </>
            )}
          </Space>
        )}
      </Drawer>
    </div>
  )
}

export default ApprovalTrackingMap
