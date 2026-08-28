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
  requiresPlayerTarget?: boolean
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

export interface MatchSettlementPlayer {
  userId?: number | string
  seatNo?: number
  deptType?: string
  resultType?: number
  maxHp?: number
  remainingHp?: number
  damageDealt?: number
  damageTaken?: number
  healingDone?: number
  shieldGranted?: number
  cardsPlayed?: number
  actionPointsUsed?: number
  expAwarded?: number
  moneyAwarded?: number
  unlockedCardId?: number | string | null
  unlockedCardName?: string | null
  unlockedCardImageUrl?: string | null
  unlockedCardDeptType?: string | null
  unlockedCardCost?: number | null
  unlockedCardType?: string | null
  unlockedCardDescription?: string | null
}

export interface UnlockedCollectibleCard {
  id: number | string
  name: string
  imageUrl: string | null
  deptType?: string | null
  cost?: number | null
  cardType?: string | null
  description?: string | null
}

export function findSettlementPlayer(
  players: MatchSettlementPlayer[] | undefined,
  userId: unknown,
): MatchSettlementPlayer | null {
  const uid = String(userId ?? '').trim()
  if (!uid || uid === 'undefined' || uid === 'null') return null
  return (players ?? []).find((player) => String(player.userId) === uid) ?? null
}

function pickUnlockField(player: Record<string, unknown>, camel: string, snake: string) {
  const value = player[camel] ?? player[snake]
  return value == null || value === '' ? null : value
}

export function unlockedCardFromSettlement(
  player: MatchSettlementPlayer | null | undefined,
): UnlockedCollectibleCard | null {
  if (!player) return null
  const raw = player as Record<string, unknown>
  const id = pickUnlockField(raw, 'unlockedCardId', 'unlocked_card_id')
  const name = pickUnlockField(raw, 'unlockedCardName', 'unlocked_card_name')
  if (id == null || name == null) return null
  const cost = pickUnlockField(raw, 'unlockedCardCost', 'unlocked_card_cost')
  return {
    id: id as number | string,
    name: String(name),
    imageUrl: (pickUnlockField(raw, 'unlockedCardImageUrl', 'unlocked_card_image_url') as string | null) ?? null,
    deptType: (pickUnlockField(raw, 'unlockedCardDeptType', 'unlocked_card_dept_type') as string | null) ?? null,
    cost: cost == null ? null : Number(cost),
    cardType: (pickUnlockField(raw, 'unlockedCardType', 'unlocked_card_type') as string | null) ?? null,
    description: (pickUnlockField(raw, 'unlockedCardDescription', 'unlocked_card_description') as string | null) ?? null,
  }
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
  players?: MatchSettlementPlayer[]
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
  appliedMultiplier?: number
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

export async function getCurrentMatch(): Promise<{ matchId?: number | string | null } | null> {
  return apiCall('/matches/current')
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

export async function declineMatchRevive(matchId: string | number): Promise<void> {
  return apiCall(`/matches/${matchId}/revive/decline`, { method: 'POST' })
}

export async function getMatchSettlement(matchId: string | number): Promise<MatchSettlementResp> {
  return apiCall(`/matches/${matchId}/settlement`)
}
