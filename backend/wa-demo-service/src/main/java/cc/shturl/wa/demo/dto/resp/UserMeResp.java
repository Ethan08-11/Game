package cc.shturl.wa.demo.dto.resp;

public record UserMeResp(Long id, String username, String displayName, String avatarUrl, String email, String phone,
                         Integer level, Integer exp, Integer winCount, Integer loseCount, Integer drawCount,
                         Long money) {
}
