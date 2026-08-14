package cc.shturl.wa.demo.dto.resp;

public record AuthResp(String token, String refreshToken, UserSummary user) {
    public record UserSummary(Long id, String username, String displayName, String avatarUrl) {
    }
}
