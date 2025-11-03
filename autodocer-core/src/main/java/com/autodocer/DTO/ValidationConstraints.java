package com.autodocer.DTO;

/**
 * A record to hold common bean validation constraints extracted from a field.
 * All fields are nullable, as they are optional.
 */
public record ValidationConstraints(
        Integer minLength,
        Integer maxLength,
        Double min,
        Double max,
        String pattern,
        String format // e.g., "email", "date-time", etc.
) {
}