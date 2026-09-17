package com.bloodbuddy.web;

import com.bloodbuddy.dto.AccountResponse;
import com.bloodbuddy.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates and destroys the server-side session for a successful login.
 *
 * <h2>Why the login endpoint is ours, not Spring's form login</h2>
 * The frontend posts JSON to {@code POST /api/auth/login} ({@code bb-api.js}) — form
 * login expects {@code application/x-www-form-urlencoded}, and a JSON login filter
 * would be a lot of machinery to avoid one call. So the credentials are verified by
 * {@code AuthService} (BCrypt lives there, in the Service layer, per §2.2) and this
 * class then establishes the authenticated {@code SecurityContext} and hands it to
 * the session repository — after which everything is standard Spring Security:
 * a {@code JSESSIONID} cookie, {@code SecurityContextHolder} populated on every
 * request, and {@code ROLE_*} authorities for {@code authorizeHttpRequests} and
 * {@code @PreAuthorize}.
 *
 * <p>What that gives up, and it is worth knowing: no
 * {@code AuthenticationManager}/{@code UserDetailsService} (so no provider-level
 * brute-force counters or {@code SessionAuthenticationStrategy} hooks). The rules
 * themselves are unaffected — this is the same verify-then-open-a-session shape the
 * proposal describes, with the decision kept in the service layer.
 */
@Component
@RequiredArgsConstructor
public class SessionAuthenticator {

    /** Spring Security's convention: an authority is {@code ROLE_} + the role name. */
    private static final String ROLE_PREFIX = "ROLE_";

    private final SecurityContextRepository securityContextRepository;

    /** Open a session for a verified account. */
    public void signIn(HttpServletRequest request, HttpServletResponse response, AccountResponse account) {
        Role role = Role.valueOf(account.role().toUpperCase());
        AuthenticatedAccount principal =
                new AuthenticatedAccount(Long.valueOf(account.id()), account.email(), role);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        request.getSession(true);
        // Save explicitly: the context is only persisted to the session by the
        // filter chain, which has already run for THIS request.
        securityContextRepository.saveContext(context, request, response);
    }

    /** Invalidate the session and empty the context. */
    public void signOut(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
    }
}
