package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisTokenService implements TokenService {
    private static final String ACCESS_PREFIX = "auth:access:";
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String REFRESH_PAIR_PREFIX = "auth:refresh:pair:";
    private static final String USER_ACCESS_PREFIX = "auth:user:access:";
    private static final String USER_REFRESH_PREFIX = "auth:user:refresh:";
    private static final String USER_SESSION_PREFIX = "auth:user:session:";
    private static final String ONLINE_PREFIX = "online:user:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.auth.access-token-ttl:30m}")
    private Duration accessTtl;

    @Value("${app.auth.refresh-token-ttl:7d}")
    private Duration refreshTtl;

    @Override
    public TokenPair issue(Long userId) {
        String access = UUID.randomUUID().toString().replace("-", "");
        String refresh = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        storeAccessToken(access, userId);
        storeRefreshToken(refresh, userId);
        storeRefreshAccessPair(refresh, access);
        storeUserAccessToken(userId, access);
        storeUserRefreshToken(userId, refresh);
        storeUserSession(userId, access, refresh);
        return new TokenPair(access, refresh);
    }

    @Override
    public TokenPair rotateRefreshToken(String oldRefreshToken, Long userId) {
        String oldAccessToken = resolveAccessTokenByRefreshToken(oldRefreshToken);
        revokeRefreshToken(oldRefreshToken);
        if (oldAccessToken != null) {
            revokeAccessToken(oldAccessToken);
        }
        String access = UUID.randomUUID().toString().replace("-", "");
        String refresh = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        storeAccessToken(access, userId);
        storeRefreshToken(refresh, userId);
        storeRefreshAccessPair(refresh, access);
        storeUserAccessToken(userId, access);
        storeUserRefreshToken(userId, refresh);
        storeUserSession(userId, access, refresh);
        return new TokenPair(access, refresh);
    }

    @Override
    public Long resolveUserId(String accessToken) {
        String value = redisTemplate.opsForValue().get(ACCESS_PREFIX + accessToken);
        return value == null ? null : Long.valueOf(value);
    }

    @Override
    public Long resolveUserIdByRefreshToken(String refreshToken) {
        String value = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);
        return value == null ? null : Long.valueOf(value);
    }

    @Override
    public String resolveAccessTokenByRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get(REFRESH_PAIR_PREFIX + refreshToken);
    }

    @Override
    public void markOnline(Long userId) {
        redisTemplate.opsForValue().set(ONLINE_PREFIX + userId, "1", accessTtl.plusMinutes(5));
    }

    @Override
    public void markOffline(Long userId) {
        redisTemplate.delete(ONLINE_PREFIX + userId);
        redisTemplate.delete(USER_SESSION_PREFIX + userId);
    }

    @Override
    public Integer resolveOnlineStatus(Long userId) {
        String value = redisTemplate.opsForValue().get(ONLINE_PREFIX + userId);
        return value == null ? 0 : 1;
    }

    @Override
    public void revokeAccessToken(String accessToken) {
        Long userId = resolveUserId(accessToken);
        redisTemplate.delete(ACCESS_PREFIX + accessToken);
        if (userId != null) {
            redisTemplate.delete(USER_ACCESS_PREFIX + userId);
        }
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        Long userId = resolveUserIdByRefreshToken(refreshToken);
        redisTemplate.delete(REFRESH_PREFIX + refreshToken);
        redisTemplate.delete(REFRESH_PAIR_PREFIX + refreshToken);
        if (userId != null) {
            redisTemplate.delete(USER_REFRESH_PREFIX + userId);
        }
    }

    private void storeAccessToken(String accessToken, Long userId) {
        redisTemplate.opsForValue().set(ACCESS_PREFIX + accessToken, userId.toString(), accessTtl);
    }

    private void storeRefreshToken(String refreshToken, Long userId) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + refreshToken, userId.toString(), refreshTtl);
    }

    private void storeRefreshAccessPair(String refreshToken, String accessToken) {
        redisTemplate.opsForValue().set(REFRESH_PAIR_PREFIX + refreshToken, accessToken, refreshTtl);
    }

    private void storeUserAccessToken(Long userId, String accessToken) {
        redisTemplate.opsForValue().set(USER_ACCESS_PREFIX + userId, accessToken, accessTtl);
    }

    private void storeUserRefreshToken(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(USER_REFRESH_PREFIX + userId, refreshToken, refreshTtl);
    }

    private void storeUserSession(Long userId, String accessToken, String refreshToken) {
        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("accessToken", accessToken);
        session.put("refreshToken", refreshToken);
        session.put("onlineStatus", 1);
        session.put("loginAt", System.currentTimeMillis());
        try {
            redisTemplate.opsForValue().set(USER_SESSION_PREFIX + userId, objectMapper.writeValueAsString(session), refreshTtl);
        } catch (Exception exception) {
            redisTemplate.opsForValue().set(USER_SESSION_PREFIX + userId,
                    "{\"userId\":" + userId + ",\"accessToken\":\"" + accessToken + "\",\"refreshToken\":\"" + refreshToken + "\",\"onlineStatus\":1,\"loginAt\":" + System.currentTimeMillis() + "}", refreshTtl);
        }
    }
}
