package com.autodocer;
import com.autodocer.DTO.FieldInfo;
import com.autodocer.DTO.SchemaInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A specialized parser responsible for recursively analyzing a class
 * and generating a structured schema for it.
 */
public class SchemaParser {

    /**
     * The main entry point for parsing a schema.
     * @param type The class to analyze.
     * @return A SchemaInfo object representing the class structure.
     */
    public SchemaInfo parseSchema(Class<?> type) {
        // We pass a new Set to track visited classes to prevent infinite loops.
        return parseSchemaRecursive(type, new HashSet<>());
    }

    /**
     * The recursive method that does the heavy lifting.
     */
    private SchemaInfo parseSchemaRecursive(Class<?> type, Set<Class<?>> visitedClasses) {
        // CRITICAL: Cycle detection to prevent infinite loops.
        // If we have already seen this class in this parsing path, stop here.
        if (visitedClasses.contains(type)) {
            return new SchemaInfo(type.getSimpleName() + " (Recursive reference)", new ArrayList<>());
        }

        // Add the current class to the set of visited classes for this path.
        visitedClasses.add(type);

        List<FieldInfo> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            Object fieldType;
            // Check if the field's type is a simple, standard Java class or another DTO.
            if (isSimpleType(field.getType())) {
                fieldType = field.getType().getSimpleName();
            } else {
                // It's a complex type, so we recurse.
                fieldType = parseSchemaRecursive(field.getType(), new HashSet<>(visitedClasses));
            }
            fields.add(new FieldInfo(field.getName(), fieldType));
        }

        return new SchemaInfo(type.getSimpleName(), fields);
    }

    /**
     * Helper method to determine if a type is simple (e.g., String, Long, int)
     * or complex (a DTO or Entity that needs further inspection).
     */
    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type.getPackageName().startsWith("java.");
    }
}
