//package com.autodocer;
//
//import com.autodocer.DTO.*;
//import org.springframework.context.ApplicationContext;
//import org.springframework.web.bind.annotation.*;
//import java.lang.reflect.Method;
//import java.lang.reflect.Parameter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//public class DocumentationParser {
//
//    private final SchemaParser schemaParser;
//
//    public DocumentationParser() {
//        this.schemaParser = new SchemaParser();
//    }
//
//    public List<ControllerInfo> parse(ApplicationContext context) {
//        System.out.println("--- [AutoDocER] Starting Enhanced Scan ---");
//        List<ControllerInfo> controllerInfos = new ArrayList<>();
//        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);
//
//        if (controllers.isEmpty()) {
//            System.out.println("--- [AutoDocER] No @RestController beans found.");
//            return controllerInfos;
//        }
//
//        System.out.println("--- [AutoDocER] Found " + controllers.size() + " controllers.");
//
//        for (Object controllerBean : controllers.values()) {
//            Class<?> controllerClass = org.springframework.aop.support.AopUtils.getTargetClass(controllerBean);
//            String controllerName = controllerClass.getSimpleName();
//
//            String basePath = "";
//            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
//                RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
//                if (requestMapping.value().length > 0) {
//                    basePath = requestMapping.value()[0];
//                }
//            }
//
//            List<EndpointInfo> endpointInfos = new ArrayList<>();
//            for (Method method : controllerClass.getDeclaredMethods()) {
//                parseMethod(method, basePath).ifPresent(endpointInfos::add);
//            }
//
//            controllerInfos.add(new ControllerInfo(controllerName, basePath, endpointInfos));
//        }
//
//        System.out.println("--- [AutoDocER] Scan Complete ---");
//        return controllerInfos;
//    }
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
//            return Optional.empty();
//        }
//
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
//            // THE FIX: Decide whether to do a deep scan or just get the name
//            if (isSimpleType(parameter.getType())) {
//                paramType = parameter.getType().getSimpleName();
//            } else {
//                paramType = schemaParser.parseSchema(parameter.getType());
//            }
//
//            String sourceType = "Unknown";
//            boolean isRequired = true;
//
//            if (parameter.isAnnotationPresent(RequestBody.class)) {
//                sourceType = "RequestBody";
//                isRequired = parameter.getAnnotation(RequestBody.class).required();
//            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
//                sourceType = "PathVariable";
//            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
//                sourceType = "RequestParam";
//                isRequired = parameter.getAnnotation(RequestParam.class).required();
//            }
//            parameterInfos.add(new ParameterInfo(parameter.getName(), paramType, sourceType, isRequired));
//        }
//
//        Object responseType;
//        // THE FIX: Decide whether to do a deep scan for the response type
//        if (isSimpleType(method.getReturnType())) {
//            responseType = method.getReturnType().getSimpleName();
//        } else {
//            responseType = schemaParser.parseSchema(method.getReturnType());
//        }
//
//        EndpointInfo endpointInfo = new EndpointInfo(method.getName(), httpMethod, fullPath, parameterInfos, responseType);
//        return Optional.of(endpointInfo);
//    }
//
//    /**
//     * Helper method to determine if a type is simple (and should not be scanned).
//     */
//    private boolean isSimpleType(Class<?> type) {
//        return type.isPrimitive()
//                || type.getPackageName().startsWith("java.")
//                || type.equals(Void.TYPE);
//    }
//}
//

package com.autodocer;

