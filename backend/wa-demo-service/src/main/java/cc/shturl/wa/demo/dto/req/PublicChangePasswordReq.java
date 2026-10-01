package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicChangePasswordReq(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 3, max = 64, message = "新密码须为3-64位") String newPassword,
        @NotBlank(message = "请再输入一次新密码") String confirmPassword) {
}
