package com.thunderyard.demo.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdsTest {

    @AfterEach
    void clearMdc() {
        CorrelationIds.close();
    }

    @Test
    void currentOrReturnsFallbackWhenEmpty() {
        assertThat(CorrelationIds.currentOr("fallback")).isEqualTo("fallback");
    }

    @Test
    void currentOrKeepsExisting() {
        CorrelationIds.open(" inbound ");
        assertThat(CorrelationIds.current()).isEqualTo("inbound");
        assertThat(CorrelationIds.currentOr("fallback")).isEqualTo("inbound");
    }

    @Test
    void openIfAbsentDoesNotReplace() {
        CorrelationIds.open("first");
        assertThat(CorrelationIds.openIfAbsent("second")).isFalse();
        assertThat(CorrelationIds.current()).isEqualTo("first");
    }

    @Test
    void fromEventIdUsesCorrPrefix() {
        UUID eventId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        assertThat(CorrelationIds.fromEventId(eventId))
                .isEqualTo("corr-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    @Test
    void runSetsMdcForWorkThenClearsWhenOwned() {
        AtomicReference<String> seen = new AtomicReference<>();
        CorrelationIds.run("corr-run", () -> seen.set(CorrelationIds.current()));
        assertThat(seen.get()).isEqualTo("corr-run");
        assertThat(CorrelationIds.current()).isNull();
    }

    @Test
    void runLeavesInboundMdcInPlace() {
        CorrelationIds.open("inbound");
        CorrelationIds.run("other", () -> assertThat(CorrelationIds.current()).isEqualTo("inbound"));
        assertThat(CorrelationIds.current()).isEqualTo("inbound");
    }
}
