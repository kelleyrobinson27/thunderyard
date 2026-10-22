package com.thunderyard.demo.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationFilterTest {

    private final CorrelationFilter filter = new CorrelationFilter();

    @AfterEach
    void clearMdc() {
        CorrelationIds.close();
        TraceContext.close();
    }

    @Test
    void echoesIncomingHeaderAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIds.HEADER, "corr-inbound");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) ->
                assertThat(CorrelationIds.current()).isEqualTo("corr-inbound");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIds.HEADER)).isEqualTo("corr-inbound");
        assertThat(CorrelationIds.current()).isNull();
    }

    @Test
    void generatesIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> assertThat(CorrelationIds.current()).isNotBlank();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIds.HEADER)).isNotBlank();
        assertThat(CorrelationIds.current()).isNull();
    }

    @Test
    void echoesW3cTraceparent() throws Exception {
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACEPARENT, traceparent);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) ->
                assertThat(TraceContext.currentTraceId())
                        .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceContext.X_TRACE_ID))
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(response.getHeader(TraceContext.TRACERESPONSE)).isEqualTo(traceparent);
        assertThat(TraceContext.currentTraceId()).isNull();
    }
}
