import { useState, useEffect, useRef, useCallback } from 'react'
import { getToken } from '@/utils/auth'

interface UseWebSocketOptions {
  url: string
  onMessage?: (message: any) => void
  onOpen?: () => void
  onClose?: () => void
  onError?: (error: Event) => void
  autoReconnect?: boolean
  reconnectInterval?: number
  maxReconnectAttempts?: number
}

export function useWebSocket(options: UseWebSocketOptions) {
  const {
    url,
    onMessage,
    onOpen,
    onClose,
    onError,
    autoReconnect = true,
    reconnectInterval = 3000,
    maxReconnectAttempts = 5
  } = options

  const [isConnected, setIsConnected] = useState(false)
  const [lastMessage, setLastMessage] = useState<any>(null)

  const wsRef = useRef<WebSocket | null>(null)
  const reconnectAttemptsRef = useRef(0)
  const reconnectTimerRef = useRef<number | null>(null)
  const manuallyClosedRef = useRef(false)

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return
    }

    manuallyClosedRef.current = false

    const token = getToken()
    const wsUrl = token ? `${url}?token=${encodeURIComponent(token)}` : url

    try {
      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        console.log('[WebSocket] 连接已建立')
        setIsConnected(true)
        reconnectAttemptsRef.current = 0
        onOpen?.()
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          setLastMessage(data)
          onMessage?.(data)
        } catch (e) {
          console.warn('[WebSocket] 解析消息失败:', event.data)
        }
      }

      ws.onclose = (event) => {
        console.log('[WebSocket] 连接已关闭, code:', event.code, 'reason:', event.reason)
        setIsConnected(false)
        onClose?.()

        if (!manuallyClosedRef.current && autoReconnect && reconnectAttemptsRef.current < maxReconnectAttempts) {
          reconnectAttemptsRef.current++
          console.log(`[WebSocket] 尝试重连 (${reconnectAttemptsRef.current}/${maxReconnectAttempts})...`)
          reconnectTimerRef.current = window.setTimeout(() => {
            connect()
          }, reconnectInterval)
        }
      }

      ws.onerror = (error) => {
        console.error('[WebSocket] 连接错误:', error)
        onError?.(error)
      }
    } catch (e) {
      console.error('[WebSocket] 创建连接失败:', e)
    }
  }, [url, autoReconnect, reconnectInterval, maxReconnectAttempts, onOpen, onMessage, onClose, onError])

  const disconnect = useCallback(() => {
    manuallyClosedRef.current = true
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current)
      reconnectTimerRef.current = null
    }
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }
    setIsConnected(false)
  }, [])

  const send = useCallback((data: any) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(data))
      return true
    }
    console.warn('[WebSocket] 连接未建立，无法发送消息')
    return false
  }, [])

  const subscribe = useCallback((instanceNo: string) => {
    return send({ type: 'subscribe', instanceNo })
  }, [send])

  const unsubscribe = useCallback((instanceNo: string) => {
    return send({ type: 'unsubscribe', instanceNo })
  }, [send])

  useEffect(() => {
    connect()
    return () => {
      disconnect()
    }
  }, [connect, disconnect])

  return {
    isConnected,
    lastMessage,
    send,
    subscribe,
    unsubscribe,
    connect,
    disconnect
  }
}
