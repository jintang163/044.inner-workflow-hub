import { useState, useEffect, useCallback } from 'react'
import { dictApi, dataSourceApi } from '@/api'
import type { DictOptionItem, SysDictDataVO, DataSourceConfig } from '@/types/form'
import { dictCacheManager, ensureDictCacheManagerStarted } from '@/store/dictCache'

const DICT_LOCAL_CACHE_PREFIX = 'dict_cache_'
const DATASOURCE_LOCAL_CACHE_PREFIX = 'ds_cache_'
const DEFAULT_CACHE_TTL = 30 * 60 * 1000

interface CacheEntry {
  data: DictOptionItem[]
  timestamp: number
  ttl: number
}

const pendingRequests: Record<string, Promise<DictOptionItem[]>> = {}

function getLocalCache(key: string): DictOptionItem[] | null {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const entry: CacheEntry = JSON.parse(raw)
    if (Date.now() - entry.timestamp > entry.ttl) {
      localStorage.removeItem(key)
      return null
    }
    return entry.data
  } catch {
    return null
  }
}

function setLocalCache(key: string, data: DictOptionItem[], ttl = DEFAULT_CACHE_TTL) {
  try {
    const entry: CacheEntry = { data, timestamp: Date.now(), ttl }
    localStorage.setItem(key, JSON.stringify(entry))
  } catch {
    // ignore quota exceeded
  }
}

function removeLocalCache(key: string) {
  try { localStorage.removeItem(key) } catch {}
}

function convertDictDataToOptions(items: SysDictDataVO[]): DictOptionItem[] {
  return items.map(item => ({
    label: item.dictLabel,
    value: item.dictValue,
    color: item.colorTag,
    children: item.children && item.children.length > 0
      ? convertDictDataToOptions(item.children)
      : undefined
  }))
}

export interface UseDictOptionsResult {
  options: DictOptionItem[]
  loading: boolean
  refresh: (forceRemote?: boolean) => void
  getCascadeChildren: (parentValue: string) => Promise<DictOptionItem[]>
}

export function useDictOptions(
  dataSource?: DataSourceConfig
): UseDictOptionsResult {
  const [options, setOptions] = useState<DictOptionItem[]>([])
  const [loading, setLoading] = useState(false)
  const dataSourceType = dataSource?.type
  const dictCode = dataSource?.dictCode
  const sourceCode = dataSource?.sourceCode
  const apiUrl = dataSource?.apiUrl
  const optionsStr = JSON.stringify(dataSource?.options || [])

  const loadOptions = useCallback(async (forceRemote = false) => {
    if (!dataSource) {
      setOptions([])
      return
    }

    setLoading(true)
    try {
      let result: DictOptionItem[] = []

      switch (dataSource.type) {
        case 'static': {
          result = dataSource.options || []
          break
        }
        case 'dict': {
          if (!dataSource.dictCode) {
            result = []
            break
          }
          const cacheKey = DICT_LOCAL_CACHE_PREFIX + dataSource.dictCode
          if (!forceRemote) {
            const cached = getLocalCache(cacheKey)
            if (cached) {
              result = cached
              break
            }
          }

          const dedupeKey = `dict_${dataSource.dictCode}`
          if (!forceRemote && pendingRequests[dedupeKey]) {
            result = await pendingRequests[dedupeKey]
          } else {
            const promise = (async () => {
              const data = await dictApi.dataList(dataSource.dictCode!)
              return convertDictDataToOptions(data)
            })()
            pendingRequests[dedupeKey] = promise
            try {
              result = await promise
              setLocalCache(cacheKey, result)
            } finally {
              delete pendingRequests[dedupeKey]
            }
          }
          break
        }
        case 'api': {
          if (dataSource.sourceCode) {
            const cacheKey = DATASOURCE_LOCAL_CACHE_PREFIX + dataSource.sourceCode
            if (!forceRemote) {
              const cached = getLocalCache(cacheKey)
              if (cached) {
                result = cached
                break
              }
            }

            const dedupeKey = `ds_${dataSource.sourceCode}`
            if (!forceRemote && pendingRequests[dedupeKey]) {
              result = await pendingRequests[dedupeKey]
            } else {
              const promise = dataSourceApi.fetchData(dataSource.sourceCode)
              pendingRequests[dedupeKey] = promise
              try {
                result = await promise
                setLocalCache(cacheKey, result)
              } finally {
                delete pendingRequests[dedupeKey]
              }
            }
          } else if (dataSource.apiUrl) {
            result = dataSource.options || []
          }
          break
        }
        default: {
          result = dataSource.options || []
        }
      }

      setOptions(result)
    } catch (err) {
      console.warn('[useDictOptions] 加载选项失败:', err)
      setOptions(dataSource.options || [])
    } finally {
      setLoading(false)
    }
  }, [dataSourceType, dictCode, sourceCode, apiUrl, optionsStr])

  const refresh = useCallback((forceRemote = true) => {
    if (!dataSource) return
    if (forceRemote) {
      if (dataSource.type === 'dict' && dataSource.dictCode) {
        removeLocalCache(DICT_LOCAL_CACHE_PREFIX + dataSource.dictCode)
        dictCacheManager.clearDictLocalCache(dataSource.dictCode)
      }
      if (dataSource.type === 'api' && dataSource.sourceCode) {
        dictCacheManager.clearDataSourceLocalCache(dataSource.sourceCode)
      }
    }
    loadOptions(forceRemote)
  }, [dataSource, loadOptions])

  const getCascadeChildren = useCallback(async (parentValue: string): Promise<DictOptionItem[]> => {
    if (!dataSource || dataSource.type !== 'dict' || !dataSource.dictCode) {
      return []
    }
    try {
      const cacheKey = DICT_LOCAL_CACHE_PREFIX + dataSource.dictCode + ':parent:' + parentValue
      const cached = getLocalCache(cacheKey)
      if (cached) return cached

      const data = await dictApi.cascadeData(dataSource.dictCode, parentValue)
      const opts = convertDictDataToOptions(data)
      setLocalCache(cacheKey, opts)
      return opts
    } catch (err) {
      console.warn('[useDictOptions] 加载级联数据失败:', err)
      return []
    }
  }, [dataSource?.type, dataSource?.dictCode])

  useEffect(() => {
    ensureDictCacheManagerStarted()
    loadOptions()
  }, [loadOptions])

  useEffect(() => {
    if (!dataSource || dataSource.type !== 'dict' || !dataSource.dictCode) return
    const myDictCode = dataSource.dictCode

    const unsubscribe = dictCacheManager.subscribe((changedDictCode: string, _changeType: string) => {
      if (changedDictCode === myDictCode) {
        loadOptions(true)
      }
    })
    return unsubscribe
  }, [dataSource?.type, dataSource?.dictCode, loadOptions])

  return { options, loading, refresh, getCascadeChildren }
}

export function useDictOptionsSimple(dictCode: string): UseDictOptionsResult {
  const dataSource: DataSourceConfig = {
    type: 'dict',
    dictCode
  }
  return useDictOptions(dataSource)
}

export function useDataSourceOptions(sourceCode: string): UseDictOptionsResult {
  const dataSource: DataSourceConfig = {
    type: 'api',
    sourceCode,
    options: []
  }
  return useDictOptions(dataSource)
}
