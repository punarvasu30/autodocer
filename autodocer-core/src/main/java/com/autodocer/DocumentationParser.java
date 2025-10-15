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

package com.autodocer;

import com.autodocer.DTO.ControllerInfo;
import com.autodocer.DTO.EndpointInfo;
import com.autodocer.DTO.ParameterInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            Class<?> controllerClass = controllerBean.getClass();
            String controllerName = controllerClass.getSimpleName();

            // 1. Get the base path from the class-level @RequestMapping
            String basePath = "";
            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
                basePath = controllerClass.getAnnotation(RequestMapping.class).value()[0];
            }

            List<EndpointInfo> endpointInfos = new ArrayList<>();
            for (Method method : controllerClass.getDeclaredMethods()) {
                // This is a helper method to extract all details from a method
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
    private java.util.Optional<EndpointInfo> parseMethod(Method method, String basePath) {
        String httpMethod = null;
        String path = "";

        // Check for each mapping annotation
        if (method.isAnnotationPresent(GetMapping.class)) {
            httpMethod = "GET";
            path = method.getAnnotation(GetMapping.class).value()[0];
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            httpMethod = "POST";
            path = method.getAnnotation(PostMapping.class).value().length > 0 ? method.getAnnotation(PostMapping.class).value()[0] : "";
        } // ... add else-if for PutMapping, DeleteMapping, etc.

        if (httpMethod == null) {
            return java.util.Optional.empty(); // Not an endpoint method
        }

        // 2. Correctly combine the base path and the method path
        String fullPath = (basePath + path).replaceAll("//", "/");

        // 3. Extract detailed parameter info
        List<ParameterInfo> parameterInfos = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            String sourceType = "Unknown";
            boolean isRequired = true; // Default

            if (parameter.isAnnotationPresent(RequestBody.class)) {
                sourceType = "RequestBody";
                isRequired = parameter.getAnnotation(RequestBody.class).required();
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                sourceType = "PathVariable";
                // PathVariables are always required
            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                sourceType = "RequestParam";
                isRequired = parameter.getAnnotation(RequestParam.class).required();
            }
            parameterInfos.add(new ParameterInfo(parameter.getName(), parameter.getType().getSimpleName(), sourceType, isRequired));
        }

        // 4. Get the response type
        String responseType = method.getReturnType().getSimpleName();

        EndpointInfo endpointInfo = new EndpointInfo(method.getName(), httpMethod, fullPath, parameterInfos, responseType);
        return java.util.Optional.of(endpointInfo);
    }
}

