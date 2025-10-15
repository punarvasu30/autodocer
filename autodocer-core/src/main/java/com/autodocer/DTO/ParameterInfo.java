package com.autodocer.DTO;

/**
 * Holds information about a single parameter of an endpoint.
 */
public record ParameterInfo(
        String name,
        Object type, // Changed from String to Object
        String sourceType,
        boolean isRequired
) {}

