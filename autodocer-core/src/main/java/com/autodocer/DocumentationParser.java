package com.autodocer;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RestController;
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

            for (java.lang.reflect.Method method : controllerClass.getDeclaredMethods()) {

                if (method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class)) {
                    System.out.println("    - Found GET Endpoint: " + method.getName());
                }
                else if(method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)){
                    System.out.println("    - Found POST EndPoint: " + method.getName());
                }


            }
        }

        System.out.println("--- [AutoDocER] Scan Complete ---");
    }
}