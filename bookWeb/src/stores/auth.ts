import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api, clearTokens, getAccessToken, getRefreshToken, saveTokens } from '@/api/client'
import type { TokenData, User } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const initializing = ref(Boolean(getAccessToken()))
  const authenticated = computed(() => Boolean(user.value && getAccessToken()))
  const isReader = computed(() => user.value?.role === 'READER')
  const isManager = computed(() => user.value?.role === 'LIBRARIAN' || user.value?.role === 'ADMIN')
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

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

  async function register(payload: {
    username: string
    password: string
    email: string
    nickname?: string
    phone?: string
  }) {
    await api.post('/api/auth/register', payload, true)
    await login(payload.username, payload.password)
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

  async function updateProfile(
    payload: Partial<Pick<User, 'nickname' | 'email' | 'phone' | 'avatarUrl'>>,
  ) {
    user.value = await api.put<User>('/api/users/profile', payload)
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
    isReader,
    isManager,
    isAdmin,
    loadProfile,
    login,
    register,
    logout,
    updateProfile,
    reset,
  }
})
