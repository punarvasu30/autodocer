package com.autodocer;

import com.autodocer.DTO.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class AutoDocerAutoConfiguration {

    @Bean
    public DocumentationParser documentationParser() {
        return new DocumentationParser();
    }

    @Bean
    public CommandLineRunner autoDocerRunner(ApplicationContext context, DocumentationParser parser) {
        return args -> {
            List<ControllerInfo> apiInfo = parser.parse(context);

            System.out.println("\n--- [AutoDocER] API Documentation Summary ---");
            if (apiInfo.isEmpty()) {
                System.out.println("No controllers to document.");
                return;
            }

            for (ControllerInfo controller : apiInfo) {
                System.out.println("\n[CONTROLLER] " + controller.className());
                System.out.println("  Base Path: " + (controller.basePath().isEmpty() ? "/" : controller.basePath()));

                for (EndpointInfo endpoint : controller.endpoints()) {
                    System.out.println("\n  -> " + endpoint.httpMethod() + " " + endpoint.path());
                    System.out.println("     Method: " + endpoint.methodName());
                    System.out.print("     Returns: ");
                    // Initial call to the recursive print method
                    printType(endpoint.responseType(), "     ", "       ");

                    if (endpoint.parameters().isEmpty()) {
                        System.out.println("     Parameters: None");
                    } else {
                        System.out.println("     Parameters:");
                        for (ParameterInfo param : endpoint.parameters()) {
                            System.out.print("       - " + param.name() +
                                    ", Source: " + param.sourceType() +
                                    ", Required: " + param.isRequired() +
                                    ", Type: ");
                            // Initial call for parameters
                            printType(param.type(), "       ", "         ");
                        }
                    }
                }
            }
            System.out.println("\n--- [AutoDocER] Summary Generation Complete ---");
        };
    }

    /**
     * Recursively prints a type. It handles simple Strings and complex, nested
     * SchemaInfo objects with proper indentation.
     * @param type The object to print (String or SchemaInfo).
     * @param baseIndent The indentation for the current schema level.
     * @param fieldIndent The indentation for the fields of the current schema.
     */
    private void printType(Object type, String baseIndent, String fieldIndent) {
        if (type instanceof SchemaInfo schema) {
            System.out.println(schema.className() + " (Schema):");
            if (schema.fields().isEmpty()) {
                System.out.println(fieldIndent + "(No fields found)");
            } else {
                for (FieldInfo field : schema.fields()) {
                    System.out.print(fieldIndent + "- " + field.name() + " (");
                    // RECURSIVE CALL: If the field is a complex type, print its schema.
                    if (field.type() instanceof SchemaInfo nestedSchema) {
                        // Print the type and recurse with increased indentation
                        System.out.println("Schema):");
                        printType(nestedSchema, fieldIndent + "  ", fieldIndent + "    ");
                    } else {
                        // It's a simple type (String), so just print it and close the line.
                        System.out.println(field.type() + ")");
                    }
                }
            }
        } else {
            // It's a simple type (String), so just print it.
            System.out.println(type);
        }
    }
}

