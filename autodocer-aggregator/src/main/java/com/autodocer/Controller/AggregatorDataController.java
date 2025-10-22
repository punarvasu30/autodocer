package com.autodocer.Controller; // Make sure package is correct

import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.registry.InstanceRegistry; // Import InstanceRegistry
import com.netflix.appinfo.InstanceInfo; // Import InstanceInfo
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired; // Autowire

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AggregatorDataController {

    private static final Logger log = LoggerFactory.getLogger(AggregatorDataController.class);

    // THE FIX: Inject Eureka's internal registry directly
    @Autowired
    private InstanceRegistry instanceRegistry; // Use InstanceRegistry interface

    private final RestTemplate restTemplate;

    // RestTemplate is injected by Spring via the AutoConfiguration
    public AggregatorDataController(RestTemplate restTemplate) {
        // We no longer need DiscoveryClient here
        this.restTemplate = restTemplate;
    }

    @GetMapping("/autodocer-aggregator/definitions")
    public Map<String, String> getAggregatedDefinitions() {
        System.out.println("HELLOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        log.info("Fetching aggregated API definitions using native Eureka registry...");
        Map<String, String> definitions = new HashMap<>();

        // 1. Get applications directly from Eureka's internal registry
        Applications applications = instanceRegistry.getApplications();

        log.info("Found {} applications in the registry.", applications.getRegisteredApplications().size());

        applications.getRegisteredApplications().forEach(app -> {
            String serviceId = app.getName().toLowerCase(); // Eureka stores names in UPPERCASE

            log.debug("Processing application: {}", serviceId);

            // Exclude the Eureka server itself
            if ("eureka-server".equalsIgnoreCase(serviceId)) {
                log.debug("Skipping eureka-server.");
                return; // Use return instead of continue in lambda
            }

            // 2. Get the instances for this service
            List<InstanceInfo> instances = app.getInstances();
            if (instances.isEmpty()) {
                log.warn("No instances found for service: {}", serviceId);
                return; // Use return instead of continue in lambda
            }

            // 3. Just use the first UP instance found
            InstanceInfo instance = instances.stream()
                    .filter(info -> info.getStatus() == InstanceInfo.InstanceStatus.UP)
                    .findFirst()
                    .orElse(null);

            if (instance == null) {
                log.warn("No UP instances found for service: {}", serviceId);
                return; // Use return instead of continue in lambda
            }

            // Use getHomePageUrl() which usually resolves correctly
            String serviceUrl = instance.getHomePageUrl();
            // Fallback if homePageUrl is not set (less common for Spring Boot apps)
            if (serviceUrl == null || serviceUrl.isBlank()) {
                serviceUrl = instance.getIPAddr() + ":" + instance.getPort();
                if (!serviceUrl.startsWith("http")) {
                    serviceUrl = (instance.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https://" : "http://") + serviceUrl;
                }
            }

            String docsUrl = serviceUrl + "/autodocer/api-docs"; // Assuming the path from autodocer-core

            try {
                // 4. Fetch the OpenAPI JSON from the service
                log.info("Fetching docs for {} from {}", serviceId, docsUrl);
                String openApiJson = restTemplate.getForObject(docsUrl, String.class);
                if (openApiJson != null && !openApiJson.isBlank()) {
                    definitions.put(serviceId, openApiJson); // Use lowercase serviceId as key
                    log.info("Successfully fetched docs for {}", serviceId);
                } else {
                    log.warn("Received empty response for docs from {}", serviceId);
                }
            } catch (Exception e) {
                log.error("Error fetching docs for service {}: {}", serviceId, e.getMessage(), e); // Log exception details
                definitions.put(serviceId, "{\"error\": \"Could not fetch docs: " + e.getMessage() + "\"}");
            }
        });

        log.info("Finished fetching definitions. Found docs for {} services.", definitions.size());
        return definitions;
    }
}