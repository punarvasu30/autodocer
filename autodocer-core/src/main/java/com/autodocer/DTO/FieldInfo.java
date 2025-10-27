//package com.autodocer.DTO;
//
//public record FieldInfo(
//        String name,
//        Object type
//) {}

package com.autodocer.DTO;

/**
 * Holds information about a single field within a schema.
 *
 * @param name The name of the field.
 * @param type The parsed type of the field (e.g., String, or another SchemaInfo).
 * @param constraints The extracted validation constraints (can be null).
 */
public record FieldInfo(
        String name,
        Object type,
        ValidationConstraints constraints // UPDATED: Changed from Object type
) {
    /**
     * Convenience constructor for fields without any constraints.
     */
    public FieldInfo(String name, Object type) {
        this(name, type, null);
    }
}