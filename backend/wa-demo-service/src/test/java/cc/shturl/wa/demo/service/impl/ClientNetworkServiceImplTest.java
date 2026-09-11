package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.constant.RedisKeyConstants;
import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientNetworkServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> values;
    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("允许同 IP 时不拦截")
    void shouldAllowWhenFlagOn() {
        ClientNetworkServiceImpl service = new ClientNetworkServiceImpl(redisTemplate, userMapper, true);
        assertThatCode(() -> service.requireDistinctNetwork(1L, 2L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("生产环境双方 IP 相同则拒绝组队")
    void shouldRejectSameIp() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(RedisKeyConstants.USER_CLIENT_IP_PREFIX + 1L)).thenReturn("203.0.113.8");
        when(values.get(RedisKeyConstants.USER_CLIENT_IP_PREFIX + 2L)).thenReturn("203.0.113.8");
        ClientNetworkServiceImpl service = new ClientNetworkServiceImpl(redisTemplate, userMapper, false);

        assertThatThrownBy(() -> service.requireDistinctNetwork(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能与同一网络下的账号组队");
    }

    @Test
    @DisplayName("测试账号不受同网络限制")
    void shouldAllowTesterOnSameIp() {
        User duane = new User();
        duane.setUsername("Duane");
        when(userMapper.selectById(1L)).thenReturn(duane);
        ClientNetworkServiceImpl service = new ClientNetworkServiceImpl(redisTemplate, userMapper, false);

        assertThatCode(() -> service.requireDistinctNetwork(1L, 2L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("缺少一侧 IP 时放行")
    void shouldAllowWhenIpMissing() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(RedisKeyConstants.USER_CLIENT_IP_PREFIX + 1L)).thenReturn("203.0.113.8");
        when(values.get(RedisKeyConstants.USER_CLIENT_IP_PREFIX + 2L)).thenReturn(null);
        ClientNetworkServiceImpl service = new ClientNetworkServiceImpl(redisTemplate, userMapper, false);

        assertThatCode(() -> service.requireDistinctNetwork(1L, 2L)).doesNotThrowAnyException();
    }
}
