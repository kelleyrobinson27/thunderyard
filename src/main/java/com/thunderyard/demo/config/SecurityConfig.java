package com.thunderyard.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity  // this makes sure SecurityConfig is used
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/actuator/**").permitAll()

                        // Allow GET /events
                        .requestMatchers(HttpMethod.GET, "/events/**").permitAll()

                        // Allow replay endpoints
                        .requestMatchers(HttpMethod.POST, "/events/replay/**").permitAll()

                        // Allow all appointment event POST endpoints
                        .requestMatchers(HttpMethod.POST, "/events/appointment-**").permitAll()

                        // Public JWKS (the private key stays in this process; Okta would host this)
                        .requestMatchers(HttpMethod.GET, "/oauth2/jwks").permitAll()

                        // Everything else requires authentication (Basic or JWT)
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // oauth2 resource-server disables the default demo/demo user; keep Basic working.
    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("demo")
                        .password("{noop}demo")
                        .roles("USER")
                        .build()
        );
    }
}
