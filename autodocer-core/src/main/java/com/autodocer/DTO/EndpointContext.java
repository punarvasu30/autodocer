package com.autodocer.DTO;

import java.util.List;

/**
 * A data record that holds all the context about a single endpoint,
 * used to provide information to the AiDescriptionService.
 */
public record EndpointContext(
        String methodName,
        String httpMethod,
        String path,
        List<String> parameters,
        String responseType
) {}

