package com.bloodbuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BloodBuddy backend entry point.
 *
 * Layered per CPJ119 §2.2: Controller (HTTP only) / Service (ALL critical business
 * logic — the Appendix A compatibility chart, donor matching, request status
 * transitions) / Repository (Spring Data JPA over MySQL, §2.3).
 */
@SpringBootApplication
public class BloodBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloodBuddyApplication.class, args);
    }
}
