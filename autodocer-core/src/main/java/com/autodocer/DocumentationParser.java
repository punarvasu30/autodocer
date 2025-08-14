package com.autodocer;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

public class DocumentationParser {

    public void parse(ApplicationContext context) {
        System.out.println("--- [AutoDocER] Starting Scan ---");

        // Find all beans that are REST controllers
        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);

        if (controllers.isEmpty()) {
            System.out.println("--- [AutoDocER] No @RestController beans found.");
            return;
        }

        System.out.println("--- [AutoDocER] Found " + controllers.size() + " controllers:");

        // Loop through each controller found and print its class name
        for (Object controllerBean : controllers.values()) {
            System.out.println("  -> Found Controller: " + controllerBean.getClass().getSimpleName());
        }

        System.out.println("--- [AutoDocER] Scan Complete ---");
    }
}