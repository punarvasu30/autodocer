package com.autodocer.DTO;

/**
 * Holds information about a single parameter of an endpoint.
 */
public record ParameterInfo(
        String name,
        String type,
        String sourceType, // e.g., "RequestBody", "PathVariable", "RequestParam"
        boolean isRequired
) {}

