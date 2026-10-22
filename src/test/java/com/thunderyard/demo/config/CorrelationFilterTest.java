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
}
