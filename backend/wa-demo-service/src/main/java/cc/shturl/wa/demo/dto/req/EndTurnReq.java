package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EndTurnReq(
        @NotBlank String clientActionId,
        @NotNull Long expectedVersion
) {
}
