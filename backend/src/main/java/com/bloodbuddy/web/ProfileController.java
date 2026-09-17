package com.bloodbuddy.web;

import com.bloodbuddy.dto.ProfileDto;
import com.bloodbuddy.service.ActorContext;
import com.bloodbuddy.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * {@code /api/profile/{role}} — the four profile editors (donor, requester,
 * hospital, admin).
 *
 * <p>The role in the path says which editor is asking; the ACCOUNT comes from the
 * session ({@code actor.userId()}), never from the body or the path — a profile
 * endpoint that took a user id would let anyone edit anyone. The service refuses a
 * null session with "Sign in to view your profile."
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profiles;

    @GetMapping("/{role}")
    public ProfileDto get(@PathVariable String role, ActorContext actor) {
        return profiles.get(role, actor.userId());
    }

    /** Answers with what is now STORED, not an echo of the request body. */
    @PutMapping("/{role}")
    public ProfileDto save(@PathVariable String role, @RequestBody ProfileDto form, ActorContext actor) {
        return profiles.save(role, actor.userId(), form);
    }
}
