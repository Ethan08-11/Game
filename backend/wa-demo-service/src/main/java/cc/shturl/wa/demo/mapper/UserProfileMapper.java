package cc.shturl.wa.demo.mapper;

import cc.shturl.wa.demo.entity.UserProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    @Update("""
            UPDATE user_profiles
            SET win_count = IFNULL(win_count, 0) + #{winDelta},
                lose_count = IFNULL(lose_count, 0) + #{loseDelta},
                draw_count = IFNULL(draw_count, 0) + #{drawDelta},
                exp = IFNULL(exp, 0) + #{expDelta},
                money = IFNULL(money, 0) + #{moneyDelta},
                weekly_money = IFNULL(weekly_money, 0) + #{moneyDelta}
            WHERE user_id = #{userId}
            """)
    int applyMatchSettlement(@Param("userId") Long userId,
                             @Param("winDelta") int winDelta,
                             @Param("loseDelta") int loseDelta,
                             @Param("drawDelta") int drawDelta,
                             @Param("expDelta") int expDelta,
                             @Param("moneyDelta") long moneyDelta);
}
