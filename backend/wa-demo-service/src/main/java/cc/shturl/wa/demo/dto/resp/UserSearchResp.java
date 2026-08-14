package cc.shturl.wa.demo.dto.resp;

/**
 * 用户搜索结果。
 *
 * @param id               用户ID
 * @param username         用户名
 * @param displayName      展示昵称
 * @param avatarUrl        头像地址
 * @param level            等级
 * @param isFriend         是否已是当前用户的好友
 * @param friendshipStatus 好友关系状态：null-无关系，0-申请中，1-已是好友，2-拉黑
 */
public record UserSearchResp(Long id, String username, String displayName, String avatarUrl, Integer level,
                             boolean isFriend, Integer friendshipStatus) {
}
