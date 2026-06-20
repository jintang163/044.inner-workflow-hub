import { useWebSocket } from '@/hooks/useWebSocket'
import { WS_BASE_URL } from '@/config/websocket'
import type { DictChangeMessage } from '@/types/dict'

const DICT_LOCAL_CACHE_PREFIX = 'dict_cache_'
const DATASOURCE_LOCAL_CACHE_PREFIX = 'ds_cache_'

type DictCacheListener = (dictCode: string, changeType: string) => void

class DictCacheManager {
  private listeners: Set<DictCacheListener> = new Set()
  private wsStarted = false
  private lastMessageMap: Map<string, any> = new Map()

  startGlobalWebSocket() {
    if (this.wsStarted) return
    this.wsStarted = true

    try {
      useWebSocket({
        url: WS_BASE_URL,
        onMessage: (msg: any) => {
          if (msg && msg.type === 'dictChange') {
            this.handleDictChange(msg as DictChangeMessage)
          }
        }
      })
    } catch (e) {
      console.warn('[DictCacheManager] 启动全局WebSocket失败:', e)
      this.wsStarted = false
    }
  }

  private handleDictChange(msg: DictChangeMessage) {
    const { dictCode, changeType } = msg

    const cacheKey = DICT_LOCAL_CACHE_PREFIX + dictCode
    try {
      localStorage.removeItem(cacheKey)
      const keysToRemove: string[] = []
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && key.startsWith(DICT_LOCAL_CACHE_PREFIX + dictCode + ':parent:')) {
          keysToRemove.push(key)
        }
      }
      keysToRemove.forEach(k => localStorage.removeItem(k))
    } catch (e) {
      console.warn('[DictCacheManager] 清理字典缓存失败:', e)
    }

    this.lastMessageMap.set(dictCode, msg)

    this.listeners.forEach(listener => {
      try {
        listener(dictCode, changeType)
      } catch (e) {
        console.warn('[DictCacheManager] 触发监听器失败:', e)
      }
    })

    console.log(`[DictCacheManager] 字典变更: ${dictCode} (${changeType}), 已清理本地缓存并通知${this.listeners.size}个订阅者`)
  }

  subscribe(listener: DictCacheListener): () => void {
    this.startGlobalWebSocket()
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  clearDictLocalCache(dictCode: string) {
    try {
      localStorage.removeItem(DICT_LOCAL_CACHE_PREFIX + dictCode)
    } catch (e) {
      // ignore
    }
  }

  clearDataSourceLocalCache(sourceCode: string) {
    try {
      const keysToRemove: string[] = []
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && key.startsWith(DATASOURCE_LOCAL_CACHE_PREFIX + sourceCode)) {
          keysToRemove.push(key)
        }
      }
      keysToRemove.forEach(k => localStorage.removeItem(k))
    } catch (e) {
      // ignore
    }
  }

  getLastMessage(dictCode: string) {
    return this.lastMessageMap.get(dictCode)
  }
}

export const dictCacheManager = new DictCacheManager()

let managerStarted = false
export function ensureDictCacheManagerStarted() {
  if (!managerStarted) {
    managerStarted = true
    dictCacheManager.startGlobalWebSocket()
  }
}
