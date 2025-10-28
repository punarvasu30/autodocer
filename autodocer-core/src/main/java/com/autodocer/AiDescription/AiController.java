package com.autodocer.AiDescription;

import com.autodocer.AiDescription.AiDescriptionService; // Ensure this import is correct
import com.autodocer.DTO.EndpointInfo;                  // Ensure this import is correct
import com.autodocer.DTO.ExampleInfo;                   // Ensure this import is correct
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller to expose AI-powered features (like example generation) to the UI.
 * This acts as a secure proxy to the actual AI service.
 */
@RestController
@RequestMapping("/autodocer/ai") // Base path for AI-related endpoints
public class AiController {

    private final AiDescriptionService aiService;

    // Inject the existing AiDescriptionService (which now has both methods)
    public AiController(AiDescriptionService aiService) {
        this.aiService = aiService;
    }

    /**
     * Endpoint for the UI to request AI-generated usage examples (e.g., cURL commands).
     *
     * @param endpointInfo The details of the specific endpoint, sent by the UI.
     * @return A list of ExampleInfo objects containing descriptions and example commands.
     */
    @PostMapping(value = "/generate-examples", consumes = "application/json", produces = "application/json")
    public List<ExampleInfo> generateExamples(@RequestBody(required = false) EndpointInfo endpointInfo) {
        // Basic validation in case the UI sends bad data
        if (endpointInfo == null || endpointInfo.path() == null || endpointInfo.httpMethod() == null) {
            System.err.println("--- [AutoDocER] Received invalid EndpointInfo for AI example generation ---");
            // Return an error message formatted like an ExampleInfo
            return List.of(new ExampleInfo("Error", "Invalid endpoint data received from UI. Cannot generate examples."));
        }

        System.out.println("--- [AutoDocER] Requesting AI examples for: "
                + endpointInfo.httpMethod() + " " + endpointInfo.path());

        // Call the generateExamples method on the injected service
        return aiService.generateExamples(endpointInfo);
    }
}