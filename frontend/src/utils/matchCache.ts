export const ACTIVE_ROOM_KEY = 'activeRoomId'
export const ACTIVE_MATCH_KEY = 'activeMatchId'

export function readCachedRoomId() {
  return sessionStorage.getItem(ACTIVE_ROOM_KEY) || ''
}

export function readCachedMatchId() {
  return sessionStorage.getItem(ACTIVE_MATCH_KEY) || ''
}

export function writeCachedRoomId(roomId: string) {
  if (roomId) sessionStorage.setItem(ACTIVE_ROOM_KEY, roomId)
  else sessionStorage.removeItem(ACTIVE_ROOM_KEY)
}

export function writeCachedMatchId(matchId: string) {
  if (matchId) sessionStorage.setItem(ACTIVE_MATCH_KEY, matchId)
  else sessionStorage.removeItem(ACTIVE_MATCH_KEY)
}

export function clearMatchCache() {
  sessionStorage.removeItem(ACTIVE_ROOM_KEY)
  sessionStorage.removeItem(ACTIVE_MATCH_KEY)
}

export function isClosedRoom(detail: { status?: string | number | null; closedAt?: string | null } | null | undefined) {
  if (!detail) return true
  return Number(detail.status) === 3 || !!detail.closedAt
}
