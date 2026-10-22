package com.thunderyard.demo.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

/**
 * Stand-in for an IdP (Okta, Auth0): public JWKS plus a client-credentials token.
 * Production would host these on the identity provider, not this service.
 */
@RestController
public class OAuth2DemoController {

    static final int EXPIRES_IN_SECONDS = 3600;

    private final RSAKey rsaKey;

    public OAuth2DemoController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> jwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    @PostMapping("/oauth2/token")
    public Map<String, Object> token() throws JOSEException {
        Date now = new Date();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .type(JOSEObjectType.JWT)
                        .build(),
                new JWTClaimsSet.Builder()
                        .subject("sms-provider")
                        .issuer("http://localhost:8080")
                        .audience("thunderyard")
                        .issueTime(now)
                        .expirationTime(new Date(now.getTime() + EXPIRES_IN_SECONDS * 1000L))
                        .build()
        );
        jwt.sign(new RSASSASigner(rsaKey.toPrivateKey()));
        return Map.of(
                "access_token", jwt.serialize(),
                "token_type", "Bearer",
                "expires_in", EXPIRES_IN_SECONDS
        );
    }
}
