import { useMemo, useCallback } from 'react'
import { Select, Cascader, Spin } from 'antd'
import { useDictOptions } from '@/hooks/useDictOptions'
import type { DataSourceConfig, SelectOption } from '@/types/form'

export interface DictSelectProps {
  value?: any
  onChange?: (value: any) => void
  dataSource?: DataSourceConfig
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
  multiple?: boolean
  loading?: boolean
}

function DictSelect(props: DictSelectProps) {
  const {
    value,
    onChange,
    dataSource,
    placeholder = '请选择',
    disabled = false,
    allowClear = true,
    multiple = false,
    loading: externalLoading
  } = props

  const { options, loading: dictLoading, getCascadeChildren } = useDictOptions(dataSource)
  const isLoading = externalLoading || dictLoading

  const isCascader = useMemo(() => {
    if (!options || options.length === 0) return false
    return options.some(opt => opt.children && opt.children.length > 0)
  }, [options])

  const handleSelectChange = useCallback((val: any) => {
    onChange?.(val)
  }, [onChange])

  const handleCascaderChange = useCallback((val: any, _selectedOptions: any) => {
    onChange?.(val)
  }, [onChange])

  if (isLoading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', height: 32 }}>
        <Spin size="small" />
        <span style={{ marginLeft: 8, color: '#999', fontSize: 13 }}>加载选项...</span>
      </div>
    )
  }

  if (isCascader) {
    return (
      <Cascader
        value={value}
        onChange={handleCascaderChange}
        options={options as any}
        placeholder={placeholder}
        disabled={disabled}
        allowClear={allowClear}
        changeOnSelect
        style={{ width: '100%' }}
      />
    )
  }

  return (
    <Select
      value={value}
      onChange={handleSelectChange}
      options={options.map(opt => ({
        label: opt.color ? (
          <span>
            <span
              style={{
                display: 'inline-block',
                width: 8,
                height: 8,
                borderRadius: '50%',
                background: getColorHex(opt.color),
                marginRight: 6,
                verticalAlign: 'middle'
              }}
            />
            {opt.label}
          </span>
        ) : opt.label,
        value: opt.value
      }))}
      placeholder={placeholder}
      disabled={disabled}
      allowClear={allowClear}
      mode={multiple ? 'multiple' : undefined}
      showSearch
      optionFilterProp="label"
      style={{ width: '100%' }}
      notFoundContent="暂无数据"
    />
  )
}

function getColorHex(color?: string): string {
  const colorMap: Record<string, string> = {
    blue: '#1677ff',
    green: '#52c41a',
    orange: '#fa8c16',
    red: '#ff4d4f',
    purple: '#722ed1',
    cyan: '#13c2c2',
    default: '#d9d9d9',
    gold: '#faad14',
    lime: '#a0d911',
    geekblue: '#2f54eb',
    magenta: '#eb2f96',
    volcano: '#fa541c',
    yellow: '#fadb14'
  }
  if (!color) return '#d9d9d9'
  return colorMap[color] || color
}

export { DictSelect }
export default DictSelect
