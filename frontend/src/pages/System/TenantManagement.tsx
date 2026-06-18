import { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  message,
  Popconfirm,
  Descriptions,
  Statistic,
  Row,
  Col,
  Tooltip,
  Tabs
} from 'antd'
import {
  PlusOutlined,
  CheckOutlined,
  CloseOutlined,
  BarChartOutlined,
  DeleteOutlined,
  EditOutlined,
  SafetyOutlined,
  UserAddOutlined
} from '@ant-design/icons'
import { tenantApi } from '@/api'
import type { TenantVO, TenantStatsVO, TenantRegisterDTO, TenantRoleVO } from '@/types/tenant'
import type { ColumnsType } from 'antd/es/table'

const { TextArea } = Input

const statusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'warning', text: '待审核' },
  1: { color: 'success', text: '已启用' },
  2: { color: 'error', text: '已禁用' }
}

const businessTypeOptions = [
  { label: 'HR', value: 'HR' },
  { label: '财务', value: '财务' },
  { label: '采购', value: '采购' },
  { label: 'IT', value: 'IT' },
  { label: '行政', value: '行政' },
  { label: '其他', value: '其他' }
]

const dataScopeOptions = [
  { label: '全部数据', value: 1 },
  { label: '自定义数据', value: 2 },
  { label: '本部门数据', value: 3 },
  { label: '本部门及以下', value: 4 },
  { label: '仅本人', value: 5 }
]

