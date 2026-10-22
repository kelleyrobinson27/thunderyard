package com.thunderyard.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity  // this makes sure SecurityConfig is used
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // Allow GET /events
                        .requestMatchers(HttpMethod.GET, "/events/**").permitAll()

                        // Allow replay endpoints
                        .requestMatchers(HttpMethod.POST, "/events/replay/**").permitAll()

                        // Allow all appointment event POST endpoints
                        .requestMatchers(HttpMethod.POST, "/events/appointment-**").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
