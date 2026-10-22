package com.thunderyard.demo.config;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2DemoControllerTest {

    private RSAKey rsaKey;
    private OAuth2DemoController controller;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        controller = new OAuth2DemoController(rsaKey);
        decoder = new JwtConfig().jwtDecoder(rsaKey, "thunderyard-demo-hmac-secret-key-32");
    }

    @Test
    @SuppressWarnings("unchecked")
    void jwksExposesPublicRsaKeyOnly() {
        Map<String, Object> body = controller.jwks();
        List<Map<String, Object>> keys = (List<Map<String, Object>>) body.get("keys");
        assertThat(keys).hasSize(1);
        assertThat(keys.getFirst().get("kty")).isEqualTo("RSA");
        assertThat(keys.getFirst().get("kid")).isEqualTo("test-key");
        assertThat(keys.getFirst()).doesNotContainKey("d");
    }

    @Test
    void tokenIsRs256AndDecodable() throws Exception {
        Map<String, Object> body = controller.token();
        assertThat(body.get("token_type")).isEqualTo("Bearer");
        assertThat(body.get("expires_in")).isEqualTo(3600);

        String accessToken = (String) body.get("access_token");
        assertThat(accessToken.split("\\.")).hasSize(3);
        assertThat(decoder.decode(accessToken).getSubject()).isEqualTo("sms-provider");
    }
}
