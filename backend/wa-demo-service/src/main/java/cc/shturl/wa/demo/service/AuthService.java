package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.req.ChangePasswordReq;
import cc.shturl.wa.demo.dto.req.LoginReq;
import cc.shturl.wa.demo.dto.req.RegisterReq;
import cc.shturl.wa.demo.dto.resp.AuthResp;
import cc.shturl.wa.demo.dto.resp.UserMeResp;

public interface AuthService {
    AuthResp register(RegisterReq request);
    AuthResp login(LoginReq request);
    AuthResp refresh(String refreshToken);
    void logout(String accessToken, String refreshToken);
    UserMeResp me(String accessToken);
    void changePassword(Long userId, ChangePasswordReq request);
}
