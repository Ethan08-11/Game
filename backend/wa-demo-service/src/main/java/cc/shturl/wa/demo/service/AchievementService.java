package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.AchievementResp;
import cc.shturl.wa.demo.dto.resp.UserAchievementResp;

import java.util.List;

public interface AchievementService {
    List<AchievementResp> listAchievements(String category);
    List<UserAchievementResp> listMyAchievements(Long userId);
}
