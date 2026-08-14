package cc.shturl.wa.demo.dto.resp;

public record UserPresenceResp(
        Long userId,
        Integer onlineStatus,
        String presenceStatus,
        boolean invitable
) {
}
