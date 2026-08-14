package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MatchReviveReq(
        @NotNull(message = "用户不能为空") Long userId,
        @Size(max = 100, message = "广告请求ID最多100个字符") String adRequestId,
        @Size(max = 100, message = "广告平台最多100个字符") String adPlatform,
        @Size(max = 255, message = "复活原因最多255个字符") String reviveReason,
        String adCallbackRaw
) {
}
