package com.autodocer.AiDescription;

import com.autodocer.DTO.AiGenerationResult;
import com.autodocer.DTO.EndpointContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
// REMOVE: import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public class GeminiAiDescriptionService implements AiDescriptionService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey; // <-- This will be injected

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    // --- UPDATED CONSTRUCTOR ---
    public GeminiAiDescriptionService(String apiKey, WebClient.Builder webClientBuilder) {
        this.apiKey = apiKey;
        // Use the builder to create a WebClient instance
        this.webClient = webClientBuilder.baseUrl(GEMINI_API_URL).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AiGenerationResult generateDescription(EndpointContext context) {
        try {
            String prompt = buildPrompt(context);
            String requestBody = buildGeminiRequest(prompt);

            String jsonResponse = webClient.post()
                    .uri("?key=" + apiKey) // URI is now relative to the base URL
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(jsonResponse);

        } catch (Exception e) {
            System.err.println("--- [AutoDocER] ERROR calling AI service: " + e.getMessage());
            return new AiGenerationResult(
                    "Error generating summary",
                    "Could not generate AI description: " + e.getMessage()
            );
        }
    }

    // ... buildPrompt, buildGeminiRequest, and parseGeminiResponse methods
    // ... remain exactly the same ...

    private String buildPrompt(EndpointContext context) {
        // ... (no changes) ...
        return String.format(
                "You are an expert technical writer for API documentation. " +
                        "Your task is to generate a 'summary' and 'description' for a REST API endpoint. " +
                        "The 'summary' must be a single, concise sentence starting with a verb (e.g., 'Retrieves...', 'Creates...'). " +
                        "The 'description' should be a brief, human-readable paragraph (1-3 sentences) explaining what the endpoint does. " +
                        "Respond ONLY with a valid JSON object in the format: {\"summary\": \"...\", \"description\": \"...\"}\n\n" +
                        "--- Endpoint Details ---\n" +
                        "Method Name: %s\n" +
                        "HTTP Method: %s\n" +
                        "Path: %s\n" +
                        "Parameters: %s\n" +
                        "Response Type: %s\n" +
                        "--- End Details ---",
                context.methodName(),
                context.httpMethod(),
                context.path(),
                context.parameters().isEmpty() ? "none" : String.join(", ", context.parameters()),
                context.responseType()
        );
    }

    private String buildGeminiRequest(String prompt) {
        // ... (no changes) ...
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode parts = contents.addObject();
        ArrayNode partsArray = parts.putArray("parts");
        partsArray.addObject().put("text", prompt);

        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");

        return requestBody.toString();
    }

    private AiGenerationResult parseGeminiResponse(String jsonResponse) throws Exception {
        // ... (no changes) ...
        JsonNode root = objectMapper.readTree(jsonResponse);

        String text = root
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText();

        if (text.isEmpty()) {
            throw new Exception("AI response was empty or in an invalid format.");
        }

        JsonNode aiJson = objectMapper.readTree(text);
        String summary = aiJson.path("summary").asText("Failed to parse summary");
        String description = aiJson.path("description").asText("Failed to parse description");

        return new AiGenerationResult(summary, description);
    }
}