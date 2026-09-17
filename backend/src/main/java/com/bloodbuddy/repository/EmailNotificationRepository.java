package com.bloodbuddy.repository;

import com.bloodbuddy.model.DeliveryStatus;
import com.bloodbuddy.model.EmailNotification;
import com.bloodbuddy.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link EmailNotification} (§2.3). The §2.4
 * evidence trail: every mail the system attempted is queryable here.
 */
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    List<EmailNotification> findByRecipient_UserIdOrderByCreatedAtDesc(Long userId);

    List<EmailNotification> findByDeliveryStatus(DeliveryStatus status);

    /** The request's notification audit (who was alerted about it). */
    List<EmailNotification> findByRequest_RequestIdOrderByCreatedAtDesc(Long requestId);

    long countByDeliveryStatus(DeliveryStatus status);

    /**
     * Seeder idempotency for the mail that has NO request behind it (the §2.4
     * registration confirmation): "has this account already been welcomed?"
     * Without it every single boot adds one more welcome row for the same user —
     * the guard in {@code DemoDataSeeder.notify} only covered request-bound mail.
     */
    boolean existsByRecipient_UserIdAndTypeAndRequestIsNull(Long userId, NotificationType type);
}
