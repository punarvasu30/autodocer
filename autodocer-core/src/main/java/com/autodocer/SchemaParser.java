package com.autodocer;


import com.autodocer.DTO.ArraySchemaInfo;
import com.autodocer.DTO.FieldInfo;
import com.autodocer.DTO.SchemaInfo;
import com.autodocer.DTO.ValidationConstraints; // <-- ADDED

import java.lang.reflect.*;
import java.util.*;

// --- ADDED VALIDATION IMPORTS (JAKARTA) ---
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public class SchemaParser {

    public Object parseSchema(Type type) {
        return parseSchemaRecursive(type, new HashSet<>());
    }

    private Object parseSchemaRecursive(Type type, Set<Class<?>> visited) {

        // --- Handle ParameterizedType (like List<UserDto>) FIRST ---
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> rawClass) {
                // Is it a Collection?
                if (Collection.class.isAssignableFrom(rawClass)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        // Recursively parse the type inside the collection (e.g., UserDto)
                        Object itemSchema = parseSchemaRecursive(typeArguments[0], new HashSet<>(visited));
                        // Assuming ArraySchemaInfo exists and takes the item schema
                        return new ArraySchemaInfo(itemSchema);
                    } else {
                        // List without a specified generic type (e.g., List) - treat as List<Object> or simple type
                        return rawClass.getSimpleName(); // Or potentially ArraySchemaInfo("Object")
                    }
                }
                // Handle Map<K,V> similarly if needed later
            }
            // Fall through to treat the raw type if not a collection recognized above
            type = rawType; // Continue processing with the raw class (e.g., List)
        }

        // --- Handle Raw Class<?> ---
        // This part handles simple types and complex DTOs/Entities
        if (type instanceof Class<?> clazz) {
            // THE GATEKEEPER: If it's simple, return name
            if (isSimpleType(clazz)) {
                return clazz.getSimpleName();
            }

            // Infinite Loop Prevention
            if (visited.contains(clazz)) {
                // Return a simple string or a specific marker for circular refs
                return "Circular Reference to " + clazz.getSimpleName();
            }
            visited.add(clazz); // Mark this class as visited for the current path

            // --- START OF VALIDATION LOGIC ---
            List<FieldInfo> fields = new ArrayList<>();
            List<String> requiredFields = new ArrayList<>(); // <-- ADDED

            // Use getDeclaredFields to get only fields directly declared in this class
            for (Field field : clazz.getDeclaredFields()) {
                // Initialize constraint holders
                Integer minLength = null;
                Integer maxLength = null;
                Double min = null;
                Double max = null;
                String pattern = null;
                String format = null;

                // 1. Check for required
                if (field.isAnnotationPresent(NotNull.class) ||
                        field.isAnnotationPresent(NotBlank.class) ||
                        field.isAnnotationPresent(NotEmpty.class)) {
                    requiredFields.add(field.getName());
                }

                // 2. Check for @Size
                if (field.isAnnotationPresent(Size.class)) {
                    Size size = field.getAnnotation(Size.class);
                    if (size.min() > 0) {
                        minLength = size.min();
                    }
                    if (size.max() < Integer.MAX_VALUE) {
                        maxLength = size.max();
                    }
                }

                // 3. Check for @Min
                if (field.isAnnotationPresent(Min.class)) {
                    min = (double) field.getAnnotation(Min.class).value();
                }

                // 4. Check for @Max
                if (field.isAnnotationPresent(Max.class)) {
                    max = (double) field.getAnnotation(Max.class).value();
                }

                // 5. Check for @Email
                if (field.isAnnotationPresent(Email.class)) {
                    format = "email";
                    // If a specific regex is also provided, capture it
                    if (!field.getAnnotation(Email.class).regexp().isEmpty()) {
                        pattern = field.getAnnotation(Email.class).regexp();
                    }
                }

                // 6. Check for @Pattern (overrides @Email's pattern if both present)
                if (field.isAnnotationPresent(Pattern.class)) {
                    pattern = field.getAnnotation(Pattern.class).regexp();
                }

                // 7. Check for java.time types for format
                if (format == null) { // Only if not already set by @Email
                    String typeName = field.getType().getSimpleName();
                    if (typeName.equals("LocalDate")) {
                        format = "date";
                    } else if (typeName.equals("LocalDateTime") || typeName.equals("Instant") || typeName.equals("OffsetDateTime") || typeName.equals("ZonedDateTime")) {
                        format = "date-time";
                    } else if (typeName.equals("UUID")) {
                        format = "uuid";
                    }
                }


                // Create constraints object *only if* any were found
                ValidationConstraints constraints = null;
                if (minLength != null || maxLength != null || min != null || max != null || pattern != null || format != null) {
                    constraints = new ValidationConstraints(minLength, maxLength, min, max, pattern, format);
                }

                // Recursively parse the field's type
                Object fieldType = parseSchemaRecursive(field.getGenericType(), new HashSet<>(visited));

                // Add the FieldInfo with its new constraints
                fields.add(new FieldInfo(field.getName(), fieldType, constraints)); // <-- UPDATED
            }

            // Return the SchemaInfo with the new requiredFields list
            return new SchemaInfo(clazz.getSimpleName(), fields, requiredFields); // <-- UPDATED
        }

        // Fallback for unknown Type implementations (like TypeVariable, WildcardType)
        // Return the best representation possible, often the type name.
        return type.getTypeName();
    }


    // --- isSimpleType method remains the same ---
    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(
            String.class, Object.class, // Treat Object as simple for schema
            Integer.class, Long.class, Short.class, Byte.class,
            Double.class, Float.class,
            Boolean.class, Character.class,
            Void.class, java.math.BigDecimal.class, java.math.BigInteger.class,
            java.util.Date.class, java.sql.Date.class, java.sql.Timestamp.class,
            java.util.UUID.class
            // Add other standard types you want to treat as simple
    );

    private boolean isSimpleType(Class<?> type) {
        return type == null
                || type.isPrimitive()
                || type.equals(Void.TYPE)
                || SIMPLE_TYPES.contains(type)
                || type.isEnum()
                || type.getPackageName().startsWith("java.time") // Treat all java.time as simple
                // An array of simple types is NOT a simple type itself
                || (type.isArray() && isSimpleType(type.getComponentType()));
    }
}
