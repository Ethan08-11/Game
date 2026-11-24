package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicAccountNamesTest {

    @Test
    void prefixesTheStaffName() {
        assertThat(PublicAccountNames.mirrorName("ETHAN", "ETHAN")).isEqualTo("HIH Ethan");
        assertThat(PublicAccountNames.mirrorName("Sean", "Sean")).isEqualTo("HIH Sean");
        assertThat(PublicAccountNames.mirrorName("HARRISON", "HARRSION")).isEqualTo("HIH Harrison");
    }

    @Test
    void doesNotPrefixTwice() {
        assertThat(PublicAccountNames.mirrorName("HIH Ethan", "HIH Ethan")).isEqualTo("HIH Ethan");
    }
}
