import { useUserStore } from '@/store/user'

const TOKEN_KEY = 'Access-Token'

export function getToken(): string {
  const state = useUserStore.getState()
  if (state.token) return state.token
  try {
    return localStorage.getItem(TOKEN_KEY) || ''
  } catch {
    return ''
  }
}

export function setToken(token: string): void {
  useUserStore.getState().setToken(token)
  try {
    localStorage.setItem(TOKEN_KEY, token)
  } catch {
    // ignore
  }
}

export function removeToken(): void {
  useUserStore.getState().setToken('')
  try {
    localStorage.removeItem(TOKEN_KEY)
  } catch {
    // ignore
  }
}

export function getUserInfo() {
  return useUserStore.getState().userInfo
}

export function getCurrentTenantId(): number | null {
  return useUserStore.getState().currentTenantId
}

export function isLoggedIn(): boolean {
  return !!getToken()
}

export function logout(): void {
  useUserStore.getState().logout()
  try {
    localStorage.removeItem(TOKEN_KEY)
  } catch {
    // ignore
  }
}
