package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.AchievementResp;
import cc.shturl.wa.demo.dto.resp.UserAchievementResp;
import cc.shturl.wa.demo.entity.AchievementDefs;
import cc.shturl.wa.demo.entity.UserAchievements;
import cc.shturl.wa.demo.mapper.AchievementDefsMapper;
import cc.shturl.wa.demo.mapper.UserAchievementsMapper;
import cc.shturl.wa.demo.service.AchievementService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {
    private final AchievementDefsMapper achievementDefsMapper;
    private final UserAchievementsMapper userAchievementsMapper;

    @Override
    public List<AchievementResp> listAchievements(String category) {
        List<AchievementDefs> defs = achievementDefsMapper.selectList(Wrappers.<AchievementDefs>lambdaQuery()
                .eq(category != null && !category.isBlank(), AchievementDefs::getCategory, category)
                .orderByAsc(AchievementDefs::getSortNo, AchievementDefs::getId));
        return defs.stream().map(def -> new AchievementResp(def.getId(), def.getAchievementCode(), def.getAchievementName(),
                def.getCategory(), def.getDescription(), def.getConditionType(), def.getConditionValue(),
                def.getRewardType(), def.getRewardValue(), def.getSortNo(), def.getStatus())).toList();
    }

    @Override
    public List<UserAchievementResp> listMyAchievements(Long userId) {
        List<UserAchievements> records = userAchievementsMapper.selectList(Wrappers.<UserAchievements>lambdaQuery()
                .eq(UserAchievements::getUserId, userId));
        return records.stream().map(record -> {
            AchievementDefs def = achievementDefsMapper.selectById(record.getAchievementId());
            return new UserAchievementResp(record.getId(), record.getUserId(), record.getAchievementId(),
                    def == null ? null : def.getAchievementCode(),
                    def == null ? null : def.getAchievementName(),
                    record.getProgressValue(), record.getUnlockStatus(), record.getUnlockedAt(),
                    record.getClaimedStatus(), record.getClaimedAt());
        }).toList();
    }
}
