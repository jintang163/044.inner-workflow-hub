import React, { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Popover, Tag, Input, Empty, Spin, Tooltip, message } from 'antd'
import {
  FormOutlined,
  FireOutlined,
  UserOutlined,
  TeamOutlined,
  GlobalOutlined,
  SettingOutlined,
  AudioOutlined,
  AudioMutedOutlined
} from '@ant-design/icons'
import { approvalApi } from '@/api'
import type { CommentTemplateVO, CommentTemplateCategoryVO } from '@/types/approval'
import useSpeechRecognition from '@/hooks/useSpeechRecognition'
import { addPunctuation, cleanTranscript } from '@/utils/speech'

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
  allowManage = true
}) => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [categories, setCategories] = useState<CommentTemplateCategoryVO[]>([])
  const [templates, setTemplates] = useState<CommentTemplateVO[]>([])
  const [activeCategory, setActiveCategory] = useState<number | null>(null)
  const [popoverOpen, setPopoverOpen] = useState(false)
  const [speechPopoverOpen, setSpeechPopoverOpen] = useState(false)

  const textAreaRef = useRef<any>(null)

  const {
    isListening,
    isSupported,
    transcript,
    interimTranscript,
    error,
    startListening,
    stopListening,
    resetTranscript
  } = useSpeechRecognition({ lang: 'zh-CN' })

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

  useEffect(() => {
    if (error) {
      message.error(error)
    }
  }, [error])

  const handleSpeechToggle = () => {
    if (!isSupported) {
      message.warning('当前浏览器不支持语音识别，请使用 Chrome 或 Edge 浏览器')
      return
    }
    if (isListening) {
      stopListening()
    } else {
      resetTranscript()
      startListening()
      setSpeechPopoverOpen(true)
    }
  }

  const handleSpeechConfirm = () => {
    const rawText = (transcript || '').trim()
    if (rawText) {
      const cleaned = cleanTranscript(rawText)
      const withPunctuation = addPunctuation(cleaned)
      const newValue = value ? value + (value.endsWith('\n') ? '' : '，') + withPunctuation : withPunctuation
      onChange?.(newValue)
      message.success('语音录入成功')
    }
    stopListening()
    setSpeechPopoverOpen(false)
    resetTranscript()
  }

  const handleSpeechCancel = () => {
    stopListening()
    setSpeechPopoverOpen(false)
    resetTranscript()
  }

  const currentDisplayText = (transcript || '') + (interimTranscript || '')

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
            icon={<SettingOutlined />}
            onClick={() => {
              setPopoverOpen(false)
              navigate('/approval/comment-template')
            }}
          >
            管理模板
          </Button>
        </div>
      )}
    </div>
  )

  const speechPopoverContent = (
    <div style={{ width: 320 }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '20px 0',
          marginBottom: 12
        }}
      >
        <div
          onClick={handleSpeechToggle}
          style={{
            width: 64,
            height: 64,
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            backgroundColor: isListening ? '#ff4d4f' : '#1890ff',
            color: '#fff',
            boxShadow: isListening ? '0 0 0 8px rgba(255,77,79,0.2)' : '0 2px 8px rgba(24,144,255,0.3)',
            transition: 'all 0.3s ease',
            animation: isListening ? 'pulse 1.5s infinite' : 'none'
          }}
        >
          {isListening ? (
            <AudioMutedOutlined style={{ fontSize: 28 }} />
          ) : (
            <AudioOutlined style={{ fontSize: 28 }} />
          )}
        </div>
      </div>

      <div style={{ textAlign: 'center', marginBottom: 12 }}>
        {isListening ? (
          <div>
            <div style={{ color: '#ff4d4f', fontWeight: 500 }}>正在聆听...</div>
            <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>请说话，说完后点击确认</div>
          </div>
        ) : (
          <div>
            <div style={{ color: '#666' }}>已停止录音</div>
            <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>点击图标继续录音</div>
          </div>
        )}
      </div>

      <div
        style={{
          padding: 12,
          backgroundColor: '#fafafa',
          borderRadius: 4,
          minHeight: 80,
          maxHeight: 150,
          overflowY: 'auto',
          marginBottom: 12
        }}
      >
        {currentDisplayText ? (
          <div>
            <span>{transcript}</span>
            {interimTranscript && (
              <span style={{ color: '#999' }}>{interimTranscript}</span>
            )}
          </div>
        ) : (
          <div style={{ color: '#bbb', textAlign: 'center', padding: '12px 0' }}>
            {isListening ? '等待语音输入...' : '暂无识别内容'}
          </div>
        )}
      </div>

      <div style={{ fontSize: 12, color: '#999', marginBottom: 12, padding: '0 4px' }}>
        <div>• 支持说出标点符号：逗号、句号、问号等</div>
        <div>• 系统会自动补全部分标点符号</div>
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <Button
          style={{ flex: 1 }}
          onClick={handleSpeechCancel}
        >
          取消
        </Button>
        <Button
          type="primary"
          style={{ flex: 1 }}
          onClick={handleSpeechConfirm}
          disabled={!transcript}
        >
          确认填入
        </Button>
      </div>

      <style>
        {`
          @keyframes pulse {
            0% { box-shadow: 0 0 0 0 rgba(255,77,79,0.4); }
            70% { box-shadow: 0 0 0 16px rgba(255,77,79,0); }
            100% { box-shadow: 0 0 0 0 rgba(255,77,79,0); }
          }
        `}
      </style>
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
        style={{ paddingRight: 160 }}
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
        <Tooltip title={isSupported ? '语音录入' : '当前浏览器不支持语音识别'}>
          <Popover
            content={speechPopoverContent}
            title="语音录入审批意见"
            trigger="click"
            open={speechPopoverOpen}
            onOpenChange={(open) => {
              if (!open && isListening) {
                stopListening()
              }
              setSpeechPopoverOpen(open)
            }}
            placement="bottomRight"
            arrow={false}
          >
            <Button
              type={isListening ? 'primary' : 'text'}
              size="small"
              danger={isListening}
              icon={isListening ? <AudioMutedOutlined /> : <AudioOutlined />}
              disabled={disabled}
            >
              语音
            </Button>
          </Popover>
        </Tooltip>
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
