package cc.shturl.wa.demo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameEditionTest {

    @Test
    void blankAudienceStaysInternal() {
        assertThat(GameEdition.normalize(null)).isEqualTo(GameEdition.INTERNAL);
        assertThat(GameEdition.normalize("")).isEqualTo(GameEdition.INTERNAL);
        assertThat(GameEdition.normalize("public")).isEqualTo(GameEdition.PUBLIC);
    }

    @Test
    void publicCatalogDoesNotIncludeInternalGossipCustomers() {
        assertThat(GameEdition.PUBLIC_CUSTOMER_CODES)
                .doesNotContain("CUSTOMER_TIMID", "CUSTOMER_ANXIOUS", "CUSTOMER_HARSH",
                        "CUSTOMER_HOTPOT", "CUSTOMER_JUJUBE", "CUSTOMER_PHONE");
        assertThat(GameEdition.isPublicCustomer("PUBLIC_STEADY")).isTrue();
        assertThat(GameEdition.isPublicCustomer("CUSTOMER_KIND")).isFalse();
    }
}
