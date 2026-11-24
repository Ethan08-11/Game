package cc.shturl.wa.demo.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerHookRevertTest {

    @Test
    @DisplayName("数据库读出的带空格 JSON 仍能对上本回合")
    void spacedJsonMatchesRound() {
        String extra = "{\"fromCustomer\": true, \"grantedRound\": 12}";
        assertThat(CustomerHookRevert.fromRound(extra, 12)).isTrue();
        assertThat(CustomerHookRevert.fromRound(extra, 1)).isFalse();
    }

    @Test
    @DisplayName("写入时的紧凑 JSON 也能对上")
    void compactJsonMatchesRound() {
        assertThat(CustomerHookRevert.fromRound("{\"fromCustomer\":true,\"grantedRound\":4}", 4)).isTrue();
    }

    @Test
    @DisplayName("不是顾客挂上的效果不撤回")
    void otherHooksStay() {
        assertThat(CustomerHookRevert.fromRound("{\"expireAtRoundEnd\": true}", 4)).isFalse();
        assertThat(CustomerHookRevert.fromRound(null, 4)).isFalse();
    }
}
