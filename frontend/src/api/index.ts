/**
 * API 层统一入口
 *
 * 后端服务需要运行在 VITE_API_BASE 指向的地址。
 */

// client
export { BASE_URL } from './client'

// auth
export { login, register, refreshAuth, logout, getMe } from './auth'
export type { LoginParams, RegisterParams, AuthResult, UserInfo, MeInfo } from './auth'

// user
export {
  getUserProfile,
  updateMyProfile,
  getUserStats,
  getFriends,
  sendFriendRequest,
  acceptFriendRequest,
  deleteFriend,
  addFriend,
  getPoints,
  addPoints,
  getAchievements,
  unlockAchievement,
  getLeaderboard,
  getMyLeaderboardRank,
} from './user'
export type { PresenceStatus, Friend, Achievement, LeaderboardEntry, UserProfile, UpdateUserProfilePayload, UserStats, FriendRequestPayload } from './user'

// game
export { fetchCardList, getAllCards, getCardsByDept, getGameConfig, getDepartments, getCurrentCustomer, getCustomers, getCustomerCatalog, submitGameResult } from './game'
export type { ApiCard, CustomerApiItem } from './game'
export type { GameConfig, GameResultPayload, GameResultResponse } from './game'

// match
export { getMatchDetail, getMatchDeck, playMatchCard, endMatchTurn, reconnectMatch, getCurrentMatch, abandonMatch, cancelStuckMatch, chooseFirstPlayer, getMatchSettlement, getMatchReviveStatus, requestMatchRevive, declineMatchRevive, findSettlementPlayer, unlockedCardFromSettlement } from './match'
export type { MatchDetailResp, MatchDeckResp, MatchPlayerResp, MatchCustomerResp, MatchHandCardResp, MatchDeckCardResp, MatchSettlementResp, MatchSettlementPlayer, UnlockedCollectibleCard, MatchReviveStatusResp, MatchReviveRequestPayload, MatchReviveRequestResp, PlayCardPayload, PlayCardResponse, EndTurnPayload, EndTurnResponse } from './match'

// social
export { getSkins, purchaseSkin, getQuests, claimQuestReward, fetchTasks, fetchMyTaskBoard, claimTask } from './social'
export type { Skin, Quest, ApiTask, UserTask, MyTaskBoard, TaskClaimResult } from './social'

// room
export { sendRoomInvite, acceptRoomInvite, rejectRoomInvite, getPendingRoomInvites, getRoomDetail, getCurrentRoom, releaseIdleRoom, setRoomDepartment, setRoomReady, setRoomFirstPlayer, leaveRoom, extractRoomId } from './room'
export type { RoomInviteResp, RoomPlayerResp, RoomDetailResp } from './room'
