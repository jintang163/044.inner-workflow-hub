import React, { useEffect, useRef, useState } from 'react'
import { Spin, Empty, Button, Space, Tooltip } from 'antd'
import { ZoomInOutlined, ZoomOutOutlined, FullscreenOutlined, ReloadOutlined } from '@ant-design/icons'
import type BpmnViewer from 'bpmn-js/lib/NavigatedViewer'

interface FlowDiagramProps {
  bpmnXml?: string
  currentNodeIds?: string[]
  historyNodeIds?: string[]
  height?: number | string
  loading?: boolean
}

const FlowDiagram: React.FC<FlowDiagramProps> = ({
  bpmnXml,
  currentNodeIds = [],
  historyNodeIds = [],
  height = 400,
  loading = false
}) => {
  const containerRef = useRef<HTMLDivElement>(null)
  const viewerRef = useRef<BpmnViewer | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let cancelled = false

    const initViewer = async () => {
      if (!containerRef.current || !bpmnXml) {
        return
      }

      try {
        const BpmnNavigatedViewer = (await import('bpmn-js/lib/NavigatedViewer')).default
        const ViewerModule = (await import('bpmn-js/lib/Viewer')).default

        if (cancelled || !containerRef.current) return

        if (viewerRef.current) {
          try {
            viewerRef.current.destroy()
          } catch (_) {}
          viewerRef.current = null
        }

        viewerRef.current = new BpmnNavigatedViewer({
          container: containerRef.current
        }) as BpmnViewer

        try {
          await viewerRef.current.importXML(bpmnXml)
          if (cancelled) return
          setReady(true)
          setError(null)

          const canvas = viewerRef.current.get('canvas') as any
          if (canvas?.zoom) {
            canvas.zoom('fit-viewport')
          }

          applyHighlights()
        } catch (importErr) {
          if (!cancelled) {
            setError('流程图解析失败')
          }
        }
      } catch (err) {
        if (!cancelled) {
          setError('流程图加载失败')
        }
      }
    }

    initViewer()

    return () => {
      cancelled = true
      setReady(false)
      if (viewerRef.current) {
        try {
          viewerRef.current.destroy()
        } catch (_) {}
        viewerRef.current = null
      }
    }
  }, [bpmnXml])

  useEffect(() => {
    if (ready) {
      applyHighlights()
    }
  }, [currentNodeIds, historyNodeIds, ready])

  const applyHighlights = () => {
    if (!viewerRef.current) return
    const canvas = (viewerRef.current as any).get('canvas')
    if (!canvas) return

    try {
      const allElements = (viewerRef.current as any).get('elementRegistry')?.getAll() || []
      allElements.forEach((el: any) => {
        const id = el.id
        try { canvas.removeMarker(id, 'current-node') } catch (_) {}
        try { canvas.removeMarker(id, 'history-node') } catch (_) {}
        try { canvas.removeMarker(id, 'highlight-current') } catch (_) {}
        try { canvas.removeMarker(id, 'highlight-history') } catch (_) {}
      })

      historyNodeIds.forEach(id => {
        try {
          canvas.addMarker(id, 'history-node')
          canvas.addMarker(id, 'highlight-history')
        } catch (_) {}
      })

      currentNodeIds.forEach(id => {
        try {
          canvas.addMarker(id, 'current-node')
          canvas.addMarker(id, 'highlight-current')
        } catch (_) {}
      })
    } catch (_) {}
  }

  const handleZoomIn = () => {
    const canvas = viewerRef.current?.get('canvas') as any
    if (canvas?.zoom) {
      canvas.zoom(canvas.zoom() * 1.2, true)
    }
  }

  const handleZoomOut = () => {
    const canvas = viewerRef.current?.get('canvas') as any
    if (canvas?.zoom) {
      canvas.zoom(canvas.zoom() * 0.8, true)
    }
  }

  const handleFit = () => {
    const canvas = viewerRef.current?.get('canvas') as any
    if (canvas?.zoom) {
      canvas.zoom('fit-viewport')
    }
  }

  const handleReset = async () => {
    if (!viewerRef.current || !bpmnXml) return
    try {
      setReady(false)
      await viewerRef.current.importXML(bpmnXml)
      setReady(true)
      handleFit()
      applyHighlights()
    } catch (_) {}
  }

  const styleTag = `
    .current-node:not(.djs-connection) .djs-visual > :nth-child(1) {
      stroke: #ff4d4f !important;
      stroke-width: 2.5px !important;
      fill: #fff1f0 !important;
    }
    .current-node .djs-label {
      font-weight: bold !important;
      color: #ff4d4f !important;
    }
    .history-node:not(.djs-connection) .djs-visual > :nth-child(1) {
      stroke: #52c41a !important;
      stroke-width: 2px !important;
      fill: #f6ffed !important;
    }
    .history-node .djs-label {
      color: #52c41a !important;
    }
    .highlight-current {
      filter: drop-shadow(0 2px 6px rgba(255,77,79,0.35)) !important;
    }
    .highlight-history {
      filter: drop-shadow(0 1px 4px rgba(82,196,26,0.25)) !important;
    }
    .djs-container {
      background: linear-gradient(135deg, #fafbfc 0%, #f0f2f5 100%);
      background-image:
        linear-gradient(rgba(0,0,0,0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(0,0,0,0.03) 1px, transparent 1px);
      background-size: 20px 20px;
    }
  `

  return (
    <div style={{ position: 'relative', width: '100%' }}>
      <style>{styleTag}</style>

      <Spin spinning={loading} tip="加载流程图...">
        <div
          style={{
            position: 'absolute',
            top: 12,
            right: 12,
            zIndex: 100,
            background: 'rgba(255,255,255,0.95)',
            borderRadius: 6,
            padding: '4px 8px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
            display: ready ? 'block' : 'none'
          }}
        >
          <Space size={4}>
            <Tooltip title="放大">
              <Button type="text" size="small" icon={<ZoomInOutlined />} onClick={handleZoomIn} />
            </Tooltip>
            <Tooltip title="缩小">
              <Button type="text" size="small" icon={<ZoomOutOutlined />} onClick={handleZoomOut} />
            </Tooltip>
            <Tooltip title="适应画布">
              <Button type="text" size="small" icon={<FullscreenOutlined />} onClick={handleFit} />
            </Tooltip>
            <Tooltip title="重置">
              <Button type="text" size="small" icon={<ReloadOutlined />} onClick={handleReset} />
            </Tooltip>
          </Space>
        </div>

        {bpmnXml ? (
          error ? (
            <Empty
              description={error}
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              style={{ height, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            />
          ) : (
            <div
              ref={containerRef}
              style={{
                width: '100%',
                height,
                border: '1px solid #e8e8e8',
                borderRadius: 8,
                overflow: 'hidden'
              }}
            />
          )
        ) : (
          <Empty
            description="暂无流程图数据"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            style={{ height, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          />
        )}

        {(currentNodeIds?.length > 0 || historyNodeIds?.length > 0) && (
          <div
            style={{
              position: 'absolute',
              bottom: 12,
              left: 12,
              zIndex: 100,
              background: 'rgba(255,255,255,0.95)',
              borderRadius: 6,
              padding: '8px 12px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
              display: 'flex',
              gap: 16,
              fontSize: 12
            }}
          >
            {historyNodeIds?.length > 0 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <span
                  style={{
                    display: 'inline-block',
                    width: 16,
                    height: 10,
                    border: '2px solid #52c41a',
                    borderRadius: 2,
                    background: '#f6ffed'
                  }}
                />
                <span style={{ color: '#666' }}>已审批</span>
              </div>
            )}
            {currentNodeIds?.length > 0 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <span
                  style={{
                    display: 'inline-block',
                    width: 16,
                    height: 10,
                    border: '2.5px solid #ff4d4f',
                    borderRadius: 2,
                    background: '#fff1f0'
                  }}
                />
                <span style={{ color: '#666' }}>当前节点</span>
              </div>
            )}
          </div>
        )}
      </Spin>
    </div>
  )
}

export default FlowDiagram
