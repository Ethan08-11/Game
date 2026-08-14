package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileReq(
        @Size(max = 50, message = "展示昵称最多50个字符") String displayName,
        @Size(max = 200, message = "签名最多200个字符") String signature,
        Integer gender,
        @Size(max = 255, message = "头像地址最多255个字符") String avatarUrl,
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱最多100个字符") String email,
        @Pattern(regexp = "^$|^[0-9+\\-\\s]{6,20}$", message = "手机号格式不正确")
        @Size(max = 20, message = "手机号最多20个字符") String phone) {
}
