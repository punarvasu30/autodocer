package com.autodocer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AutoDocerAutoConfiguration {

    @Bean
    public DocumentationParser documentationParser() {
        return new DocumentationParser();
    }

    @Bean
    public CommandLineRunner autoDocerRunner(ApplicationContext context, DocumentationParser parser) {
        return args -> {
            parser.parse(context);
        };
    }
}