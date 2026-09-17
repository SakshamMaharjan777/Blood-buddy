package com.bloodbuddy.web;

import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.User;
import com.bloodbuddy.repository.DonorRepository;
import com.bloodbuddy.repository.UserRepository;
import com.bloodbuddy.service.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Injects {@link ActorContext} into controller methods from the authenticated
 * SESSION — the "who is asking" the service layer has been taking as a parameter
 * since B2, now fed by Spring Security instead of a header.
 *
 * <p>This is the class B3 flagged as the replacement point, and the replacement was
 * exactly this one method: the principal is the {@link AuthenticatedAccount} put in
 * the session by {@link SessionAuthenticator}, the entity is loaded fresh per
 * request (so a role/status change takes effect immediately instead of living on in
 * a stale session copy), and no controller or service changed.
 *
 * <p>No session → {@link ActorContext#anonymous()} (never null, so a guest request
 * cannot NPE). That is what keeps the public emergency flow working: an anonymous
 * actor may still submit a guest request, and every member-only operation refuses
 * it in the service layer with a message that says so.
 */
@Component
@RequiredArgsConstructor
public class ActorContextArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository users;
    private final DonorRepository donors;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ActorContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedAccount principal)) {
            return ActorContext.anonymous();
        }
        User user = users.findById(principal.userId()).orElse(null);
        if (user == null) {
            // The account was deleted while the session lived on: treat as nobody.
            return ActorContext.anonymous();
        }
        Donor donor = donors.findByUser_UserId(user.getUserId()).orElse(null);
        return ActorContext.of(user, donor);
    }
}
