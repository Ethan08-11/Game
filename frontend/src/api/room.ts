import { apiCall } from './client'

export interface RoomPlayerResp {
  userId?: number | string
  id?: number | string
  memberId?: number | string
  username?: string
  displayName?: string
  nickname?: string
  seatNo?: number
  slot?: number
  dept?: string
  department?: string
  deptType?: string | null
  ready?: boolean | number
  readyStatus?: boolean | number
  ready_status?: boolean | number
  onlineStatus?: number
  online_status?: number
}

export interface RoomDetailResp {
  id?: number | string
  roomId?: number | string
  roomCode?: string
  code?: string
  ownerId?: number | string
  hostUserId?: number | string
  createdBy?: number | string
  status?: string | number
  closedAt?: string | null
  firstPlayerIndex?: number
  firstPlayerUserId?: number | string
  firstUserId?: number | string
  matchId?: number | string
  members?: RoomPlayerResp[]
  players?: RoomPlayerResp[]
}

export interface RoomInviteResp {
  inviteId?: number | string
  id?: number | string
  roomId?: number | string
  roomCode?: string
  players?: RoomPlayerResp[]
  inviter?: RoomPlayerResp
  invitee?: RoomPlayerResp
}

export async function sendRoomInvite(friendId: string): Promise<RoomInviteResp> {
  return apiCall('/rooms/invites', {
    method: 'POST',
    body: { friendId, targetUserId: friendId, inviteeId: friendId },
  })
}

export interface RoomInvitePendingResp {
  inviteId: number | string
  fromUserId: number | string
  fromUsername: string
  toUserId?: number | string
  expiredAt?: string
  createdAt?: string
}

export async function getPendingRoomInvites(): Promise<RoomInvitePendingResp[]> {
  return apiCall('/rooms/invites/pending')
}

export async function acceptRoomInvite(inviteId: string): Promise<RoomDetailResp> {
  return apiCall(`/rooms/invites/${inviteId}/accept`, { method: 'POST' })
}

export async function rejectRoomInvite(inviteId: string): Promise<void> {
  return apiCall(`/rooms/invites/${inviteId}/reject`, { method: 'POST' })
}

export async function getRoomDetail(roomId: string): Promise<RoomDetailResp> {
  return apiCall(`/rooms/${roomId}`, { method: 'GET' })
}

export async function getCurrentRoom(): Promise<RoomDetailResp | null> {
  return apiCall('/rooms/current', { method: 'GET' })
}

export async function releaseIdleRoom(): Promise<void> {
  await apiCall('/rooms/release-idle', { method: 'POST' })
}

export async function setRoomDepartment(roomId: string, deptType: string): Promise<RoomDetailResp> {
  return apiCall(`/rooms/${roomId}/department`, {
    method: 'POST',
    body: { deptType },
  })
}

export async function setRoomReady(roomId: string, ready: boolean): Promise<RoomDetailResp> {
  return apiCall(`/rooms/${roomId}/ready`, {
    method: 'POST',
    body: { ready },
  })
}

export async function setRoomFirstPlayer(roomId: string, firstPlayerIndex: 0 | 1): Promise<RoomDetailResp> {
  return apiCall(`/rooms/${roomId}/first-player`, {
    method: 'POST',
    body: { firstPlayerIndex },
  })
}

export async function leaveRoom(roomId: string): Promise<void> {
  return apiCall(`/rooms/${roomId}/leave`, { method: 'POST' })
}

export function extractRoomId(payload: any): string {
  if (!payload) return ''

  const looksLikeRoomId = (value: unknown) => {
    const text = String(value ?? '').trim()
    if (!text) return false
    // 排除事件 UUID / eventId，房间 id 一般为数字
    if (/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(text)) return false
    if (/^[0-9a-f]{32}$/i.test(text)) return false
    return true
  }

  const candidates = [
    payload.roomId,
    payload.data?.roomId,
    payload.room?.roomId,
    payload.room?.id,
    payload.data?.room?.roomId,
    payload.data?.room?.id,
    payload.roomInfo?.roomId,
    payload.roomInfo?.id,
    payload.data?.roomInfo?.roomId,
    payload.data?.roomInfo?.id,
    // RoomDetailResp 的 id 才是房间 id；事件对象的 id/eventId 不可靠
    payload.id,
    payload.data?.id,
  ]

  for (const candidate of candidates) {
    if (looksLikeRoomId(candidate)) return String(candidate)
  }

  console.warn('[extractRoomId] Failed to extract roomId from payload:', JSON.stringify(payload).slice(0, 200))
  return ''
}
