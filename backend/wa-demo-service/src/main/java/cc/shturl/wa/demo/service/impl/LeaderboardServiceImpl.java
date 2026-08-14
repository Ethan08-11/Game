package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.LeaderboardResp;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.LeaderboardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;

    @Override
    public List<LeaderboardResp> listLeaderboard(Long currentUserId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = (safePage - 1) * safeSize;

        List<UserProfile> profiles = userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery()
                .orderByDesc(UserProfile::getMoney)
                .orderByAsc(UserProfile::getUserId));

        List<LeaderboardResp> ranked = new ArrayList<>();
        int rank = 1;
        for (UserProfile profile : profiles) {
            if (profile.getUserId() == null) {
                continue;
            }
            ranked.add(toResp(rank, profile));
            rank++;
        }

        if (fromIndex >= ranked.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + safeSize, ranked.size());
        return ranked.subList(fromIndex, toIndex);
    }

    @Override
    public LeaderboardResp getMyRank(Long currentUserId) {
        List<UserProfile> profiles = userProfileMapper.selectList(Wrappers.<UserProfile>lambdaQuery()
                .orderByDesc(UserProfile::getMoney)
                .orderByAsc(UserProfile::getUserId));

        int rank = 1;
        for (UserProfile profile : profiles) {
            if (profile.getUserId() == null) {
                continue;
            }
            if (currentUserId.equals(profile.getUserId())) {
                return toResp(rank, profile);
            }
            rank++;
        }

        User user = userMapper.selectById(currentUserId);
        return new LeaderboardResp(rank, currentUserId,
                user == null ? null : user.getUsername(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getAvatarUrl(),
                0L);
    }

    private LeaderboardResp toResp(int rank, UserProfile profile) {
        User user = userMapper.selectById(profile.getUserId());
        return new LeaderboardResp(
                rank,
                profile.getUserId(),
                user == null ? null : user.getUsername(),
                profile.getDisplayName() == null ? (user == null ? null : user.getUsername()) : profile.getDisplayName(),
                user == null ? null : user.getAvatarUrl(),
                profile.getMoney() == null ? 0L : profile.getMoney());
    }
}
