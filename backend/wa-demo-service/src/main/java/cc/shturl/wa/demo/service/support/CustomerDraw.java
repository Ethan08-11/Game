package cc.shturl.wa.demo.service.support;

import cc.shturl.wa.demo.entity.CustomerTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 开局只避开调用方给出的上一把顾客，保证连续两把不同。再往前的可以再遇到。
 * 这些顾客把池子挤空时，留下最久的一位，避免无顾客可开。
 */
public final class CustomerDraw {

    private CustomerDraw() {
    }

    public static List<CustomerTypes> preferFresh(List<CustomerTypes> pool, List<Long> recentNewestFirst) {
        if (pool == null || pool.size() <= 1 || recentNewestFirst == null || recentNewestFirst.isEmpty()) {
            return pool;
        }
        Set<Long> poolIds = new HashSet<>();
        for (CustomerTypes customer : pool) {
            if (customer != null && customer.getId() != null) {
                poolIds.add(customer.getId());
            }
        }
        if (poolIds.size() <= 1) {
            return pool;
        }
        LinkedHashSet<Long> recent = new LinkedHashSet<>();
        for (Long customerId : recentNewestFirst) {
            if (customerId != null && poolIds.contains(customerId)) {
                recent.add(customerId);
            }
        }
        Set<Long> blocked = new HashSet<>();
        for (Long customerId : recent) {
            if (blocked.size() + 1 >= poolIds.size()) {
                break;
            }
            blocked.add(customerId);
        }
        if (blocked.isEmpty()) {
            return pool;
        }
        List<CustomerTypes> fresh = new ArrayList<>();
        for (CustomerTypes customer : pool) {
            if (customer == null || customer.getId() == null || !blocked.contains(customer.getId())) {
                fresh.add(customer);
            }
        }
        return fresh.isEmpty() ? pool : fresh;
    }
}
