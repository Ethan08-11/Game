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
}
