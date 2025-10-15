//package com.autodocer;
//
//import org.springframework.context.ApplicationContext;
//import org.springframework.web.bind.annotation.*;
//
//import java.lang.reflect.Method;
//import java.lang.reflect.Parameter;
//import java.util.Map;
//
//public class DocumentationParser {
//
//    public void parse(ApplicationContext context) {
//        System.out.println("--- [AutoDocER] Starting Scan ---");
//
//        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);
//
//        if (controllers.isEmpty()) {
//            System.out.println("--- [AutoDocER] No @RestController beans found.");
//            return;
//        }
//
//        System.out.println("--- [AutoDocER] Found " + controllers.size() + " controllers:");
//
//        for (Object controllerBean : controllers.values()) {
//            Class<?> controllerClass = controllerBean.getClass();
//            System.out.println("  -> Scanning Controller: " + controllerClass.getSimpleName());
//
//            // Inspect all methods declared in the controller class
//            for (Method method : controllerClass.getDeclaredMethods()) {
//                String endpointType = null;
//                String path = "/";
//
//                // Check for each mapping annotation
//                if (method.isAnnotationPresent(GetMapping.class)) {
//                    endpointType = "GET";
//                    GetMapping annotation = method.getAnnotation(GetMapping.class);
//                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
//                } else if (method.isAnnotationPresent(PostMapping.class)) {
//                    endpointType = "POST";
//                    PostMapping annotation = method.getAnnotation(PostMapping.class);
//                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
//                } else if (method.isAnnotationPresent(PutMapping.class)) {
//                    endpointType = "PUT";
//                    PutMapping annotation = method.getAnnotation(PutMapping.class);
//                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
//                } else if (method.isAnnotationPresent(DeleteMapping.class)) {
//                    endpointType = "DELETE";
//                    DeleteMapping annotation = method.getAnnotation(DeleteMapping.class);
//                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
//                } else if (method.isAnnotationPresent(PatchMapping.class)) {
//                    endpointType = "PATCH";
//                    PatchMapping annotation = method.getAnnotation(PatchMapping.class);
//                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
//                }
//
//                // If we found an endpoint, print its details
//                if (endpointType != null) {
//                    System.out.println("    - Found " + endpointType + " Endpoint: " + method.getName() + ", Path: " + path);
//
//                    // Now, inspect its parameters for @RequestBody
//                    for (Parameter parameter : method.getParameters()) {
//                        if (parameter.isAnnotationPresent(RequestBody.class)) {
//                            System.out.println("      -> Expects Request Body of type: " + parameter.getType().getSimpleName());
//                        }
//                    }
//                }
//            }
//        }
//
//        System.out.println("--- [AutoDocER] Scan Complete ---");
//    }
//}
//

//package com.autodocer;
//
//import com.autodocer.DTO.ControllerInfo;
//import com.autodocer.DTO.EndpointInfo;
//import com.autodocer.DTO.ParameterInfo;
//import org.springframework.context.ApplicationContext;
//import org.springframework.web.bind.annotation.*;
//import java.lang.reflect.Method;
//import java.lang.reflect.Parameter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class DocumentationParser {
//
//    /**
//     * Parses the application context to find all REST controllers and their endpoints.
//     * @param context The Spring ApplicationContext.
//     * @return A list of ControllerInfo objects containing the structured API documentation.
//     */
//    public List<ControllerInfo> parse(ApplicationContext context) {
//        System.out.println("--- [AutoDocER] Starting Enhanced Scan ---");
//        List<ControllerInfo> controllerInfos = new ArrayList<>();
//
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
//            Class<?> controllerClass = controllerBean.getClass();
//            String controllerName = controllerClass.getSimpleName();
//
//            // 1. Get the base path from the class-level @RequestMapping
//            String basePath = "";
//            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
//                basePath = controllerClass.getAnnotation(RequestMapping.class).value()[0];
//            }
//
//            List<EndpointInfo> endpointInfos = new ArrayList<>();
//            for (Method method : controllerClass.getDeclaredMethods()) {
//                // This is a helper method to extract all details from a method
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
//    /**
//     * Helper method to parse a single method for endpoint details.
//     */
//    private java.util.Optional<EndpointInfo> parseMethod(Method method, String basePath) {
//        String httpMethod = null;
//        String path = "";
//
//        // Check for each mapping annotation
//        if (method.isAnnotationPresent(GetMapping.class)) {
//            httpMethod = "GET";
//            path = method.getAnnotation(GetMapping.class).value()[0];
//        } else if (method.isAnnotationPresent(PostMapping.class)) {
//            httpMethod = "POST";
//            path = method.getAnnotation(PostMapping.class).value().length > 0 ? method.getAnnotation(PostMapping.class).value()[0] : "";
//        } // ... add else-if for PutMapping, DeleteMapping, etc.
//
//        if (httpMethod == null) {
//            return java.util.Optional.empty(); // Not an endpoint method
//        }
//
//        // 2. Correctly combine the base path and the method path
//        String fullPath = (basePath + path).replaceAll("//", "/");
//
//        // 3. Extract detailed parameter info
//        List<ParameterInfo> parameterInfos = new ArrayList<>();
//        for (Parameter parameter : method.getParameters()) {
//            String sourceType = "Unknown";
//            boolean isRequired = true; // Default
//
//            if (parameter.isAnnotationPresent(RequestBody.class)) {
//                sourceType = "RequestBody";
//                isRequired = parameter.getAnnotation(RequestBody.class).required();
//            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
//                sourceType = "PathVariable";
//                // PathVariables are always required
//            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
//                sourceType = "RequestParam";
//                isRequired = parameter.getAnnotation(RequestParam.class).required();
//            }
//            parameterInfos.add(new ParameterInfo(parameter.getName(), parameter.getType().getSimpleName(), sourceType, isRequired));
//        }
//
//        // 4. Get the response type
//        String responseType = method.getReturnType().getSimpleName();
//
//        EndpointInfo endpointInfo = new EndpointInfo(method.getName(), httpMethod, fullPath, parameterInfos, responseType);
//        return java.util.Optional.of(endpointInfo);
//    }
//}
//




