package cc.shturl.wa.demo.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestAccountsTest {

    @Test
    @DisplayName("Ethan / Duane / Amy 视为测试账号")
    void shouldRecognizeTesters() {
        assertThat(TestAccounts.isTester("Ethan")).isTrue();
        assertThat(TestAccounts.isTester("duane")).isTrue();
        assertThat(TestAccounts.isTester(" AMY ")).isTrue();
        assertThat(TestAccounts.isTester("Carl")).isFalse();
        assertThat(TestAccounts.isTester(null)).isFalse();
    }
}
