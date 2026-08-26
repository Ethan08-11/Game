function defaultHttpBase() {
  if (typeof window === 'undefined') return 'http://127.0.0.1:8080'
  return window.location.origin
}

export const BACKEND_HTTP_BASE = (
  import.meta.env.VITE_BACKEND_HTTP
  || (import.meta.env.VITE_ROOM_WS_BASE
    ? String(import.meta.env.VITE_ROOM_WS_BASE).replace(/^ws/, 'http').replace(/\/ws\/room$/, '')
    : defaultHttpBase())
).replace(/\/$/, '')

export function getImageUrl(path: string | null | undefined): string | null {
  if (!path) return null
  if (/^(https?:|data:|blob:)/i.test(path)) return path
  const normalized = path.startsWith('/') ? path : `/${path}`
  const webpPath = /\.(png|jpe?g)$/i.test(normalized)
    ? normalized.replace(/\.(png|jpe?g)$/i, '.webp')
    : normalized
  const url = `${BACKEND_HTTP_BASE}${webpPath}`
  // 顾客立绘、卡面曾被长缓存；换 query 才能立刻拿到新图
  if (/\/images\/(customer|cards)\//i.test(webpPath)) {
    return `${url}?v=20260826e`
  }
  return url
}
