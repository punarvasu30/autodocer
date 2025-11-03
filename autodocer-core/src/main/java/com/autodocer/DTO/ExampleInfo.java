package com.autodocer.DTO;

/**
 * A simple record to hold a generated test example (like a cURL command).
 * The AI service will be instructed to return data in this format.
 */
public record ExampleInfo(
        String description,
        String command
) {}