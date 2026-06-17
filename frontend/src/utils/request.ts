import axios, { InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'antd'
import { useUserStore } from '@/store/user'
import { ApiResponse } from '@/types'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { token } = useUserStore.getState()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data as ApiResponse
    if (res.code !== 200) {
      message.error(res.message || '请求失败')
      if (res.code === 401) {
        const { logout } = useUserStore.getState()
        logout()
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  (error) => {
    const { response } = error
    if (response) {
      const status = response.status
      const data = response.data as ApiResponse
      if (status === 401) {
        message.error(data?.message || '登录已过期，请重新登录')
        const { logout } = useUserStore.getState()
        logout()
        window.location.href = '/login'
      } else if (status === 403) {
        message.error('没有权限访问该资源')
      } else if (status === 404) {
        message.error('请求的资源不存在')
      } else if (status === 500) {
        message.error(data?.message || '服务器内部错误')
      } else {
        message.error(data?.message || `请求错误 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      message.error('请求超时，请稍后重试')
    } else if (error.message?.includes('Network Error')) {
      message.error('网络错误，请检查网络连接')
    } else {
      message.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default service
