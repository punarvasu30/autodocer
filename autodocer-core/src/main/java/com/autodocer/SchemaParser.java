package com.autodocer;

import com.autodocer.DTO.FieldInfo;
import com.autodocer.DTO.SchemaInfo;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A specialist class responsible for recursively parsing a Java Class
 * to build a structured SchemaInfo object. It handles nested DTOs
 * and prevents infinite loops from circular dependencies.
 */
public class SchemaParser {

    /**
     * The main entry point for parsing a schema.
     * @param type The class to parse.
     * @return An Object which is either a simple String (for basic types) or a SchemaInfo object.
     */
    public Object parseSchema(Class<?> type) {
        // We pass a new Set to track visited classes for this top-level call.
        return parseSchemaRecursive(type, new HashSet<>());
    }

    /**
     * The recursive helper method that does the actual parsing.
     * @param type The class to parse.
     * @param visited A set of classes already seen in the current parsing path to prevent infinite loops.
     * @return An Object which is either a simple String or a SchemaInfo object.
     */
    private Object parseSchemaRecursive(Class<?> type, Set<Class<?>> visited) {
        // 1. THE GATEKEEPER: If it's a simple type, stop immediately and return its name.
        if (isSimpleType(type)) {
            return type.getSimpleName();
        }

        // 2. Infinite Loop Prevention: If we have already seen this class in this path, stop here.
        if (visited.contains(type)) {
            return "Circular Reference to " + type.getSimpleName();
        }
        visited.add(type); // Mark this class as visited for the current path

        // 3. Recursive Step: It's a complex DTO/Entity, so inspect its fields.
        List<FieldInfo> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            // For each field, we make a recursive call to find its type.
            Object fieldType = parseSchemaRecursive(field.getType(), new HashSet<>(visited));
            fields.add(new FieldInfo(field.getName(), fieldType));
        }

        return new SchemaInfo(type.getSimpleName(), fields);
    }

    /**
     * Helper method to determine if a type is simple and should not be scanned.
     * This is the "gatekeeper" logic.
     */

    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(
            String.class,
            Integer.class, Long.class, Short.class,
            Double.class, Float.class, Byte.class,
            Boolean.class, Character.class,
            Void.class
    );


    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || SIMPLE_TYPES.contains(type)
                || type.getPackageName().startsWith("java.")
                || type.isEnum();
    }
}

