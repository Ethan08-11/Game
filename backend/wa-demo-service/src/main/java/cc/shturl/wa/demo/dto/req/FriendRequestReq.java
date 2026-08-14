package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotNull;

public record FriendRequestReq(@NotNull Long friendId) {
}
