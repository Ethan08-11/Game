package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.req.EndTurnReq;
import cc.shturl.wa.demo.dto.req.ChooseFirstPlayerReq;
import cc.shturl.wa.demo.dto.req.MatchReviveReq;
import cc.shturl.wa.demo.dto.req.PlayCardReq;
import cc.shturl.wa.demo.dto.resp.*;

public interface MatchService {
    Long initializeMatch(Long roomId);
    MatchStateResp getMatchState(Long currentUserId, Long matchId);
    MatchDeckResp getMatchDeck(Long currentUserId, Long matchId);
    MatchSettlementResp getMatchSettlement(Long currentUserId, Long matchId);
    MatchFirstPlayerResp chooseFirstPlayer(Long currentUserId, Long matchId, ChooseFirstPlayerReq request);
    void reconnect(Long currentUserId, Long matchId);
    void abandon(Long currentUserId, Long matchId);
    void markPlayerDisconnected(Long userId);
    MatchActionResp playCard(Long currentUserId, Long matchId, PlayCardReq request);
    EndTurnResp endTurn(Long currentUserId, Long matchId, EndTurnReq request);
    MatchReviveStatusResp getReviveStatus(Long currentUserId, Long matchId);
    MatchReviveResp requestRevive(Long currentUserId, Long matchId, MatchReviveReq request);
    void timeoutReviveMatches();
}
