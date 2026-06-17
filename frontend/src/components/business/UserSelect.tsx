import { useState, useEffect } from 'react'
import { Modal, Table, Input, Select, Spin, Empty, Tag, Button, Space } from 'antd'
import { SearchOutlined, TeamOutlined, UserOutlined, CloseOutlined } from '@ant-design/icons'
import { userApi, deptApi } from '@/api'
import type { UserVO, DeptVO, PageResult } from '@/types'

export interface UserSelectProps {
  value?: number[] | number
  onChange?: (value: number[] | number) => void
  multiple?: boolean
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
  maxCount?: number
}

interface UserItem {
  id: number
  username: string
  nickname: string
  deptId: number
  deptName: string
  avatar: string
}

function UserSelect(props: UserSelectProps) {
  const {
    value,
    onChange,
    multiple = true,
    placeholder = '请选择人员',
    disabled = false,
    allowClear = true,
    maxCount
  } = props

  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [selectedUsers, setSelectedUsers] = useState<UserItem[]>([])
  const [deptTree, setDeptTree] = useState<DeptVO[]>([])
  const [selectedDept, setSelectedDept] = useState<number | null>(null)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [userList, setUserList] = useState<UserVO[]>([])
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const [tempSelectedIds, setTempSelectedIds] = useState<number[]>([])

  const getFinalValue = (): number[] => {
    if (value === undefined || value === null) return []
    return Array.isArray(value) ? value : [value]
  }

  useEffect(() => {
    const ids = getFinalValue()
    if (ids.length === 0) {
      setSelectedUsers([])
      return
    }
    const fetchUsers = async () => {
      try {
        const users: UserItem[] = []
        for (const id of ids) {
          try {
            const user = await userApi.get(id)
            users.push({
              id: user.id,
              username: user.username,
              nickname: user.nickname,
              deptId: user.deptId,
              deptName: user.deptName,
              avatar: user.avatar
            })
          } catch (_e) {
            // ignore
          }
        }
        setSelectedUsers(users)
      } catch (_e) {
        // ignore
      }
    }
    fetchUsers()
  }, [JSON.stringify(getFinalValue())])

  const loadDeptTree = async () => {
    try {
      const tree = await deptApi.tree()
      setDeptTree(tree)
    } catch (_e) {
      // ignore
    }
  }

  const loadUserList = async (page = 1) => {
    setLoading(true)
    try {
      const params: any = {
        pageNum: page,
        pageSize: pagination.pageSize
      }
      if (selectedDept) {
        params.deptId = selectedDept
      }
      if (searchKeyword) {
        params.keyword = searchKeyword
      }
      const result: PageResult<UserVO> = await userApi.list(params)
      setUserList(result.list)
      setPagination({
        current: page,
        pageSize: result.pageSize,
        total: result.total
      })
    } catch (_e) {
      setUserList([])
    } finally {
      setLoading(false)
    }
  }

  const handleOpen = () => {
    setOpen(true)
    setTempSelectedIds(getFinalValue())
    setSearchKeyword('')
    setSelectedDept(null)
    loadDeptTree()
    loadUserList(1)
  }

  const handleClose = () => {
    setOpen(false)
  }

  const handleConfirm = () => {
    setOpen(false)
    if (multiple) {
      const result = maxCount ? tempSelectedIds.slice(0, maxCount) : tempSelectedIds
      onChange?.(result)
    } else {
      onChange?.(tempSelectedIds[0])
    }
  }

  const handleDeptSelect = (deptId: number | null) => {
    setSelectedDept(deptId)
    loadUserList(1)
  }

  const handleSearch = () => {
    loadUserList(1)
  }

  const toggleUserSelection = (userId: number, checked: boolean) => {
    if (multiple) {
      setTempSelectedIds(prev => {
        if (checked) {
          if (maxCount && prev.length >= maxCount) return prev
          return [...prev, userId]
        }
        return prev.filter(id => id !== userId)
      })
    } else {
      setTempSelectedIds(checked ? [userId] : [])
    }
  }

  const handleTagRemove = (userId: number) => {
    const ids = getFinalValue().filter(id => id !== userId)
    onChange?.(multiple ? ids : (ids[0] as any))
  }

  const renderDeptTree = (items: DeptVO[], level = 0) => {
    return items.map(item => (
      <div key={item.id}>
        <div
          style={{
            padding: '8px 12px',
            paddingLeft: 12 + level * 20,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            background: selectedDept === item.id ? '#e6f4ff' : 'transparent',
            borderRadius: 4,
            fontSize: 13
          }}
          onClick={() => handleDeptSelect(selectedDept === item.id ? null : item.id)}
        >
          <TeamOutlined style={{ color: '#1677ff' }} />
          <span>{item.deptName}</span>
        </div>
        {item.children && item.children.length > 0 && renderDeptTree(item.children, level + 1)}
      </div>
    ))
  }

  const renderTags = () => {
    if (selectedUsers.length === 0) return null
    return (
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, flex: 1 }}>
        {selectedUsers.map(user => (
          <Tag
            key={user.id}
            closable={!disabled}
            onClose={(e) => {
              e.preventDefault()
              handleTagRemove(user.id)
            }}
            style={{ margin: 0 }}
          >
            {user.nickname || user.username}
          </Tag>
        ))}
      </div>
    )
  }

  const columns = [
    {
      title: '姓名',
      dataIndex: 'nickname',
      key: 'nickname',
      render: (_text: string, record: UserVO) => (
        <Space>
          <UserOutlined />
          <span>{record.nickname || record.username}</span>
        </Space>
      )
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username'
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      key: 'deptName'
    }
  ]

  return (
    <div style={{ width: '100%' }}>
      <div
        style={{
          minHeight: 32,
          padding: '4px 11px',
          border: '1px solid #d9d9d9',
          borderRadius: 6,
          display: 'flex',
          alignItems: 'center',
          gap: 4,
          cursor: disabled ? 'not-allowed' : 'pointer',
          background: disabled ? '#f5f5f5' : '#fff',
          flexWrap: 'wrap'
        }}
        onClick={() => !disabled && handleOpen()}
      >
        {selectedUsers.length > 0 ? (
          renderTags()
        ) : (
          <span style={{ color: '#bfbfbf' }}>{placeholder}</span>
        )}
        {allowClear && selectedUsers.length > 0 && !disabled && (
          <CloseOutlined
            style={{ color: '#bfbfbf', fontSize: 12 }}
            onClick={(e) => {
              e.stopPropagation()
              onChange?.(multiple ? [] : (undefined as any))
            }}
          />
        )}
      </div>

      <Modal
        title="选择人员"
        open={open}
        onCancel={handleClose}
        onOk={handleConfirm}
        width={900}
        okText="确定"
        cancelText="取消"
        styles={{ body: { padding: 0 } }}
      >
        <div style={{ display: 'flex', height: 480 }}>
          <div
            style={{
              width: 220,
              borderRight: '1px solid #f0f0f0',
              overflow: 'auto',
              padding: '12px 8px'
            }}
          >
            <div style={{ fontWeight: 500, marginBottom: 8, paddingLeft: 8 }}>组织架构</div>
            <div
              style={{
                padding: '8px 12px',
                cursor: 'pointer',
                background: selectedDept === null ? '#e6f4ff' : 'transparent',
                borderRadius: 4,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                fontSize: 13
              }}
              onClick={() => handleDeptSelect(null)}
            >
              <TeamOutlined style={{ color: '#1677ff' }} />
              <span>全部人员</span>
            </div>
            {renderDeptTree(deptTree)}
          </div>

          <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <div
              style={{
                padding: 12,
                borderBottom: '1px solid #f0f0f0',
                display: 'flex',
                gap: 8,
                alignItems: 'center'
              }}
            >
              <Input
                placeholder="搜索姓名/用户名"
                prefix={<SearchOutlined />}
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onPressEnter={handleSearch}
                style={{ width: 260 }}
                allowClear
              />
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                搜索
              </Button>
              {maxCount && (
                <span style={{ marginLeft: 'auto', color: '#8c8c8c', fontSize: 12 }}>
                  已选 {tempSelectedIds.length}/{maxCount}
                </span>
              )}
              {!maxCount && multiple && (
                <span style={{ marginLeft: 'auto', color: '#8c8c8c', fontSize: 12 }}>
                  已选 {tempSelectedIds.length} 人
                </span>
              )}
            </div>

            <div style={{ flex: 1, overflow: 'auto' }}>
              <Spin spinning={loading}>
                {userList.length === 0 && !loading ? (
                  <Empty style={{ marginTop: 80 }} description="暂无人员数据" />
                ) : (
                  <Table
                    dataSource={userList}
                    columns={columns}
                    rowKey="id"
                    size="small"
                    pagination={{
                      ...pagination,
                      onChange: (page) => loadUserList(page),
                      showSizeChanger: false
                    }}
                    rowSelection={{
                      type: multiple ? 'checkbox' : 'radio',
                      selectedRowKeys: tempSelectedIds,
                      onChange: (_keys, rows) => {
                        if (multiple) {
                          const ids = rows.map(r => r.id)
                          if (maxCount && ids.length > maxCount) {
                            setTempSelectedIds(ids.slice(0, maxCount))
                          } else {
                            setTempSelectedIds(ids)
                          }
                        } else {
                          setTempSelectedIds(rows.map(r => r.id))
                        }
                      },
                      onSelect: (record, checked) => {
                        toggleUserSelection(record.id, checked)
                      }
                    }}
                  />
                )}
              </Spin>
            </div>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export { UserSelect }
export default UserSelect
