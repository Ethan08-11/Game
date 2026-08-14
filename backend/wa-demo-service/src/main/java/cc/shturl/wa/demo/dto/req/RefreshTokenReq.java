package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenReq(@NotBlank String refreshToken) {
}
