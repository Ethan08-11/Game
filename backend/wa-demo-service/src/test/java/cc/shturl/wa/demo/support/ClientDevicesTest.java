package cc.shturl.wa.demo.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientDevicesTest {

    @Test
    @DisplayName("机器码去空白并转成小写")
    void shouldNormalizeMachineId() {
        assertThat(ClientDevices.normalize(" WIN:00112233-4455-6677-8899-AABBCCDDEEFF "))
                .isEqualTo("win:00112233-4455-6677-8899-aabbccddeeff");
    }

    @Test
    @DisplayName("过短或带空格的值不当成机器码")
    void shouldRejectInvalidDeviceId() {
        assertThat(ClientDevices.normalize("short")).isNull();
        assertThat(ClientDevices.normalize("win:bad id")).isNull();
        assertThat(ClientDevices.normalize(null)).isNull();
    }
}
