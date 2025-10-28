package com.autodocer.DTO;

/**
 * A simple record to hold a generated test example (like a cURL command).
 * The AI service will be instructed to return data in this format.
 *
 * @param description A human-readable description (e.g., "Basic POST request").
 * @param command The full command-line example (e.g., a cURL command string).
 */
public record ExampleInfo(
        String description,
        String command
) {}