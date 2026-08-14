const WS_BASE = import.meta.env.VITE_ROOM_WS_BASE || 'ws://127.0.0.1:8080/ws/room'

export const BACKEND_HTTP_BASE = (
  import.meta.env.VITE_BACKEND_HTTP
  || WS_BASE.replace(/^ws/, 'http').replace(/\/ws\/room$/, '')
  || 'http://127.0.0.1:8080'
).replace(/\/$/, '')

export function getImageUrl(path: string | null | undefined): string | null {
  if (!path) return null
  if (/^(https?:|data:|blob:)/i.test(path)) return path
  const normalized = path.startsWith('/') ? path : `/${path}`
  return `${BACKEND_HTTP_BASE}${normalized}`
}
