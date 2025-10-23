package com.autodocer.DTO; // Or com.autodocer.DTO

/**
 * Simple record to hold the extracted server information.
 */
public record ServerData(
        String url,
        String description
) {}