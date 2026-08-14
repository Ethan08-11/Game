package cc.shturl.wa.demo.dto.resp;

public record FriendResp(Long id, Long userId, Long friendId, Integer status, String remarkName, String username,
                         String displayName, String avatarUrl, Integer onlineStatus, String presenceStatus,
                         boolean invitable) {
}
