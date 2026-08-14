package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.dto.req.ChangePasswordReq;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthServiceImpl#changePassword} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplChangePasswordTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("正常修改密码：校验旧密码后用 BCrypt 加密新密码并落库")
    void shouldChangePassword() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("old-hash");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        authService.changePassword(1L, new ChangePasswordReq("old-pass", "new-pass"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    @DisplayName("用户不存在抛异常")
    void shouldThrowWhenUserMissing() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> authService.changePassword(1L, new ChangePasswordReq("a", "bbbbbbbb")))
                .hasMessageContaining("用户不存在");
        verify(userMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("原密码错误抛异常")
    void shouldThrowWhenOldPasswordWrong() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("old-hash");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L, new ChangePasswordReq("wrong", "newpass123")))
                .hasMessageContaining("原密码错误");
        verify(userMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("新旧密码相同抛异常")
    void shouldThrowWhenSamePassword() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("old-hash");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("same1234", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(1L, new ChangePasswordReq("same1234", "same1234")))
                .hasMessageContaining("不能与原密码相同");
        verify(userMapper, never()).updateById(any());
    }
}
