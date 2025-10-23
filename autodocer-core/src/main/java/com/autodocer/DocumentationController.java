package com.autodocer;

// Import the necessary wrapper class
import com.autodocer.DocumentationParser.ApiDocumentationResult;

import com.autodocer.DTO.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is automatically registered by the library and exposes
 * the endpoint that serves the generated OpenAPI documentation.
 */
@RestController
public class DocumentationController {

    private final ApplicationContext context;
    private final DocumentationParser parser;
    private final OpenApiGenerator generator;

    public DocumentationController(ApplicationContext context, DocumentationParser parser, OpenApiGenerator generator) {
        this.context = context;
        this.parser = parser;
        this.generator = generator;
    }

    /**
     * The main endpoint for serving the OpenAPI 3.0 JSON specification.
     * @return A string containing the JSON documentation.
     */
    @GetMapping(value = "/autodocer/api-docs", produces = "application/json")
    public String getApiDocs() {
        // 1. Parse the application to get the combined result

        System.out.println("Hiiiiiiiiiiiiiiiiiiiiiiiiii");
        ApiDocumentationResult documentationResult = parser.parse(context);

        // 2. Generate the OpenAPI JSON string from the combined result
        return generator.generate(documentationResult);
    }
}