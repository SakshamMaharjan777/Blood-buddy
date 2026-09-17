package com.bloodbuddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * The single password-hashing policy (BR-9).
 *
 * <p>It is a bean rather than a {@code new BCryptPasswordEncoder()} per caller so
 * the seed data, registration and login can never drift into different strength
 * settings — a bug that would look like "the password I just set does not work".
 *
 * <p>B6 (Spring Security) will keep this bean and wire it into the
 * {@code AuthenticationManager}; the artifact is already on the classpath.
 */
@Configuration
public class CryptoConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
