//package com.autodocer;
//
//import com.autodocer.DTO.FieldInfo;
//import com.autodocer.DTO.SchemaInfo;
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
///**
// * A specialist class responsible for recursively parsing a Java Class
// * to build a structured SchemaInfo object. It handles nested DTOs
// * and prevents infinite loops from circular dependencies.
// */
//public class SchemaParser {
//
//    /**
//     * The main entry point for parsing a schema.
//     * @param type The class to parse.
//     * @return An Object which is either a simple String (for basic types) or a SchemaInfo object.
//     */
//    public Object parseSchema(Class<?> type) {
//        // We pass a new Set to track visited classes for this top-level call.
//        return parseSchemaRecursive(type, new HashSet<>());
//    }
//
//    /**
//     * The recursive helper method that does the actual parsing.
//     * @param type The class to parse.
//     * @param visited A set of classes already seen in the current parsing path to prevent infinite loops.
//     * @return An Object which is either a simple String or a SchemaInfo object.
//     */
//    private Object parseSchemaRecursive(Class<?> type, Set<Class<?>> visited) {
//        // 1. THE GATEKEEPER: If it's a simple type, stop immediately and return its name.
//        if (isSimpleType(type)) {
//            return type.getSimpleName();
//        }
//
//        // 2. Infinite Loop Prevention: If we have already seen this class in this path, stop here.
//        if (visited.contains(type)) {
//            return "Circular Reference to " + type.getSimpleName();
//        }
//        visited.add(type); // Mark this class as visited for the current path
//
//        // 3. Recursive Step: It's a complex DTO/Entity, so inspect its fields.
//        List<FieldInfo> fields = new ArrayList<>();
//        for (Field field : type.getDeclaredFields()) {
//            // For each field, we make a recursive call to find its type.
//            Object fieldType = parseSchemaRecursive(field.getType(), new HashSet<>(visited));
//            fields.add(new FieldInfo(field.getName(), fieldType));
//        }
//
//        return new SchemaInfo(type.getSimpleName(), fields);
//    }
//
//    /**
//     * Helper method to determine if a type is simple and should not be scanned.
//     * This is the "gatekeeper" logic.
//     */
//
//    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(
//            String.class,
//            Integer.class, Long.class, Short.class,
//            Double.class, Float.class, Byte.class,
//            Boolean.class, Character.class,
//            Void.class
//    );
//
//
//    private boolean isSimpleType(Class<?> type) {
//        return type.isPrimitive()
//                || SIMPLE_TYPES.contains(type)
//                || type.getPackageName().startsWith("java.")
//                || type.isEnum();
//    }
//}
//

package com.autodocer;

// Ensure correct import for your DTOs/API package
import com.autodocer.DTO.ArraySchemaInfo; // Or com.autodocer.DTO.*;
import com.autodocer.DTO.FieldInfo;
import com.autodocer.DTO.SchemaInfo;
import java.lang.reflect.*; // Import reflection types
import java.util.*;

public class SchemaParser {

    /**
     * UPDATED: Main entry point. Accepts Type to handle generics.
     */
    public Object parseSchema(Type type) {
        return parseSchemaRecursive(type, new HashSet<>());
    }

    /**
     * UPDATED: Recursive helper. Accepts Type.
     */
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

            // Recursive Step: It's a complex DTO/Entity, so inspect its fields.
            List<FieldInfo> fields = new ArrayList<>();
            // Use getDeclaredFields to get only fields directly declared in this class
            for (Field field : clazz.getDeclaredFields()) {
                // Important: Use field.getGenericType() for recursive call to handle nested generics
                Object fieldType = parseSchemaRecursive(field.getGenericType(), new HashSet<>(visited));
                // Assuming FieldInfo exists and takes (String name, Object type)
                fields.add(new FieldInfo(field.getName(), fieldType));
            }
            // Assuming SchemaInfo exists and takes (String className, List<FieldInfo> fields)
            return new SchemaInfo(clazz.getSimpleName(), fields);
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
            java.util.Date.class, java.util.UUID.class
            // Add other standard types you want to treat as simple
    );

    private boolean isSimpleType(Class<?> type) {
        return type == null
                || type.isPrimitive()
                || type.equals(Void.TYPE)
                || SIMPLE_TYPES.contains(type)
                || type.isEnum()
                || type.getPackageName().startsWith("java.time") // Treat all java.time as simple strings for now
                // Adjust this check if you want deeper inspection of standard Java collections
                || (type.isArray() && isSimpleType(type.getComponentType()));
    }
}


