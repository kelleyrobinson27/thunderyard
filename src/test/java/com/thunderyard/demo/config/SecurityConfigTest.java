package com.thunderyard.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void demoUserExistsForHttpBasic() {
        UserDetailsService users = new SecurityConfig().users();
        UserDetails demo = users.loadUserByUsername("demo");
        assertThat(demo.getUsername()).isEqualTo("demo");
        assertThat(demo.getPassword()).isEqualTo("{noop}demo");
    }
}
