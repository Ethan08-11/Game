package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotNull;

public record ChooseFirstPlayerReq(
        @NotNull(message = "先手玩家不能为空")
        Long firstPlayerUserId
) {
}
