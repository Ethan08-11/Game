import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RoomDetailResp, RoomPlayerResp } from '@/api'

export interface Player {
  id: string
  username: string
}

export const useRoomStore = defineStore('room', () => {
  const roomId = ref<string>('')
  const roomCode = ref<string>('')
  const players = ref<Player[]>([])
  const isConnected = ref<boolean>(false)
  const hostUserId = ref<string>('')
  const currentUserId = ref<string>('')

  const player1Dept = ref<string>('')
  const player2Dept = ref<string>('')
  const player1Ready = ref<boolean>(false)
  const player2Ready = ref<boolean>(false)

  const inviteId = ref<string>('')
  const invitedBy = ref<string>('')
  const matchRoomId = ref<string>('')
  const matchId = ref<string>('')
  const isHost = ref<boolean>(false)
  const firstPlayerIndex = ref<0 | 1>(0)

  const ACTIVE_ROOM_KEY = 'activeRoomId'
  const depts = ['销售部', '采购部']

  const bothReady = computed(() => {
    return players.value.length === 2 && player1Ready.value && player2Ready.value
  })

  const canStart = computed(() => {
    if (players.value.length !== 2) return false
    if (!player1Dept.value || !player2Dept.value) return false
    if (firstPlayerIndex.value !== 0 && firstPlayerIndex.value !== 1) return false
    return true
  })

  function normalizePlayer(member: RoomPlayerResp): Player {
    const id = String(member.userId ?? member.id ?? member.memberId ?? '')
    return {
      id,
      username: member.displayName ?? member.username ?? member.nickname ?? (id ? `玩家${id}` : '玩家'),
    }
  }

  function normalizeDept(dept?: string | null): string {
    if (!dept) return ''
    const value = dept.toLowerCase()
    if (value === 'purchase' || value === 'buy' || value === 'procurement') return '采购部'
    if (value === 'sales' || value === 'sell') return '销售部'
    return dept
  }

  function syncRoomDetail(detail: RoomDetailResp, currentUserIdValue?: string, currentUsername?: string, friendNames?: Map<string, string>) {
    if (Number(detail.status) === 3 || detail.closedAt) {
      return
    }
    roomId.value = String(detail.roomId ?? detail.id ?? roomId.value)
    if (roomId.value) {
      sessionStorage.setItem(ACTIVE_ROOM_KEY, roomId.value)
    }
    roomCode.value = String(detail.roomCode ?? detail.code ?? roomCode.value)
    hostUserId.value = String(detail.hostUserId ?? detail.ownerId ?? detail.createdBy ?? hostUserId.value)
    if (detail.matchId != null && detail.matchId !== '') {
      matchId.value = String(detail.matchId)
    }
    const members = [...(detail.members ?? detail.players ?? [])].sort((a, b) => (a.seatNo ?? a.slot ?? 0) - (b.seatNo ?? b.slot ?? 0))
    players.value = members.map(normalizePlayer)
    for (const player of players.value) {
      if (currentUserIdValue && currentUsername && player.id === currentUserIdValue) {
        player.username = currentUsername
        continue
      }
      if (friendNames) {
        const friendName = friendNames.get(player.id)
        if (friendName) {
          player.username = friendName
          continue
        }
      }
      if (player.username.startsWith('玩家') && currentUserIdValue && currentUsername && player.id === currentUserIdValue) {
        player.username = currentUsername
      }
    }
    const seat1 = members.find(m => (m.seatNo ?? m.slot) === 1) ?? members[0]
    const seat2 = members.find(m => (m.seatNo ?? m.slot) === 2) ?? members[1]
    player1Dept.value = normalizeDept(seat1?.deptType ?? seat1?.dept ?? seat1?.department)
    player2Dept.value = normalizeDept(seat2?.deptType ?? seat2?.dept ?? seat2?.department)
    player1Ready.value = seat1?.ready === true || seat1?.ready === 1 || seat1?.readyStatus === true || seat1?.readyStatus === 1 || seat1?.ready_status === true || seat1?.ready_status === 1
    player2Ready.value = seat2?.ready === true || seat2?.ready === 1 || seat2?.readyStatus === true || seat2?.readyStatus === 1 || seat2?.ready_status === true || seat2?.ready_status === 1
    const firstPlayerUserId = String(detail.firstPlayerUserId ?? detail.firstUserId ?? '')
    if (firstPlayerUserId) {
      const selectedIndex = members.findIndex(member => String(member.userId ?? member.id ?? member.memberId ?? '') === firstPlayerUserId)
      if (selectedIndex === 0 || selectedIndex === 1) firstPlayerIndex.value = selectedIndex
    } else if (detail.firstPlayerIndex === 0 || detail.firstPlayerIndex === 1) {
      firstPlayerIndex.value = detail.firstPlayerIndex
    }
    if (currentUserIdValue) {
      currentUserId.value = currentUserIdValue
      isHost.value = hostUserId.value === currentUserIdValue
    }
    if (currentUserIdValue && players.value.length === 2) {
      const selfIndex = players.value.findIndex((player) => player.id === currentUserIdValue)
      if (selfIndex === 0 || selfIndex === 1) firstPlayerIndex.value = selfIndex
    }
  }

  function createRoom(code: string) {
    roomCode.value = code
    players.value = []
    resetReady()
  }

  function joinRoom(code: string) {
    roomCode.value = code
  }

  function syncPlayers(list: Player[]) {
    players.value = list
  }

  function selectDept(playerIndex: 0 | 1, dept: string) {
    if (playerIndex === 0) {
      player1Dept.value = dept
    } else {
      player2Dept.value = dept
    }
  }

  function toggleReady(playerIndex: 0 | 1) {
    if (playerIndex === 0) {
      player1Ready.value = !player1Ready.value
    } else {
      player2Ready.value = !player2Ready.value
    }
  }

  function resetReady() {
    player1Dept.value = ''
    player2Dept.value = ''
    player1Ready.value = false
    player2Ready.value = false
  }

  function sendInvite(targetFriendId: string) {
    inviteId.value = targetFriendId
    isHost.value = true
  }

  function setCurrentUser(userId: string) {
    currentUserId.value = userId
  }

  function isSelfPlayer(playerId: string) {
    return !!currentUserId.value && playerId === currentUserId.value
  }

  function receiveInvite(fromUsername: string, roomId: string) {
    invitedBy.value = fromUsername
    matchRoomId.value = roomId
    isHost.value = false
  }

  function acceptInvite() {
    inviteId.value = ''
    invitedBy.value = ''
  }

  function declineInvite() {
    invitedBy.value = ''
    matchRoomId.value = ''
  }

  function setFirstPlayer(index: 0 | 1) {
    firstPlayerIndex.value = index
  }

  function setMatchId(value: string | number) {
    matchId.value = String(value)
  }

  function resetMatchMaking() {
    inviteId.value = ''
    invitedBy.value = ''
    matchRoomId.value = ''
    matchId.value = ''
    isHost.value = false
    firstPlayerIndex.value = 0
    sessionStorage.removeItem(ACTIVE_ROOM_KEY)
    leaveRoom()
  }

  function leaveRoom() {
    roomId.value = ''
    roomCode.value = ''
    hostUserId.value = ''
    players.value = []
    sessionStorage.removeItem(ACTIVE_ROOM_KEY)
    resetReady()
  }

  return {
    roomId, roomCode, players, isConnected, hostUserId, currentUserId,
    player1Dept, player2Dept, player1Ready, player2Ready,
    inviteId, invitedBy, matchRoomId, matchId, isHost, firstPlayerIndex,
    depts, bothReady, canStart,
    createRoom, joinRoom, syncPlayers, syncRoomDetail,
    selectDept, toggleReady, resetReady,
    sendInvite, receiveInvite, acceptInvite, declineInvite, setFirstPlayer, setMatchId, resetMatchMaking,
    setCurrentUser, isSelfPlayer, leaveRoom,
  }
})
