//package com.autodocer;
//
//// Import necessary classes for annotations and Spring Boot App detection
//import com.autodocer.annotations.ApiServers;
//import com.autodocer.annotations.ServerInfo;
//// Ensure your DTO/API package is imported correctly
//import com.autodocer.DTO.*; // Assuming this is your DTO package
//import org.springframework.boot.autoconfigure.SpringBootApplication; // Import this
//import org.springframework.context.ApplicationContext;
//import org.springframework.web.bind.annotation.*;
//import java.lang.reflect.Method;
//import java.lang.reflect.Parameter;
//import java.lang.reflect.Type; // Import Type
//import java.util.*;
//
//public class DocumentationParser {
//
//    private final SchemaParser schemaParser;
//
//    public DocumentationParser() {
//        this.schemaParser = new SchemaParser();
//    }
//
//    /**
//     * DEFINITION: Wrapper class to hold both server info and controller info.
//     */
//    public record ApiDocumentationResult(
//            List<ServerData> servers,
//            List<ControllerInfo> controllers
//    ) {}
//
//    /**
//     * UPDATED: Parses the application context to find controllers, endpoints, AND server info annotations.
//     * @param context The Spring ApplicationContext.
//     * @return An ApiDocumentationResult object containing all parsed information.
//     */
//    public ApiDocumentationResult parse(ApplicationContext context) {
//        System.out.println("--- [AutoDocER] Starting Full Scan (Controllers + Config) ---");
//
//        // --- NEW: Scan for Server Info ---
//        List<ServerData> serverInfos = extractServerInfo(context);
//
//        // --- Existing Controller Scanning Logic ---
//        List<ControllerInfo> controllerInfos = new ArrayList<>();
//        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);
//
//        if (controllers.isEmpty()) {
//            System.out.println("--- [AutoDocER] No @RestController beans found.");
//        } else {
//            System.out.println("--- [AutoDocER] Found " + controllers.size() + " controllers.");
//            for (Object controllerBean : controllers.values()) {
//                Class<?> controllerClass = org.springframework.aop.support.AopUtils.getTargetClass(controllerBean);
//                String controllerName = controllerClass.getSimpleName();
//
//                String basePath = "";
//                if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
//                    RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
//                    if (requestMapping.value().length > 0) {
//                        basePath = requestMapping.value()[0];
//                    }
//                }
//
//                List<EndpointInfo> endpointInfos = new ArrayList<>();
//                for (Method method : controllerClass.getDeclaredMethods()) {
//                    parseMethod(method, basePath).ifPresent(endpointInfos::add);
//                }
//
//                controllerInfos.add(new ControllerInfo(controllerName, basePath, endpointInfos));
//            }
//        }
//        System.out.println("--- [AutoDocER] Scan Complete ---");
//        // UPDATED: Return the combined result
//        return new ApiDocumentationResult(serverInfos, controllerInfos);
//    }
//
//    /**
//     * NEW & UPDATED: Helper method to find the @SpringBootApplication bean and read @ApiServers annotation,
//     * handling potential proxies correctly.
//     */
//    private List<ServerData> extractServerInfo(ApplicationContext context) {
//        List<ServerData> servers = new ArrayList<>();
//        Map<String, Object> mainAppBeans = context.getBeansWithAnnotation(SpringBootApplication.class);
//
//        if (!mainAppBeans.isEmpty()) {
//            // Get the name and instance of the first main application bean found
//            String mainAppBeanName = mainAppBeans.keySet().iterator().next();
//            Object mainAppBean = mainAppBeans.get(mainAppBeanName);
//
//            // Reliably get the original user-defined class, bypassing potential Spring proxies
//            Class<?> mainAppClass = org.springframework.aop.support.AopUtils.getTargetClass(mainAppBean);
//
//            if (mainAppClass != null) {
//                System.out.println("--- [AutoDocER] Found main application class: " + mainAppClass.getName()); // Debug log
//
//                if (mainAppClass.isAnnotationPresent(ApiServers.class)) {
//                    ApiServers apiServersAnnotation = mainAppClass.getAnnotation(ApiServers.class);
//                    System.out.println("--- [AutoDocER] Found @ApiServers annotation on " + mainAppClass.getSimpleName());
//                    for (ServerInfo serverInfoAnnotation : apiServersAnnotation.value()) {
//                        servers.add(new ServerData(serverInfoAnnotation.url(), serverInfoAnnotation.description()));
//                        System.out.println("    -> Server Added: URL=" + serverInfoAnnotation.url() + ", Desc=" + serverInfoAnnotation.description());
//                    }
//                } else {
//                    System.out.println("--- [AutoDocER] No @ApiServers annotation found on main application class: " + mainAppClass.getSimpleName());
//                    // Add a default server if none are explicitly defined
//                    servers.add(new ServerData("/", "Default Server (Relative Path)"));
//                }
//            } else {
//                System.out.println("--- [AutoDocER] Could not determine type for main application bean: " + mainAppBeanName);
//                servers.add(new ServerData("/", "Default Server (Relative Path)"));
//            }
//
//        } else {
//            System.out.println("--- [AutoDocER] Could not find @SpringBootApplication class to scan for @ApiServers.");
//            // Add a default server if the main class wasn't found
//            servers.add(new ServerData("/", "Default Server (Relative Path)"));
//        }
//        return servers;
//    }
//
//
//    private Optional<EndpointInfo> parseMethod(Method method, String basePath) {
//        String httpMethod = null;
//        String path = "";
//
//        if (method.isAnnotationPresent(GetMapping.class)) {
//            httpMethod = "GET";
//            GetMapping annotation = method.getAnnotation(GetMapping.class);
//            if (annotation.value().length > 0) path = annotation.value()[0];
//        } else if (method.isAnnotationPresent(PostMapping.class)) {
//            httpMethod = "POST";
//            PostMapping annotation = method.getAnnotation(PostMapping.class);
//            if (annotation.value().length > 0) path = annotation.value()[0];
//        } else if (method.isAnnotationPresent(PutMapping.class)) {
//            httpMethod = "PUT";
//            PutMapping annotation = method.getAnnotation(PutMapping.class);
//            if (annotation.value().length > 0) path = annotation.value()[0];
//        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
//            httpMethod = "DELETE";
//            DeleteMapping annotation = method.getAnnotation(DeleteMapping.class);
//            if (annotation.value().length > 0) path = annotation.value()[0];
//        } else if (method.isAnnotationPresent(PatchMapping.class)) {
//            httpMethod = "PATCH";
//            PatchMapping annotation = method.getAnnotation(PatchMapping.class);
//            if (annotation.value().length > 0) path = annotation.value()[0];
//        }
//
//        if (httpMethod == null) {
//            return Optional.empty(); // Not a web endpoint method
//        }
//
//        // Robustly combine the base path and the method path
//        String fullPath = (basePath + "/" + path).replaceAll("/+", "/");
//        if (fullPath.length() > 1 && fullPath.endsWith("/")) {
//            fullPath = fullPath.substring(0, fullPath.length() - 1);
//        }
//        if (fullPath.isEmpty()) {
//            fullPath = "/";
//        }
//
//
//        List<ParameterInfo> parameterInfos = new ArrayList<>();
//        for (Parameter parameter : method.getParameters()) {
//            Object paramType;
//            Type genericParamType = parameter.getParameterizedType(); // Use generic type
//            Class<?> rawParamType = parameter.getType(); // Get raw class
//
//            // Use isSimpleType on the raw class to decide
//            if (isSimpleType(rawParamType)) {
//                paramType = rawParamType.getSimpleName();
//            } else {
//                // Pass the potentially generic type to the schema parser
//                paramType = schemaParser.parseSchema(genericParamType);
//            }
//
//            String sourceType = "Unknown";
//            boolean isRequired = true; // Default assumption
//
//            if (parameter.isAnnotationPresent(RequestBody.class)) {
//                sourceType = "RequestBody";
//                isRequired = parameter.getAnnotation(RequestBody.class).required();
//            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
//                sourceType = "PathVariable";
//                // PathVariables are implicitly required
//            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
//                sourceType = "RequestParam";
//                isRequired = parameter.getAnnotation(RequestParam.class).required();
//                // We could also check defaultValue here later
//            }
//            // Ensure ParameterInfo constructor matches your record definition
//            // Assuming: ParameterInfo(String name, Object type, String sourceType, boolean isRequired)
//            parameterInfos.add(new ParameterInfo(parameter.getName(), paramType, sourceType, isRequired));
//        }
//
//        Object responseType;
//        Type genericReturnType = method.getGenericReturnType(); // Use generic type
//        Class<?> rawReturnType = method.getReturnType(); // Get raw class
//
//        // Use isSimpleType on the raw class to decide
//        if (isSimpleType(rawReturnType)) {
//            responseType = rawReturnType.getSimpleName();
//        } else {
//            // Pass the potentially generic type to the schema parser
//            responseType = schemaParser.parseSchema(genericReturnType);
//        }
//
//        // Ensure EndpointInfo constructor matches your record definition
//        // Assuming: EndpointInfo(String methodName, String httpMethod, String path, List<ParameterInfo> parameters, Object responseType, String summary, String description)
//        EndpointInfo endpointInfo = new EndpointInfo(method.getName(), httpMethod, fullPath, parameterInfos, responseType, null, null); // Pass nulls for summary/desc
//        return Optional.of(endpointInfo);
//    }
//
//    /**
//     * Helper method to determine if a type is simple (and should not be scanned deeply).
//     * Includes check for standard Java library classes.
//     */
//    private boolean isSimpleType(Class<?> type) {
//        // Check for primitives (int, boolean, etc.)
//        return type == null
//                || type.isPrimitive()
//                // Check common java.lang types (String, Long, Integer, Void)
//                || type.getPackageName().equals("java.lang")
//                // Check other common java.util types we want to treat as simple strings in OpenAPI
//                || type.getPackageName().equals("java.util") // e.g. List, Map - SchemaParser handles generics inside
//                || type.getPackageName().startsWith("java.time") // LocalDate, LocalDateTime etc.
//                || type.equals(Void.TYPE) // Special case for void primitive
//                || type.equals(Void.class)
//                || java.util.Date.class.isAssignableFrom(type)
//                || java.util.UUID.class.equals(type)
//                || java.math.BigDecimal.class.equals(type)
//                || java.math.BigInteger.class.equals(type)
//                || type.isEnum() // Consider enums simple
//                || (type.isArray() && isSimpleType(type.getComponentType())); // Simple arrays like String[]
//    }
//}


