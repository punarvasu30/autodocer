package com.autodocer.AiDescription;

import com.autodocer.AiDescription.AiDescriptionService; // Ensure this import is correct
import com.autodocer.DTO.EndpointInfo;                  // Ensure this import is correct
import com.autodocer.DTO.ExampleInfo;                   // Ensure this import is correct
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/autodocer/ai") // Base path for AI-related endpoints
public class AiController {

    private final AiDescriptionService aiService;

    // Inject the existing AiDescriptionService (which now has both methods)
    public AiController(AiDescriptionService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(value = "/generate-examples", consumes = "application/json", produces = "application/json")
    public List<ExampleInfo> generateExamples(@RequestBody(required = false) EndpointInfo endpointInfo,@RequestParam String serverUrl) {
        if (endpointInfo == null || endpointInfo.path() == null || endpointInfo.httpMethod() == null) {
            System.err.println("--- [AutoDocER] Received invalid EndpointInfo for AI example generation ---");
            return List.of(new ExampleInfo("Error", "Invalid endpoint data received from UI. Cannot generate examples."));
        }

        System.out.println("--- [AutoDocER] Requesting AI examples for: "
                + endpointInfo.httpMethod() + " " + endpointInfo.path());

        return aiService.generateExamples(endpointInfo,serverUrl);
    }
}