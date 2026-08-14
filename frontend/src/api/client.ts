/**
 * HTTP 请求客户端
 */

const BASE_URL = import.meta.env.VITE_API_BASE || '/api'

function getToken(): string {
  return localStorage.getItem('token') || ''
}

interface RequestOptions {
  method?: string
  body?: unknown
  headers?: Record<string, string>
}

interface ApiResponse<T> {
  code: number
  message: string
  success: boolean
  data: T
}

const REQUEST_TIMEOUT = 10000

let refreshPromise: Promise<string> | null = null

function applyAuthResult(result: { token: string; refreshToken?: string; user?: { id?: number | string; username?: string; displayName?: string } }) {
  localStorage.setItem('token', result.token)
  if (result.refreshToken) localStorage.setItem('refreshToken', result.refreshToken)
  if (result.user?.id != null) localStorage.setItem('userId', String(result.user.id))
  if (result.user?.displayName || result.user?.username) {
    localStorage.setItem('loginUsername', result.user.displayName || result.user.username || '')
  }
  window.dispatchEvent(new CustomEvent('auth:token-refreshed', { detail: result }))
}

async function refreshAccessToken(): Promise<string> {
  if (refreshPromise) return refreshPromise
  const refreshToken = localStorage.getItem('refreshToken') || ''
  if (!refreshToken) {
    window.dispatchEvent(new CustomEvent('auth:expired'))
    throw new Error('登录已过期，请重新登录')
  }
  refreshPromise = request<{ token: string; refreshToken?: string; user?: { id?: number | string; username?: string; displayName?: string } }>(
    '/auth/refresh',
    { method: 'POST', body: { refreshToken }, headers: { Authorization: '' } },
    true,
  ).then((result) => {
    applyAuthResult(result)
    return result.token
  }).catch((e) => {
    window.dispatchEvent(new CustomEvent('auth:expired'))
    throw e
  }).finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

export async function refreshAccessTokenForWs(): Promise<string> {
  return refreshAccessToken()
}

async function request<T>(endpoint: string, options: RequestOptions = {}, skipRefresh = false): Promise<T> {
  const { method = 'GET', body, headers = {} } = options
  const url = `${BASE_URL}${endpoint}`

  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)

  try {
    const res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`,
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    })

    if (!res.ok) {
      if (res.status === 401 && !skipRefresh) {
        await refreshAccessToken()
        return request<T>(endpoint, options, true)
      }
      if (res.status === 405 || res.status === 502 || res.status === 504) {
        console.error(`[API] ${method} ${url} → HTTP ${res.status}（后端不可用）`)
        throw new Error('后端服务未就绪，请确认已在 Zeabur 部署 backend（及 MySQL/Redis/RabbitMQ）')
      }
      const err = await res.json().catch(() => ({ message: res.statusText }))
      const msg = err.message || `HTTP ${res.status}`
      console.error(`[API] ${method} ${url} → HTTP ${res.status}: ${msg}`)
      throw new Error(msg)
    }

    const json: ApiResponse<T> = await res.json()
    if (!json.success) {
      if (json.code === 401 && !skipRefresh) {
        await refreshAccessToken()
        return request<T>(endpoint, options, true)
      }
      console.error(`[API] ${method} ${url} → 业务错误: ${json.message}`)
      throw new Error(json.message || '请求失败')
    }
    return json.data
  } catch (e: any) {
    if (e.name === 'AbortError') {
      console.error(`[API] ${method} ${url} → 请求超时`)
      throw new Error('请求超时，请稍后重试')
    }
    if (e.message === 'Failed to fetch' || e.name === 'TypeError') {
      console.error(`[API] ${method} ${url} → 网络连接失败`)
      throw new Error('无法连接服务器，请确认后端已启动 (192.168.1.25:8080)')
    }
    throw e
  } finally {
    clearTimeout(timer)
  }
}

export async function apiCall<T>(endpoint: string, options?: RequestOptions): Promise<T> {
  return request<T>(endpoint, options)
}

export { BASE_URL, getToken, request }