package com.autodocer;

// Import necessary classes for annotations and Spring Boot App detection
import com.autodocer.annotations.ApiServers;
import com.autodocer.annotations.ServerInfo;
// Ensure your DTO/API package is imported correctly
import com.autodocer.DTO.*; // Assuming this is your DTO package
import org.springframework.boot.autoconfigure.SpringBootApplication; // Import this
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType; // ADDED: For generic type inspection
import java.util.*;

public class DocumentationParser {

    private final SchemaParser schemaParser;

    public DocumentationParser() {
        this.schemaParser = new SchemaParser();
    }

    /**
     * DEFINITION: Wrapper class to hold both server info and controller info.
     */
    public record ApiDocumentationResult(
            List<ServerData> servers,
            List<ControllerInfo> controllers
    ) {}

    /**
     * UPDATED: Parses the application context to find controllers, endpoints, AND server info annotations.
     * @param context The Spring ApplicationContext.
     * @return An ApiDocumentationResult object containing all parsed information.
     */
    public ApiDocumentationResult parse(ApplicationContext context) {
        System.out.println("--- [AutoDocER] Starting Full Scan (Controllers + Config) ---");

        // --- NEW: Scan for Server Info ---
        List<ServerData> serverInfos = extractServerInfo(context);

        // --- Existing Controller Scanning Logic ---
        List<ControllerInfo> controllerInfos = new ArrayList<>();
        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);

