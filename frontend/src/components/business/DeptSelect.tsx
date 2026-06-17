import { useState, useEffect } from 'react'
import { Modal, Tree, Input, Button, Space, Tag, Empty, Spin } from 'antd'
import {
  SearchOutlined,
  TeamOutlined,
  CloseOutlined,
  ExpandOutlined,
  CollapseOutlined
} from '@ant-design/icons'
import { deptApi } from '@/api'
import type { DeptVO } from '@/types'

export interface DeptSelectProps {
  value?: number[] | number
  onChange?: (value: number[] | number) => void
  multiple?: boolean
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
  maxCount?: number
}

interface DeptItem {
  id: number
  parentId: number
  deptName: string
  leader: string
  children?: DeptItem[]
}

function DeptSelect(props: DeptSelectProps) {
  const {
    value,
    onChange,
    multiple = false,
    placeholder = '请选择部门',
    disabled = false,
    allowClear = true,
    maxCount
  } = props

  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [deptTree, setDeptTree] = useState<DeptItem[]>([])
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([])
  const [autoExpandParent, setAutoExpandParent] = useState(true)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [selectedDepts, setSelectedDepts] = useState<DeptItem[]>([])
  const [tempSelectedIds, setTempSelectedIds] = useState<number[]>([])

  const getFinalValue = (): number[] => {
    if (value === undefined || value === null) return []
    return Array.isArray(value) ? value : [value]
  }

  const flattenDept = (tree: DeptItem[]): DeptItem[] => {
    const result: DeptItem[] = []
    const walk = (items: DeptItem[]) => {
      items.forEach(item => {
        result.push(item)
        if (item.children && item.children.length > 0) {
          walk(item.children)
        }
      })
    }
    walk(tree)
    return result
  }

  const findDeptsByIds = (ids: number[], tree: DeptItem[]): DeptItem[] => {
    const flat = flattenDept(tree)
    return ids.map(id => flat.find(d => d.id === id)).filter(Boolean) as DeptItem[]
  }

  const mapDeptTree = (items: DeptVO[]): DeptItem[] => {
    return items.map(item => ({
      id: item.id,
      parentId: item.parentId,
      deptName: item.deptName,
      leader: item.leader,
      children: item.children ? mapDeptTree(item.children) : undefined
    }))
  }

  const getAllKeys = (items: DeptItem[]): number[] => {
    const keys: number[] = []
    const walk = (list: DeptItem[]) => {
      list.forEach(item => {
        keys.push(item.id)
        if (item.children && item.children.length > 0) {
          walk(item.children)
        }
      })
    }
    walk(items)
    return keys
  }

  const loadDeptTree = async () => {
    setLoading(true)
    try {
      const tree = await deptApi.tree()
      const mapped = mapDeptTree(tree)
      setDeptTree(mapped)
      const ids = getFinalValue()
      setSelectedDepts(findDeptsByIds(ids, mapped))
    } catch (_e) {
      setDeptTree([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (deptTree.length === 0) return
    const ids = getFinalValue()
    setSelectedDepts(findDeptsByIds(ids, deptTree))
  }, [JSON.stringify(getFinalValue())])

  const handleOpen = () => {
    setOpen(true)
    setTempSelectedIds(getFinalValue())
    setSearchKeyword('')
    loadDeptTree()
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

  const handleTagRemove = (deptId: number) => {
    const ids = getFinalValue().filter(id => id !== deptId)
    onChange?.(multiple ? ids : (ids[0] as any))
  }

  const renderTreeNodes = (items: DeptItem[]) => {
    return items.map(item => {
      const title = (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            fontSize: 13
          }}
        >
          <TeamOutlined style={{ color: '#1677ff' }} />
          <span>{item.deptName}</span>
          {item.leader && (
            <span style={{ color: '#8c8c8c', fontSize: 12 }}>({item.leader})</span>
          )}
        </div>
      )
      const nodeProps: any = {
        key: item.id,
        title
      }
      if (item.children && item.children.length > 0) {
        nodeProps.children = renderTreeNodes(item.children)
      }
      return {
        ...nodeProps,
        _data: item
      }
    })
  }

  const filterTreeNode = (node: any) => {
    if (!searchKeyword) return true
    const item = node._data as DeptItem
    return item.deptName.toLowerCase().includes(searchKeyword.toLowerCase())
  }

  const handleExpandAll = () => {
    setExpandedKeys(getAllKeys(deptTree) as any)
    setAutoExpandParent(false)
  }

  const handleCollapseAll = () => {
    setExpandedKeys([])
    setAutoExpandParent(false)
  }

  const handleSelect = (keys: React.Key[]) => {
    if (multiple) {
      const ids = keys as number[]
      if (maxCount && ids.length > maxCount) {
        setTempSelectedIds(ids.slice(0, maxCount))
      } else {
        setTempSelectedIds(ids)
      }
    } else {
      setTempSelectedIds((keys as number[]).slice(0, 1))
    }
  }

  const handleCheck = (checked: any) => {
    const ids = Array.isArray(checked) ? checked : checked.checked
    if (maxCount && ids.length > maxCount) {
      setTempSelectedIds(ids.slice(0, maxCount))
    } else {
      setTempSelectedIds(ids)
    }
  }

  const renderTags = () => {
    if (selectedDepts.length === 0) return null
    return (
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, flex: 1 }}>
        {selectedDepts.map(dept => (
          <Tag
            key={dept.id}
            closable={!disabled}
            onClose={(e) => {
              e.preventDefault()
              handleTagRemove(dept.id)
            }}
            style={{ margin: 0 }}
          >
            {dept.deptName}
          </Tag>
        ))}
      </div>
    )
  }

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
        {selectedDepts.length > 0 ? (
          renderTags()
        ) : (
          <span style={{ color: '#bfbfbf' }}>{placeholder}</span>
        )}
        {allowClear && selectedDepts.length > 0 && !disabled && (
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
        title="选择部门"
        open={open}
        onCancel={handleClose}
        onOk={handleConfirm}
        width={600}
        okText="确定"
        cancelText="取消"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, minHeight: 420 }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <Input
              placeholder="搜索部门名称"
              prefix={<SearchOutlined />}
              value={searchKeyword}
              onChange={(e) => {
                const val = e.target.value
                setSearchKeyword(val)
                if (val) {
                  const flat = flattenDept(deptTree)
                  const matched = flat.filter(d =>
                    d.deptName.toLowerCase().includes(val.toLowerCase())
                  )
                  const parentKeys: number[] = []
                  matched.forEach(m => {
                    let pid = m.parentId
                    while (pid) {
                      parentKeys.push(pid)
                      const parent = flat.find(f => f.id === pid)
                      pid = parent ? parent.parentId : 0
                    }
                  })
                  setExpandedKeys(Array.from(new Set([...parentKeys, ...matched.map(m => m.id)])) as any)
                  setAutoExpandParent(true)
                }
              }}
              allowClear
              style={{ flex: 1 }}
            />
            <Space>
              <Button size="small" icon={<ExpandOutlined />} onClick={handleExpandAll}>
                展开
              </Button>
              <Button size="small" icon={<CollapseOutlined />} onClick={handleCollapseAll}>
                收起
              </Button>
            </Space>
          </div>

          <div style={{ flex: 1, overflow: 'auto', border: '1px solid #f0f0f0', padding: 12, borderRadius: 6 }}>
            <Spin spinning={loading}>
              {deptTree.length === 0 && !loading ? (
                <Empty style={{ marginTop: 60 }} description="暂无部门数据" />
              ) : (
                <Tree
                  treeData={renderTreeNodes(deptTree)}
                  expandedKeys={expandedKeys}
                  autoExpandParent={autoExpandParent}
                  onExpand={(keys) => {
                    setExpandedKeys(keys)
                    setAutoExpandParent(false)
                  }}
                  selectedKeys={multiple ? [] : tempSelectedIds as any}
                  checkedKeys={multiple ? tempSelectedIds as any : undefined}
                  checkable={multiple}
                  onSelect={handleSelect}
                  onCheck={multiple ? handleCheck : undefined}
                  defaultExpandAll
                  filterTreeNode={filterTreeNode}
                  blockNode
                />
              )}
            </Spin>
          </div>

          {maxCount && (
            <div style={{ color: '#8c8c8c', fontSize: 12, textAlign: 'right' }}>
              已选 {tempSelectedIds.length}/{maxCount}
            </div>
          )}
          {!maxCount && multiple && (
            <div style={{ color: '#8c8c8c', fontSize: 12, textAlign: 'right' }}>
              已选 {tempSelectedIds.length} 个部门
            </div>
          )}
        </div>
      </Modal>
    </div>
  )
}

export { DeptSelect }
export default DeptSelect
