package com.autodocer.DTO; // Or com.autodocer.DTO

import java.util.List;

public record EndpointInfo(
        String methodName,
        String httpMethod,
        String path,
        List<ParameterInfo> parameters,
        Object responseType,
        // ADDED: Fields for summary and description (can be null for now)
        String summary,
        String description
) {}

