package com.bloodbuddy.model;

/**
 * Delivery outcome for an EMAIL_NOTIFICATION row (report Table 3-2
 * notificationId row). QUEUED → SENT, or FAILED (recorded, never thrown away,
 * so the admin can see what was not delivered and an SMTP outage does not
 * silently drop the §2.4 evidence).
 */
public enum DeliveryStatus {
    QUEUED,
    SENT,
    FAILED
}
