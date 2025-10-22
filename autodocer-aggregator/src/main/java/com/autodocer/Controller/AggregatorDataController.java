package com.autodocer.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller that provides the aggregated OpenAPI definitions.
 */
@RestController
public class AggregatorDataController {

    private static final Logger log = LoggerFactory.getLogger(AggregatorDataController.class);
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    // DiscoveryClient and RestTemplate are injected by Spring via the AutoConfiguration
    public AggregatorDataController(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
    }

    /**
     * Endpoint to get a map of service names to their OpenAPI JSON specs.
     * @return A Map where keys are service names (e.g., "user-service") and
     * values are the OpenAPI JSON strings for that service.
     */
    @GetMapping("/autodocer-aggregator/definitions")
    public Map<String, String> getAggregatedDefinitions() {
        log.info("Fetching aggregated API definitions...");
        Map<String, String> definitions = new HashMap<>();

        // 1. Get all registered service IDs from Eureka
        List<String> serviceIds = discoveryClient.getServices();
        log.info("Found services: {}", serviceIds);

        for (String serviceId : serviceIds) {
            // Exclude the Eureka server itself and potentially the aggregator
            if ("eureka-server".equalsIgnoreCase(serviceId) || "autodocer-aggregator".equalsIgnoreCase(serviceId)) {
                continue;
            }

            // 2. Get the instances for this service
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances.isEmpty()) {
                log.warn("No instances found for service: {}", serviceId);
                continue;
            }

            // 3. Just use the first instance found (simplification for now)
            ServiceInstance instance = instances.get(0);
            String serviceUrl = instance.getUri().toString();
            String docsUrl = serviceUrl + "/autodocer/api-docs"; // Assuming the path from autodocer-core

            try {
                // 4. Fetch the OpenAPI JSON from the service
                log.info("Fetching docs for {} from {}", serviceId, docsUrl);
                String openApiJson = restTemplate.getForObject(docsUrl, String.class);
                if (openApiJson != null && !openApiJson.isBlank()) {
                    definitions.put(serviceId, openApiJson);
                    log.info("Successfully fetched docs for {}", serviceId);
                } else {
                    log.warn("Received empty response for docs from {}", serviceId);
                }
            } catch (Exception e) {
                log.error("Error fetching docs for service {}: {}", serviceId, e.getMessage());
                // Optionally put an error message in the map
                // definitions.put(serviceId, "{\"error\": \"Could not fetch docs: " + e.getMessage() + "\"}");
            }
        }

        log.info("Finished fetching definitions. Found docs for {} services.", definitions.size());
        return definitions;
    }
}
