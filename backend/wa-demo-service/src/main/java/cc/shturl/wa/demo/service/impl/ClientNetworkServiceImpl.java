package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.common.constant.RedisKeyConstants;
import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.service.ClientNetworkService;
import cc.shturl.wa.demo.support.TestAccounts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ClientNetworkServiceImpl implements ClientNetworkService {
    private static final Duration IP_TTL = Duration.ofMinutes(10);
    private static final String SAME_NETWORK_MESSAGE = "不能与同一网络下的账号组队";

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final boolean allowSameIp;

    public ClientNetworkServiceImpl(StringRedisTemplate redisTemplate,
                                    UserMapper userMapper,
                                    @Value("${app.match.allow-same-ip:true}") boolean allowSameIp) {
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
        this.allowSameIp = allowSameIp;
    }

    @Override
    public void rememberIp(Long userId, String ip) {
        if (userId == null || ip == null || ip.isBlank()) {
            return;
        }
        redisTemplate.opsForValue().set(key(userId), ip, IP_TTL);
    }

    @Override
    public String ipOf(Long userId) {
        if (userId == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(key(userId));
    }

    @Override
    public void requireDistinctNetwork(Long leftUserId, Long rightUserId) {
        if (allowSameIp || leftUserId == null || rightUserId == null || leftUserId.equals(rightUserId)) {
            return;
        }
        if (isTester(leftUserId) || isTester(rightUserId)) {
            return;
        }
        String left = ipOf(leftUserId);
        String right = ipOf(rightUserId);
        if (left == null || right == null) {
            return;
        }
        if (left.equals(right)) {
            throw new BusinessException(SAME_NETWORK_MESSAGE);
        }
    }

    private boolean isTester(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && TestAccounts.isTester(user.getUsername());
    }

    private String key(Long userId) {
        return RedisKeyConstants.USER_CLIENT_IP_PREFIX + userId;
    }
}
