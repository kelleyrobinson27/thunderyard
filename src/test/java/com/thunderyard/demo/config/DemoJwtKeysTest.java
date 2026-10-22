package com.thunderyard.demo.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoJwtKeysTest {

    @Test
    void generatesRsaKeyWithKid() throws Exception {
        RSAKey key = new DemoJwtKeys().demoRsaKey();
        assertThat(key.getKeyID()).isEqualTo(DemoJwtKeys.KEY_ID);
        assertThat(key.toRSAPublicKey()).isNotNull();
        assertThat(key.toPrivateKey()).isNotNull();
        assertThat(key.toPublicJWK().toJSONObject()).doesNotContainKey("d");
    }
}
