package cc.shturl.wa.demo.config;

import cc.shturl.wa.demo.service.ClientNetworkService;
import cc.shturl.wa.demo.service.TokenService;
import cc.shturl.wa.demo.support.ClientIps;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ClientIpCaptureFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final ClientNetworkService clientNetworkService;

    public ClientIpCaptureFilter(TokenService tokenService, ClientNetworkService clientNetworkService) {
        this.tokenService = tokenService;
        this.clientNetworkService = clientNetworkService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        remember(request);
        filterChain.doFilter(request, response);
    }

    private void remember(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return;
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return;
        }
        try {
            Long userId = tokenService.resolveUserId(token);
            if (userId != null) {
                clientNetworkService.rememberIp(userId, ClientIps.fromRequest(request));
            }
        } catch (Exception ignored) {
        }
    }
}
