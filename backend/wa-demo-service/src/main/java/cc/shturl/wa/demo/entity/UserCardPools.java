package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_card_pools")
public class UserCardPools extends BaseEntity {
    private Long userId;
    private Long cardId;
    private Integer ownedCount;
    private Integer unlockedStatus;
    private Integer level;
}

