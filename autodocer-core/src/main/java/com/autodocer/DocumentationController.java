package com.autodocer;
import com.autodocer.DTO.ControllerInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * This controller is automatically registered by the library and exposes
 * the endpoint that serves the generated OpenAPI documentation.
 */
@RestController
public class DocumentationController {

    private final ApplicationContext context;
    private final DocumentationParser parser;
    private final OpenApiGenerator generator;

    // The necessary components are injected via the constructor
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
        // 1. Parse the application to get the structured data
        List<ControllerInfo> apiInfo = parser.parse(context);

        // 2. Generate the OpenAPI JSON string from the data
        return generator.generate(apiInfo);
    }
}
