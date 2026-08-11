package com.witbank.carwash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Security configuration.
 *
 * Authentication is handled manually via HttpSession (StaffAuthController,
 * CustomerAuthController) — Spring Security is used here only for:
 *   1. BCryptPasswordEncoder — password hashing for staff and customers.
 *   2. CSRF protection — every state-changing POST is protected.
 *
 * /webhooks/** is exempt from CSRF because Yoco posts server-to-server
 * and can never supply a browser CSRF token; it is verified instead via
 * Yoco's Svix webhook signature (YocoService.verifyWebhookSignature).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/webhooks/**"))
            .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }
}
