package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotNull;

public record RoomCustomerReq(@NotNull Long customerTypeId) {
}
