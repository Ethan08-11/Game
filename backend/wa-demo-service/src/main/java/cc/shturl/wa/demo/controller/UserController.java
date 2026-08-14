package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.req.UpdateProfileReq;
import cc.shturl.wa.demo.dto.resp.UserProfileResp;
import cc.shturl.wa.demo.dto.resp.UserSearchResp;
import cc.shturl.wa.demo.dto.resp.UserStatsResp;
import cc.shturl.wa.demo.security.AuthTokenSupport;
import cc.shturl.wa.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthTokenSupport authTokenSupport;

    @GetMapping("/{id}/profile")
    public Result<UserProfileResp> getProfile(@RequestHeader("Authorization") String authorization, @PathVariable("id") Long id) {
        return Result.ok(userService.getProfile(authTokenSupport.requireUserIdFromAccessToken(authorization), id));
    }

    @PutMapping("/me/profile")
    public Result<UserProfileResp> updateMyProfile(@RequestHeader("Authorization") String authorization,
                                                   @Valid @RequestBody UpdateProfileReq request) {
        return Result.ok(userService.updateMyProfile(authTokenSupport.requireUserIdFromAccessToken(authorization), request));
    }

    @GetMapping("/{id}/stats")
    public Result<UserStatsResp> getStats(@RequestHeader("Authorization") String authorization, @PathVariable("id") Long id) {
        return Result.ok(userService.getStats(authTokenSupport.requireUserIdFromAccessToken(authorization), id));
    }

    /** 用户搜索（按用户名模糊匹配，纯数字时按 ID 精确匹配）。 */
    @GetMapping("/search")
    public Result<List<UserSearchResp>> searchUsers(@RequestHeader("Authorization") String authorization,
                                                    @RequestParam("keyword") String keyword) {
        return Result.ok(userService.searchUsers(authTokenSupport.requireUserIdFromAccessToken(authorization), keyword));
    }
}
