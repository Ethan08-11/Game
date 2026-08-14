package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterReq(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{4,50}$", message = "用户名须为4-50位字母、数字或下划线") String username,
        @NotBlank @Size(min = 3, max = 64, message = "密码须为3-64位") String password) {
}
