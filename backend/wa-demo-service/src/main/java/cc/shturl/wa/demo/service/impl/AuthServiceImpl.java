package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.common.result.ResultCode;
import cc.shturl.wa.demo.dto.req.ChangePasswordReq;
import cc.shturl.wa.demo.dto.req.LoginReq;
import cc.shturl.wa.demo.dto.req.RegisterReq;
import cc.shturl.wa.demo.dto.resp.AuthResp;
import cc.shturl.wa.demo.dto.resp.UserMeResp;
import cc.shturl.wa.demo.entity.Friendships;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.enums.FriendshipStatus;
import cc.shturl.wa.demo.mapper.FriendshipsMapper;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.AuthService;
import cc.shturl.wa.demo.service.TokenService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final FriendshipsMapper friendshipsMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional
    public AuthResp register(RegisterReq request) {
        String username = normalizeUsername(request.username());
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("用户名已存在");
        }
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setDisplayName(username);
        profile.setLevel(1);
        profile.setExp(0);
        profile.setWinCount(0);
        profile.setLoseCount(0);
        profile.setDrawCount(0);
        profile.setMoney(0L);
        profileMapper.insert(profile);
        linkExistingFriends(user.getId());
        return response(user, profile);
    }

    @Override
    public AuthResp login(LoginReq request) {
        String username = normalizeUsername(request.username());
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null && !username.equals(request.username().trim())) {
            user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username().trim()));
        }
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, user.getId()));
        return response(user, profile);
    }

    @Override
    public AuthResp refresh(String refreshToken) {
        Long userId = tokenService.resolveUserIdByRefreshToken(refreshToken);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "刷新令牌无效或已过期");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, userId));
        TokenService.TokenPair rotated = tokenService.rotateRefreshToken(refreshToken, userId);
        String displayName = profile == null ? user.getUsername() : profile.getDisplayName();
        return new AuthResp(rotated.accessToken(), rotated.refreshToken(),
                new AuthResp.UserSummary(user.getId(), user.getUsername(), displayName, user.getAvatarUrl()));
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        Long userId = tokenService.resolveUserId(accessToken);
        tokenService.revokeAccessToken(accessToken);
        tokenService.revokeRefreshToken(refreshToken);
        if (userId != null) {
            tokenService.markOffline(userId);
        }
    }

    @Override
    public UserMeResp me(String accessToken) {
        Long userId = tokenService.resolveUserId(accessToken);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录态已失效");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserProfile profile = profileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery().eq(UserProfile::getUserId, userId));
        return new UserMeResp(
                user.getId(),
                user.getUsername(),
                profile == null ? user.getUsername() : profile.getDisplayName(),
                user.getAvatarUrl(),
                user.getEmail(),
                user.getPhone(),
                profile == null ? 1 : profile.getLevel(),
                profile == null ? 0 : profile.getExp(),
                profile == null ? 0 : profile.getWinCount(),
                profile == null ? 0 : profile.getLoseCount(),
                profile == null ? 0 : profile.getDrawCount(),
                profile == null || profile.getMoney() == null ? 0L : profile.getMoney());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordReq request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    private AuthResp response(User user, UserProfile profile) {
        TokenService.TokenPair tokens = tokenService.issue(user.getId());
        tokenService.markOnline(user.getId());
        String displayName = profile == null ? user.getUsername() : profile.getDisplayName();
        return new AuthResp(tokens.accessToken(), tokens.refreshToken(),
                new AuthResp.UserSummary(user.getId(), user.getUsername(), displayName, user.getAvatarUrl()));
    }

    private void linkExistingFriends(Long newUserId) {
        List<User> others = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .ne(User::getId, newUserId)
                .eq(User::getStatus, 1));
        for (User other : others) {
            if (other.getId() == null) {
                continue;
            }
            Friendships friendships = new Friendships();
            friendships.setUserId(newUserId);
            friendships.setFriendId(other.getId());
            friendships.setStatus(FriendshipStatus.ACCEPTED.getCode());
            friendshipsMapper.insert(friendships);
        }
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
