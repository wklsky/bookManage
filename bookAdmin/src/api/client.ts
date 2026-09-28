/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/api/client.ts
 * @Description: Fetch 封装，负责令牌注入、401 自动续期与统一错误解析
 */
import type { ApiResponse, TokenData } from '@/types/api'

// 后台与前台共用后端，但各自独立登录：令牌键加 admin 前缀，
// 避免同一浏览器里前后台切换时互相顶掉会话
const ACCESS_TOKEN_KEY = 'book_admin_access_token'
const REFRESH_TOKEN_KEY = 'book_admin_refresh_token'

let refreshPromise: Promise<string | null> | null = null

export class ApiError extends Error {
  status: number
  errors: Array<{ field: string; message: string }>

  constructor(message: string, status = 0, errors: Array<{ field: string; message: string }> = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
  }
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function saveTokens(tokens: TokenData) {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

function toQuery(params?: Record<string, unknown>) {
  if (!params) return ''
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

async function parseError(response: Response) {
  try {
    const body = await response.json()
    return new ApiError(body.message || '请求失败', response.status, body.errors || [])
  } catch {
    return new ApiError(response.statusText || '请求失败', response.status)
  }
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return null

  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!response.ok) {
    clearTokens()
    return null
  }
  const result = (await response.json()) as ApiResponse<TokenData>
  saveTokens(result.data)
  return result.data.accessToken
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
  query?: Record<string, unknown>
  skipAuth?: boolean
  retry?: boolean
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, query, skipAuth = false, retry = true, ...init } = options
  const headers = new Headers(init.headers)
  if (body !== undefined) headers.set('Content-Type', 'application/json')
  const token = getAccessToken()
  if (token && !skipAuth) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${path}${toQuery(query)}`, {
    ...init,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 401 && !skipAuth && retry) {
    // 并发请求同时触发续期时复用同一个 Promise，避免重复换发令牌把旧令牌顶失效
    refreshPromise ||= refreshAccessToken().finally(() => {
      refreshPromise = null
    })
    const refreshed = await refreshPromise
    if (refreshed) return request<T>(path, { ...options, retry: false })
    window.dispatchEvent(new CustomEvent('auth-expired'))
  }

  if (!response.ok) throw await parseError(response)
  if (response.status === 204) return undefined as T
  const result = (await response.json()) as ApiResponse<T>
  return result.data
}

export const api = {
  get: <T>(path: string, query?: Record<string, unknown>) =>
    request<T>(path, { method: 'GET', query }),
  post: <T>(path: string, body?: unknown, skipAuth = false) =>
    request<T>(path, { method: 'POST', body, skipAuth }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PATCH', body }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
