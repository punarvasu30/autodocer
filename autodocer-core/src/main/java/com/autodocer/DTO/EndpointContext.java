package com.autodocer.DTO;

import java.util.List;

/**
 * A data record that holds all the context about a single endpoint,
 * used to provide information to the AiDescriptionService.
 *
 * @param methodName The name of the Java method (e.g., "getUserById").
 * @param httpMethod The HTTP method (e.g., "GET").
 * @param path The full URL path (e.g., "/api/v1/users/{id}").
 * @param parameters A list of strings describing the parameters
 * (e.g., "PathVariable String", "RequestBody UserDto").
 * @param responseType A string describing the return type (e.g., "UserDto", "List<UserDto>").
 */
public record EndpointContext(
        String methodName,
        String httpMethod,
        String path,
        List<String> parameters,
        String responseType
) {}