        if (controllers.isEmpty()) {
            System.out.println("--- [AutoDocER] No @RestController beans found.");
        } else {
            System.out.println("--- [AutoDocER] Found " + controllers.size() + " controllers.");
            for (Object controllerBean : controllers.values()) {
                Class<?> controllerClass = org.springframework.aop.support.AopUtils.getTargetClass(controllerBean);
                String controllerName = controllerClass.getSimpleName();

                String basePath = "";
                if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
                    if (requestMapping.value().length > 0) {
                        basePath = requestMapping.value()[0];
                    }
                }

                List<EndpointInfo> endpointInfos = new ArrayList<>();
                for (Method method : controllerClass.getDeclaredMethods()) {
                    parseMethod(method, basePath).ifPresent(endpointInfos::add);
                }

                controllerInfos.add(new ControllerInfo(controllerName, basePath, endpointInfos));
            }
        }
        System.out.println("--- [AutoDocER] Scan Complete ---");
        // UPDATED: Return the combined result
        return new ApiDocumentationResult(serverInfos, controllerInfos);
    }

    /**
     * NEW & UPDATED: Helper method to find the @SpringBootApplication bean and read @ApiServers annotation,
     * handling potential proxies correctly.
     */
    private List<ServerData> extractServerInfo(ApplicationContext context) {
        List<ServerData> servers = new ArrayList<>();
        Map<String, Object> mainAppBeans = context.getBeansWithAnnotation(SpringBootApplication.class);

        if (!mainAppBeans.isEmpty()) {
            // Get the name and instance of the first main application bean found
            String mainAppBeanName = mainAppBeans.keySet().iterator().next();
            Object mainAppBean = mainAppBeans.get(mainAppBeanName);

            // Reliably get the original user-defined class, bypassing potential Spring proxies
            Class<?> mainAppClass = org.springframework.aop.support.AopUtils.getTargetClass(mainAppBean);

            if (mainAppClass != null) {
                System.out.println("--- [AutoDocER] Found main application class: " + mainAppClass.getName()); // Debug log

                if (mainAppClass.isAnnotationPresent(ApiServers.class)) {
                    ApiServers apiServersAnnotation = mainAppClass.getAnnotation(ApiServers.class);
                    System.out.println("--- [AutoDocER] Found @ApiServers annotation on " + mainAppClass.getSimpleName());
                    for (ServerInfo serverInfoAnnotation : apiServersAnnotation.value()) {
                        servers.add(new ServerData(serverInfoAnnotation.url(), serverInfoAnnotation.description()));
                        System.out.println("    -> Server Added: URL=" + serverInfoAnnotation.url() + ", Desc=" + serverInfoAnnotation.description());
                    }
                } else {
                    System.out.println("--- [AutoDocER] No @ApiServers annotation found on main application class: " + mainAppClass.getSimpleName());
                    // Add a default server if none are explicitly defined
                    servers.add(new ServerData("/", "Default Server (Relative Path)"));
                }
            } else {
                System.out.println("--- [AutoDocER] Could not determine type for main application bean: " + mainAppBeanName);
                servers.add(new ServerData("/", "Default Server (Relative Path)"));
            }

        } else {
            System.out.println("--- [AutoDocER] Could not find @SpringBootApplication class to scan for @ApiServers.");
            // Add a default server if the main class wasn't found
            servers.add(new ServerData("/", "Default Server (Relative Path)"));
        }
        return servers;
    }


    private Optional<EndpointInfo> parseMethod(Method method, String basePath) {
        String httpMethod = null;
        String path = "";

        if (method.isAnnotationPresent(GetMapping.class)) {
            httpMethod = "GET";
            GetMapping annotation = method.getAnnotation(GetMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            httpMethod = "POST";
            PostMapping annotation = method.getAnnotation(PostMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            httpMethod = "PUT";
            PutMapping annotation = method.getAnnotation(PutMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            httpMethod = "DELETE";
            DeleteMapping annotation = method.getAnnotation(DeleteMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            httpMethod = "PATCH";
            PatchMapping annotation = method.getAnnotation(PatchMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        }

        if (httpMethod == null) {
            return Optional.empty(); // Not a web endpoint method
        }

        // Robustly combine the base path and the method path
        String fullPath = (basePath + "/" + path).replaceAll("/+", "/");
        if (fullPath.length() > 1 && fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        if (fullPath.isEmpty()) {
            fullPath = "/";
        }


        List<ParameterInfo> parameterInfos = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            Object paramType;
            Type genericParamType = parameter.getParameterizedType(); // Use generic type
            Class<?> rawParamType = parameter.getType(); // Get raw class

            // UPDATED: Check if it's a collection type first
            paramType = parseTypeWithGenerics(genericParamType, rawParamType);

            String sourceType = "Unknown";
            boolean isRequired = true; // Default assumption

            if (parameter.isAnnotationPresent(RequestBody.class)) {
                sourceType = "RequestBody";
                isRequired = parameter.getAnnotation(RequestBody.class).required();
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                sourceType = "PathVariable";
                // PathVariables are implicitly required
            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                sourceType = "RequestParam";
                isRequired = parameter.getAnnotation(RequestParam.class).required();
                // We could also check defaultValue here later
            }

            parameterInfos.add(new ParameterInfo(parameter.getName(), paramType, sourceType, isRequired));
        }

        // UPDATED: Handle response type with generics
        Object responseType;
        Type genericReturnType = method.getGenericReturnType(); // Use generic type
        Class<?> rawReturnType = method.getReturnType(); // Get raw class

        responseType = parseTypeWithGenerics(genericReturnType, rawReturnType);

        EndpointInfo endpointInfo = new EndpointInfo(method.getName(), httpMethod, fullPath, parameterInfos, responseType, null, null);
        return Optional.of(endpointInfo);
    }

    /**
     * NEW METHOD: Parses a type, detecting if it's a generic collection (List, Set, etc.)
     * and extracting the element type if so.
     *
     * @param genericType The generic Type (may contain type parameters)
     * @param rawType The raw Class
     * @return Object representing the parsed type (String, SchemaInfo, or ArraySchemaInfo)
     */
    private Object parseTypeWithGenerics(Type genericType, Class<?> rawType) {
        // Check if it's a simple type first
        if (isSimpleType(rawType)) {
            return rawType.getSimpleName();
        }

        // UPDATED: Check if it's a parameterized type (e.g., List<UserDTO>)
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type rawTypeFromParam = parameterizedType.getRawType();

            // Check if the raw type is a Collection (List, Set, etc.)
            if (rawTypeFromParam instanceof Class<?>) {
                Class<?> rawClass = (Class<?>) rawTypeFromParam;

                // Handle Collection types: List, Set, Collection, etc.
                if (Collection.class.isAssignableFrom(rawClass)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        Type elementType = typeArguments[0];

                        // Recursively parse the element type
                        Object itemTypeSchema;
                        if (elementType instanceof Class<?>) {
                            Class<?> elementClass = (Class<?>) elementType;
                            if (isSimpleType(elementClass)) {
                                itemTypeSchema = elementClass.getSimpleName();
                            } else {
                                itemTypeSchema = schemaParser.parseSchema(elementType);
                            }
                        } else {
                            // Handle nested generics if needed
                            itemTypeSchema = schemaParser.parseSchema(elementType);
                        }

                        // Return ArraySchemaInfo with the proper element type
                        System.out.println("    -> Detected Collection type: " + rawClass.getSimpleName() +
                                " with element type: " + elementType);
                        return new ArraySchemaInfo(itemTypeSchema);
                    }
                }
            }
        }

        // If it's an array type (e.g., UserDTO[])
        if (rawType.isArray()) {
            Class<?> componentType = rawType.getComponentType();
            Object itemTypeSchema;
            if (isSimpleType(componentType)) {
                itemTypeSchema = componentType.getSimpleName();
            } else {
                itemTypeSchema = schemaParser.parseSchema(componentType);
            }
            System.out.println("    -> Detected Array type: " + rawType.getSimpleName());
            return new ArraySchemaInfo(itemTypeSchema);
        }

        // Otherwise, parse as a complex object
        return schemaParser.parseSchema(genericType);
    }

    /**
     * Helper method to determine if a type is simple (and should not be scanned deeply).
     * Includes check for standard Java library classes.
     */
    private boolean isSimpleType(Class<?> type) {
        // Check for primitives (int, boolean, etc.)
        return type == null
                || type.isPrimitive()
                // Check common java.lang types (String, Long, Integer, Void)
                || type.getPackageName().equals("java.lang")
                // UPDATED: Don't treat Collection interfaces as simple anymore
                // (we want to inspect their generic types)
                || type.getPackageName().startsWith("java.time") // LocalDate, LocalDateTime etc.
                || type.equals(Void.TYPE) // Special case for void primitive
                || type.equals(Void.class)
                || java.util.Date.class.isAssignableFrom(type)
                || java.util.UUID.class.equals(type)
                || java.math.BigDecimal.class.equals(type)
                || java.math.BigInteger.class.equals(type)
                || type.isEnum(); // Consider enums simple
    }
}