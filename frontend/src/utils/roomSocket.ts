type RoomWsHandler<T = any> = (data: T, message: RoomWsMessage<T>) => void

export interface RoomWsMessage<T = any> {
  type: string
  message?: string
  data?: T
  heartbeatIntervalSeconds?: number
  onlineTimeoutSeconds?: number
}

const WS_BASE = import.meta.env.VITE_ROOM_WS_BASE || 'ws://127.0.0.1:8080/ws/room'
const HEARTBEAT_INTERVAL = 20_000
const RECONNECT_DELAY = 3_000

let socket: WebSocket | null = null
const handlers = new Map<string, Set<RoomWsHandler>>()
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let manuallyClosed = false
let connectedToken = ''
let lastHeartbeatAckAt = 0

async function getFreshTokenForReconnect() {
  const currentToken = localStorage.getItem('token') || connectedToken
  if (!currentToken) return ''

  try {
    const { refreshAccessTokenForWs } = await import('@/api/client')
    return await refreshAccessTokenForWs()
  } catch {
    return localStorage.getItem('token') || currentToken
  }
}

function emit(message: RoomWsMessage) {
  handlers.get(message.type)?.forEach((handler) => handler(message.data as any, message))
  handlers.get('*')?.forEach((handler) => handler(message.data as any, message))
}

function buildUrl(accessToken: string) {
  const url = new URL(WS_BASE)
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

function scheduleReconnect() {
  if (reconnectTimer || !connectedToken || manuallyClosed) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    void reconnectWithFreshToken()
  }, RECONNECT_DELAY)
}

export function connectRoomSocket(accessToken: string) {
  if (!accessToken) return
  if ((socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) && connectedToken === accessToken) return
  if (socket && connectedToken !== accessToken) {
    stopHeartbeat()
    socket.onclose = null
    socket.close()
    socket = null
  }
  connectedToken = accessToken

  manuallyClosed = false
  socket = new WebSocket(buildUrl(accessToken))

  socket.onopen = () => {
    startHeartbeat()
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

  socket.onclose = () => {
    stopHeartbeat()
    socket = null
    if (!manuallyClosed) scheduleReconnect()
  }

  socket.onerror = () => {
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
