package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.req.FriendRemarkReq;
import cc.shturl.wa.demo.dto.req.FriendRequestReq;
import cc.shturl.wa.demo.dto.resp.FriendResp;
import cc.shturl.wa.demo.dto.resp.UserPresenceResp;
import cc.shturl.wa.demo.dto.resp.UserSearchResp;
import cc.shturl.wa.demo.entity.Friendships;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserProfile;
import cc.shturl.wa.demo.enums.FriendshipStatus;
import cc.shturl.wa.demo.mapper.FriendshipsMapper;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.mapper.UserProfileMapper;
import cc.shturl.wa.demo.service.RoomNotificationService;
import cc.shturl.wa.demo.service.UserPresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link UserServiceImpl} 单元测试，覆盖本次新增/改造的好友、拉黑、搜索、备注逻辑。
 * 使用 Mockito 桩接所有 Mapper，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper profileMapper;
    @Mock
    private FriendshipsMapper friendshipsMapper;
    @Mock
    private UserPresenceService userPresenceService;
    @Mock
    private RoomNotificationService notificationService;

    @InjectMocks
    private UserServiceImpl userService;

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;
    private static final Long THIRD = 3L;

    private User user(long id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        u.setStatus(1);
        u.setAvatarUrl("http://avatar/" + id);
        return u;
    }

    private UserProfile profile(long userId) {
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        p.setLevel(5);
        p.setExp(100);
        p.setWinCount(3);
        p.setLoseCount(2);
        p.setDrawCount(1);
        p.setMoney(10L);
        return p;
    }

    private void mockUser(long id, String name) {
        when(userMapper.selectById(id)).thenReturn(user(id, name));
        // toFriendResp/toSearchResp 会按 friendId 查 profile
        when(profileMapper.selectOne(any())).thenReturn(profile(id));
    }

    private Friendships relation(Long id, Long userId, Long friendId, int status) {
        Friendships f = new Friendships();
        f.setId(id);
        f.setUserId(userId);
        f.setFriendId(friendId);
        f.setStatus(status);
        return f;
    }

    private void mockPresence(long userId) {
        when(userPresenceService.getPresence(userId)).thenReturn(
                new UserPresenceResp(userId, 1, "IDLE", true));
    }

    // ==================== requestFriend ====================

    @Nested
    @DisplayName("requestFriend - 发送好友申请")
    class RequestFriendTest {

        @Test
        @DisplayName("正常发起申请：插入 PENDING 记录并通知被申请方")
        void shouldCreatePendingAndNotify() {
            mockUser(ME, "me");
            mockUser(OTHER, "other");
            when(friendshipsMapper.selectOne(any())).thenReturn(null);
            mockPresence(OTHER);

            FriendResp resp = userService.requestFriend(ME, new FriendRequestReq(OTHER));

            ArgumentCaptor<Friendships> captor = ArgumentCaptor.forClass(Friendships.class);
            verify(friendshipsMapper).insert(captor.capture());
            Friendships saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(ME);
            assertThat(saved.getFriendId()).isEqualTo(OTHER);
            assertThat(saved.getStatus()).isEqualTo(FriendshipStatus.PENDING.getCode());

            // 通知被申请方
            verify(notificationService).notifyUser(eq(OTHER), any());
            assertThat(resp.status()).isEqualTo(FriendshipStatus.PENDING.getCode());
        }

        @Test
        @DisplayName("不能添加自己为好友")
        void shouldRejectSelfAdd() {
            assertThatThrownBy(() -> userService.requestFriend(ME, new FriendRequestReq(ME)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("自己");
            verify(friendshipsMapper, never()).insert(any());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("目标用户不存在抛异常")
        void shouldThrowWhenTargetNotExist() {
            when(userMapper.selectById(OTHER)).thenReturn(null);

            assertThatThrownBy(() -> userService.requestFriend(ME, new FriendRequestReq(OTHER)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("已是好友：幂等返回，不重复插入、不重复通知")
        void shouldIdempotentWhenAlreadyFriend() {
            mockUser(ME, "me");
            mockUser(OTHER, "other");
            Friendships existing = relation(100L, ME, OTHER, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectOne(any())).thenReturn(existing);
            mockPresence(OTHER);

            FriendResp resp = userService.requestFriend(ME, new FriendRequestReq(OTHER));

            verify(friendshipsMapper, never()).insert(any());
            verifyNoInteractions(notificationService);
            assertThat(resp.id()).isEqualTo(100L);
            assertThat(resp.status()).isEqualTo(FriendshipStatus.ACCEPTED.getCode());
        }

        @Test
        @DisplayName("对方拉黑了我：抛异常且不暴露被拉黑事实")
        void shouldThrowWhenBlockedByOther() {
            mockUser(ME, "me");
            mockUser(OTHER, "other");
            // 对方(OTHER)拉黑了我(ME)：user_id=OTHER, friend_id=ME, status=BLOCKED
            Friendships blocked = relation(200L, OTHER, ME, FriendshipStatus.BLOCKED.getCode());
            when(friendshipsMapper.selectOne(any())).thenReturn(blocked);

            assertThatThrownBy(() -> userService.requestFriend(ME, new FriendRequestReq(OTHER)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("无法添加该用户");
            verify(friendshipsMapper, never()).insert(any());
        }

        @Test
        @DisplayName("双向查重：对方此前给我发过申请，我再加对方不产生重复记录")
        void shouldDetectReversePending() {
            mockUser(ME, "me");
            mockUser(OTHER, "other");
            // 对方(OTHER)→我(ME) 已有 PENDING
            Friendships reverse = relation(300L, OTHER, ME, FriendshipStatus.PENDING.getCode());
            when(friendshipsMapper.selectOne(any())).thenReturn(reverse);
            // toFriendResp: currentUserId=ME, relation.userId=OTHER -> friendId=OTHER
            mockPresence(OTHER);

            FriendResp resp = userService.requestFriend(ME, new FriendRequestReq(OTHER));

            verify(friendshipsMapper, never()).insert(any());
            // 幂等返回：对方视角的记录，friendId 应为对方
            assertThat(resp.id()).isEqualTo(300L);
        }
    }

    // ==================== acceptFriend ====================

    @Nested
    @DisplayName("acceptFriend - 接受好友申请")
    class AcceptFriendTest {

        @Test
        @DisplayName("正常接受：状态置为 ACCEPTED 并通知申请发起方")
        void shouldAcceptAndNotify() {
            Friendships pending = relation(10L, OTHER, ME, FriendshipStatus.PENDING.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(pending);
            mockUser(ME, "me");
            mockUser(OTHER, "other");
            mockPresence(OTHER);
            when(userMapper.selectById(ME)).thenReturn(user(ME, "me"));

            FriendResp resp = userService.acceptFriend(ME, 10L, new FriendRemarkReq("备注"));

            assertThat(pending.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED.getCode());
            assertThat(pending.getRemarkName()).isEqualTo("备注");
            verify(friendshipsMapper).updateById(pending);
            verify(notificationService).notifyUser(eq(OTHER), any());
            assertThat(resp.status()).isEqualTo(FriendshipStatus.ACCEPTED.getCode());
        }

        @Test
        @DisplayName("申请不存在抛异常")
        void shouldThrowWhenNotFound() {
            when(friendshipsMapper.selectById(anyLong())).thenReturn(null);
            assertThatThrownBy(() -> userService.acceptFriend(ME, 99L, null))
                    .hasMessageContaining("不存在");
        }

        @Test
        @DisplayName("无权处理：不是申请的接收方")
        void shouldThrowWhenNotReceiver() {
            Friendships pending = relation(10L, ME, OTHER, FriendshipStatus.PENDING.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(pending);

            assertThatThrownBy(() -> userService.acceptFriend(ME, 10L, null))
                    .hasMessageContaining("无权");
        }

        @Test
        @DisplayName("已 ACCEPTED：防止重复接受")
        void shouldThrowWhenAlreadyAccepted() {
            Friendships accepted = relation(10L, OTHER, ME, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(accepted);

            assertThatThrownBy(() -> userService.acceptFriend(ME, 10L, null))
                    .hasMessageContaining("已经是好友");
            verify(friendshipsMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("非 PENDING（如 BLOCKED）不可接受")
        void shouldThrowWhenNotPending() {
            Friendships blocked = relation(10L, OTHER, ME, FriendshipStatus.BLOCKED.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(blocked);

            assertThatThrownBy(() -> userService.acceptFriend(ME, 10L, null))
                    .hasMessageContaining("不可接受");
        }
    }

    // ==================== rejectFriend ====================

    @Nested
    @DisplayName("rejectFriend - 拒绝好友申请")
    class RejectFriendTest {

        @Test
        @DisplayName("正常拒绝：删除记录")
        void shouldDeleteWhenReject() {
            Friendships pending = relation(10L, OTHER, ME, FriendshipStatus.PENDING.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(pending);

            userService.rejectFriend(ME, 10L);

            verify(friendshipsMapper).deleteById(10L);
        }

        @Test
        @DisplayName("无权拒绝：非接收方")
        void shouldThrowWhenNotReceiver() {
            Friendships pending = relation(10L, ME, OTHER, FriendshipStatus.PENDING.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(pending);

            assertThatThrownBy(() -> userService.rejectFriend(ME, 10L))
                    .hasMessageContaining("无权");
            verify(friendshipsMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("非 PENDING 不可拒绝")
        void shouldThrowWhenNotPending() {
            Friendships accepted = relation(10L, OTHER, ME, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(accepted);

            assertThatThrownBy(() -> userService.rejectFriend(ME, 10L))
                    .hasMessageContaining("不可拒绝");
        }
    }

    // ==================== deleteFriend ====================

    @Nested
    @DisplayName("deleteFriend - 删除好友")
    class DeleteFriendTest {

        @Test
        @DisplayName("任一方可删除")
        void shouldDeleteWhenInvolved() {
            Friendships rel = relation(10L, ME, OTHER, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(rel);

            userService.deleteFriend(ME, 10L);
            verify(friendshipsMapper).deleteById(10L);
        }

        @Test
        @DisplayName("记录不存在：抛异常而非静默返回")
        void shouldThrowWhenMissing() {
            when(friendshipsMapper.selectById(10L)).thenReturn(null);
            assertThatThrownBy(() -> userService.deleteFriend(ME, 10L))
                    .hasMessageContaining("不存在");
        }

        @Test
        @DisplayName("无关用户无权删除")
        void shouldThrowWhenNotInvolved() {
            Friendships rel = relation(10L, OTHER, THIRD, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(rel);

            assertThatThrownBy(() -> userService.deleteFriend(ME, 10L))
                    .hasMessageContaining("无权");
        }
    }

    // ==================== updateFriendRemark ====================

    @Nested
    @DisplayName("updateFriendRemark - 修改备注")
    class UpdateRemarkTest {

        @Test
        @DisplayName("正常修改好友备注")
        void shouldUpdateRemark() {
            Friendships rel = relation(10L, ME, OTHER, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(rel);
            mockUser(OTHER, "other");
            mockPresence(OTHER);

            FriendResp resp = userService.updateFriendRemark(ME, 10L, new FriendRemarkReq("老王"));

            assertThat(rel.getRemarkName()).isEqualTo("老王");
            verify(friendshipsMapper).updateById(rel);
            assertThat(resp.remarkName()).isEqualTo("老王");
        }

        @Test
        @DisplayName("仅好友关系可改备注")
        void shouldThrowWhenNotAccepted() {
            Friendships rel = relation(10L, ME, OTHER, FriendshipStatus.PENDING.getCode());
            when(friendshipsMapper.selectById(10L)).thenReturn(rel);

            assertThatThrownBy(() -> userService.updateFriendRemark(ME, 10L, new FriendRemarkReq("x")))
                    .hasMessageContaining("仅好友");
        }
    }

    // ==================== 申请列表 ====================

    @Nested
    @DisplayName("申请列表 - incoming / outgoing")
    class RequestListTest {

        @Test
        @DisplayName("incoming：仅返回 friend_id=我 且 PENDING 的记录")
        void listIncomingShouldFilterPendingForMe() {
            Friendships incoming = relation(1L, OTHER, ME, FriendshipStatus.PENDING.getCode());
            when(friendshipsMapper.selectList(any())).thenReturn(List.of(incoming));
            mockUser(OTHER, "other");
            mockPresence(OTHER);

            List<FriendResp> result = userService.listIncomingRequests(ME);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("outgoing：仅返回 user_id=我 且 PENDING 的记录")
        void listOutgoingShouldFilterPendingFromMe() {
            Friendships outgoing = relation(2L, ME, OTHER, FriendshipStatus.PENDING.getCode());
            when(friendshipsMapper.selectList(any())).thenReturn(List.of(outgoing));
            mockUser(OTHER, "other");
            mockPresence(OTHER);

            List<FriendResp> result = userService.listOutgoingRequests(ME);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(2L);
        }
    }

    // ==================== 拉黑 ====================

    @Nested
    @DisplayName("block / unblock / 拉黑列表")
    class BlockTest {

        @Test
        @DisplayName("正常拉黑：新建 BLOCKED 记录（user_id=我, friend_id=对方）")
        void shouldBlockNewUser() {
            when(userMapper.selectById(OTHER)).thenReturn(user(OTHER, "other"));
            when(friendshipsMapper.selectOne(any())).thenReturn(null);

            userService.blockUser(ME, new FriendRequestReq(OTHER));

            ArgumentCaptor<Friendships> captor = ArgumentCaptor.forClass(Friendships.class);
            verify(friendshipsMapper).insert(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(ME);
            assertThat(captor.getValue().getFriendId()).isEqualTo(OTHER);
            assertThat(captor.getValue().getStatus()).isEqualTo(FriendshipStatus.BLOCKED.getCode());
        }

        @Test
        @DisplayName("不能拉黑自己")
        void shouldRejectBlockSelf() {
            assertThatThrownBy(() -> userService.blockUser(ME, new FriendRequestReq(ME)))
                    .hasMessageContaining("自己");
            verify(friendshipsMapper, never()).insert(any());
        }

        @Test
        @DisplayName("已是好友再拉黑：删除旧记录并以正确方向重建为 BLOCKED")
        void shouldRewriteRelationWhenBlockingFriend() {
            when(userMapper.selectById(OTHER)).thenReturn(user(OTHER, "other"));
            // 反向记录：user_id=OTHER, friend_id=ME, ACCEPTED
            Friendships existing = relation(50L, OTHER, ME, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectOne(any())).thenReturn(existing);

            userService.blockUser(ME, new FriendRequestReq(OTHER));

            verify(friendshipsMapper).deleteById(50L);
            ArgumentCaptor<Friendships> captor = ArgumentCaptor.forClass(Friendships.class);
            verify(friendshipsMapper).insert(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(ME);
            assertThat(captor.getValue().getStatus()).isEqualTo(FriendshipStatus.BLOCKED.getCode());
        }

        @Test
        @DisplayName("重复拉黑：幂等，不重复插入")
        void shouldIdempotentWhenAlreadyBlocked() {
            when(userMapper.selectById(OTHER)).thenReturn(user(OTHER, "other"));
            Friendships existing = relation(60L, ME, OTHER, FriendshipStatus.BLOCKED.getCode());
            when(friendshipsMapper.selectOne(any())).thenReturn(existing);
            when(userMapper.selectById(OTHER)).thenReturn(user(OTHER, "other"));
            when(profileMapper.selectOne(any())).thenReturn(profile(OTHER));

            userService.blockUser(ME, new FriendRequestReq(OTHER));

            verify(friendshipsMapper, never()).insert(any());
            verify(friendshipsMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("取消拉黑：拉黑方成功删除")
        void shouldUnblock() {
            Friendships blocked = relation(60L, ME, OTHER, FriendshipStatus.BLOCKED.getCode());
            when(friendshipsMapper.selectById(60L)).thenReturn(blocked);

            userService.unblockUser(ME, 60L);
            verify(friendshipsMapper).deleteById(60L);
        }

        @Test
        @DisplayName("非拉黑方无权取消")
        void shouldThrowWhenUnblockByOther() {
            Friendships blocked = relation(60L, OTHER, ME, FriendshipStatus.BLOCKED.getCode());
            when(friendshipsMapper.selectById(60L)).thenReturn(blocked);

            assertThatThrownBy(() -> userService.unblockUser(ME, 60L))
                    .hasMessageContaining("无权");
        }

        @Test
        @DisplayName("非 BLOCKED 状态不可取消拉黑")
        void shouldThrowWhenUnblockNonBlocked() {
            Friendships accepted = relation(60L, ME, OTHER, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectById(60L)).thenReturn(accepted);

            assertThatThrownBy(() -> userService.unblockUser(ME, 60L))
                    .hasMessageContaining("不是拉黑");
        }

        @Test
        @DisplayName("拉黑列表：仅返回我发起的 BLOCKED 记录")
        void shouldListMyBlocked() {
            Friendships blocked = relation(70L, ME, OTHER, FriendshipStatus.BLOCKED.getCode());
            when(friendshipsMapper.selectList(any())).thenReturn(List.of(blocked));
            when(userMapper.selectById(OTHER)).thenReturn(user(OTHER, "other"));
            when(profileMapper.selectOne(any())).thenReturn(profile(OTHER));

            List<FriendResp> result = userService.listBlockedUsers(ME);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(FriendshipStatus.BLOCKED.getCode());
        }
    }

    // ==================== 搜索 ====================

    @Nested
    @DisplayName("searchUsers - 用户搜索")
    class SearchTest {

        @Test
        @DisplayName("空关键词返回空列表")
        void shouldReturnEmptyWhenKeywordBlank() {
            assertThat(userService.searchUsers(ME, "")).isEmpty();
            assertThat(userService.searchUsers(ME, "   ")).isEmpty();
            assertThat(userService.searchUsers(ME, null)).isEmpty();
            verify(userMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("纯数字：按 ID 精确匹配")
        void shouldSearchByNumericId() {
            User found = user(OTHER, "other");
            when(userMapper.selectById(2L)).thenReturn(found);
            // 关系预查
            when(friendshipsMapper.selectList(any())).thenReturn(List.of());
            when(profileMapper.selectOne(any())).thenReturn(profile(OTHER));

            List<UserSearchResp> result = userService.searchUsers(ME, "2");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(OTHER);
            assertThat(result.get(0).isFriend()).isFalse();
            assertThat(result.get(0).friendshipStatus()).isNull();
        }

        @Test
        @DisplayName("纯数字但用户不存在：返回空")
        void shouldReturnEmptyWhenNumericIdNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThat(userService.searchUsers(ME, "999")).isEmpty();
        }

        @Test
        @DisplayName("用户名模糊匹配，且识别已是好友的关系")
        void shouldSearchByKeywordAndMarkFriendship() {
            User u = user(OTHER, "other_user");
            when(userMapper.selectList(any())).thenReturn(List.of(u));
            Friendships accepted = relation(80L, ME, OTHER, FriendshipStatus.ACCEPTED.getCode());
            when(friendshipsMapper.selectList(any())).thenReturn(List.of(accepted));
            when(profileMapper.selectOne(any())).thenReturn(profile(OTHER));

            List<UserSearchResp> result = userService.searchUsers(ME, "other");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).username()).isEqualTo("other_user");
            assertThat(result.get(0).isFriend()).isTrue();
            assertThat(result.get(0).friendshipStatus()).isEqualTo(FriendshipStatus.ACCEPTED.getCode());
        }
    }
}
