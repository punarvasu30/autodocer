package com.autodocer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * The main auto-configuration class for the AutoDocER library.
 * It creates and registers all the necessary beans.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.autodocer")
public class AutoDocerAutoConfiguration {

    @Bean
    public DocumentationParser documentationParser() {
        return new DocumentationParser();
    }

    @Bean
    public OpenApiGenerator openApiGenerator() {
        return new OpenApiGenerator();
    }

//    // NEW: This bean creates and registers our documentation controller
//    // so its endpoints become active in the host application.
//    @Bean
//    public DocumentationController documentationController(ApplicationContext context, DocumentationParser parser, OpenApiGenerator generator) {
//        return new DocumentationController(context, parser, generator);
//    }
//
//    @Bean
//    public SwaggerController swaggerController(){
//        return new SwaggerController();
//    }

    // The CommandLineRunner has been removed to stop printing to the console on startup.
}


