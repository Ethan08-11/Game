import { apiCall } from './client'

export interface MatchPlayerResp {
  userId?: number | string
  seatNo?: number
  deptType?: string | null
  maxHp?: number
  currentHp?: number
  shield?: number
  actionPoints?: number
  endedTurn?: number
  playerStatus?: string
  handCount?: number
  deckCount?: number
  discardCount?: number
}

export interface MatchCustomerResp {
  id?: number | string
  customerCode?: string
  customerName?: string
  description?: string
  effectType?: string
  effectValue?: number
  triggerChance?: number
  status?: number
  imageUrl?: string | null
}

export interface MatchHandCardResp {
  instanceId?: number | string
  cardId?: number | string
  cardCode?: string
  cardName?: string
  deptType?: string
  cost?: number
  cardType?: string
  description?: string
  imageUrl?: string | null
  zone?: string
  deckOrder?: number | null
  drawnRound?: number
}

export interface MatchDetailResp {
  matchId?: number | string
  matchCode?: string
  roomId?: number | string
  status?: number
  phase?: string
  currentRound?: number
  version?: number
  customer?: MatchCustomerResp
  bullyId?: number | string
  bossName?: string
  bossMaxHp?: number
  bossCurrentHp?: number
  bossBaseAttack?: number
  bossCurrentAttack?: number
  customerTriggered?: number
  customerEffectType?: string
  customerEffectValue?: number
  firstPlayerUserId?: number | string
  secondPlayerUserId?: number | string
  matchEnded?: boolean
  winnerType?: number
  players?: MatchPlayerResp[]
  hand?: MatchHandCardResp[]
}

export interface MatchSettlementResp {
  matchId?: number | string
  matchCode?: string
  winnerType?: number
  victory?: boolean
  totalRounds?: number
  durationSeconds?: number
  bossMaxHp?: number
  bossRemainingHp?: number
  players?: Array<Record<string, any>>
}

export interface MatchDeckCardResp {
  instanceId?: number | string
  cardId?: number | string
  cardCode?: string
  cardName?: string
  deptType?: string
  cost?: number
  cardType?: string
  description?: string
  imageUrl?: string | null
  zone?: string
  deckOrder?: number | null
  drawnRound?: number
}

export interface MatchDeckResp {
  matchId?: number | string
  userId?: number | string
  totalCount?: number
  cards?: MatchDeckCardResp[]
}

export interface MatchReviveStatusResp {
  matchId?: number | string
  userId?: number | string
  reviveEnabled?: boolean
  canRevive?: boolean
  reviveCount?: number
  reviveLimit?: number
  currentHp?: number
  maxHp?: number
  remainingSeconds?: number | null
  lastReviveAt?: string | null
  reviveStatus?: number
  message?: string
}

export interface MatchReviveRequestPayload {
  userId: number | string
  adRequestId: string
  adPlatform?: string
  reviveReason?: string
  adCallbackRaw?: string
}

export interface MatchReviveRequestResp {
  matchId?: number | string
  userId?: number | string
  beforeHp?: number
  afterHp?: number
  reviveCount?: number
  reviveLimit?: number
  reviveStatus?: number
  currentRound?: number
  version?: number
  revivedAt?: string
  message?: string
}

export interface PlayCardPayload {
  cardInstanceId: number | string
  targetType: 'BOSS' | 'PLAYER' | 'SELF'
  targetUserId: number | string | null
  clientActionId: string
  expectedVersion: number
}

export interface PlayCardResponse {
  matchId?: number | string
  actionId?: number | string
  clientActionId?: string
  actionType?: string
  actorUserId?: number | string
  cardInstanceId?: number | string
  cardId?: number | string
  cardName?: string
  effectType?: string
  effectValue?: number
  targetType?: 'BOSS' | 'PLAYER' | 'SELF'
  targetUserId?: number | string | null
  beforeValue?: number | null
  afterValue?: number | null
  remainingActionPoints?: number
  effects?: Array<Record<string, any>>
  version?: number
  matchEnded?: boolean
  winnerType?: number
}

export interface EndTurnPayload {
  clientActionId: string
  expectedVersion: number
}

export interface EndTurnResponse {
  matchId?: number | string
  userId?: number | string
  endedTurn?: number
  discardedCount?: number
  allPlayersEnded?: boolean
  bossAttackResolved?: boolean
  resolvedRound?: number
  bossAttackTargets?: Array<Record<string, any>>
  matchEnded?: boolean
  winnerType?: number
  currentRound?: number
  phase?: string
  version?: number
  handCount?: number
  deckCount?: number
  discardCount?: number
}

export async function getMatchDetail(matchId: string | number): Promise<MatchDetailResp> {
  return apiCall(`/matches/${matchId}`)
}

export async function getMatchDeck(matchId: string | number): Promise<MatchDeckResp> {
  return apiCall(`/matches/${matchId}/deck`)
}

export async function playMatchCard(matchId: string | number, payload: PlayCardPayload): Promise<PlayCardResponse> {
  return apiCall(`/matches/${matchId}/actions/play-card`, { method: 'POST', body: payload })
}

export async function endMatchTurn(matchId: string | number, payload: EndTurnPayload): Promise<EndTurnResponse> {
  return apiCall(`/matches/${matchId}/actions/end-turn`, { method: 'POST', body: payload })
}

export async function reconnectMatch(matchId: string | number): Promise<MatchDetailResp> {
  return apiCall(`/matches/${matchId}/reconnect`, { method: 'POST' })
}

export async function abandonMatch(matchId: string | number): Promise<MatchDetailResp> {
  return apiCall(`/matches/${matchId}/abandon`, { method: 'POST' })
}

export async function chooseFirstPlayer(matchId: string | number, firstPlayerUserId: string | number): Promise<MatchDetailResp> {
  return apiCall(`/matches/${matchId}/choose-first-player`, {
    method: 'POST',
    body: { firstPlayerUserId },
  })
}

export async function getMatchReviveStatus(matchId: string | number, userId: string | number): Promise<MatchReviveStatusResp> {
  return apiCall(`/matches/${matchId}/revive/status?userId=${encodeURIComponent(String(userId))}`)
}

export async function requestMatchRevive(matchId: string | number, payload: MatchReviveRequestPayload): Promise<MatchReviveRequestResp> {
  return apiCall(`/matches/${matchId}/revive/request`, { method: 'POST', body: payload })
}

export async function getMatchSettlement(matchId: string | number): Promise<MatchSettlementResp> {
  return apiCall(`/matches/${matchId}/settlement`)
}
