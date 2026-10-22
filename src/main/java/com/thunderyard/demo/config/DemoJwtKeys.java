package com.thunderyard.demo.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoJwtKeys {

    public static final String KEY_ID = "thunderyard-demo";

    @Bean
    public RSAKey demoRsaKey() throws JOSEException {
        return new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
    }
}
