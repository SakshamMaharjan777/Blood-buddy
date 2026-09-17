package com.bloodbuddy.service;

import com.bloodbuddy.dto.AccountResponse;
import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.BloodRequest;
import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.EmailNotification;
import com.bloodbuddy.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * THE SMTP EDGE (B4, CPJ119 §2.4). Everything that knows how to talk to a mail
 * server lives here; everything that knows WHEN to notify lives in
 * {@link NotificationService}. Splitting them keeps the audit-trail logic
 * testable without a mail server and the message wording in one readable file.
 *
 * <p>{@link #send} COMPOSES from the audit row and returns the plain-text body it
 * actually sent, so the caller can store the message on the row. Every mail goes
 * out as both parts: plain text (the fallback, and what the audit row keeps) and
 * HTML (what the inbox actually renders).
 *
 * <p><b>Which server it talks to is configuration, not code.</b> The whole
 * integration is driven by {@code spring.mail.*} / {@code bloodbuddy.mail.from},
 * which come from environment variables (see application.properties for the
 * Mailpit / Gmail / any-SMTP table). Swapping the dev inbox for real SMTP — the
 * §2.4 demo — is four env vars and no change here.
 */
@Component
@RequiredArgsConstructor
public class NotificationMailer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    /** Brand red, matched to landing.css so the mail looks like the site. */
    private static final String BRAND = "#C0202A";

    private final JavaMailSender mailSender;

    @Value("${bloodbuddy.mail.from:BloodBuddy <no-reply@bloodbuddy.local>}")
    private String from;

    /**
     * Compose and send one notification.
     *
     * @return the plain-text body that was sent (stored on the row by the caller)
     * @throws MessagingException if the message could not be built or sent
     */
    public String send(EmailNotification n) throws MessagingException {
        String to = n.getRecipientEmail();
        if (to == null || to.isBlank()) {
            // Reachable when an audit row is recorded without an address (the
            // FAILED branch). Throwing keeps the caller's contract simple: it
            // records the failure and keeps the row.
            throw new MessagingException("no recipient email address on the notification row");
        }

        Content content = content(n);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(n.getSubject() == null ? "BloodBuddy notification" : n.getSubject());
        helper.setText(content.text(), content.html());
        mailSender.send(message);
        return content.text();
    }

    /* ------------------------------------------------------------------ wording */

    /** Same message twice: plain text (fallback + audit row) and HTML (what renders). */
    private record Content(String text, String html) {
    }

    private Content content(EmailNotification n) {
        return switch (n.getType()) {
            case REGISTRATION -> registration(n);
            case URGENT_REQUEST -> urgentRequest(n);
            case STATUS_UPDATE -> statusUpdate(n);
            case PASSWORD_RESET -> passwordReset(n);
        };
    }

    /** §2.4's mandatory notification: "your registration is confirmed". */
    private Content registration(EmailNotification n) {
        User u = n.getRecipient();
        String email = u == null ? n.getRecipientEmail() : u.getEmail();
        String role = u == null ? null : AccountResponse.label(u.getRole());
        String closing = "You are receiving this because this address was used to register a "
                + "BloodBuddy account. If that was not you, you can ignore this message.";

        String text = greeting(u) + ",\n\n"
                + "Your BloodBuddy account is ready.\n\n"
                + textRow("Email", email)
                + (role == null ? "" : textRow("Role", role))
                + "\nSign in any time to register as a donor, search for compatible donors, "
                + "submit a blood request, or manage your availability.\n\n"
                + closing + "\n\n"
                + signature();

        return new Content(text, html(n.getSubject(),
                "<p>" + esc(greeting(u)) + ",</p>"
                        + "<p>Your <strong>BloodBuddy</strong> account is ready.</p>"
                        + table(htmlRow("Email", email) + (role == null ? "" : htmlRow("Role", role)))
                        + "<p>Sign in any time to register as a donor, search for compatible donors, "
                        + "submit a blood request, or manage your availability.</p>"
                        + footnote(closing)));
    }

    /** The proposal-level donor alert (objective 2): this patient can use YOUR group. */
    private Content urgentRequest(EmailNotification n) {
        User u = n.getRecipient();
        BloodRequest r = n.getRequest();
        String text = greeting(u) + ",\n\n"
                + headline(n) + "\n\n";
        String body = "";
        if (r != null) {
            text += "  Request:      " + orDash(r.getPublicCode()) + "\n"
                    + "  Blood group:  " + label(r.getBloodGroup()) + "\n"
                    + "  Units:        " + r.getUnits() + "\n"
                    + "  Hospital:     " + hospitalOf(r) + "\n"
                    + "  District:     " + orDash(r.getDistrict()) + "\n"
                    + "  Needed by:    " + (r.getNeededBy() == null ? "—" : r.getNeededBy().format(DATE)) + "\n"
                    + "  Urgency:      " + (r.getUrgency() == null ? "—" : r.getUrgency().label()) + "\n";
            body = table(
                    htmlRow("Request", orDash(r.getPublicCode()))
                    + htmlRow("Blood group", label(r.getBloodGroup()))
                    + htmlRow("Units", String.valueOf(r.getUnits()))
                    + htmlRow("Hospital", hospitalOf(r))
                    + htmlRow("District", orDash(r.getDistrict()))
                    + htmlRow("Needed by", r.getNeededBy() == null ? "—" : r.getNeededBy().format(DATE))
                    + htmlRow("Urgency", r.getUrgency() == null ? "—" : r.getUrgency().label()));
        }
        String why = whyCompatible(u, r);
        text += "\n" + why + "\n\n" + signature();
        return new Content(text, html(n.getSubject(),
                "<p>" + esc(greeting(u)) + ",</p>"
                        + "<p><strong>" + esc(headline(n)) + "</strong></p>"
                        + body
                        + "<p>" + esc(why) + "</p>"
                        + footnote("You are receiving this because your donor profile is registered as "
                        + "available and compatible with this patient.")));
    }

    /** Accept / fulfil courtesy mail (proposal-level; §2.4 only mandates REGISTRATION). */
    private Content statusUpdate(EmailNotification n) {
        User u = n.getRecipient();
        BloodRequest r = n.getRequest();
        String status = r == null || r.getStatus() == null ? "—" : r.getStatus().label();
        String text = greeting(u) + ",\n\n"
                + headline(n) + "\n\n"
                + "  Current status: " + status + "\n\n"
                + signature();
        return new Content(text, html(n.getSubject(),
                "<p>" + esc(greeting(u)) + ",</p>"
                        + "<p>" + esc(headline(n)) + "</p>"
                        + table(htmlRow("Current status", status))
                        + footnote("You are receiving this because you submitted this blood request.")));
    }

    /**
     * Honest about what the build actually does: the acknowledgement is real, the
     * self-service reset link is not shipped yet, so the mail says who to ask
     * instead of promising a link that never arrives.
     */
    private Content passwordReset(EmailNotification n) {
        User u = n.getRecipient();
        String text = greeting(u) + ",\n\n"
                + "We received a request to reset the password for your BloodBuddy account.\n\n"
                + "Self-service password reset is not enabled in this build. Please contact the "
                + "platform administrator to have your password reissued.\n\n"
                + "If you did not request this, you can ignore this message — nothing has changed.\n\n"
                + signature();
        return new Content(text, html(n.getSubject(),
                "<p>" + esc(greeting(u)) + ",</p>"
                        + "<p>We received a request to reset the password for your BloodBuddy account.</p>"
                        + "<p>Self-service password reset is not enabled in this build. Please contact the "
                        + "platform administrator to have your password reissued.</p>"
                        + footnote("If you did not request this, you can ignore this message — nothing "
                        + "has changed.")));
    }

    /**
     * "Why you" line. Reads the donor's own group from the User's donor row when
     * there is one — the alert is only ever fanned out to donors the BR-1 chart
     * already approved, so this CANNOT disagree with the matching logic; it only
     * explains the decision that was taken in {@code DonorService.compatible}.
     */
    private static String whyCompatible(User u, BloodRequest r) {
        if (r == null || r.getBloodGroup() == null) {
            return "You are registered as an available donor.";
        }
        String accepts = r.getBloodGroup().getCompatibleDonorGroups().stream()
                .sorted().map(BloodGroup::getLabel).collect(Collectors.joining(", "));
        Donor d = u == null ? null : u.getDonor();
        if (d != null && d.getBloodGroup() != null) {
            return "You can help: a " + r.getBloodGroup().getLabel() + " patient accepts your "
                    + d.getBloodGroup().getLabel() + " donation (compatible donor groups: " + accepts + ").";
        }
        return "Your donor group is compatible with this patient (compatible donor groups: " + accepts + ").";
    }

    /* ------------------------------------------------------------------ rendering */

    private static String html(String heading, String body) {
        return "<!doctype html><html><body style=\"margin:0;padding:0;background:#F7F6F4;"
                + "font-family:Arial,Helvetica,sans-serif;color:#1c1c1c;\">"
                + "<div style=\"max-width:560px;margin:0 auto;padding:24px;\">"
                + "<h1 style=\"font-size:20px;color:" + BRAND + ";margin:0 0 4px;\">BloodBuddy</h1>"
                + "<div style=\"background:#fff;border:1px solid #e6e4e0;border-radius:12px;padding:20px;"
                + "font-size:14px;line-height:1.6;\">"
                + (heading == null ? "" : "<h2 style=\"font-size:16px;margin:0 0 12px;\">" + esc(heading) + "</h2>")
                + body
                + "</div>"
                + "<p style=\"font-size:12px;color:#6b6b6b;margin:12px 2px 0;\">"
                + "BloodBuddy — blood donor management for Nepal. This is an automated message.</p>"
                + "</div></body></html>";
    }

    private static String table(String rows) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\">" + rows + "</table>";
    }

    private static String htmlRow(String label, String value) {
        return "<tr><td style=\"padding:2px 12px 2px 0;color:#6b6b6b;\">" + esc(label)
                + "</td><td style=\"padding:2px 0;\">" + esc(value == null ? "—" : value) + "</td></tr>";
    }

    private static String footnote(String what) {
        return "<p style=\"font-size:12px;color:#6b6b6b;\">" + esc(what) + "</p>";
    }

    /** "  Email:        you@example.com" — column-aligned for the plain-text part. */
    private static String textRow(String label, String value) {
        return String.format("  %-14s %s%n", label + ":", value == null ? "—" : value);
    }

    private static String signature() {
        return "— BloodBuddy\n(automated message; please do not reply)";
    }

    /** The subject line doubles as the mail's headline — one wording, two places. */
    private static String headline(EmailNotification n) {
        return n.getSubject() == null ? "BloodBuddy notification" : n.getSubject();
    }

    private static String hospitalOf(BloodRequest r) {
        if (r.getHospital() != null) {
            return r.getHospital().getLabel();
        }
        return r.getHospitalLabel() == null || r.getHospitalLabel().isBlank()
                ? "Not routed to a hospital yet" : r.getHospitalLabel();
    }

    private static String label(BloodGroup g) {
        return g == null ? "—" : g.getLabel();
    }

    private static String orDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String greeting(User u) {
        if (u == null || u.getName() == null || u.getName().isBlank()) {
            return "Hello";
        }
        return "Hi " + u.getName().trim().split("\\s+")[0];
    }

    /** Minimal HTML escaping for the values that came from user input. */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
