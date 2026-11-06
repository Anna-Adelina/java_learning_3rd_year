package com.booktracker.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Для лабораторної вхідні ендпоінти reporting-service відкриті (це лише демонстрація/перевірка).
 * Захист потрібен було б додати так само, як у book-service. Власний chain також вимикає
 * стандартну форму логіну, яку інакше підняв би oauth2-client стартер.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
