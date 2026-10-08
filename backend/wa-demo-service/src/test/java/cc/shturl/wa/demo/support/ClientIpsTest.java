package cc.shturl.wa.demo.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpsTest {

    @Test
    @DisplayName("X-Forwarded-For 取第一跳并去掉端口")
    void shouldUseFirstForwardedHop() {
        assertThat(ClientIps.firstForwarded("203.0.113.10:12345, 10.0.0.1")).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("回环地址归一成 127.0.0.1")
    void shouldNormalizeLoopback() {
        assertThat(ClientIps.normalize("::1")).isEqualTo("127.0.0.1");
        assertThat(ClientIps.normalize("::ffff:127.0.0.1")).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("空值和 unknown 视为没有 IP")
    void shouldRejectBlank() {
        assertThat(ClientIps.normalize(null)).isNull();
        assertThat(ClientIps.normalize("unknown")).isNull();
        assertThat(ClientIps.firstForwarded("  ")).isNull();
    }

    @Test
    @DisplayName("临时 IPv6 只比前 64 位")
    void shouldTreatTemporaryIpv6AsSameNetwork() {
        assertThat(ClientIps.sameNetwork(
                "2001:db8:1:2:aaaa:bbbb:cccc:dddd",
                "2001:0db8:0001:0002::9")).isTrue();
        assertThat(ClientIps.sameNetwork("2001:db8:1:2::1", "2001:db8:1:3::1")).isFalse();
        assertThat(ClientIps.sameNetwork("203.0.113.8", "203.0.113.9")).isFalse();
        assertThat(ClientIps.sameNetwork("203.0.113.8", "2001:db8:1:2::1")).isFalse();
    }
}
