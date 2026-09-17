package com.bloodbuddy.config;

import com.bloodbuddy.web.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * B6 — RBAC on the real API (proposal §1.3, report Table 3-1; CPJ119 §3).
 *
 * <p>The role split the frontend enforces in {@code nav.js} (client-side, and
 * therefore only a UX guard) is now enforced server-side: the same four roles, the
 * same page-to-role mapping, but a request that ignores it gets 401/403 instead of
 * a bounced page.
 *
 * <h2>The rules</h2>
 * <pre>
 *   public        static site, POST /api/auth/{register,login,forgot}, POST /api/contact,
 *                 POST /api/requests (the guest emergency form), GET /api/requests/lookup/* (BR-8)
 *   ADMIN         /api/admin/**            — user management
 *   HOSPITAL|ADMIN /api/hospitals/**       — inventory boards (BR-5 scoping still applies inside)
 *   any signed-in /api/**                  — donors, requests, profile, auth/me
 * </pre>
 *
 * <p>Two things are deliberately NOT done here, and they are the interesting part:
 *
 * <ul>
 *   <li><b>CSRF is disabled.</b> The frontend is pure {@code fetch} JSON with
 *       same-origin credentials and no token plumbing; turning CSRF on would break
 *       every write until {@code bb-api.js} reads an {@code XSRF-TOKEN} cookie and
 *       echoes it in a header. That is a real change to a finished frontend, so it
 *       belongs to a deliberate decision (and a note in the report), not to a
 *       silent default. Session fixation is still protected by Spring's session
 *       management, and the risk this leaves is a cross-site POST from another
 *       origin — which is exactly what the report should say.</li>
 *   <li><b>BR-5 is not a URL rule.</b> "A hospital may only touch its own stock"
 *       cannot be expressed as a path pattern: it is a comparison between the
 *       session's hospital and the row's, so it stays in the service layer — and
 *       {@code HospitalController} prefers the session's hospital over any
 *       {@code ?hospitalId} a caller passes.</li>
 * </ul>
 *
 * <p>Authorization is stated at the URL level here and in {@code @PreAuthorize} on
 * the two role-restricted controllers (defence in depth: a future path change cannot
 * accidentally open the admin surface).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Public API paths — the access rules, in the order they are matched. */
    private static final String[] PUBLIC_API = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/forgot",
            "/api/contact",
            "/api/requests/lookup/**",
    };

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              SecurityContextRepository securityContextRepository,
                                              ObjectMapper mapper) throws Exception {
        http
                // See the class comment: the frontend has no CSRF token plumbing yet.
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // No form login (the login page posts JSON to /api/auth/login) and no
                // BASIC challenge (a browser popup is not an API contract).
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Logout is ours too: POST /api/auth/logout so the response is JSON.
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(mapper, response, HttpStatus.UNAUTHORIZED, "unauthorized",
                                        "Sign in to use this endpoint."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(mapper, response, HttpStatus.FORBIDDEN, "forbidden",
                                        "Your account does not have access to this action.")))
                .authorizeHttpRequests(auth -> auth
                        // The guest emergency flow and the public status lookup must
                        // work with nobody signed in (BR-8, and the whole point of
                        // emergency-request.html).
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/requests").permitAll()
                        .requestMatchers(PUBLIC_API).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/hospitals/**").hasAnyRole("HOSPITAL", "ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        // Everything else is the static frontend (marketing pages,
                        // the emergency form, the auth screens) — public by design.
                        .anyRequest().permitAll())
                // The QA/responsive harnesses load pages in IFRAMES; Spring Security's
                // default DENY would break them (and any future embedding).
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /** The same {@code ApiError} body the controllers' advice produces, so the frontend parses one shape. */
    private static void writeJson(ObjectMapper mapper, jakarta.servlet.http.HttpServletResponse response,
                                  HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getWriter(), ApiError.of(status, error, message));
    }
}
