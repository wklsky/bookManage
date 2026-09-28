/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/stores/auth.ts
 * @Description: 后台登录状态与角色判断
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api, clearTokens, getAccessToken, getRefreshToken, saveTokens } from '@/api/client'
import type { TokenData, User } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const initializing = ref(Boolean(getAccessToken()))
  const authenticated = computed(() => Boolean(user.value && getAccessToken()))
  const isLibrarian = computed(() => user.value?.role === 'LIBRARIAN')
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  /** 后台只面向管理端：读者即使登录成功也不允许进入 */
  const isManager = computed(() => isLibrarian.value || isAdmin.value)

  async function loadProfile() {
    if (!getAccessToken()) {
      initializing.value = false
      return
    }
    try {
      user.value = await api.get<User>('/api/users/profile')
    } catch {
      clearTokens()
      user.value = null
    } finally {
      initializing.value = false
    }
  }

  async function login(account: string, password: string) {
    const tokens = await api.post<TokenData>('/api/auth/login', { account, password }, true)
    saveTokens(tokens)
    await loadProfile()
  }

  async function logout() {
    const refreshToken = getRefreshToken()
    try {
      if (refreshToken) await api.post('/api/auth/logout', { refreshToken })
    } finally {
      clearTokens()
      user.value = null
    }
  }

  function reset() {
    clearTokens()
    user.value = null
    initializing.value = false
  }

  return {
    user,
    initializing,
    authenticated,
    isLibrarian,
    isAdmin,
    isManager,
    loadProfile,
    login,
    logout,
    reset,
  }
})
