package cc.shturl.wa.demo.security;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenSupport {
    private final TokenService tokenService;

    public String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException("缺少Authorization请求头");
        }
        String token = authorization.replaceFirst("(?i)^Bearer\\s+", "");
        if (token.isBlank()) {
            throw new BusinessException("无效的Authorization请求头");
        }
        return token;
    }

    public Long requireUserIdFromAccessToken(String authorization) {
        Long userId = tokenService.resolveUserId(extractBearerToken(authorization));
        if (userId == null) {
            throw new BusinessException("登录态已失效");
        }
        return userId;
    }
}
