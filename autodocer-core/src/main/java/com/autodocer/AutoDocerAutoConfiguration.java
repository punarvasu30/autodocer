package com.autodocer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext; // Required import
import org.springframework.context.annotation.Bean;
// Removed ComponentScan import

/**
 * The main auto-configuration class for the AutoDocER library.
 * It explicitly creates and registers all the necessary beans.
 */
@AutoConfiguration
// REMOVED: @ComponentScan(basePackages = "com.autodocer.Controller")
public class AutoDocerAutoConfiguration {

    @Bean
    public DocumentationParser documentationParser() {
        System.out.println("--- [AutoDocER] Creating DocumentationParser bean ---"); // Added debug
        return new DocumentationParser();
    }

    @Bean
    public OpenApiGenerator openApiGenerator() {
        System.out.println("--- [AutoDocER] Creating OpenApiGenerator bean ---"); // Added debug
        return new OpenApiGenerator();
    }

    /**
     * THE FIX: Explicitly create the DocumentationController as a bean.
     * Spring will automatically provide the other beans this method needs
     * (context, parser, generator) because they are also defined here.
     */
    @Bean
    public DocumentationController documentationController(ApplicationContext context, DocumentationParser parser, OpenApiGenerator generator) {
        System.out.println("--- [AutoDocER] Creating DocumentationController bean ---"); // Added debug
        return new DocumentationController(context, parser, generator);
    }

    /**
     * THE FIX: Explicitly create the SwaggerController as a bean.
     * This ensures the /autodocer/ui endpoint is always registered.
     */
    @Bean
    public SwaggerController swaggerController() {
        System.out.println("--- [AutoDocER] Creating SwaggerController bean ---"); // Added debug
        return new SwaggerController();
    }
}