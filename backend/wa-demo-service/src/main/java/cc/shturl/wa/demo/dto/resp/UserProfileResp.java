package cc.shturl.wa.demo.dto.resp;

public record UserProfileResp(Long userId, String username, String displayName, String signature, Integer gender,
                              String avatarUrl, Integer level, Integer exp, Integer winCount, Integer loseCount,
                              Integer drawCount, Long money) {
}
