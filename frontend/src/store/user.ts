import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import { UserInfoVO, MenuVO } from '@/types'

interface UserState {
  token: string
  userInfo: UserInfoVO | null
  menus: MenuVO[]
  setToken: (token: string) => void
  setUserInfo: (userInfo: UserInfoVO | null) => void
  setMenus: (menus: MenuVO[]) => void
  logout: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      token: '',
      userInfo: null,
      menus: [],
      setToken: (token) => set({ token }),
      setUserInfo: (userInfo) => set({ userInfo }),
      setMenus: (menus) => set({ menus }),
      logout: () => set({ token: '', userInfo: null, menus: [] })
    }),
    {
      name: 'user-storage',
      storage: createJSONStorage(() => localStorage)
    }
  )
)
