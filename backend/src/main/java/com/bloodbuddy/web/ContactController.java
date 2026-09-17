package com.bloodbuddy.web;

import com.bloodbuddy.dto.ApiAck;
import com.bloodbuddy.dto.ContactRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/contact} — the site contact form. Validates and acknowledges;
 * nothing is stored (no CONTACT table in the ERD). See {@link ContactRequest} for
 * why that is said out loud rather than faked.
 */
@Slf4j
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    @PostMapping
    public ApiAck submit(@Valid @RequestBody ContactRequest request) {
        log.info("Contact form message from {} <{}> ({} characters)",
                request.name(), request.email(), request.message().length());
        return ApiAck.of("Thanks " + request.name() + " — your message has reached the BloodBuddy team.");
    }
}
