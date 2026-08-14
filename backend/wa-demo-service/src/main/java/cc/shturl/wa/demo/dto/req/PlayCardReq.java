package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlayCardReq(
        @NotNull Long cardInstanceId,
        String targetType,
        Long targetUserId,
        @NotBlank String clientActionId,
        @NotNull Long expectedVersion
) {
}
