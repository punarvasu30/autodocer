package com.autodocer.AiDescription;

import com.autodocer.DTO.AiGenerationResult;
import com.autodocer.DTO.EndpointContext; // Assuming this DTO/record exists
import java.lang.reflect.Method;

/**
 * A placeholder implementation of the AI service.
 * It does not call a real LLM, but instead generates simple, rule-based
 * descriptions based on the method name. This is for testing the integration.
 */
public class PlaceholderAiDescriptionService implements AiDescriptionService {

    @Override
    public AiGenerationResult generateDescription(EndpointContext context) {
        String methodName = context.methodName().toLowerCase();
        String summary = "A default summary for " + context.methodName();
        String description = "This endpoint " + context.methodName() + " operates on the path " + context.path();

        // Simple rule-based generation
        if (methodName.startsWith("get") || methodName.startsWith("find")) {
            if (methodName.contains("byid")) {
                summary = "Retrieves a specific resource by its ID.";
            } else if (methodName.contains("all")) {
                summary = "Retrieves a list of all resources.";
            } else {
                summary = "Retrieves a resource.";
            }
            description = summary + " This is a GET operation.";
        } else if (methodName.startsWith("create") || methodName.startsWith("add")) {
            summary = "Creates a new resource.";
            description = "Creates a new resource, typically using the provided request body. This is a POST operation.";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            summary = "Updates an existing resource.";
            description = "Updates an existing resource, often identified by an ID in the path. This is a PUT or PATCH operation.";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            summary = "Deletes a resource.";
            description = "Deletes an existing resource, typically identified by an ID in the path. This is a DELETE operation.";
        }

        return new AiGenerationResult(summary, description);
    }
}
