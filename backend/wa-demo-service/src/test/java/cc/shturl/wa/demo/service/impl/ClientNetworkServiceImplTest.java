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
import static org.mockito.Mockito.lenient;
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
        noSharedDevice();
        when(values.get(RedisKeyConstants.CACHE_PREFIX + "presence:ip:" + 1L)).thenReturn("203.0.113.8");
        when(values.get(RedisKeyConstants.CACHE_PREFIX + "presence:ip:" + 2L)).thenReturn("203.0.113.8");
        ClientNetworkServiceImpl service = new ClientNetworkServiceImpl(redisTemplate, userMapper, false);

        assertThatThrownBy(() -> service.requireDistinctNetwork(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能与同一台电脑或同一网络下的账号组队");
    }

    @Test
    @DisplayName("同一 IPv6 网段的临时地址也拒绝组队")
    void shouldRejectSameIpv6Prefix() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        noSharedDevice();
        when(values.get(RedisKeyConstants.USER_CLIENT_IP_PREFIX + 1L))
                .thenReturn("2001:db8:1:2:aaaa:bbbb:cccc:dddd");
        when(values.get(RedisKeyConstants.USER_CLIENT_IP_PREFIX + 2L))
                .thenReturn("2001:db8:1:2::9");
        ClientNetworkServiceImpl service = new ClientNetworkServiceImpl(redisTemplate, userMapper, false);

        assertThatThrownBy(() -> service.requireDistinctNetwork(1L, 2L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("出口地址不同但电脑相同则拒绝组队")
    void shouldRejectSameDeviceWhenIpsDiffer() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(RedisKeyConstants.USER_CLIENT_DEVICE_PREFIX + 1L))
                .thenReturn("win:00112233-4455-6677-8899-aabbccddeeff");
        when(values.get(RedisKeyConstants.USER_CLIENT_DEVICE_PREFIX + 2L))
                .thenReturn("win:00112233-4455-6677-8899-aabbccddeeff");
        ClientNetworkServiceImpl service = new ClientNetworkServiceImpl(redisTemplate, userMapper, false);

        assertThatThrownBy(() -> service.requireDistinctNetwork(1L, 2L))
                .isInstanceOf(BusinessException.class);
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
        noSharedDevice();
        when(values.get(RedisKeyConstants.CACHE_PREFIX + "presence:ip:" + 1L)).thenReturn("203.0.113.8");
        when(values.get(RedisKeyConstants.CACHE_PREFIX + "presence:ip:" + 2L)).thenReturn(null);
        ClientNetworkServiceImpl service = new ClientNetworkServiceImpl(redisTemplate, userMapper, false);

        assertThatCode(() -> service.requireDistinctNetwork(1L, 2L)).doesNotThrowAnyException();
    }

    private void noSharedDevice() {
        lenient().when(values.get(RedisKeyConstants.USER_CLIENT_DEVICE_PREFIX + 1L)).thenReturn(null);
        lenient().when(values.get(RedisKeyConstants.USER_CLIENT_DEVICE_PREFIX + 2L)).thenReturn(null);
    }
}
