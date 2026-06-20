import React, { useState, useEffect, useRef } from 'react'
import { Button, Popover, Tag, Input, Empty, Spin } from 'antd'
import {
  FormOutlined,
  FireOutlined,
  UserOutlined,
  TeamOutlined,
  GlobalOutlined,
  PlusOutlined
} from '@ant-design/icons'
import { approvalApi } from '@/api'
import type { CommentTemplateVO, CommentTemplateCategoryVO } from '@/types/approval'

const { TextArea } = Input

interface CommentTemplateSelectProps {
  value?: string
  onChange?: (value: string) => void
  onTemplateSelect?: (template: CommentTemplateVO) => void
  placeholder?: string
  disabled?: boolean
  maxLength?: number
  rows?: number
  showCount?: boolean
  allowManage?: boolean
  onManageClick?: () => void
}

const CommentTemplateSelect: React.FC<CommentTemplateSelectProps> = ({
  value,
  onChange,
  onTemplateSelect,
  placeholder = '请输入审批意见',
  disabled = false,
  maxLength = 500,
  rows = 4,
  showCount = true,
  allowManage = true,
  onManageClick
}) => {
  const [loading, setLoading] = useState(false)
  const [categories, setCategories] = useState<CommentTemplateCategoryVO[]>([])
  const [templates, setTemplates] = useState<CommentTemplateVO[]>([])
  const [activeCategory, setActiveCategory] = useState<number | null>(null)
  const [popoverOpen, setPopoverOpen] = useState(false)

  const textAreaRef = useRef<any>(null)

  useEffect(() => {
    loadTemplates()
  }, [])

  const loadTemplates = async () => {
    try {
      setLoading(true)
      const [categoryRes, templateRes] = await Promise.all([
        approvalApi.commentTemplateCategoryAvailable(),
        approvalApi.commentTemplateMyAvailable()
      ])
      setCategories(categoryRes.data || [])
      setTemplates(templateRes.data || [])
      if (categoryRes.data && categoryRes.data.length > 0) {
        setActiveCategory(categoryRes.data[0].id)
      }
    } catch (e) {
      console.error('加载意见模板失败:', e)
    } finally {
      setLoading(false)
    }
  }

  const handleTemplateClick = (template: CommentTemplateVO) => {
    onChange?.(template.templateContent)
    onTemplateSelect?.(template)
    approvalApi.commentTemplateUse(template.id).catch(() => {})
    setPopoverOpen(false)
  }

  const getScopeIcon = (scopeType: number) => {
    switch (scopeType) {
      case 0:
        return <UserOutlined />
      case 1:
        return <TeamOutlined />
      case 2:
        return <GlobalOutlined />
      default:
        return <UserOutlined />
    }
  }

  const getScopeColor = (scopeType: number) => {
    switch (scopeType) {
      case 0:
        return 'blue'
      case 1:
        return 'green'
      case 2:
        return 'gold'
      default:
        return 'default'
    }
  }

  const filteredTemplates = activeCategory
    ? templates.filter(t => t.categoryId === activeCategory)
    : templates

  const hotTemplates = [...templates]
    .sort((a, b) => b.useCount - a.useCount)
    .slice(0, 5)

  const templatePopoverContent = (
    <div style={{ width: 320 }}>
      <div style={{ marginBottom: 12 }}>
        <div style={{ fontSize: 12, color: '#999', marginBottom: 8 }}>
          <FireOutlined style={{ color: '#ff4d4f', marginRight: 4 }} />
          常用模板
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {hotTemplates.length > 0 ? (
            hotTemplates.map(template => (
              <Tag
                key={template.id}
                color={getScopeColor(template.scopeType)}
                style={{
                  cursor: 'pointer',
                  margin: 0,
                  padding: '4px 8px',
                  fontSize: 12
                }}
                onClick={() => handleTemplateClick(template)}
              >
                {template.templateName}
              </Tag>
            ))
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ margin: '12px 0' }} />
          )}
        </div>
      </div>

      <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 12 }}>
        <div style={{ fontSize: 12, color: '#999', marginBottom: 8 }}>
          按分类选择
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 12 }}>
          {categories.map(category => (
            <Tag
              key={category.id}
              color={activeCategory === category.id ? 'blue' : 'default'}
              style={{
                cursor: 'pointer',
                margin: 0,
                padding: '4px 8px',
                fontSize: 12
              }}
              onClick={() => setActiveCategory(category.id)}
            >
              {category.categoryName}
            </Tag>
          ))}
        </div>

        <div style={{ maxHeight: 200, overflowY: 'auto' }}>
          {loading ? (
            <div style={{ textAlign: 'center', padding: '20px 0' }}>
              <Spin size="small" />
            </div>
          ) : filteredTemplates.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {filteredTemplates.map(template => (
                <div
                  key={template.id}
                  style={{
                    padding: '8px 12px',
                    borderRadius: 4,
                    cursor: 'pointer',
                    transition: 'background-color 0.2s'
                  }}
                  onClick={() => handleTemplateClick(template)}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.backgroundColor = '#f5f5f5'
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = 'transparent'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                    <Tag color={getScopeColor(template.scopeType)} style={{ margin: 0, fontSize: 11 }}>
                      {getScopeIcon(template.scopeType)}
                      <span style={{ marginLeft: 4 }}>{template.templateName}</span>
                    </Tag>
                    {template.useCount > 0 && (
                      <span style={{ fontSize: 11, color: '#999' }}>
                        <FireOutlined style={{ color: '#ff4d4f', fontSize: 10 }} /> {template.useCount}
                      </span>
                    )}
                  </div>
                  <div style={{
                    fontSize: 12,
                    color: '#666',
                    lineHeight: 1.4,
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                    overflow: 'hidden'
                  }}>
                    {template.templateContent}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无模板" style={{ margin: '12px 0' }} />
          )}
        </div>
      </div>

      {allowManage && (
        <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 12, marginTop: 12, textAlign: 'right' }}>
          <Button
            type="link"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => {
              setPopoverOpen(false)
              onManageClick?.()
            }}
          >
            管理模板
          </Button>
        </div>
      )}
    </div>
  )

  return (
    <div style={{ position: 'relative' }}>
      <TextArea
        ref={textAreaRef}
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        placeholder={placeholder}
        rows={rows}
        maxLength={maxLength}
        showCount={showCount}
        disabled={disabled}
        style={{ paddingRight: 100 }}
      />
      <div
        style={{
          position: 'absolute',
          top: 8,
          right: 8,
          display: 'flex',
          gap: 4
        }}
      >
        <Popover
          content={templatePopoverContent}
          title="选择意见模板"
          trigger="click"
          open={popoverOpen}
          onOpenChange={setPopoverOpen}
          placement="bottomRight"
          arrow={false}
        >
          <Button
            type="text"
            size="small"
            icon={<FormOutlined />}
            disabled={disabled}
          >
            模板
          </Button>
        </Popover>
      </div>
    </div>
  )
}

export default CommentTemplateSelect
