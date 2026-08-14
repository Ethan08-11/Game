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
  // 顾客立绘曾被压成不透明白底，必须换 URL 才能立刻绕过 30 天缓存
  if (/\/images\/customer\//i.test(webpPath)) {
    return `${url}?v=alpha`
  }
  return url
}
