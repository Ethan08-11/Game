function defaultHttpBase() {
  if (typeof window === 'undefined') return 'http://127.0.0.1:8080'
  return window.location.origin
}

function defaultWsBase() {
  if (typeof window === 'undefined') return 'ws://127.0.0.1:8080/ws/room'
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${proto}://${window.location.host}/ws/room`
}

const WS_BASE = import.meta.env.VITE_ROOM_WS_BASE || defaultWsBase()

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
  return `${BACKEND_HTTP_BASE}${normalized}`
}
