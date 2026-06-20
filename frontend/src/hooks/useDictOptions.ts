import { useState, useEffect, useCallback, useRef } from 'react'
import { dictApi, dataSourceApi } from '@/api'
import type { DictOptionItem, SysDictDataVO, DataSourceConfig } from '@/types/form'
import type { DictChangeMessage } from '@/types/dict'

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
  localStorage.removeItem(key)
}

function convertDictDataToOptions(items: SysDictDataVO[]): DictOptionItem[] {
  return items.map(item => ({
    label: item.dictLabel,
    value: item.dictValue,
    color: item.colorTag,
    children: item.children ? convertDictDataToOptions(item.children) : undefined
  }))
}

export interface UseDictOptionsResult {
  options: DictOptionItem[]
  loading: boolean
  refresh: () => void
  getCascadeChildren: (parentValue: string) => Promise<DictOptionItem[]>
}

export function useDictOptions(
  dataSource?: DataSourceConfig,
  wsRef?: React.RefObject<{ lastMessage: any } | null>
): UseDictOptionsResult {
  const [options, setOptions] = useState<DictOptionItem[]>([])
  const [loading, setLoading] = useState(false)
  const refreshRef = useRef<(() => void) | null>(null)

  const loadOptions = useCallback(async () => {
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
          const cached = getLocalCache(cacheKey)
          if (cached) {
            result = cached
            break
          }

          const dedupeKey = `dict_${dataSource.dictCode}`
          if (pendingRequests[dedupeKey]) {
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
            const cached = getLocalCache(cacheKey)
            if (cached) {
              result = cached
              break
            }

            const dedupeKey = `ds_${dataSource.sourceCode}`
            if (pendingRequests[dedupeKey]) {
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
  }, [dataSource?.type, dataSource?.dictCode, dataSource?.sourceCode, dataSource?.apiUrl,
    JSON.stringify(dataSource?.options)])

  const refresh = useCallback(() => {
    if (!dataSource) return

    if (dataSource.type === 'dict' && dataSource.dictCode) {
      removeLocalCache(DICT_LOCAL_CACHE_PREFIX + dataSource.dictCode)
    }
    if (dataSource.type === 'api' && dataSource.sourceCode) {
      removeLocalCache(DATASOURCE_LOCAL_CACHE_PREFIX + dataSource.sourceCode)
    }
    loadOptions()
  }, [dataSource, loadOptions])

  refreshRef.current = refresh

  const getCascadeChildren = useCallback(async (parentValue: string): Promise<DictOptionItem[]> => {
    if (!dataSource || dataSource.type !== 'dict' || !dataSource.dictCode) {
      return []
    }
    try {
      const data = await dictApi.cascadeData(dataSource.dictCode, parentValue)
      return convertDictDataToOptions(data)
    } catch (err) {
      console.warn('[useDictOptions] 加载级联数据失败:', err)
      return []
    }
  }, [dataSource?.type, dataSource?.dictCode])

  useEffect(() => {
    loadOptions()
  }, [loadOptions])

  useEffect(() => {
    if (!wsRef?.current?.lastMessage) return
    const msg = wsRef.current.lastMessage as DictChangeMessage
    if (msg.type !== 'dictChange') return
    if (dataSource?.type === 'dict' && dataSource.dictCode === msg.dictCode) {
      if (refreshRef.current) {
        refreshRef.current()
      }
    }
  }, [wsRef?.current?.lastMessage, dataSource?.type, dataSource?.dictCode])

  return { options, loading, refresh, getCascadeChildren }
}

export function useDictOptionsSimple(dictCode: string): UseDictOptionsResult {
  const dataSource: DataSourceConfig = {
    type: 'dict',
    dictCode
  }
  return useDictOptions(dataSource)
}

export function useDataSourceOptions(sourceCode: string, params?: Record<string, any>): UseDictOptionsResult {
  const dataSource: DataSourceConfig = {
    type: 'api',
    sourceCode,
    options: []
  }
  return useDictOptions(dataSource)
}
