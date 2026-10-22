package com.thunderyard.demo.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(RSAKey demoRsaKey, @Value("${thunderyard.jwt.secret}") String secret)
            throws JOSEException {
        JwtDecoder rsa = NimbusJwtDecoder.withPublicKey(demoRsaKey.toRSAPublicKey()).build();
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtDecoder hmac = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        return token -> decodeByAlg(token, rsa, hmac);
    }

    static org.springframework.security.oauth2.jwt.Jwt decodeByAlg(
            String token, JwtDecoder rsa, JwtDecoder hmac) {
        try {
            SignedJWT parsed = SignedJWT.parse(token);
            JWSAlgorithm alg = parsed.getHeader().getAlgorithm();
            if (JWSAlgorithm.RS256.equals(alg)) {
                return rsa.decode(token);
            }
            return hmac.decode(token);
        } catch (ParseException e) {
            throw new JwtException("Malformed JWT", e);
        }
    }
}
