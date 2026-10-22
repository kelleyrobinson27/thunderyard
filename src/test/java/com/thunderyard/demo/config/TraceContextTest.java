package com.thunderyard.demo.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextTest {

    @AfterEach
    void clearMdc() {
        TraceContext.close();
    }

    @Test
    void parsesValidTraceparent() {
        String header = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        TraceContext.Parsed parsed = TraceContext.parseOrCreate(header);
        assertThat(parsed.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(parsed.spanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(parsed.flags()).isEqualTo("01");
        assertThat(parsed.asTraceparent()).isEqualTo(header);
    }

    @Test
    void generatesWhenHeaderMissingOrInvalid() {
        TraceContext.Parsed missing = TraceContext.parseOrCreate(null);
        assertThat(missing.traceId()).hasSize(32);
        assertThat(missing.spanId()).hasSize(16);

        TraceContext.Parsed zeros = TraceContext.parseOrCreate(
                "00-00000000000000000000000000000000-0000000000000000-00");
        assertThat(zeros.traceId()).isNotEqualTo("00000000000000000000000000000000");
    }

    @Test
    void openPutsMdcAndCloseClears() {
        TraceContext.Parsed parsed = TraceContext.parseOrCreate(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        TraceContext.open(parsed);
        assertThat(TraceContext.currentTraceId()).isEqualTo(parsed.traceId());
        TraceContext.close();
        assertThat(TraceContext.currentTraceId()).isNull();
    }
}
