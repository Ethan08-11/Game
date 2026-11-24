package cc.shturl.wa.demo.service;

import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.demo.entity.CustomerTypes;
import cc.shturl.wa.demo.mapper.CustomerTypesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameEditionService {
    private final JdbcTemplate jdbcTemplate;
    private final CustomerTypesMapper customerTypesMapper;

    public String audienceOf(Long userId) {
        if (userId == null) {
            return GameEdition.INTERNAL;
        }
        try {
            String audience = jdbcTemplate.queryForObject(
                    "SELECT audience FROM users WHERE id = ?",
                    String.class,
                    userId);
            return GameEdition.normalize(audience);
        } catch (Exception ignored) {
            return GameEdition.INTERNAL;
        }
    }

    public boolean isPublic(Long userId) {
        return GameEdition.isPublic(audienceOf(userId));
    }

    public void requireSameAudience(Long leftUserId, Long rightUserId) {
        if (leftUserId == null || rightUserId == null) {
            return;
        }
        if (!audienceOf(leftUserId).equals(audienceOf(rightUserId))) {
            throw new BusinessException("内部版和对外版不能一起组队");
        }
    }

    /** 列还没建好时返回 null，调用方保持原名单。 */
    public Set<Long> userIds(String audience) {
        try {
            return new HashSet<>(jdbcTemplate.query(
                    "SELECT id FROM users WHERE UPPER(IFNULL(audience, 'INTERNAL')) = ?",
                    (rs, row) -> rs.getLong(1),
                    GameEdition.normalize(audience)));
        } catch (Exception ignored) {
            return null;
        }
    }

    public CustomerTypes requirePublicCustomer(Long customerTypeId) {
        if (customerTypeId == null) {
            throw new BusinessException("请先选择本局顾客");
        }
        CustomerTypes customer = customerTypesMapper.selectById(customerTypeId);
        if (customer == null
                || customer.getStatus() == null
                || customer.getStatus() != 1
                || !GameEdition.PUBLIC.equalsIgnoreCase(customer.getEdition())
                || !GameEdition.isPublicCustomer(customer.getCustomerCode())) {
            throw new BusinessException("只能选择对外版顾客");
        }
        return customer;
    }
}
