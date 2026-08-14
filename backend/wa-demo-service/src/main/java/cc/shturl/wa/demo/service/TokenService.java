package cc.shturl.wa.demo.service;

public interface TokenService {
    TokenPair issue(Long userId);
    TokenPair rotateRefreshToken(String oldRefreshToken, Long userId);
    Long resolveUserId(String accessToken);
    Long resolveUserIdByRefreshToken(String refreshToken);
    String resolveAccessTokenByRefreshToken(String refreshToken);
    void markOnline(Long userId);
    void markOffline(Long userId);
    Integer resolveOnlineStatus(Long userId);
    void revokeAccessToken(String accessToken);
    void revokeRefreshToken(String refreshToken);
    record TokenPair(String accessToken, String refreshToken) {}
}
