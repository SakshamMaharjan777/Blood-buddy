package com.bloodbuddy.web;

import com.bloodbuddy.dto.*;
import com.bloodbuddy.service.ActorContext;
import com.bloodbuddy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * {@code /api/auth/*} (proposal Appendix B) — registration, login, the reset
 * request and the current-account lookup.
 *
 * <p>HTTP only: every decision (uniqueness, BCrypt, suspension, role match) is
 * made by {@link AuthService}. Errors leave as 409/400 via {@link ApiExceptionHandler}.
 *
 * <p>Login is the one place that touches the session: the credentials are verified
 * in the service, and {@link SessionAuthenticator} then opens the authenticated
 * session (a {@code JSESSIONID} cookie). Everything after that is ordinary Spring
 * Security — see {@code config/SecurityConfig}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final SessionAuthenticator sessions;

    /** 201 — a new account is created. */
    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }

    /**
     * 200 + a session cookie. A wrong password, an unknown address and a suspended
     * account all answer 409 with the service's own message.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        LoginResponse response = auth.login(request);
        sessions.signIn(httpRequest, httpResponse, response.user());
        return response;
    }

    /**
     * Always 200 with the same wording, whether or not the address exists — the
     * endpoint must not be usable to discover which emails have accounts.
     */
    @PostMapping("/forgot")
    public ApiAck forgot(@Valid @RequestBody ForgotRequest request) {
        auth.forgotPassword(request.email());
        return ApiAck.of("If that email has a BloodBuddy account, a reset link is on its way.");
    }

    /** The account behind the current session — the real replacement for the demo's {@code bb_session}. */
    @GetMapping("/me")
    public AccountResponse me(ActorContext actor) {
        return auth.me(actor.userId());
    }

    /**
     * Ends the session server-side (JSON, not a redirect). The demo's Log out button
     * still only clears {@code bb_session} in the browser — wiring it to this call is
     * part of the {@code MOCK = false} work, and until then a closed tab was the only
     * real logout there was.
     */
    @PostMapping("/logout")
    public ApiAck logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        sessions.signOut(httpRequest, httpResponse);
        return ApiAck.of("Signed out.");
    }
}
