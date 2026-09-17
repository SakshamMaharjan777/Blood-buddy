package com.bloodbuddy.web;

import com.bloodbuddy.dto.PageResponse;
import com.bloodbuddy.dto.UserAdminResponse;
import com.bloodbuddy.dto.UserStatusUpdateRequest;
import com.bloodbuddy.service.ActorContext;
import com.bloodbuddy.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code /api/admin/*} (proposal Appendix B) — user management: list, filter,
 * suspend, restore.
 *
 * <p>"Administrators only" is enforced twice since B6: the URL rule in
 * {@code config/SecurityConfig} and the {@code @PreAuthorize} below (defence in
 * depth — a future path change cannot accidentally open the admin surface).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService admin;

    /** The user table, filtered as the dashboard's toolbar does it. */
    @GetMapping("/users")
    public Object users(@RequestParam(required = false) String role,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false, name = "q") String query,
                        @RequestParam(required = false) Integer page,
                        @RequestParam(required = false) Integer size) {
        List<UserAdminResponse> rows = admin.listUsers(role, status, query);
        // Additive pagination (see PageResponse): no `page` → the array the
        // dashboard has always consumed; `?page=&size=` → a paged envelope.
        return page == null ? rows : PageResponse.of(rows, page, size == null ? 10 : size);
    }

    /** Suspend / restore one account (the dashboard's toggle). */
    @PatchMapping("/users/{id}")
    public UserAdminResponse setStatus(@PathVariable String id,
                                       @RequestBody UserStatusUpdateRequest request,
                                       ActorContext actor) {
        return admin.setStatus(id, request.status(), actor);
    }

    /**
     * Proposal Appendix B's explicit form:
     * {@code POST /api/admin/users/{id}/deactivate}. Same operation as the PATCH
     * toggle, addressed the way the appendix names it, so both paths exist and
     * neither can drift from the other (both call {@code AdminService.setStatus}).
     */
    @PostMapping("/users/{id}/deactivate")
    public UserAdminResponse deactivate(@PathVariable String id, ActorContext actor) {
        return admin.setStatus(id, "Suspended", actor);
    }

    @GetMapping("/users/{id}")
    public UserAdminResponse user(@PathVariable String id) {
        return admin.get(id);
    }
}
