package com.bloodbuddy.model;

/**
 * Request urgency, as chosen on the request and emergency forms.
 */
public enum Urgency {
    NORMAL,
    URGENT;

    /** The label the forms and queues print ("Normal" / "Urgent"). */
    public String label() {
        return this == URGENT ? "Urgent" : "Normal";
    }

    /** Parse a submitted label; blank means NORMAL (the form's default). */
    public static Urgency fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return NORMAL;
        }
        return "urgent".equalsIgnoreCase(label.trim()) ? URGENT : NORMAL;
    }
}
