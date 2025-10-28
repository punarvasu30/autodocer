package com.autodocer.AiDescription;

import com.autodocer.DTO.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<ExampleInfo> generateExamples(EndpointInfo endpoint) {
        try {
            // 1. Build the specific prompt for examples
            String prompt = buildExamplePrompt(endpoint); // New helper

            // 2. Build the standard Gemini request payload
            String requestBody = buildGeminiRequest(prompt); // Reuse your existing helper

            System.out.println("--- [AutoDocER] Calling AI to generate examples for: "
                    + endpoint.httpMethod() + " " + endpoint.path());

            // 3. Make the API call - Mirroring generateDescription's structure
            //    Using the SAME hardcoded model name 'gemini-2.5-flash'
            String jsonResponse = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("gemini-2.5-flash:generateContent") // Hardcoded model, matching generateDescription
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        System.err.println("--- [AutoDocER] API Error Response (Examples): " + errorBody); // Specify context
                                        // Return Mono error
                                        return Mono.error(new RuntimeException("API Error during example generation: " + clientResponse.statusCode() + " Body: " + errorBody));
                                    }))
                    .bodyToMono(String.class)
                    .block(); // Make call synchronous

            // 4. Parse the AI's response (expected to be a JSON array string)
            return parseExampleResponse(jsonResponse); // New helper

        } catch (Exception e) {
            System.err.println("--- [AutoDocER] ERROR calling AI for examples: " + e.getMessage());
            e.printStackTrace(); // Keep stack trace
            return Collections.singletonList( // Return list with one error item
                    new ExampleInfo("Error", "Could not generate examples via AI: " + e.getMessage())
            );
        }
    }

    // --- NEW: Helper for Building Example Prompt ---
    private String buildExamplePrompt(EndpointInfo endpoint) {
        String httpMethod = endpoint.httpMethod().toUpperCase();

        // Prepare context strings for the prompt
        String requestBodySchemaString = endpoint.parameters().stream()
                .filter(p -> "RequestBody".equals(p.sourceType()))
                .map(p -> formatSchemaForPrompt(p.type()))
                .findFirst()
                .orElse("None");

        // Separate path parameters and query parameters
        List<ParameterInfo> pathParams = endpoint.parameters().stream()
                .filter(p -> "PathVariable".equals(p.sourceType()))
                .collect(Collectors.toList());

        List<ParameterInfo> queryParams = endpoint.parameters().stream()
                .filter(p -> "RequestParam".equals(p.sourceType()))
                .collect(Collectors.toList());

        String pathParamsString = pathParams.stream()
                .map(p -> String.format("%s (PathVariable, %s, type: %s)",
                        p.name(),
                        p.isRequired() ? "required" : "optional",
                        p.type()))
                .collect(Collectors.joining("; "));
        if (pathParamsString.isEmpty()) pathParamsString = "None";

        String queryParamsString = queryParams.stream()
                .map(p -> String.format("%s (%s, type: %s)",
                        p.name(),
                        p.isRequired() ? "required" : "optional",
                        p.type()))
                .collect(Collectors.joining("; "));
        if (queryParamsString.isEmpty()) queryParamsString = "None";

        // Build the prompt based on HTTP method
        String prompt;

        if ("GET".equals(httpMethod) && !queryParams.isEmpty()) {
            // Special handling for GET with query parameters - request ALL combinations
            long requiredCount = queryParams.stream().filter(ParameterInfo::isRequired).count();
            long optionalCount = queryParams.size() - requiredCount;

            prompt = String.format(
                    "You are an expert API documentation assistant. Generate realistic cURL command examples for a GET endpoint.\n\n" +
                            "IMPORTANT: For GET endpoints with query parameters, generate ALL POSSIBLE COMBINATIONS:\n" +
                            "- If there are only optional parameters, generate examples for each individual parameter, pairs, triplets, and the complete set.\n" +
                            "- If there are required parameters, they MUST be present in EVERY example.\n" +
                            "- Generate examples showing different combinations to demonstrate filtering flexibility.\n" +
                            "- Use realistic sample values (e.g., names like 'John', 'Alice'; ages like 25, 30; cities like 'NewYork', 'London').\n\n" +
                            "For example, if the endpoint has 3 optional query params (name, age, city), generate:\n" +
                            "1. ?name=John\n" +
                            "2. ?age=25\n" +
                            "3. ?city=NewYork\n" +
                            "4. ?name=John&age=25\n" +
                            "5. ?name=John&city=NewYork\n" +
                            "6. ?age=25&city=NewYork\n" +
                            "7. ?name=John&age=25&city=NewYork\n\n" +
                            "Replace path variables (like {userId}) with realistic sample values (like 123).\n" +
                            "Assume the API runs at http://localhost:8080.\n\n" +
                            "Format your response as a JSON array with objects containing 'description' and 'command' fields.\n" +
                            "Example: [{\"description\": \"Filter by name only\", \"command\": \"curl -X GET 'http://localhost:8080/users?name=John'\"}]\n\n" +
                            "--- API Endpoint Details ---\n" +
                            "HTTP Method: %s\n" +
                            "Path: %s\n" +
                            "Path Parameters: %s\n" +
                            "Query Parameters: %s (%d required, %d optional)\n" +
                            "--- End Details ---\n\n" +
                            "Generate ALL meaningful parameter combinations!",
                    httpMethod,
                    endpoint.path(),
                    pathParamsString,
                    queryParamsString,
                    requiredCount,
                    optionalCount
            );
        } else if ("POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod)) {
            // For POST/PUT/PATCH - generate example with request body
            prompt = String.format(
                    "You are an expert API documentation assistant. Generate realistic cURL command examples for a %s endpoint.\n\n" +
                            "IMPORTANT: Generate a complete, realistic JSON request body based on the provided schema.\n" +
                            "- Include ALL fields (both required and optional) with realistic sample values.\n" +
                            "- Respect all constraints: minLength, maxLength, min, max, pattern, format, enum values.\n" +
                            "- Use realistic data:\n" +
                            "  * For 'name' fields: use real names like 'John Doe', 'Alice Smith'\n" +
                            "  * For 'email' fields: use valid emails like 'john.doe@example.com'\n" +
                            "  * For 'age' fields: use realistic ages like 25, 30, 35\n" +
                            "  * For 'phone' fields: use valid formats like '+1-555-123-4567'\n" +
                            "  * For date/datetime fields: use ISO 8601 format\n" +
                            "- Replace path variables (like {id}) with sample values (like 123).\n" +
                            "- Assume the API runs at http://localhost:8080.\n\n" +
                            "Format your response as a JSON array with objects containing 'description' and 'command' fields.\n" +
                            "The 'command' should be a complete cURL command with proper JSON escaping.\n" +
                            "Example: [{\"description\": \"Create a new user\", \"command\": \"curl -X POST 'http://localhost:8080/users' -H 'Content-Type: application/json' -d '{\\\"name\\\":\\\"John Doe\\\",\\\"email\\\":\\\"john@example.com\\\"}'\"}]\n\n" +
                            "--- API Endpoint Details ---\n" +
                            "HTTP Method: %s\n" +
                            "Path: %s\n" +
                            "Path Parameters: %s\n" +
                            "Request Body Schema: %s\n" +
                            "--- End Details ---",
                    httpMethod,
                    httpMethod,
                    endpoint.path(),
                    pathParamsString,
                    requestBodySchemaString
            );
        } else {
            // For DELETE and simple GET (no query params)
            prompt = String.format(
                    "You are an expert API documentation assistant. Generate a realistic cURL command example for a %s endpoint.\n\n" +
                            "- Replace path variables (like {userId}) with realistic sample values (like 123).\n" +
                            "- Assume the API runs at http://localhost:8080.\n" +
                            "- Keep it simple and straightforward.\n\n" +
                            "Format your response as a JSON array with one object containing 'description' and 'command' fields.\n" +
                            "Example: [{\"description\": \"Delete user by ID\", \"command\": \"curl -X DELETE 'http://localhost:8080/users/123'\"}]\n\n" +
                            "--- API Endpoint Details ---\n" +
                            "HTTP Method: %s\n" +
                            "Path: %s\n" +
                            "Path Parameters: %s\n" +
                            "--- End Details ---",
                    httpMethod,
                    httpMethod,
                    endpoint.path(),
                    pathParamsString
            );
        }

        return prompt;
    }

    // --- NEW: Helper for Parsing Example Response ---
    private List<ExampleInfo> parseExampleResponse(String jsonResponse) throws Exception {
        System.out.println("--- [AutoDocER] Gemini Example Response: " + jsonResponse); // Log raw response
        JsonNode root = objectMapper.readTree(jsonResponse);

        // Reuse the text extraction and cleanup logic from parseGeminiResponse
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty() || !candidates.isArray()) {
            // Check for promptFeedback if no candidates
            JsonNode promptFeedback = root.path("promptFeedback");
            if (!promptFeedback.isMissingNode()) {
                throw new Exception("AI request blocked or failed (Examples). Feedback: " + promptFeedback.toString());
            }
            throw new Exception("No candidates in AI example response");
        }
        String text = candidates.path(0).path("content").path("parts").path(0).path("text").asText();
        if (text.isEmpty()) { throw new Exception("AI example response text was empty"); }

        // Clean potential markdown ```json ... ``` blocks
        text = text.trim();
        if (text.startsWith("```json")) { text = text.substring(7); }
        if (text.startsWith("```")) { text = text.substring(3); }
        if (text.endsWith("```")) { text = text.substring(0, text.length() - 3); }
        text = text.trim();

        System.out.println("--- [AutoDocER] Extracted Example Text: " + text); // Log cleaned text

        // Attempt to parse the cleaned text directly as a List<ExampleInfo>
        try {
            return objectMapper.readValue(text, new TypeReference<List<ExampleInfo>>() {});
        } catch (Exception e) {
            System.err.println("--- [AutoDocER] Failed to parse AI example JSON array: " + text);
            e.printStackTrace();
            throw new Exception("AI returned examples in an invalid JSON format. Raw cleaned text: " + text, e);
        }
    }

    // --- NEW: Helper to format schema for prompt (with depth limiting) ---
    private String formatSchemaForPrompt(Object type) {
        if (type == null) return "null";
        try {
            if (type instanceof String s) return s;
            if (type instanceof SchemaInfo s) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("type", "object");
                node.put("className", s.className());
                if (s.requiredFields() != null && !s.requiredFields().isEmpty()) {
                    ArrayNode required = node.putArray("required");
                    s.requiredFields().forEach(required::add);
                }
                ObjectNode properties = node.putObject("properties");
                for (var field : s.fields()) {
                    properties.put(field.name(), formatSchemaForPromptInternal(field.type(), 1));
                    // Maybe add simple constraints here if useful for AI
                    // e.g., if(field.constraints() != null && field.constraints().format() != null) properties.putObject(field.name()).put("format", field.constraints().format());
                }
                return objectMapper.writer().writeValueAsString(node); // Use compact writer
            }
            if (type instanceof ArraySchemaInfo a) {
                return "List<" + formatSchemaForPromptInternal(a.itemType(), 1) + ">";
            }
        } catch (Exception e) {
            System.err.println("--- [AutoDocER] Error formatting schema for prompt: " + e.getMessage());
            return "Error formatting schema";
        }
        return type.getClass().getSimpleName();
    }

    private String formatSchemaForPromptInternal(Object type, int depth) {
        final int MAX_DEPTH = 2; // Limit recursion depth in prompt
        if (depth > MAX_DEPTH) return "...";
        if (type == null) return "null";
        if (type instanceof String s) return s;
        if (type instanceof SchemaInfo s) return s.className() + " object"; // Simplified
        if (type instanceof ArraySchemaInfo a) return "List<" + formatSchemaForPromptInternal(a.itemType(), depth + 1) + ">";
        return type.getClass().getSimpleName();
    }
}