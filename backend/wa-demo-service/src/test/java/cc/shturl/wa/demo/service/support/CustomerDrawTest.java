package cc.shturl.wa.demo.service.support;

import cc.shturl.wa.demo.entity.CustomerTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerDrawTest {

    @Test
    @DisplayName("没有近期记录时保持原池")
    void emptyHistoryKeepsPool() {
        List<CustomerTypes> pool = pool(1L, 2L, 3L);
        assertThat(CustomerDraw.preferFresh(pool, List.of())).containsExactlyElementsOf(pool);
        assertThat(CustomerDraw.preferFresh(pool, null)).containsExactlyElementsOf(pool);
    }

    @Test
    @DisplayName("连续同一顾客时排除该顾客")
    void repeatedCustomerIsExcluded() {
        List<CustomerTypes> fresh = CustomerDraw.preferFresh(pool(1L, 2L, 6L), List.of(6L, 6L, 6L, 6L, 6L));
        assertThat(fresh).extracting(CustomerTypes::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("近期盖满整池时留下最久没出现的一位")
    void fullCoverageLeavesOldest() {
        List<CustomerTypes> fresh = CustomerDraw.preferFresh(pool(1L, 2L, 3L), List.of(3L, 2L, 1L));
        assertThat(fresh).extracting(CustomerTypes::getId).containsExactly(1L);
    }

    @Test
    @DisplayName("不在当前池里的顾客不占排除名额")
    void outsidePoolDoesNotBlock() {
        List<CustomerTypes> fresh = CustomerDraw.preferFresh(pool(6L, 8L, 10L), List.of(1L, 2L, 6L));
        assertThat(fresh).extracting(CustomerTypes::getId).containsExactly(8L, 10L);
    }

    @Test
    @DisplayName("池里只剩一位时不排除")
    void singleCandidateStays() {
        List<CustomerTypes> pool = pool(6L);
        assertThat(CustomerDraw.preferFresh(pool, List.of(6L))).containsExactlyElementsOf(pool);
    }

    private static List<CustomerTypes> pool(Long... ids) {
        return java.util.Arrays.stream(ids).map(id -> {
            CustomerTypes customer = new CustomerTypes();
            customer.setId(id);
            return customer;
        }).toList();
    }
}
