package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotBlank;

public record LoginReq(@NotBlank String username, @NotBlank String password) {
}
