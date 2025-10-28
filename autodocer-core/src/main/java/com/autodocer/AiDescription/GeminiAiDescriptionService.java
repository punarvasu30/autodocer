package com.autodocer.AiDescription;

import com.autodocer.DTO.AiGenerationResult;
import com.autodocer.DTO.EndpointContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public class GeminiAiDescriptionService implements AiDescriptionService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    // Base URL without the key parameter (use v1, not v1beta)
    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1/models/";

    public GeminiAiDescriptionService(String apiKey, WebClient.Builder webClientBuilder) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API key cannot be null or empty");
        }

        System.out.println("--- [AutoDocER] Initializing Gemini with API key: " +
                apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

        this.apiKey = apiKey;
        this.webClient = webClientBuilder.baseUrl(GEMINI_BASE_URL).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AiGenerationResult generateDescription(EndpointContext context) {
        try {
            String prompt = buildPrompt(context);
            String requestBody = buildGeminiRequest(prompt);

            // Corrected URI construction with gemini-2.5-flash
            String jsonResponse = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("gemini-2.5-flash:generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        System.err.println("--- [AutoDocER] API Error Response: " + errorBody);
                                        return clientResponse.createException();
                                    }))
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(jsonResponse);

        } catch (Exception e) {
            System.err.println("--- [AutoDocER] ERROR calling AI service: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for debugging
            return new AiGenerationResult(
                    "Error generating summary",
                    "Could not generate AI description: " + e.getMessage()
            );
        }
    }

    private String buildPrompt(EndpointContext context) {
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
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode contentItem = contents.addObject();
        ArrayNode partsArray = contentItem.putArray("parts");
        partsArray.addObject().put("text", prompt);

        // Add generation config for JSON output
        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("temperature", 0.2);
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);

        try {
            String jsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
            System.out.println("--- [AutoDocER] Gemini Request Body: " + jsonRequest);
            return jsonRequest;
        } catch (Exception e) {
            return requestBody.toString();
        }
    }

    private AiGenerationResult parseGeminiResponse(String jsonResponse) throws Exception {
        System.out.println("--- [AutoDocER] Gemini Response: " + jsonResponse);

        JsonNode root = objectMapper.readTree(jsonResponse);

        // Navigate to the text content
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty() || !candidates.isArray()) {
            throw new Exception("No candidates in AI response");
        }

        String text = candidates.path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();

        if (text.isEmpty()) {
            throw new Exception("AI response text was empty");
        }

        // Clean up the text (remove markdown code blocks if present)
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        }
        if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        System.out.println("--- [AutoDocER] Extracted text: " + text);

        // Parse the JSON from the text
        JsonNode aiJson = objectMapper.readTree(text);
        String summary = aiJson.path("summary").asText("");
        String description = aiJson.path("description").asText("");

        if (summary.isEmpty() || description.isEmpty()) {
            throw new Exception("Failed to extract summary or description from AI response");
        }

        return new AiGenerationResult(summary, description);
    }
}