// Import necessary classes for annotations and Spring Boot App detection
import com.autodocer.annotations.ApiServers;
import com.autodocer.annotations.ServerInfo;
// Ensure your DTO/API package is imported correctly
import com.autodocer.DTO.*; // Assuming this is your DTO package
import org.springframework.beans.factory.config.BeanDefinition; // Import this
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory; // Import this
import org.springframework.boot.autoconfigure.SpringBootApplication; // Import this
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext; // Import this
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type; // Import Type
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
     * UPDATED: Helper method to find the @SpringBootApplication bean and read @ApiServers annotation,
     * handling potential proxies robustly by checking the bean definition.
     */
    private List<ServerData> extractServerInfo(ApplicationContext context) {
        List<ServerData> servers = new ArrayList<>();
        // Find bean names annotated with @SpringBootApplication
        String[] mainAppBeanNames = context.getBeanNamesForAnnotation(SpringBootApplication.class);

        if (mainAppBeanNames.length > 0) {
            String mainAppBeanName = mainAppBeanNames[0]; // Usually only one
            Class<?> mainAppClass = null;

            // Try to get the original class from the bean definition (more reliable)
            if (context instanceof ConfigurableApplicationContext configurableContext) {
                ConfigurableListableBeanFactory beanFactory = configurableContext.getBeanFactory();
                try {
                    BeanDefinition beanDefinition = beanFactory.getBeanDefinition(mainAppBeanName);
                    String originalClassName = beanDefinition.getBeanClassName();
                    if (originalClassName != null) {
                        // Use the application context's classloader
                        mainAppClass = Class.forName(originalClassName, true, context.getClassLoader());
                        System.out.println("--- [AutoDocER] Found original class from BeanDefinition: " + mainAppClass.getName());
                    }
                } catch (Exception e) {
                    System.err.println("--- [AutoDocER] Error getting original class from BeanDefinition: " + e.getMessage() + ". Falling back...");
                    mainAppClass = null; // Ensure fallback is triggered
                }
            }

            // Fallback: If getting from bean definition failed, try AopUtils on the instance
            if (mainAppClass == null) {
                try {
                    Object mainAppBean = context.getBean(mainAppBeanName);
                    mainAppClass = org.springframework.aop.support.AopUtils.getTargetClass(mainAppBean);
                    System.out.println("--- [AutoDocER] Found class using AopUtils fallback: " + (mainAppClass != null ? mainAppClass.getName() : "null"));
                } catch (Exception e) {
                    System.err.println("--- [AutoDocER] Error during AopUtils fallback for bean " + mainAppBeanName + ": " + e.getMessage());
                    mainAppClass = null;
                }
            }


            if (mainAppClass != null) {
                if (mainAppClass.isAnnotationPresent(ApiServers.class)) {
                    ApiServers apiServersAnnotation = mainAppClass.getAnnotation(ApiServers.class);
                    System.out.println("--- [AutoDocER] Found @ApiServers annotation on target class: " + mainAppClass.getSimpleName());
                    for (ServerInfo serverInfoAnnotation : apiServersAnnotation.value()) {
                        servers.add(new ServerData(serverInfoAnnotation.url(), serverInfoAnnotation.description()));
                        System.out.println("    -> Server Added: URL=" + serverInfoAnnotation.url() + ", Desc=" + serverInfoAnnotation.description());
                    }
                } else {
                    System.out.println("--- [AutoDocER] No @ApiServers annotation found on target class: " + mainAppClass.getSimpleName());
                    servers.add(new ServerData("/", "Default Server (Relative Path)"));
                }
            } else {
                System.out.println("--- [AutoDocER] Could not determine type for main application bean: " + mainAppBeanName);
                servers.add(new ServerData("/", "Default Server (Relative Path)"));
            }

        } else {
            System.out.println("--- [AutoDocER] Could not find bean annotated with @SpringBootApplication.");
            servers.add(new ServerData("/", "Default Server (Relative Path)"));
        }
        return servers;
    }


    private Optional<EndpointInfo> parseMethod(Method method, String basePath) {
        String httpMethod = null;
        String path = "";

        // Determine HTTP Method and Path from annotations
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

        // Combine and normalize the path
        String fullPath = (basePath + "/" + path).replaceAll("/+", "/");
        if (fullPath.length() > 1 && fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        if (fullPath.isEmpty()) {
            fullPath = "/";
        }

        // Parse Parameters
        List<ParameterInfo> parameterInfos = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            Object paramType;
            Type genericParamType = parameter.getParameterizedType();
            Class<?> rawParamType = parameter.getType();

            if (isSimpleType(rawParamType)) {
                paramType = rawParamType.getSimpleName();
            } else {
                paramType = schemaParser.parseSchema(genericParamType);
            }

            String sourceType = "Unknown";
            boolean isRequired = true; // Default assumption

            if (parameter.isAnnotationPresent(RequestBody.class)) {
                sourceType = "RequestBody";
                isRequired = parameter.getAnnotation(RequestBody.class).required();
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                sourceType = "PathVariable";
                // PathVariables are implicitly required in Spring unless Optional/required=false
            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                sourceType = "RequestParam";
                isRequired = parameter.getAnnotation(RequestParam.class).required();
            }
            // Ensure ParameterInfo constructor matches your record definition
            parameterInfos.add(new ParameterInfo(parameter.getName(), paramType, sourceType, isRequired));
        }

        // Parse Response Type
        Object responseType;
        Type genericReturnType = method.getGenericReturnType();
        Class<?> rawReturnType = method.getReturnType();

        if (isSimpleType(rawReturnType)) {
            responseType = rawReturnType.getSimpleName();
        } else {
            responseType = schemaParser.parseSchema(genericReturnType);
        }

        // Ensure EndpointInfo constructor matches your record definition
        // Pass null for summary/description for now
        EndpointInfo endpointInfo = new EndpointInfo(method.getName(), httpMethod, fullPath, parameterInfos, responseType, null, null);
        return Optional.of(endpointInfo);
    }

    /**
     * Helper method to determine if a type is simple (and should not be scanned deeply).
     * Includes check for standard Java library classes.
     */
    private boolean isSimpleType(Class<?> type) {
        return type == null
                || type.isPrimitive()
                || type.getPackageName().startsWith("java.lang") // Covers String, Long, Integer, Void, etc.
                || type.getPackageName().startsWith("java.math") // BigDecimal, BigInteger
                || type.getPackageName().startsWith("java.util") // List, Map, Set, Date, UUID etc. (SchemaParser handles generics inside)
                || type.getPackageName().startsWith("java.time") // LocalDate, LocalDateTime etc.
                || type.equals(Void.TYPE)
                || type.isEnum()
                || (type.isArray() && isSimpleType(type.getComponentType())); // Simple arrays
    }
}