package com.autodocer;

import com.autodocer.DTO.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DocumentationParser {

    /**
     * Parses the application context to find all REST controllers and their endpoints.
     * @param context The Spring ApplicationContext.
     * @return A list of ControllerInfo objects containing the structured API documentation.
     */
    public List<ControllerInfo> parse(ApplicationContext context) {
        System.out.println("--- [AutoDocER] Starting Enhanced Scan ---");
        List<ControllerInfo> controllerInfos = new ArrayList<>();

        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);

        if (controllers.isEmpty()) {
            System.out.println("--- [AutoDocER] No @RestController beans found.");
            return controllerInfos;
        }

        System.out.println("--- [AutoDocER] Found " + controllers.size() + " controllers.");

        for (Object controllerBean : controllers.values()) {
            // IMPROVEMENT: Use AopUtils to get the real class behind a potential Spring proxy
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

        System.out.println("--- [AutoDocER] Scan Complete ---");
        return controllerInfos;
    }

    /**
     * Helper method to parse a single method for endpoint details.
     */
    private Optional<EndpointInfo> parseMethod(Method method, String basePath) {
        String httpMethod = null;
        String path = "";

        // BUG FIX: Added safety checks to handle annotations with no value.
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
            return Optional.empty(); // Not an endpoint method
        }

        // IMPROVEMENT: Robustly combine the base path and the method path.
        String fullPath = (basePath + "/" + path).replaceAll("/+", "/");
        if (fullPath.length() > 1 && fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        if (fullPath.isEmpty()) {
            fullPath = "/";
        }


        List<ParameterInfo> parameterInfos = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            String sourceType = "Unknown";
            boolean isRequired = true;
            Object paramType; // Can now hold a String or a SchemaInfo

            if (parameter.isAnnotationPresent(RequestBody.class)) {
                sourceType = "RequestBody";
                isRequired = parameter.getAnnotation(RequestBody.class).required();
                // Recursively get the schema for the request body DTO
                paramType = getSchemaForType(parameter.getType());
            } else {
                // For simple parameters, just get the name
                paramType = parameter.getType().getSimpleName();
                if (parameter.isAnnotationPresent(PathVariable.class)) {
                    sourceType = "PathVariable";
                } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                    sourceType = "RequestParam";
                    isRequired = parameter.getAnnotation(RequestParam.class).required();
                }
            }
            parameterInfos.add(new ParameterInfo(parameter.getName(), paramType, sourceType, isRequired));
        }

        // NEW: Get the schema for the response type
        Object responseType = getSchemaForType(method.getReturnType());
        EndpointInfo endpointInfo = new EndpointInfo(method.getName(), httpMethod, fullPath, parameterInfos, responseType);
        return Optional.of(endpointInfo);
    }

    /**
     * Recursively inspects a class to get its schema.
     * If it's a simple type, it returns its name.
     * If it's a complex DTO/Entity, it returns a SchemaInfo object with its fields.
     */
    private Object getSchemaForType(Class<?> type) {
        // Base case: If it's a primitive, standard Java class, or void, just return the name.
        if (type.isPrimitive() || type.getPackageName().startsWith("java.") || type.equals(Void.TYPE)) {
            return type.getSimpleName();
        }

        // Recursive step: It's a DTO/Entity, so inspect its fields.
        List<FieldInfo> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            // For simplicity, we only go one level deep for field types.
            fields.add(new FieldInfo(field.getName(), field.getType().getSimpleName()));
        }
        return new SchemaInfo(type.getSimpleName(), fields);
    }
}

