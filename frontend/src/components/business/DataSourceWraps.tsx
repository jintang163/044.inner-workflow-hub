import { useMemo } from 'react'
import { Select, Cascader, Radio, Checkbox, Spin } from 'antd'
import { useDictOptions } from '@/hooks/useDictOptions'
import type { DataSourceConfig, SelectOption } from '@/types/form'

type AnyProps = Record<string, any>

function hasDataSourceConfig(props: AnyProps): boolean {
  const ds = props.dataSource
  return !!ds && !!ds.type && ds.type !== 'static'
}

function getDataSource(props: AnyProps): DataSourceConfig | undefined {
  const ds = props.dataSource as DataSourceConfig
  if (!ds || !ds.type) return undefined
  if (ds.type === 'static' && ds.options) return undefined
  return ds
}

function flattenOptionsWithLoadingIndicator(items: SelectOption[]) {
  return items.map(o => ({
    label: o.label,
    value: o.value,
    ...(o.children ? { children: flattenOptionsWithLoadingIndicator(o.children) } : {})
  }))
}

interface WrapProps {
  dataSource?: DataSourceConfig
  options?: SelectOption[]
  [key: string]: any
}

function DataSourceSelectWrap(props: WrapProps) {
  const { dataSource, options: staticOptions, ...rest } = props
  const ds = getDataSource({ dataSource } as AnyProps)
  const { options, loading } = useDictOptions(ds)

  const mergedOptions = useMemo(() => {
    if (options && options.length > 0) {
      return flattenOptionsWithLoadingIndicator(options as SelectOption[])
    }
    return staticOptions || []
  }, [options, staticOptions])

  return (
    <Select
      {...rest}
      options={mergedOptions as any}
      loading={rest.loading || loading}
    />
  )
}

function DataSourceCascaderWrap(props: WrapProps) {
  const { dataSource, options: staticOptions, loadData, ...rest } = props
  const ds = getDataSource({ dataSource } as AnyProps)
  const { options, loading, getCascadeChildren } = useDictOptions(ds)

  const mergedOptions = useMemo(() => {
    if (options && options.length > 0) {
      return flattenOptionsWithLoadingIndicator(options as SelectOption[])
    }
    return staticOptions || []
  }, [options, staticOptions])

  const mergedLoadData = useMemo(() => {
    if (loadData) return loadData
    if (!ds || ds.type !== 'dict') return undefined
    return async (selectedOptions: any[]) => {
      const last = selectedOptions[selectedOptions.length - 1]
      if (!last) return
      const parentValue = String(last.value)
      const children = await getCascadeChildren(parentValue)
      const normalized = children.map(c => ({ label: c.label, value: c.value }))
      last.children = normalized
    }
  }, [loadData, ds, getCascadeChildren])

  const showLoading = loading && mergedOptions.length === 0

  return showLoading ? (
    <div style={{ display: 'flex', alignItems: 'center', height: 32 }}>
      <Spin size="small" />
      <span style={{ marginLeft: 8, color: '#999', fontSize: 13 }}>加载选项...</span>
    </div>
  ) : (
    <Cascader
      {...rest}
      options={mergedOptions as any}
      loadData={mergedLoadData}
    />
  )
}

function DataSourceRadioGroupWrap(props: WrapProps) {
  const { dataSource, options: staticOptions, ...rest } = props
  const ds = getDataSource({ dataSource } as AnyProps)
  const { options, loading } = useDictOptions(ds)

  const mergedOptions = useMemo(() => {
    if (options && options.length > 0) {
      return options.map(o => ({
        label: o.label,
        value: o.value as any
      }))
    }
    return staticOptions || []
  }, [options, staticOptions])

  if (loading && mergedOptions.length === 0) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', height: 32 }}>
        <Spin size="small" />
        <span style={{ marginLeft: 8, color: '#999', fontSize: 13 }}>加载选项...</span>
      </div>
    )
  }

  return <Radio.Group {...rest} options={mergedOptions as any} />
}

function DataSourceCheckboxGroupWrap(props: WrapProps) {
  const { dataSource, options: staticOptions, ...rest } = props
  const ds = getDataSource({ dataSource } as AnyProps)
  const { options, loading } = useDictOptions(ds)

  const mergedOptions = useMemo(() => {
    if (options && options.length > 0) {
      return options.map(o => ({
        label: o.label,
        value: o.value as any
      }))
    }
    return staticOptions || []
  }, [options, staticOptions])

  if (loading && mergedOptions.length === 0) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', height: 32 }}>
        <Spin size="small" />
        <span style={{ marginLeft: 8, color: '#999', fontSize: 13 }}>加载选项...</span>
      </div>
    )
  }

  return <Checkbox.Group {...rest} options={mergedOptions as any} />
}

export {
  DataSourceSelectWrap,
  DataSourceCascaderWrap,
  DataSourceRadioGroupWrap,
  DataSourceCheckboxGroupWrap,
  hasDataSourceConfig,
  getDataSource
}
