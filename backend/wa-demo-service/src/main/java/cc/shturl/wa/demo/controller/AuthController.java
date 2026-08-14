package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.req.ChangePasswordReq;
import cc.shturl.wa.demo.dto.req.LoginReq;
import cc.shturl.wa.demo.dto.req.RefreshTokenReq;
import cc.shturl.wa.demo.dto.req.RegisterReq;
import cc.shturl.wa.demo.dto.resp.AuthResp;
import cc.shturl.wa.demo.dto.resp.UserMeResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthTokenSupport authTokenSupport;

    @PostMapping("/register")
    public Result<AuthResp> register(@Valid @RequestBody RegisterReq request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResp> login(@Valid @RequestBody LoginReq request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<AuthResp> refresh(@Valid @RequestBody RefreshTokenReq request) {
        return Result.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization,
                               @Valid @RequestBody RefreshTokenReq request) {
        authService.logout(authTokenSupport.extractBearerToken(authorization), request.refreshToken());
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserMeResp> me(@RequestHeader("Authorization") String authorization) {
        return Result.ok(authService.me(authTokenSupport.extractBearerToken(authorization)));
    }

    /** 修改密码（需验证原密码）。 */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestHeader("Authorization") String authorization,
                                       @Valid @RequestBody ChangePasswordReq request) {
        authService.changePassword(authTokenSupport.requireUserIdFromAccessToken(authorization), request);
        return Result.ok();
    }
}
