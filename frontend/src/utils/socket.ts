import { io, Socket } from 'socket.io-client'

let socket: Socket | null = null

export function connectSocket(serverUrl: string): Socket {
  if (socket?.connected) return socket
  socket = io(serverUrl, {
    autoConnect: true,
    reconnection: true,
    reconnectionAttempts: 10,
    reconnectionDelay: 1000,
  })
  return socket
}

export function getSocket(): Socket | null {
  return socket
}

export function disconnectSocket(): void {
  if (socket) {
    socket.disconnect()
    socket = null
  }
}

export const ROOM_EVENTS = {
  CREATE_ROOM: 'room:create',
  JOIN_ROOM: 'room:join',
  PLAYER_JOIN: 'room:playerJoin',
  PLAYER_LEAVE: 'room:playerLeave',
} as const

export const GAME_EVENTS = {
  PLAY_CARD: 'game:playCard',
  SHOP_BUY: 'game:shopBuy',
  BOSS_ACTION: 'game:bossAction',
  SHIELD_CHANGE: 'game:shieldChange',
  SYNC_STATE: 'game:syncState',
} as const

export const RESULT_EVENTS = {
  GAME_OVER: 'result:gameOver',
  REPLAY_DATA: 'result:replayData',
} as const

export const MATCH_EVENTS = {
  INVITE: 'match:invite',
  INVITED: 'match:invited',
  ACCEPT: 'match:accept',
  DECLINE: 'match:decline',
  CANCELLED: 'match:cancelled',
  MATCHED: 'match:matched',
} as const
