package com.autodocer.DTO;

import java.util.List;

/**
 * Holds all the information about a single API endpoint (a method).
 */

public record EndpointInfo(
        String methodName,
        String httpMethod,
        String path,
        List<ParameterInfo> parameters,
        Object responseType // Changed from String to Object
) {}