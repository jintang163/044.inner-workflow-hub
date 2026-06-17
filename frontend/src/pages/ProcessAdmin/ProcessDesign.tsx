import { useState, useEffect, useRef, useCallback } from 'react'
import {
  Button,
  Space,
  Breadcrumb,
  App,
  Modal,
  Input,
  Tag,
  Upload,
  Tooltip,
  Dropdown,
  Menu,
  Divider
} from 'antd'
import {
  ArrowLeftOutlined,
  SaveOutlined,
  RocketOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined,
  ImportOutlined,
  ExportOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  FullscreenOutlined,
  FileTextOutlined
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import type { MenuProps } from 'antd'

import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

import NodePalette from './components/NodePalette'
import NodePropertiesPanel from './components/NodePropertiesPanel'
import ValidateResultModal from './components/ValidateResultModal'
import SimulateModal from './components/SimulateModal'

import { processApi } from '@/api'
import type { NodeConfig, SequenceFlowConfig, ProcessDefinitionVO, ValidateResultVO } from '@/types'

declare module 'bpmn-js/lib/Modeler' {
  const _default: any
  export default _default
}

const DEFAULT_BPMN_XML = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  id="Definitions_1"
  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" name="新流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:endEvent id="EndEvent_1" name="结束">
      <bpmn:incoming>Flow_1</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="EndEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
        <dc:Bounds x="150" y="200" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="_BPMNShape_EndEvent_2" bpmnElement="EndEvent_1">
        <dc:Bounds x="500" y="200" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="186" y="218" />
        <di:waypoint x="500" y="218" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

const NODE_TYPE_MAP: Record<string, string> = {
  'bpmn:StartEvent': 'START_EVENT',
  'bpmn:EndEvent': 'END_EVENT',
  'bpmn:UserTask': 'USER_TASK',
  'bpmn:ExclusiveGateway': 'EXCLUSIVE_GATEWAY',
  'bpmn:ParallelGateway': 'PARALLEL_GATEWAY',
  'bpmn:SubProcess': 'SUB_PROCESS'
}

export default function ProcessDesign() {
  const navigate = useNavigate()
  const { id } = useParams()
  const processId = id ? parseInt(id, 10) : null
  const { message, modal } = App.useApp()

  const containerRef = useRef<HTMLDivElement>(null)
  const modelerRef = useRef<any>(null)

  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [deploying, setDeploying] = useState(false)
  const [processInfo, setProcessInfo] = useState<ProcessDefinitionVO | null>(null)

  const [selectedElement, setSelectedElement] = useState<any>(null)
  const [nodeConfigs, setNodeConfigs] = useState<Map<string, NodeConfig>>(new Map())
  const [flowConfigs, setFlowConfigs] = useState<Map<string, SequenceFlowConfig>>(new Map())

  const [validateModalVisible, setValidateModalVisible] = useState(false)
  const [validateResults, setValidateResults] = useState<ValidateResultVO[]>([])
  const [simulateVisible, setSimulateVisible] = useState(false)

  const [versionRemark, setVersionRemark] = useState('')
  const [deployModalVisible, setDeployModalVisible] = useState(false)

  const initModeler = useCallback(async () => {
    if (!containerRef.current) return

    const BpmnModeler = (await import('bpmn-js/lib/Modeler')).default

    const modeler = new BpmnModeler({
      container: containerRef.current,
      keyboard: { bindTo: document }
    })

    modelerRef.current = modeler

    modeler.on('selection.changed', (e: any) => {
      const newSelection = e.newSelection && e.newSelection[0]
      if (!newSelection) {
        setSelectedElement(null)
        return
      }
      const businessObject = newSelection.businessObject
      const type = businessObject.$type
      const isFlow = type === 'bpmn:SequenceFlow'

      let elementInfo: any
      if (isFlow) {
        elementInfo = {
          type: 'flow',
          id: businessObject.id,
          name: businessObject.name || '',
          flowType: 'SEQUENCE_FLOW',
          sourceNodeId: businessObject.sourceRef?.id,
          targetNodeId: businessObject.targetRef?.id
        }
      } else {
        elementInfo = {
          type: 'node',
          id: businessObject.id,
          name: businessObject.name || '',
          nodeType: NODE_TYPE_MAP[type] || 'UNKNOWN'
        }
      }
      setSelectedElement(elementInfo)
    })

    modeler.on('element.changed', (e: any) => {
      const el = e.element
      if (!el) return
      const businessObject = el.businessObject
      if (!businessObject) return

      setSelectedElement((prev) => {
        if (!prev || prev.id !== businessObject.id) return prev
        return {
          ...prev,
          name: businessObject.name || ''
        }
      })
    })

    modeler.on('element.removed', (e: any) => {
      const el = e.element
      if (!el) return
      const businessObject = el.businessObject
      if (!businessObject) return
      const id = businessObject.id
      const type = businessObject.$type

      if (type === 'bpmn:SequenceFlow') {
        setFlowConfigs((prev) => {
          const next = new Map(prev)
          next.delete(id)
          return next
        })
      } else {
        setNodeConfigs((prev) => {
          const next = new Map(prev)
          next.delete(id)
          return next
        })
      }
    })

    try {
      await loadProcessDesign()
    } catch (e) {
      // ignore
    }
  }, [processId])

  const loadProcessDesign = async () => {
    setLoading(true)
    try {
      let bpmnXml = DEFAULT_BPMN_XML

      if (processId) {
        const [info, design] = await Promise.all([
          processApi.definitionGet(processId) as Promise<any>,
          processApi.designGet(processId).catch(() => null)
        ])
        setProcessInfo(info || null)

        if (design?.bpmnXml) {
          bpmnXml = design.bpmnXml
        } else if (info?.processName || info?.processKey) {
          bpmnXml = bpmnXml
            .replace('新流程', info.processName || '新流程')
            .replace('Process_1', info.processKey || 'Process_1')
        }

        if (design?.nodeConfigs) {
          const ncMap = new Map<string, NodeConfig>()
          design.nodeConfigs.forEach((nc: NodeConfig) => ncMap.set(nc.nodeId, nc))
          setNodeConfigs(ncMap)
        }
        if (design?.sequenceFlowConfigs) {
          const fcMap = new Map<string, SequenceFlowConfig>()
          design.sequenceFlowConfigs.forEach((fc: SequenceFlowConfig) => fcMap.set(fc.flowId, fc))
          setFlowConfigs(fcMap)
        }
      }

      if (modelerRef.current) {
        await modelerRef.current.importXML(bpmnXml)
        modelerRef.current.get('canvas').zoom('fit-viewport')
      }
    } catch (e) {
      console.error('加载流程设计失败:', e)
      if (modelerRef.current) {
        await modelerRef.current.importXML(DEFAULT_BPMN_XML)
        modelerRef.current.get('canvas').zoom('fit-viewport')
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    initModeler()
    return () => {
      if (modelerRef.current) {
        modelerRef.current.destroy()
        modelerRef.current = null
      }
    }
  }, [initModeler])

  const handleExportXml = async () => {
    try {
      const { xml } = await modelerRef.current.saveXML({ format: true })
      const blob = new Blob([xml], { type: 'application/xml' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${processInfo?.processKey || 'process'}.bpmn`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      message.success('导出成功')
    } catch (e) {
      message.error('导出失败')
    }
  }

  const handleImportXml = (file: File) => {
    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const xml = e.target?.result as string
        await modelerRef.current.importXML(xml)
        modelerRef.current.get('canvas').zoom('fit-viewport')
        message.success('导入成功')
      } catch (err) {
        message.error('导入失败，请检查 XML 格式')
      }
    }
    reader.readAsText(file)
    return false
  }

  const handleZoomIn = () => {
    const canvas = modelerRef.current?.get('canvas')
    if (canvas) canvas.zoom(canvas.zoom() * 1.2)
  }

  const handleZoomOut = () => {
    const canvas = modelerRef.current?.get('canvas')
    if (canvas) canvas.zoom(canvas.zoom() / 1.2)
  }

  const handleZoomReset = () => {
    const canvas = modelerRef.current?.get('canvas')
    if (canvas) canvas.zoom('fit-viewport')
  }

  const getCurrentXml = async (): Promise<string> => {
    const { xml } = await modelerRef.current.saveXML({ format: true })
    return xml
  }

  const handleSave = async () => {
    if (!processId) {
      message.warning('请先创建流程定义')
      return
    }
    setSaving(true)
    try {
      const bpmnXml = await getCurrentXml()
      await processApi.designSave({
        processDefinitionId: processId,
        bpmnXml,
        nodeConfigs: Array.from(nodeConfigs.values()),
        sequenceFlowConfigs: Array.from(flowConfigs.values())
      })
      message.success('保存草稿成功')
    } catch (e) {
      // error handled
    } finally {
      setSaving(false)
    }
  }

  const handleValidate = async () => {
    if (!processId) {
      message.warning('请先创建流程定义')
      return
    }
    try {
      const bpmnXml = await getCurrentXml()
      const results = (await processApi.validate({
        processDefinitionId: processId,
        bpmnXml,
        nodeConfigs: Array.from(nodeConfigs.values()),
        sequenceFlowConfigs: Array.from(flowConfigs.values())
      })) as unknown as ValidateResultVO[]
      setValidateResults(results || [])
      setValidateModalVisible(true)
    } catch (e) {
      // error handled
    }
  }

  const handleShowDeploy = () => {
    setVersionRemark('')
    setDeployModalVisible(true)
  }

  const handleDeploy = async () => {
    if (!processId) {
      message.warning('请先创建流程定义')
      return
    }
    setDeploying(true)
    try {
      const bpmnXml = await getCurrentXml()
      await processApi.designDeploy({
        processDefinitionId: processId,
        bpmnXml,
        nodeConfigs: Array.from(nodeConfigs.values()),
        sequenceFlowConfigs: Array.from(flowConfigs.values()),
        versionRemark
      })
      message.success('发布成功')
      setDeployModalVisible(false)
    } catch (e) {
      // error handled
    } finally {
      setDeploying(false)
    }
  }

  const handleElementNameChange = (name: string) => {
    if (!selectedElement || !modelerRef.current) return
    const modeling = modelerRef.current.get('modeling')
    const elementRegistry = modelerRef.current.get('elementRegistry')
    const element = elementRegistry.get(selectedElement.id)
    if (element && modeling) {
      modeling.updateProperties(element, { name })
    }
  }

  const handleNodeConfigChange = (updates: Partial<NodeConfig>) => {
    if (!selectedElement) return
    setNodeConfigs((prev) => {
      const next = new Map(prev)
      const current = next.get(selectedElement.id) || {
        nodeId: selectedElement.id,
        nodeName: selectedElement.name,
        nodeType: selectedElement.nodeType,
        approveType: 1,
        assigneeType: 1,
        assigneeValue: [],
        assigneeDeptLevel: 1,
        assigneeScript: '',
        formPermission: {},
        timeoutStrategy: 4,
        timeoutHours: 0,
        timeoutEscalateLevels: 1,
        passRate: 100,
        notifyConfig: { channels: [], ccUserIds: [] },
        emptyAssigneeStrategy: 1,
        refuseStrategy: 1,
        refuseTargetNodeId: '',
        canAddSign: 0,
        canTransfer: 0,
        canDelegate: 0,
        needSignature: 0,
        needComment: 0
      }
      next.set(selectedElement.id, { ...current, ...updates })
      return next
    })
  }

  const handleFlowConfigChange = (updates: Partial<SequenceFlowConfig>) => {
    if (!selectedElement) return
    setFlowConfigs((prev) => {
      const next = new Map(prev)
      const current = next.get(selectedElement.id) || {
        flowId: selectedElement.id,
        sourceNodeId: selectedElement.sourceNodeId,
        targetNodeId: selectedElement.targetNodeId,
        conditionExpression: '',
        isDefault: 0
      }
      next.set(selectedElement.id, { ...current, ...updates })
      return next
    })
  }

  const toolsMenu: MenuProps['items'] = [
    {
      key: 'validate',
      icon: <CheckCircleOutlined />,
      label: '流程校验',
      onClick: handleValidate
    },
    { type: 'divider' },
    {
      key: 'zoomIn',
      icon: <ZoomInOutlined />,
      label: '放大',
      onClick: handleZoomIn
    },
    {
      key: 'zoomOut',
      icon: <ZoomOutOutlined />,
      label: '缩小',
      onClick: handleZoomOut
    },
    {
      key: 'zoomReset',
      icon: <FullscreenOutlined />,
      label: '重置视图',
      onClick: handleZoomReset
    }
  ]

  return (
    <div style={{ height: 'calc(100vh - 160px)', display: 'flex', flexDirection: 'column' }}>
      <div
        style={{
          padding: '12px 16px',
          background: '#fff',
          borderBottom: '1px solid #f0f0f0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/process/definition')}>
            返回列表
          </Button>
          <Breadcrumb
            items={[
              { title: '流程管理' },
              { title: '流程定义', href: '/process/definition' },
              {
                title: (
                  <Space>
                    <FileTextOutlined style={{ color: '#1890ff' }} />
                    <span style={{ fontWeight: 500 }}>{processInfo?.processName || '流程设计'}</span>
                    {processInfo?.version && <Tag color="blue">v{processInfo.version}</Tag>}
                    {processInfo?.status === 1 && <Tag color="green">已发布</Tag>}
                  </Space>
                )
              }
            ]}
          />
        </Space>
        <Space>
          <Dropdown menu={{ items: toolsMenu }} placement="bottomRight">
            <Button>工具</Button>
          </Dropdown>
          <Divider type="vertical" style={{ margin: 0 }} />
          <Space size={4}>
            <Tooltip title="放大">
              <Button icon={<ZoomInOutlined />} onClick={handleZoomIn} />
            </Tooltip>
            <Tooltip title="缩小">
              <Button icon={<ZoomOutOutlined />} onClick={handleZoomOut} />
            </Tooltip>
            <Tooltip title="重置视图">
              <Button icon={<FullscreenOutlined />} onClick={handleZoomReset} />
            </Tooltip>
          </Space>
          <Divider type="vertical" style={{ margin: 0 }} />
          <Upload
            accept=".bpmn,.xml"
            showUploadList={false}
            beforeUpload={handleImportXml}
          >
            <Tooltip title="导入XML">
              <Button icon={<ImportOutlined />}>导入</Button>
            </Tooltip>
          </Upload>
          <Tooltip title="导出XML">
            <Button icon={<ExportOutlined />} onClick={handleExportXml}>
              导出
            </Button>
          </Tooltip>
          <Divider type="vertical" style={{ margin: 0 }} />
          <Button icon={<CheckCircleOutlined />} onClick={handleValidate}>
            校验
          </Button>
          <Button
            icon={<PlayCircleOutlined />}
            onClick={() => setSimulateVisible(true)}
          >
            模拟运行
          </Button>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSave}
            loading={saving}
          >
            保存草稿
          </Button>
          <Button
            type="primary"
            danger
            icon={<RocketOutlined />}
            onClick={handleShowDeploy}
            loading={deploying}
          >
            发布版本
          </Button>
        </Space>
      </div>

      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        <div style={{ width: 220, flexShrink: 0, overflow: 'auto' }}>
          <NodePalette />
        </div>
        <div
          ref={containerRef}
          style={{
            flex: 1,
            position: 'relative',
            background: '#f0f2f5'
          }}
        />
        <div style={{ width: 320, flexShrink: 0, overflow: 'hidden' }}>
          <NodePropertiesPanel
            selectedElement={selectedElement}
            nodeConfig={selectedElement?.type === 'node' ? nodeConfigs.get(selectedElement.id) || null : null}
            flowConfig={selectedElement?.type === 'flow' ? flowConfigs.get(selectedElement.id) || null : null}
            formId={processInfo?.formId || null}
            onNodeConfigChange={handleNodeConfigChange}
            onFlowConfigChange={handleFlowConfigChange}
            onElementNameChange={handleElementNameChange}
          />
        </div>
      </div>

      <ValidateResultModal
        open={validateModalVisible}
        onCancel={() => setValidateModalVisible(false)}
        results={validateResults}
      />

      {processId && (
        <SimulateModal
          open={simulateVisible}
          onCancel={() => setSimulateVisible(false)}
          processDefinitionId={processId}
        />
      )}

      <Modal
        title={
          <Space>
            <RocketOutlined style={{ color: '#ff4d4f' }} />
            <span>发布新版本</span>
          </Space>
        }
        open={deployModalVisible}
        onOk={handleDeploy}
        onCancel={() => setDeployModalVisible(false)}
        confirmLoading={deploying}
        okText="确认发布"
        okButtonProps={{ danger: true }}
      >
        <div style={{ marginBottom: 16 }}>
          <div style={{ padding: 12, background: '#fff7e6', borderRadius: 6, border: '1px solid #ffd591' }}>
            <Space direction="vertical" size={4}>
              <div>
                <Tag color="blue">流程名称</Tag>
                <span>{processInfo?.processName}</span>
              </div>
              <div>
                <Tag color="green">当前版本</Tag>
                <span>v{processInfo?.version || 0}</span>
                <Tag color="cyan" style={{ marginLeft: 8 }}>
                  → 发布后将成为 v{(processInfo?.version || 0) + 1}
                </Tag>
              </div>
            </Space>
          </div>
        </div>
        <div>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>版本备注（可选）</div>
          <Input.TextArea
            rows={3}
            placeholder="请输入此版本的更新说明..."
            value={versionRemark}
            onChange={(e) => setVersionRemark(e.target.value)}
          />
        </div>
      </Modal>
    </div>
  )
}