export default function TenantManagement() {
  const [data, setData] = useState<TenantVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [registerOpen, setRegisterOpen] = useState(false)
  const [statsVisible, setStatsVisible] = useState(false)
  const [currentStats, setCurrentStats] = useState<TenantStatsVO | null>(null)
  const [detailTenant, setDetailTenant] = useState<TenantVO | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [tenantRoles, setTenantRoles] = useState<TenantRoleVO[]>([])
  const [roleFormOpen, setRoleFormOpen] = useState(false)
  const [editingRole, setEditingRole] = useState<TenantRoleVO | null>(null)
  const [roleForm] = Form.useForm()
  const [form] = Form.useForm()

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await tenantApi.page({ pageNum, pageSize })
      setData(res.list || [])
      setTotal(res.total)
    } catch {
      // ignore
    }
    setLoading(false)
  }

  useEffect(() => {
    loadData()
  }, [pageNum, pageSize])

  const loadTenantRoles = async (tenantId: number) => {
    try {
      const roles = await tenantApi.listRoles(tenantId)
      setTenantRoles(roles || [])
    } catch {
      setTenantRoles([])
    }
  }

  const handleRegister = async (values: TenantRegisterDTO) => {
    try {
      await tenantApi.register(values)
      message.success('租户注册成功，等待管理员审核')
      setRegisterOpen(false)
      form.resetFields()
      loadData()
    } catch {
      // error handled by interceptor
    }
  }

  const handleApprove = async (id: number) => {
    try {
      await tenantApi.approve(id)
      message.success('审核通过')
      loadData()
    } catch {
      // ignore
    }
  }

  const handleReject = async (id: number) => {
    try {
      await tenantApi.reject(id)
      message.success('已驳回')
      loadData()
    } catch {
      // ignore
    }
  }

  const handleRemove = async (id: number) => {
    try {
      await tenantApi.remove(id)
      message.success('删除成功')
      loadData()
    } catch {
      // ignore
    }
  }

  const handleViewStats = async (tenant: TenantVO) => {
    try {
      const stats = await tenantApi.getStats(tenant.id)
      setCurrentStats(stats)
      setStatsVisible(true)
    } catch {
      // ignore
    }
  }

  const handleViewDetail = (tenant: TenantVO) => {
    setDetailTenant(tenant)
    setDetailOpen(true)
    loadTenantRoles(tenant.id)
  }

  const handleSaveRole = async () => {
    try {
      const values = await roleForm.validateFields()
      if (editingRole) {
        await tenantApi.updateRole({ ...values, id: editingRole.id })
        message.success('角色更新成功')
      } else {
        await tenantApi.saveRole({ ...values, tenantId: detailTenant!.id })
        message.success('角色创建成功')
      }
      setRoleFormOpen(false)
      setEditingRole(null)
      roleForm.resetFields()
      loadTenantRoles(detailTenant!.id)
    } catch {
      // ignore
    }
  }

  const handleRemoveRole = async (roleId: number) => {
    try {
      await tenantApi.removeRole(roleId)
      message.success('角色删除成功')
      loadTenantRoles(detailTenant!.id)
    } catch {
      // ignore
    }
  }

  const columns: ColumnsType<TenantVO> = [
    { title: '租户名称', dataIndex: 'tenantName', key: 'tenantName', width: 160 },
    { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 120 },
    {
      title: '业务线', dataIndex: 'businessType', key: 'businessType', width: 100,
      render: (v: string) => <Tag color="blue">{v}</Tag>
    },
    { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 100 },
    { title: '联系邮箱', dataIndex: 'contactEmail', key: 'contactEmail', width: 180 },
    { title: '用户数', dataIndex: 'userCount', key: 'userCount', width: 80, align: 'center' },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 90,
      render: (v: number) => {
        const s = statusMap[v] || { color: 'default', text: '未知' }
        return <Tag color={s.color}>{s.text}</Tag>
      }
    },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
    {
      title: '操作', key: 'action', width: 300, fixed: 'right',
      render: (_: any, record: TenantVO) => (
        <Space size={4}>
          {record.status === 0 && (
            <>
              <Tooltip title="通过">
                <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => handleApprove(record.id)} />
              </Tooltip>
              <Tooltip title="驳回">
                <Button size="small" danger icon={<CloseOutlined />} onClick={() => handleReject(record.id)} />
              </Tooltip>
            </>
          )}
          <Tooltip title="详情与角色">
            <Button size="small" icon={<SafetyOutlined />} onClick={() => handleViewDetail(record)} />
          </Tooltip>
          <Tooltip title="统计">
            <Button size="small" icon={<BarChartOutlined />} onClick={() => handleViewStats(record)} />
          </Tooltip>
          <Popconfirm title="确认删除该租户？" onConfirm={() => handleRemove(record.id)}>
            <Tooltip title="删除">
              <Button size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const roleColumns: ColumnsType<TenantRoleVO> = [
    { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 140 },
    { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode', width: 140 },
    {
      title: '数据范围', dataIndex: 'dataScope', key: 'dataScope', width: 120,
      render: (v: number) => dataScopeOptions.find(d => d.value === v)?.label || '-'
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (v: number) => <Tag color={v === 1 ? 'success' : 'error'}>{v === 1 ? '启用' : '停用'}</Tag>
    },
    {
      title: '操作', key: 'action', width: 120,
      render: (_: any, record: TenantRoleVO) => (
        <Space size={4}>
          <Tooltip title="编辑">
            <Button size="small" icon={<EditOutlined />} onClick={() => {
              setEditingRole(record)
              roleForm.setFieldsValue(record)
              setRoleFormOpen(true)
            }} />
          </Tooltip>
          <Popconfirm title="确认删除该角色？" onConfirm={() => handleRemoveRole(record.id)}>
            <Tooltip title="删除">
              <Button size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ]

  return (
    <div>
      <Card
        title="租户管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setRegisterOpen(true)}>
            租户注册
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 1300 }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, ps) => { setPageNum(p); setPageSize(ps) }
          }}
        />
      </Card>

      <Modal
        title="租户注册"
        open={registerOpen}
        onCancel={() => { setRegisterOpen(false); form.resetFields() }}
        onOk={() => form.submit()}
        width={560}
      >
        <Form form={form} layout="vertical" onFinish={handleRegister}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="tenantName" label="租户名称" rules={[{ required: true, message: '请输入租户名称' }]}>
                <Input placeholder="请输入租户名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="tenantCode" label="租户编码" rules={[{ required: true, message: '请输入租户编码' }]}>
                <Input placeholder="请输入租户编码（唯一标识）" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="contactName" label="联系人" rules={[{ required: true, message: '请输入联系人' }]}>
                <Input placeholder="请输入联系人" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessType" label="业务线" rules={[{ required: true, message: '请选择业务线' }]}>
                <Select placeholder="请选择业务线" options={businessTypeOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="contactEmail" label="联系邮箱">
                <Input placeholder="请输入联系邮箱" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="contactPhone" label="联系电话">
                <Input placeholder="请输入联系电话" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="租户使用统计"
        open={statsVisible}
        onCancel={() => setStatsVisible(false)}
        footer={null}
        width={600}
      >
        {currentStats && (
          <div>
            <Descriptions column={1} bordered size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="租户名称">{currentStats.tenantName}</Descriptions.Item>
            </Descriptions>
            <Row gutter={16}>
              <Col span={8}>
                <Statistic title="流程发起量" value={currentStats.processCount} suffix="个" valueStyle={{ color: '#1890ff' }} />
              </Col>
              <Col span={8}>
                <Statistic
                  title="待办积压"
                  value={currentStats.pendingCount}
                  suffix="件"
                  valueStyle={{ color: currentStats.pendingCount > 50 ? '#cf1322' : '#3f8600' }}
                />
              </Col>
              <Col span={8}>
                <Statistic title="平均耗时" value={currentStats.avgDuration} suffix="秒" valueStyle={{ color: '#722ed1' }} />
              </Col>
            </Row>
          </div>
        )}
      </Modal>

      <Modal
        title={`租户详情 - ${detailTenant?.tenantName || ''}`}
        open={detailOpen}
        onCancel={() => { setDetailOpen(false); setDetailTenant(null); setTenantRoles([]) }}
        footer={null}
        width={800}
      >
        {detailTenant && (
          <Tabs
            items={[
              {
                key: 'info',
                label: '基本信息',
                children: (
                  <Descriptions column={2} bordered size="small">
                    <Descriptions.Item label="租户名称">{detailTenant.tenantName}</Descriptions.Item>
                    <Descriptions.Item label="租户编码">{detailTenant.tenantCode}</Descriptions.Item>
                    <Descriptions.Item label="业务线">{detailTenant.businessType}</Descriptions.Item>
                    <Descriptions.Item label="状态">
                      <Tag color={statusMap[detailTenant.status]?.color}>{statusMap[detailTenant.status]?.text}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="联系人">{detailTenant.contactName}</Descriptions.Item>
                    <Descriptions.Item label="联系邮箱">{detailTenant.contactEmail}</Descriptions.Item>
                    <Descriptions.Item label="联系电话">{detailTenant.contactPhone}</Descriptions.Item>
                    <Descriptions.Item label="用户数">{detailTenant.userCount}</Descriptions.Item>
                    <Descriptions.Item label="备注" span={2}>{detailTenant.remark || '-'}</Descriptions.Item>
                  </Descriptions>
                )
              },
              {
                key: 'roles',
                label: '租户角色',
                children: (
                  <div>
                    <div style={{ marginBottom: 16, textAlign: 'right' }}>
                      <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        size="small"
                        onClick={() => {
                          setEditingRole(null)
                          roleForm.resetFields()
                          setRoleFormOpen(true)
                        }}
                      >
                        新建角色
                      </Button>
                    </div>
                    <Table
                      rowKey="id"
                      columns={roleColumns}
                      dataSource={tenantRoles}
                      pagination={false}
                      size="small"
                    />
                  </div>
                )
              }
            ]}
          />
        )}
      </Modal>

      <Modal
        title={editingRole ? '编辑租户角色' : '新建租户角色'}
        open={roleFormOpen}
        onCancel={() => { setRoleFormOpen(false); setEditingRole(null); roleForm.resetFields() }}
        onOk={handleSaveRole}
        width={520}
      >
        <Form form={roleForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
                <Input placeholder="请输入角色名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="roleCode" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]}>
                <Input placeholder="请输入角色编码" disabled={!!editingRole} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="roleSort" label="排序" initialValue={0}>
                <Input type="number" placeholder="排序" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="dataScope" label="数据范围" initialValue={1}>
                <Select options={dataScopeOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <TextArea rows={2} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
