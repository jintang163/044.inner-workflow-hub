import React, { useState, useEffect, useMemo } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Form,
  Select,
  Input,
  message,
  Modal,
  Alert,
  Tag,
  Statistic,
  Row,
  Col,
  List,
  Progress,
  Tooltip,
  Divider,
  Drawer,
  Steps,
  Checkbox,
  Typography,
  Tabs
} from 'antd'
import {
  SwapOutlined,
  SearchOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
  RiseOutlined,
  HistoryOutlined,
  DatabaseOutlined
} from '@ant-design/icons'
import { approvalApi } from '@/api'
import type {
  MigrateInstanceVO,
  ProcessVersion,
  ProcessMigrationResultVO,
  CompatibilityCheckVO,
  ProcessMigrationRecordVO
} from '@/types/approval'
import type { PageResult } from '@/types'

const { Text, Paragraph } = Typography
const { TabPane } = Tabs

const ProcessInstanceMigration: React.FC = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [migrating, setMigrating] = useState(false)
  const [data, setData] = useState<PageResult<MigrateInstanceVO>>({
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: 20
  })
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([])
  const [selectedRows, setSelectedRows] = useState<MigrateInstanceVO[]>([])
  const [activeTab, setActiveTab] = useState<'migrate' | 'history'>('migrate')

  const [processDefinitions, setProcessDefinitions] = useState<any[]>([])
  const [versionsByDef, setVersionsByDef] = useState<Record<number, ProcessVersion[]>>({})

  const [selectedProcessDefId, setSelectedProcessDefId] = useState<number | null>(null)
  const [targetVersionId, setTargetVersionId] = useState<number | null>(null)
  const [targetVersionOptions, setTargetVersionOptions] = useState<ProcessVersion[]>([])

  const [checkModalOpen, setCheckModalOpen] = useState(false)
  const [checkLoading, setCheckLoading] = useState(false)
  const [checkResult, setCheckResult] = useState<CompatibilityCheckVO | null>(null)
  const [checkingInstance, setCheckingInstance] = useState<MigrateInstanceVO | null>(null)

  const [migrateModalOpen, setMigrateModalOpen] = useState(false)
  const [migrateResult, setMigrateResult] = useState<ProcessMigrationResultVO | null>(null)
  const [migrateRemark, setMigrateRemark] = useState('')
  const [forceMigrate, setForceMigrate] = useState(false)

  const [historyData, setHistoryData] = useState<PageResult<ProcessMigrationRecordVO>>({
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: 20
  })
  const [historyLoading, setHistoryLoading] = useState(false)

  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false)
  const [detailRecord, setDetailRecord] = useState<ProcessMigrationResultVO | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  useEffect(() => {
    mockProcessDefs()
  }, [])

  useEffect(() => {
    if (activeTab === 'migrate') {
      fetchData(data.pageNum, data.pageSize)
    } else {
      fetchHistory(historyData.pageNum, historyData.pageSize)
    }
  }, [activeTab])

  const mockProcessDefs = () => {
    const defs = [
      { id: 1, name: '请假审批流程', key: 'leave_approval' },
      { id: 2, name: '报销审批流程', key: 'expense_approval' },
      { id: 3, name: '采购审批流程', key: 'purchase_approval' }
    ]
    setProcessDefinitions(defs)
  }

  const fetchData = async (pageNum: number, pageSize: number) => {
    try {
      setLoading(true)
      const values = form.getFieldsValue()
      const res = await approvalApi.migrationPageInstances({
        pageNum,
        pageSize,
        processDefinitionId: selectedProcessDefId,
        ...values
      })
      setData({ ...res.data, pageNum, pageSize })

      const newVersionsMap: Record<number, ProcessVersion[]> = { ...versionsByDef }
      res.data.list.forEach(inst => {
        if (inst.availableVersions && inst.availableVersions.length > 0) {
          if (!newVersionsMap[inst.processDefinitionId]) {
            newVersionsMap[inst.processDefinitionId] = inst.availableVersions
          }
        }
      })
      setVersionsByDef(newVersionsMap)
    } catch (err: any) {
      message.error(err?.message || '加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchHistory = async (pageNum: number, pageSize: number) => {
    try {
      setHistoryLoading(true)
      const res = await approvalApi.migrationPageRecords(selectedProcessDefId, { pageNum, pageSize })
      setHistoryData({ ...res.data, pageNum, pageSize })
    } catch (err: any) {
      message.error(err?.message || '加载历史记录失败')
    } finally {
      setHistoryLoading(false)
    }
  }

  const handleProcessDefChange = (defId: number) => {
    setSelectedProcessDefId(defId)
    setTargetVersionId(null)
    const versions = versionsByDef[defId] || []
    const allLatest = new Map<number, ProcessVersion>()
    data.list.forEach(inst => {
      if (inst.processDefinitionId === defId && inst.availableVersions) {
        inst.availableVersions.forEach(v => {
          if (!allLatest.has(v.id)) {
            allLatest.set(v.id, v)
          }
        })
      }
    })
    const versionList = Array.from(allLatest.values())
    versionList.sort((a, b) => b.version - a.version)
    setTargetVersionOptions(versionList)
  }

  const selectedMigrateRows = useMemo(() => {
    return selectedRows.filter(r => r.canMigrate)
  }, [selectedRows])

  const handleCheckCompatibility = async (instance: MigrateInstanceVO) => {
    if (!instance.latestVersionId) {
      message.warning('没有可用的目标版本')
      return
    }
    setCheckingInstance(instance)
    setCheckModalOpen(true)
    setCheckLoading(true)
    try {
      const res = await approvalApi.migrationCheckCompatibility(instance.id, instance.latestVersionId)
      setCheckResult(res.data)
    } catch (err: any) {
      message.error(err?.message || '兼容性检查失败')
    } finally {
      setCheckLoading(false)
    }
  }

  const handleBatchMigrate = () => {
    if (selectedMigrateRows.length === 0) {
      message.warning('请选择至少一个可迁移的实例')
      return
    }
    const firstRow = selectedMigrateRows[0]
    const firstDefId = firstRow.processDefinitionId
    const hasSameDef = selectedMigrateRows.every(r => r.processDefinitionId === firstDefId)
    if (!hasSameDef) {
      message.warning('批量迁移仅支持同一流程定义的实例')
      return
    }

    handleProcessDefChange(firstDefId)

    const targetVersion = firstRow.availableVersions?.[0]
    if (targetVersion) {
      setTargetVersionId(targetVersion.id)
    }
    setMigrateModalOpen(true)
    setMigrateResult(null)
    setMigrateRemark('')
    setForceMigrate(false)
  }

  const doMigrate = async () => {
    const firstRow = selectedMigrateRows[0]
    let targetVerId = targetVersionId
    if (!targetVerId) {
      targetVerId = firstRow.latestVersionId
    }
    if (!targetVerId) {
      message.error('请选择目标版本')
      return
    }

    try {
      setMigrating(true)
      const res = await approvalApi.migrationBatchMigrate({
        targetVersionId: targetVerId,
        processDefinitionId: firstRow.processDefinitionId,
        instanceIds: selectedMigrateRows.map(r => r.id),
        remark: migrateRemark,
        forceMigrate: forceMigrate
      })
      setMigrateResult(res.data)
      message.success(`迁移执行完成：成功 ${res.data.successCount}，失败 ${res.data.failCount}，跳过 ${res.data.skipCount}`)
      fetchData(data.pageNum, data.pageSize)
      setSelectedRowKeys([])
      setSelectedRows([])
    } catch (err: any) {
      message.error(err?.message || '迁移失败')
    } finally {
      setMigrating(false)
    }
  }

  const handleViewDetail = async (record: ProcessMigrationRecordVO) => {
    setDetailLoading(true)
    setDetailDrawerOpen(true)
    try {
      const res = await approvalApi.migrationGetResult(record.id)
      setDetailRecord(res.data)
    } catch (err: any) {
      message.error(err?.message || '加载详情失败')
    } finally {
      setDetailLoading(false)
    }
  }

  const handleDownloadBackup = (record: ProcessMigrationRecordVO) => {
    const url = approvalApi.migrationDownloadBackup(record.id)
    const a = document.createElement('a')
    a.href = url
    a.download = `migration_backup_${record.id}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  }

  const renderVersionTag = (record: MigrateInstanceVO) => {
    if (record.currentVersion && record.latestVersion) {
      const gap = record.latestVersion - record.currentVersion
      if (gap === 0) {
        return <Tag color="success">v{record.currentVersion}（最新）</Tag>
      }
      if (gap <= 2) {
        return <Tag color="gold">v{record.currentVersion} → v{record.latestVersion}</Tag>
      }
      return <Tag color="red">v{record.currentVersion} → v{record.latestVersion}（落后{gap}个版本）</Tag>
    }
    return <Tag>v{record.currentVersion || '-'}</Tag>
  }

  const getMigrationStatusColor = (status: number) => {
    switch (status) {
      case 2: return 'success'
      case 3: return 'warning'
      case 4: return 'error'
      default: return 'default'
    }
  }

  const getMigrationResultColor = (result: number) => {
    switch (result) {
      case 1: return '#52c41a'
      case 2: return '#ff4d4f'
      case 3: return '#faad14'
      default: return '#999'
    }
  }

  const columns = [
    { title: '编号', dataIndex: 'instanceNo', width: 140 },
    { title: '标题', dataIndex: 'title', width: 240, ellipsis: true },
    {
      title: '版本',
      dataIndex: 'currentVersion',
      width: 220,
      render: (_: any, r: MigrateInstanceVO) => renderVersionTag(r)
    },
    { title: '发起人', dataIndex: 'startUserName', width: 100 },
    { title: '发起部门', dataIndex: 'startDeptName', width: 120 },
    { title: '发起时间', dataIndex: 'startTime', width: 170 },
    {
      title: '当前节点',
      dataIndex: 'currentNodeIds',
      width: 150,
      render: (nodes: string[]) =>
        nodes?.length > 0 ? nodes.map(n => <Tag key={n} color="blue">{n}</Tag>) : '-'
    },
    {
      title: '状态',
      dataIndex: 'canMigrate',
      width: 150,
      render: (can: boolean, r: MigrateInstanceVO) =>
        can ? (
          <Tag icon={<RiseOutlined />} color="green">可迁移</Tag>
        ) : (
          <Tooltip title={r.migrateTip}>
            <Tag icon={<InfoCircleOutlined />} color="default">不可迁移</Tag>
          </Tooltip>
        )
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right' as const,
      render: (_: any, r: MigrateInstanceVO) => (
        <Space>
          <Button
            size="small"
            type="link"
            disabled={!r.canMigrate}
            onClick={() => handleCheckCompatibility(r)}
          >
            兼容性检查
          </Button>
        </Space>
      )
    }
  ]

  const historyColumns = [
    { title: '批次号', dataIndex: 'migrationNo', width: 180 },
    { title: '流程标识', dataIndex: 'processKey', width: 140 },
    {
      title: '版本迁移',
      width: 180,
      render: (_: any, r: ProcessMigrationRecordVO) => (
        <Space>
          <Text>v{r.sourceVersion || '-'}</Text>
          <SwapOutlined />
          <Text strong>v{r.targetVersion || '-'}</Text>
        </Space>
      )
    },
    {
      title: '统计',
      width: 240,
      render: (_: any, r: ProcessMigrationRecordVO) => (
        <Space>
          <span>共 <Text strong>{r.totalCount}</Text></span>
          <Tag color="success">成功 {r.successCount}</Tag>
          {r.failCount > 0 && <Tag color="error">失败 {r.failCount}</Tag>}
          {r.skipCount > 0 && <Tag color="warning">跳过 {r.skipCount}</Tag>}
        </Space>
      )
    },
    {
      title: '状态',
      dataIndex: 'migrationStatus',
      width: 110,
      render: (s: number, r: ProcessMigrationRecordVO) => (
        <Tag color={getMigrationStatusColor(s)}>{r.migrationStatusName}</Tag>
      )
    },
    { title: '操作人', dataIndex: 'createByName', width: 100 },
    { title: '执行时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right' as const,
      render: (_: any, r: ProcessMigrationRecordVO) => (
        <Space>
          <Button
            size="small"
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(r)}
          >
            详情
          </Button>
          <Button
            size="small"
            type="link"
            icon={<DownloadOutlined />}
            onClick={() => handleDownloadBackup(r)}
          >
            备份
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div style={{ padding: 16 }}>
      <Card
        title={
          <Space>
            <DatabaseOutlined />
            <span>历史流程实例迁移</span>
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <Alert
          type="info"
          showIcon
          message="迁移说明"
          description={
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              <li>流程模板升级后，管理员可将运行中的旧实例迁移至新版本</li>
              <li>迁移前请先进行兼容性检查，确认当前节点在目标版本中存在</li>
              <li>迁移前系统会自动备份实例数据、任务数据和流程变量</li>
              <li>支持批量选择同一流程定义的实例进行迁移</li>
              <li>仅管理员可执行迁移操作</li>
            </ul>
          }
          style={{ marginBottom: 16 }}
        />

        <Tabs
          activeKey={activeTab}
          onChange={(k) => setActiveTab(k as any)}
          items={[
            {
              key: 'migrate',
              label: <><SwapOutlined /> 执行迁移</>,
              children: (
                <>
                  <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
                    <Form.Item name="processDefinitionId" label="流程定义">
                      <Select
                        style={{ width: 220 }}
                        placeholder="请选择流程定义"
                        allowClear
                        options={processDefinitions.map(d => ({ label: d.name, value: d.id }))}
                        onChange={(v) => {
                          handleProcessDefChange(v)
                        }}
                      />
                    </Form.Item>
                    <Form.Item name="instanceNo" label="实例编号">
                      <Input style={{ width: 180 }} placeholder="输入实例编号" allowClear prefix={<SearchOutlined />} />
                    </Form.Item>
                    <Form.Item name="title" label="标题">
                      <Input style={{ width: 200 }} placeholder="输入标题关键词" allowClear />
                    </Form.Item>
                    <Form.Item>
                      <Space>
                        <Button type="primary" icon={<SearchOutlined />} onClick={() => fetchData(1, data.pageSize)}>
                          查询
                        </Button>
                        <Button icon={<ReloadOutlined />} onClick={() => { form.resetFields(); setSelectedProcessDefId(null); fetchData(1, data.pageSize) }}>
                          重置
                        </Button>
                      </Space>
                    </Form.Item>
                  </Form>

                  <div style={{ marginBottom: 12, textAlign: 'right' }}>
                    <Space>
                      <span style={{ color: '#666' }}>
                        已选 <Text strong style={{ color: '#1890ff' }}>{selectedRowKeys.length}</Text> 项，
                        可迁移 <Text strong style={{ color: '#52c41a' }}>{selectedMigrateRows.length}</Text> 项
                      </span>
                      <Button
                        type="primary"
                        icon={<SwapOutlined />}
                        disabled={selectedMigrateRows.length === 0}
                        onClick={handleBatchMigrate}
                      >
                        批量迁移
                      </Button>
                    </Space>
                  </div>

                  <Table
                    rowKey="id"
                    loading={loading}
                    columns={columns}
                    dataSource={data.list}
                    pagination={{
                      current: data.pageNum,
                      pageSize: data.pageSize,
                      total: data.total,
                      showSizeChanger: true,
                      showQuickJumper: true,
                      showTotal: (total) => `共 ${total} 条`,
                      onChange: (p, s) => fetchData(p, s)
                    }}
                    rowSelection={{
                      selectedRowKeys,
                      onChange: (keys, rows) => {
                        setSelectedRowKeys(keys as number[])
                        setSelectedRows(rows as MigrateInstanceVO[])
                      },
                      getCheckboxProps: (record: MigrateInstanceVO) => ({
                        disabled: !record.canMigrate
                      })
                    }}
                    scroll={{ x: 1400 }}
                  />
                </>
              )
            },
            {
              key: 'history',
              label: <><HistoryOutlined /> 迁移历史</>,
              children: (
                <>
                  <div style={{ marginBottom: 12 }}>
                    <Select
                      style={{ width: 220 }}
                      placeholder="按流程定义筛选"
                      allowClear
                      value={selectedProcessDefId || undefined}
                      options={processDefinitions.map(d => ({ label: d.name, value: d.id }))}
                      onChange={(v) => {
                        setSelectedProcessDefId(v)
                        setTimeout(() => fetchHistory(1, historyData.pageSize), 0)
                      }}
                    />
                  </div>
                  <Table
                    rowKey="id"
                    loading={historyLoading}
                    columns={historyColumns}
                    dataSource={historyData.list}
                    pagination={{
                      current: historyData.pageNum,
                      pageSize: historyData.pageSize,
                      total: historyData.total,
                      showSizeChanger: true,
                      showQuickJumper: true,
                      showTotal: (total) => `共 ${total} 条`,
                      onChange: (p, s) => fetchHistory(p, s)
                    }}
                    scroll={{ x: 1300 }}
                  />
                </>
              )
            }
          ]}
        />
      </Card>

      <Modal
        title={
          <Space>
            <InfoCircleOutlined style={{ color: '#1890ff' }} />
            <span>兼容性检查 - {checkingInstance?.instanceNo}</span>
          </Space>
        }
        open={checkModalOpen}
        onCancel={() => setCheckModalOpen(false)}
        width={680}
        footer={[
          <Button key="close" onClick={() => setCheckModalOpen(false)}>
            关闭
          </Button>
        ]}
      >
        {checkLoading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <div>兼容性检查中...</div>
          </div>
        ) : checkResult ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              showIcon
              type={checkResult.compatible ? 'success' : 'error'}
              message={checkResult.compatible ? '兼容性检查通过' : '兼容性检查不通过'}
              description={checkResult.compatible ? '可以安全迁移到新版本' : '检测到以下问题，可能导致迁移失败'}
            />

            <div>
              <Divider orientation="left" plain style={{ margin: '8px 0' }}>
                <Space><DatabaseOutlined />节点检查</Space>
              </Divider>
              {checkResult.nodeCheck && (
                <>
                  <Paragraph style={{ marginBottom: 8 }}>
                    <Text type="secondary">当前节点：</Text>
                    {checkResult.nodeCheck.sourceNodes.length > 0
                      ? checkResult.nodeCheck.sourceNodes.map(n => <Tag key={n} color="blue">{n}</Tag>)
                      : <span style={{ color: '#999' }}>-</span>}
                  </Paragraph>
                  {checkResult.nodeCheck.missingNodes.length > 0 && (
                    <Alert
                      type="error"
                      showIcon
                      message="缺失节点"
                      description={
                        checkResult.nodeCheck.missingNodes.map(n => <Tag key={n} color="error">{n}</Tag>)
                      }
                    />
                  )}
                  {checkResult.nodeCheck.matchedNodes.length > 0 && (
                    <Alert
                      type="success"
                      showIcon
                      message="匹配节点"
                      description={
                        checkResult.nodeCheck.matchedNodes.map(n => <Tag key={n} color="success">{n}</Tag>)
                      }
                    />
                  )}
                </>
              )}
            </div>

            {checkResult.variableCheck && (
              <div>
                <Divider orientation="left" plain style={{ margin: '8px 0' }}>
                  <Space><DatabaseOutlined />变量检查</Space>
                </Divider>
                <Text type="secondary">流程变量数量：</Text>
                <Text strong>{checkResult.variableCheck.sourceVariables.length}</Text>
              </div>
            )}

            {checkResult.errors.length > 0 && (
              <List
                size="small"
                header={<Text type="danger" strong><CloseCircleOutlined /> 错误信息</Text>}
                bordered
                dataSource={checkResult.errors}
                renderItem={(item) => (
                  <List.Item style={{ color: '#ff4d4f' }}>{item}</List.Item>
                )}
              />
            )}
            {checkResult.warnings.length > 0 && (
              <List
                size="small"
                header={<Text type="warning" strong><ExclamationCircleOutlined /> 警告信息</Text>}
                bordered
                dataSource={checkResult.warnings}
                renderItem={(item) => (
                  <List.Item style={{ color: '#faad14' }}>{item}</List.Item>
                )}
              />
            )}
            {checkResult.infos.length > 0 && (
              <List
                size="small"
                header={<Text type="info" strong><InfoCircleOutlined /> 附加信息</Text>}
                bordered
                dataSource={checkResult.infos}
                renderItem={(item) => (
                  <List.Item style={{ color: '#1890ff' }}>{item}</List.Item>
                )}
              />
            )}
          </Space>
        ) : null}
      </Modal>

      <Modal
        title={
          <Space>
            <SwapOutlined style={{ color: '#1890ff' }} />
            <span>批量迁移流程实例</span>
          </Space>
        }
        open={migrateModalOpen}
        onCancel={() => !migrating && setMigrateModalOpen(false)}
        width={720}
        confirmLoading={migrating}
        maskClosable={!migrating}
        okText={migrateResult ? '关闭' : '确认迁移'}
        cancelText="取消"
        okButtonProps={migrateResult ? { onClick: () => setMigrateModalOpen(false) } as any : undefined}
        onOk={() => !migrateResult && !migrating && doMigrate()}
        cancelButtonProps={{ disabled: migrating }}
      >
        {!migrateResult ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type="warning"
              showIcon
              message="迁移前请仔细阅读"
              description={
                <ul style={{ margin: 0, paddingLeft: 18 }}>
                  <li>迁移操作不可逆，系统会自动备份实例数据</li>
                  <li>请先对部分实例执行兼容性检查</li>
                  <li>迁移后，流程实例将按新版流程继续流转</li>
                  <li>建议先在测试环境验证迁移流程</li>
                </ul>
              }
            />

            <Row gutter={16}>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="迁移数量" value={selectedMigrateRows.length} suffix="个实例" valueStyle={{ color: '#1890ff' }} />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic
                    title="当前版本"
                    value={`v${selectedMigrateRows[0]?.currentVersion || '-'}`}
                    valueStyle={{ color: '#722ed1' }}
                  />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic
                    title="目标版本"
                    value={selectedMigrateRows[0]?.latestVersion
                      ? `v${selectedMigrateRows[0].latestVersion}`
                      : '-'}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Card>
              </Col>
            </Row>

            <div>
              <div style={{ marginBottom: 8 }}>
                <Text strong>目标版本：</Text>
              </div>
              <Select
                style={{ width: '100%' }}
                value={targetVersionId || undefined}
                onChange={setTargetVersionId as any}
                placeholder="请选择目标版本"
                options={targetVersionOptions.length > 0
                  ? targetVersionOptions.map(v => ({
                    label: `v${v.version} ${v.versionRemark ? `(${v.versionRemark})` : ''}${v.isCurrent === 1 ? ' - 最新版' : ''}`,
                    value: v.id
                  }))
                  : (selectedMigrateRows[0]?.availableVersions || []).map(v => ({
                    label: `v${v.version} ${v.versionRemark ? `(${v.versionRemark})` : ''}`,
                    value: v.id
                  }))
                }
              />
            </div>

            <div>
              <div style={{ marginBottom: 8 }}>
                <Text strong>迁移备注：</Text>
              </div>
              <Input.TextArea
                rows={3}
                value={migrateRemark}
                onChange={(e) => setMigrateRemark(e.target.value)}
                placeholder="请输入迁移备注（可选），如：修复xxx流程bug后的版本迁移"
                maxLength={500}
                showCount
              />
            </div>

            <Checkbox
              checked={forceMigrate}
              onChange={(e) => setForceMigrate(e.target.checked)}
            >
              <Text type="danger">
                <ExclamationCircleOutlined /> 强制执行（忽略兼容性警告，可能导致迁移失败或流程异常）
              </Text>
            </Checkbox>
          </Space>
        ) : (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              showIcon
              type={migrateResult.successCount > 0 && migrateResult.failCount === 0
                ? 'success'
                : migrateResult.successCount === 0
                  ? 'error'
                  : 'warning'}
              message={migrateResult.migrationStatusName}
              description={
                <Space>
                  <Text>批次号：<Text code>{migrateResult.migrationNo}</Text></Text>
                </Space>
              }
            />

            <Row gutter={12}>
              <Col span={6}>
                <Card size="small">
                  <Statistic title="总计" value={migrateResult.totalCount} />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="成功"
                    value={migrateResult.successCount}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="失败"
                    value={migrateResult.failCount}
                    valueStyle={{ color: '#ff4d4f' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="跳过"
                    value={migrateResult.skipCount}
                    valueStyle={{ color: '#faad14' }}
                  />
                </Card>
              </Col>
            </Row>

            {migrateResult.details.length > 0 && (
              <div>
                <Divider orientation="left" plain>迁移详情</Divider>
                <div style={{ maxHeight: 300, overflowY: 'auto', border: '1px solid #f0f0f0', borderRadius: 4 }}>
                  {migrateResult.details.map(d => (
                    <div
                      key={d.detailId || d.instanceId}
                      style={{
                        padding: '8px 12px',
                        borderBottom: '1px solid #f0f0f0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between'
                      }}
                    >
                      <Space>
                        <span style={{
                          display: 'inline-block',
                          width: 8, height: 8, borderRadius: '50%',
                          backgroundColor: getMigrationResultColor(d.migrationResult)
                        }} />
                        <Text strong>{d.instanceNo}</Text>
                        <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>{d.title}</Text>
                      </Space>
                      <Space>
                        <Tag color={getMigrationResultColor(d.migrationResult) as any}>
                          {d.migrationResultName}
                        </Tag>
                        {(d.skipReason || d.errorMessage) && (
                          <Tooltip title={d.skipReason || d.errorMessage}>
                            <InfoCircleOutlined style={{
                              color: d.migrationResult === 2 ? '#ff4d4f' : '#faad14'
                            }} />
                          </Tooltip>
                        )}
                      </Space>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Space>
        )}
      </Modal>

      <Drawer
        title={
          <Space>
            <HistoryOutlined />
            <span>迁移详情报告</span>
          </Space>
        }
        width={720}
        open={detailDrawerOpen}
        onClose={() => setDetailDrawerOpen(false)}
        loading={detailLoading}
        extra={detailRecord && (
          <Space>
            <Button
              icon={<DownloadOutlined />}
              onClick={() => window.open(approvalApi.migrationDownloadBackup(detailRecord.recordId))}
            >
              下载备份
            </Button>
          </Space>
        )}
      >
        {detailRecord && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Steps
              size="small"
              current={2}
              items={[
                { title: '备份数据', status: 'finish' },
                { title: '执行迁移', status: detailRecord.failCount === 0 ? 'finish' : detailRecord.successCount > 0 ? 'process' : 'error' },
                { title: '完成', status: detailRecord.failCount === 0 ? 'finish' : detailRecord.successCount > 0 ? 'finish' : 'error' }
              ]}
              style={{ marginBottom: 16 }}
            />

            <Card size="small" title="基本信息">
              <Row gutter={12}>
                <Col span={12}>批次号：<Text code>{detailRecord.migrationNo}</Text></Col>
                <Col span={12}>流程标识：<Tag color="blue">{detailRecord.processKey}</Tag></Col>
                <Col span={12}>
                  版本：v{detailRecord.sourceVersion || '-'} <SwapOutlined style={{ margin: '0 4px' }} /> v{detailRecord.targetVersion || '-'}
                </Col>
                <Col span={12}>执行时间：{detailRecord.createTime}</Col>
              </Row>
            </Card>

            <Card size="small" title="迁移统计">
              <Row gutter={12}>
                <Col span={6}>
                  <Progress
                    type="dashboard"
                    percent={detailRecord.totalCount > 0
                      ? Math.round(detailRecord.successCount / detailRecord.totalCount * 100)
                      : 0}
                    success={{ percent: detailRecord.totalCount > 0
                      ? Math.round(detailRecord.successCount / detailRecord.totalCount * 100)
                      : 0 }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title={<><CheckCircleOutlined style={{ color: '#52c41a' }} /> 成功</>}
                    value={detailRecord.successCount}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title={<><CloseCircleOutlined style={{ color: '#ff4d4f' }} /> 失败</>}
                    value={detailRecord.failCount}
                    valueStyle={{ color: '#ff4d4f' }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title={<><ExclamationCircleOutlined style={{ color: '#faad14' }} /> 跳过</>}
                    value={detailRecord.skipCount}
                    valueStyle={{ color: '#faad14' }}
                  />
                </Col>
              </Row>
            </Card>

            <Card
              size="small"
              title={`迁移明细 (${detailRecord.details.length})`}
            >
              {detailRecord.details.map((d, idx) => (
                <div key={idx} style={{
                  padding: '8px 0',
                  borderBottom: idx < detailRecord.details.length - 1 ? '1px solid #f0f0f0' : 'none'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <Space>
                      <Tag color={getMigrationResultColor(d.migrationResult) as any}>
                        {d.migrationResultName}
                      </Tag>
                      <Text strong>{d.instanceNo}</Text>
                      <Text type="secondary">{d.title}</Text>
                    </Space>
                    <Text type="secondary">{d.startUserName}</Text>
                  </div>
                  {d.sourceCurrentNodeIds && d.sourceCurrentNodeIds.length > 0 && (
                    <div style={{ paddingLeft: 30, marginBottom: 4 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        源节点：{d.sourceCurrentNodeIds.join(', ')}
                      </Text>
                    </div>
                  )}
                  {(d.skipReason || d.errorMessage) && (
                    <div style={{
                      paddingLeft: 30,
                      fontSize: 12,
                      color: d.migrationResult === 2 ? '#ff4d4f' : '#faad14'
                    }}>
                      {d.skipReason || d.errorMessage}
                    </div>
                  )}
                  {d.compatibilityCheck && (d.compatibilityCheck.errors?.length > 0 || d.compatibilityCheck.warnings?.length > 0) && (
                    <div style={{ paddingLeft: 30, fontSize: 12 }}>
                      {d.compatibilityCheck.errors?.length > 0 && (
                        <div style={{ color: '#ff4d4f' }}>
                          错误：{d.compatibilityCheck.errors.join('; ')}
                        </div>
                      )}
                      {d.compatibilityCheck.warnings?.length > 0 && (
                        <div style={{ color: '#faad14' }}>
                          警告：{d.compatibilityCheck.warnings.join('; ')}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </Card>
          </Space>
        )}
      </Drawer>
    </div>
  )
}

export default ProcessInstanceMigration
