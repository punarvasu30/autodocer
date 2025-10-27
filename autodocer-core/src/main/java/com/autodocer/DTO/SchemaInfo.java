//package com.autodocer.DTO;
//
//import java.util.List;
//
//public record SchemaInfo(
//        String className,
//        List<FieldInfo> fields
//) {}


package com.autodocer.DTO;

import java.util.List;

/**
 * Holds information about a complex object schema.
 *
 * @param className The simple name of the class.
 * @param fields A list of fields in this schema.
 * @param requiredFields A list of field names that are marked as required (e.g., @NotNull).
 */
public record SchemaInfo(
        String className,
        List<FieldInfo> fields,
        List<String> requiredFields // ADDED
) {
    /**
     * Convenience constructor for schemas without required fields (though this is rare).
     */
    public SchemaInfo(String className, List<FieldInfo> fields) {
        this(className, fields, List.of()); // Default to empty list
    }
}