package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordReq(
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 64, message = "新密码须为8-64位") String newPassword) {
}
