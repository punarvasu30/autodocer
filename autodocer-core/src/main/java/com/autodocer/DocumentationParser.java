package com.autodocer;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

public class DocumentationParser {

    public void parse(ApplicationContext context) {
        System.out.println("--- [AutoDocER] Starting Scan ---");

        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);

        if (controllers.isEmpty()) {
            System.out.println("--- [AutoDocER] No @RestController beans found.");
            return;
        }

        System.out.println("--- [AutoDocER] Found " + controllers.size() + " controllers:");

        for (Object controllerBean : controllers.values()) {
            Class<?> controllerClass = controllerBean.getClass();
            System.out.println("  -> Scanning Controller: " + controllerClass.getSimpleName());

            // Inspect all methods declared in the controller class
            for (Method method : controllerClass.getDeclaredMethods()) {
                String endpointType = null;
                String path = "/";

                // Check for each mapping annotation
                if (method.isAnnotationPresent(GetMapping.class)) {
                    endpointType = "GET";
                    GetMapping annotation = method.getAnnotation(GetMapping.class);
                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
                } else if (method.isAnnotationPresent(PostMapping.class)) {
                    endpointType = "POST";
                    PostMapping annotation = method.getAnnotation(PostMapping.class);
                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
                } else if (method.isAnnotationPresent(PutMapping.class)) {
                    endpointType = "PUT";
                    PutMapping annotation = method.getAnnotation(PutMapping.class);
                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
                } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                    endpointType = "DELETE";
                    DeleteMapping annotation = method.getAnnotation(DeleteMapping.class);
                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
                } else if (method.isAnnotationPresent(PatchMapping.class)) {
                    endpointType = "PATCH";
                    PatchMapping annotation = method.getAnnotation(PatchMapping.class);
                    path = (annotation.value().length > 0) ? annotation.value()[0] : "/";
                }

                // If we found an endpoint, print its details
                if (endpointType != null) {
                    System.out.println("    - Found " + endpointType + " Endpoint: " + method.getName() + ", Path: " + path);

                    // Now, inspect its parameters for @RequestBody
                    for (Parameter parameter : method.getParameters()) {
                        if (parameter.isAnnotationPresent(RequestBody.class)) {
                            System.out.println("      -> Expects Request Body of type: " + parameter.getType().getSimpleName());
                        }
                    }
                }
            }
        }

        System.out.println("--- [AutoDocER] Scan Complete ---");
    }
}

