type RoomWsHandler<T = any> = (data: T, message: RoomWsMessage<T>) => void

export interface RoomWsMessage<T = any> {
  type: string
  message?: string
  data?: T
  heartbeatIntervalSeconds?: number
  onlineTimeoutSeconds?: number
}

function defaultWsBase() {
  if (typeof window === 'undefined') return 'ws://127.0.0.1:8080/ws/room'
  // Vite 开发服本身不处理房间 WS。连 location.host(:5173) 时若代理未生效，
  // 后端收不到连接，好友会全部显示离线、邀请按钮灰掉。
  if (import.meta.env.DEV) {
    const host = window.location.hostname || '127.0.0.1'
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
    return `${proto}://${host}:8080/ws/room`
  }
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${proto}://${window.location.host}/ws/room`
}

function getWsBase() {
  return String(import.meta.env.VITE_ROOM_WS_BASE || defaultWsBase())
}
const HEARTBEAT_INTERVAL = 20_000
const RECONNECT_DELAY = 3_000

let socket: WebSocket | null = null
const handlers = new Map<string, Set<RoomWsHandler>>()
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let manuallyClosed = false
let connectedToken = ''
let connectedUrl = ''
let lastHeartbeatAckAt = 0

function currentAccessToken() {
  return localStorage.getItem('token') || connectedToken
}

async function getFreshTokenForReconnect() {
  const currentToken = currentAccessToken()
  if (!currentToken) return ''
  try {
    const { refreshAccessTokenForWs } = await import('@/api/client')
    return await refreshAccessTokenForWs()
  } catch {
    return currentAccessToken()
  }
}

function emit(message: RoomWsMessage) {
  handlers.get(message.type)?.forEach((handler) => handler(message.data as any, message))
  handlers.get('*')?.forEach((handler) => handler(message.data as any, message))
}

function buildUrl(accessToken: string) {
  const url = new URL(getWsBase())
  url.searchParams.set('accessToken', accessToken)
  return url.toString()
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'ws.heartbeat', timestamp: Date.now() }))
    }
  }, HEARTBEAT_INTERVAL)
}

function stopHeartbeat() {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  heartbeatTimer = null
}

async function reconnectWithFreshToken() {
  const freshToken = await getFreshTokenForReconnect()
  if (!freshToken || manuallyClosed) return
  connectRoomSocket(freshToken)
}

function scheduleReconnect(forceRefresh = false) {
  if (reconnectTimer || !connectedToken || manuallyClosed) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    if (forceRefresh) {
      void reconnectWithFreshToken()
      return
    }
    const token = currentAccessToken()
    if (!token || manuallyClosed) return
    connectRoomSocket(token)
  }, RECONNECT_DELAY)
}

export function connectRoomSocket(accessToken: string) {
  if (!accessToken) return
  const nextUrl = buildUrl(accessToken)
  const sameEndpoint = connectedToken === accessToken && connectedUrl === nextUrl
  if (sameEndpoint && (socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING)) return
  if (socket) {
    stopHeartbeat()
    socket.onclose = null
    socket.close()
    socket = null
  }
  connectedToken = accessToken
  connectedUrl = nextUrl

  manuallyClosed = false
  socket = new WebSocket(nextUrl)

  socket.onopen = () => {
    startHeartbeat()
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'ws.heartbeat', timestamp: Date.now() }))
    }
  }

  socket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data) as RoomWsMessage
      if (message.type === 'ws.connected' || message.type === 'ws.heartbeat.ack') {
        lastHeartbeatAckAt = Date.now()
      }
      emit(message)
    } catch {
      // ignore invalid websocket payload
    }
  }

  socket.onclose = (event) => {
    stopHeartbeat()
    socket = null
    if (manuallyClosed) return
    const needRefresh = event.code === 1008 || event.code === 4401
    scheduleReconnect(needRefresh)
  }

  socket.onerror = () => {
    console.warn('[roomSocket] 连接失败', connectedUrl)
    socket?.close()
  }
}

export function subscribeRoomEvent<T = any>(type: string, handler: RoomWsHandler<T>) {
  const set = handlers.get(type) || new Set<RoomWsHandler>()
  set.add(handler as RoomWsHandler)
  handlers.set(type, set)
  return () => set.delete(handler as RoomWsHandler)
}

export function disconnectRoomSocket() {
  manuallyClosed = true
  stopHeartbeat()
  if (reconnectTimer) clearTimeout(reconnectTimer)
  reconnectTimer = null
  socket?.close()
  socket = null
}

export function isRoomSocketConnected() {
  return socket?.readyState === WebSocket.OPEN
}

export function getLastHeartbeatAckAt() {
  return lastHeartbeatAckAt
}
