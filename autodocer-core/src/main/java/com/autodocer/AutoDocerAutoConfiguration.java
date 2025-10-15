package com.autodocer;

import com.autodocer.DTO.ControllerInfo;
import com.autodocer.DTO.EndpointInfo;
import com.autodocer.DTO.ParameterInfo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import java.util.List;

@AutoConfiguration
public class AutoDocerAutoConfiguration {

    /**
     * Creates an instance of our DocumentationParser, making it available
     * for other beans in the application context.
     */
    @Bean
    public DocumentationParser documentationParser() {
        return new DocumentationParser();
    }

    /**
     * The main runner for the auto-documentation process.
     * This will be executed once the host application starts.
     * It uses the DocumentationParser to get the structured API data
     * and then prints a formatted summary to the console.
     */
    @Bean
    public CommandLineRunner autoDocerRunner(ApplicationContext context, DocumentationParser parser) {
        return args -> {
            // 1. Call the parser to get the structured list of controller information
            List<ControllerInfo> apiInfo = parser.parse(context);

            // 2. Format and print the structured output
            System.out.println("\n--- [AutoDocER] API Documentation Summary ---");

            if (apiInfo.isEmpty()) {
                System.out.println("No controllers to document.");
                return;
            }

            for (ControllerInfo controller : apiInfo) {
                System.out.println("\n[CONTROLLER] " + controller.className());
                System.out.println("  Base Path: " + (controller.basePath().isEmpty() ? "/" : controller.basePath()));

                if (controller.endpoints().isEmpty()) {
                    System.out.println("  No endpoints found.");
                    continue;
                }

                for (EndpointInfo endpoint : controller.endpoints()) {
                    System.out.println("\n  -> " + endpoint.httpMethod() + " " + endpoint.path());
                    System.out.println("     Method: " + endpoint.methodName());
                    System.out.println("     Returns: " + endpoint.responseType());

                    if (endpoint.parameters().isEmpty()) {
                        System.out.println("     Parameters: None");
                    } else {
                        System.out.println("     Parameters:");
                        for (ParameterInfo param : endpoint.parameters()) {
                            System.out.println("       - " + param.name() +
                                    " (" + param.type() + ")" +
                                    ", Source: " + param.sourceType() +
                                    ", Required: " + param.isRequired());
                        }
                    }
                }
            }
            System.out.println("\n--- [AutoDocER] Summary Generation Complete ---");
        };
    }
}
