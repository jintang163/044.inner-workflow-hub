import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import { UserInfoVO, MenuVO, TenantSimpleVO } from '@/types'

interface UserState {
  token: string
  userInfo: UserInfoVO | null
  menus: MenuVO[]
  currentTenantId: number | null
  tenants: TenantSimpleVO[]
  setToken: (token: string) => void
  setUserInfo: (userInfo: UserInfoVO | null) => void
  setMenus: (menus: MenuVO[]) => void
  setCurrentTenantId: (tenantId: number | null) => void
  setTenants: (tenants: TenantSimpleVO[]) => void
  logout: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      token: '',
      userInfo: null,
      menus: [],
      currentTenantId: null,
      tenants: [],
      setToken: (token) => set({ token }),
      setUserInfo: (userInfo) =>
        set({
          userInfo,
          currentTenantId: userInfo?.tenantId ?? null,
          tenants: userInfo?.tenants ?? []
        }),
      setMenus: (menus) => set({ menus }),
      setCurrentTenantId: (tenantId) => set({ currentTenantId: tenantId }),
      setTenants: (tenants) => set({ tenants }),
      logout: () =>
        set({ token: '', userInfo: null, menus: [], currentTenantId: null, tenants: [] })
    }),
    {
      name: 'user-storage',
      storage: createJSONStorage(() => localStorage)
    }
  )
)
