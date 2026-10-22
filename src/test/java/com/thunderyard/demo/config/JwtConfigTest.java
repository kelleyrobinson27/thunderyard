package com.thunderyard.demo.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    static final String SECRET = "thunderyard-demo-hmac-secret-key-32";

    private RSAKey rsaKey;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        decoder = new JwtConfig().jwtDecoder(rsaKey, SECRET);
    }

    @Test
    void decodesHs256TokenSignedWithConfiguredSecret() throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .subject("sms-provider")
                        .issueTime(new Date())
                        .build()
        );
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));

        Jwt decoded = decoder.decode(jwt.serialize());

        assertThat(decoded.getSubject()).isEqualTo("sms-provider");
    }

    @Test
    void decodesRs256TokenSignedWithDemoKey() throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                new JWTClaimsSet.Builder().subject("sms-provider").issueTime(new Date()).build()
        );
        jwt.sign(new RSASSASigner(rsaKey.toPrivateKey()));

        Jwt decoded = decoder.decode(jwt.serialize());

        assertThat(decoded.getSubject()).isEqualTo("sms-provider");
    }

    @Test
    void rejectsTokenSignedWithWrongSecret() throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder().subject("sms-provider").build()
        );
        jwt.sign(new MACSigner("this-secret-is-not-the-configured-one!!".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> decoder.decode(jwt.serialize()))
                .isInstanceOf(Exception.class);
    }

    @Test
    void rejectsMalformedJwt() {
        assertThatThrownBy(() -> decoder.decode("not-a-jwt"))
                .isInstanceOf(Exception.class);
    }
}
