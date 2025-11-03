//package com.autodocer.Controller; // Make sure package is correct
//
//import com.netflix.discovery.shared.Applications;
//import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
//import com.netflix.eureka.registry.InstanceRegistry; // Import InstanceRegistry
//import com.netflix.appinfo.InstanceInfo; // Import InstanceInfo
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.beans.factory.annotation.Autowired; // Autowire
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//public class AggregatorDataController {
//
//    private static final Logger log = LoggerFactory.getLogger(AggregatorDataController.class);
//
//    // THE FIX: Inject Eureka's internal registry directly
//    @Autowired
//    private InstanceRegistry instanceRegistry; // Use InstanceRegistry interface
//
//    private final RestTemplate restTemplate;
//
//    // RestTemplate is injected by Spring via the AutoConfiguration
//    public AggregatorDataController(RestTemplate restTemplate) {
//        // We no longer need DiscoveryClient here
//        this.restTemplate = restTemplate;
//    }
//
//    @GetMapping("/autodocer-aggregator/definitions")
//    public Map<String, String> getAggregatedDefinitions() {
//        System.out.println("HELLOOOOOOOOOOOOOOOOOOOOOOOOOOO");
//        log.info("Fetching aggregated API definitions using native Eureka registry...");
//        Map<String, String> definitions = new HashMap<>();
//
//        // 1. Get applications directly from Eureka's internal registry
//        Applications applications = instanceRegistry.getApplications();
//
//        log.info("Found {} applications in the registry.", applications.getRegisteredApplications().size());
//
//        applications.getRegisteredApplications().forEach(app -> {
//            String serviceId = app.getName().toLowerCase(); // Eureka stores names in UPPERCASE
//
//            log.debug("Processing application: {}", serviceId);
//
//            // Exclude the Eureka server itself
//            if ("eureka-server".equalsIgnoreCase(serviceId)) {
//                log.debug("Skipping eureka-server.");
//                return; // Use return instead of continue in lambda
//            }
//
//            // 2. Get the instances for this service
//            List<InstanceInfo> instances = app.getInstances();
//            if (instances.isEmpty()) {
//                log.warn("No instances found for service: {}", serviceId);
//                return; // Use return instead of continue in lambda
//            }
//
//            // 3. Just use the first UP instance found
//            InstanceInfo instance = instances.stream()
//                    .filter(info -> info.getStatus() == InstanceInfo.InstanceStatus.UP)
//                    .findFirst()
//                    .orElse(null);
//
//            if (instance == null) {
//                log.warn("No UP instances found for service: {}", serviceId);
//                return; // Use return instead of continue in lambda
//            }
//
//            // Use getHomePageUrl() which usually resolves correctly
//            String serviceUrl = instance.getHomePageUrl();
//            // Fallback if homePageUrl is not set (less common for Spring Boot apps)
//            if (serviceUrl == null || serviceUrl.isBlank()) {
//                serviceUrl = instance.getIPAddr() + ":" + instance.getPort();
//                if (!serviceUrl.startsWith("http")) {
//                    serviceUrl = (instance.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https://" : "http://") + serviceUrl;
//                }
//            }
//
//            String docsUrl = serviceUrl + "/autodocer/api-docs"; // Assuming the path from autodocer-core
//
//            try {
//                // 4. Fetch the OpenAPI JSON from the service
//                log.info("Fetching docs for {} from {}", serviceId, docsUrl);
//                String openApiJson = restTemplate.getForObject(docsUrl, String.class);
//                if (openApiJson != null && !openApiJson.isBlank()) {
//                    definitions.put(serviceId, openApiJson); // Use lowercase serviceId as key
//                    log.info("Successfully fetched docs for {}", serviceId);
//                } else {
//                    log.warn("Received empty response for docs from {}", serviceId);
//                }
//            } catch (Exception e) {
//                log.error("Error fetching docs for service {}: {}", serviceId, e.getMessage(), e); // Log exception details
//                definitions.put(serviceId, "{\"error\": \"Could not fetch docs: " + e.getMessage() + "\"}");
//            }
//        });
//
//        log.info("Finished fetching definitions. Found docs for {} services.", definitions.size());
//        return definitions;
//    }
//}



//
//
//package com.autodocer.Controller; // Ensure correct package
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.netflix.appinfo.InstanceInfo;
//import com.netflix.discovery.shared.Applications;
//import com.netflix.eureka.registry.InstanceRegistry; // Use InstanceRegistry
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestClientException;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//public class AggregatorDataController {
//
//    private static final Logger log = LoggerFactory.getLogger(AggregatorDataController.class);
//
//    @Autowired
//    private InstanceRegistry instanceRegistry; // Use Eureka's internal registry
//
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper; // Jackson's JSON processor
//
//    // Inject RestTemplate via constructor
//    public AggregatorDataController(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//        this.objectMapper = new ObjectMapper();
//    }
//
//    // UPDATED: Endpoint now returns a single merged JSON string
//    @GetMapping(value = "/autodocer-aggregator/definitions", produces = "application/json")
//    public String getAggregatedDefinitions() {
//        System.out.println("--- [AutoDocER Aggregator] Unified Definitions Endpoint Called ---"); // Added specific debug
//        log.info("Fetching and merging API definitions...");
//
//        // 1. Create the base structure for the merged OpenAPI document
//        ObjectNode mergedRoot = objectMapper.createObjectNode();
//        mergedRoot.put("openapi", "3.0.0");
//        ObjectNode infoNode = mergedRoot.putObject("info");
//        infoNode.put("title", "Aggregated API Documentation");
//        infoNode.put("version", "1.0.0"); // Consider making dynamic
//        infoNode.put("description", "Combined documentation from all registered services (generated by AutoDocER)");
//        ObjectNode mergedPathsNode = mergedRoot.putObject("paths");
//        // TODO: Add merging logic for components/schemas later if needed
//
//        // 2. Get applications directly from Eureka's internal registry
//        Applications applications = instanceRegistry.getApplications();
//        log.info("Found {} applications in the registry.", applications.getRegisteredApplications().size());
//
//        applications.getRegisteredApplications().forEach(app -> {
//            String serviceId = app.getName().toLowerCase(); // Eureka stores names in UPPERCASE
//            log.debug("Processing application: {}", serviceId);
//
//            // Exclude the Eureka server itself
//            if ("eureka-server".equalsIgnoreCase(serviceId)) {
//                log.debug("Skipping eureka-server.");
//                return; // Use return instead of continue in lambda
//            }
//
//            // Get UP instances
//            List<InstanceInfo> instances = app.getInstancesAsIsFromEureka(); // More direct way
//            InstanceInfo instance = instances.stream()
//                    .filter(info -> info.getStatus() == InstanceInfo.InstanceStatus.UP)
//                    .findFirst()
//                    .orElse(null);
//
//            if (instance == null) {
//                System.out.println("No UP instances found for service: {}" + serviceId);
//                log.warn("No UP instances found for service: {}", serviceId);
//                return; // Use return instead of continue in lambda
//            }
//
//            // Construct the documentation URL
//            String serviceUrl = instance.getHomePageUrl(); // Prefer homePageUrl
//            if (serviceUrl == null || serviceUrl.isBlank()) { // Fallback
//                serviceUrl = instance.getIPAddr() + ":" + instance.getPort();
//                if (!serviceUrl.startsWith("http")) {
//                    serviceUrl = (instance.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https://" : "http://") + serviceUrl;
//                }
//            }
//            // Ensure serviceUrl doesn't end with '/' before appending
//            if (serviceUrl.endsWith("/")) {
//                serviceUrl = serviceUrl.substring(0, serviceUrl.length() -1);
//            }
//            // IMPORTANT: Ensure this path matches the endpoint in your autodocer-core library
//            String docsUrl = serviceUrl + "/autodocer/api-docs";
//
//            try {
//                // 3. Fetch the individual OpenAPI JSON
//                log.info("Fetching docs for {} from {}", serviceId, docsUrl);
//                String openApiJsonString = restTemplate.getForObject(docsUrl, String.class);
//
//                if (openApiJsonString != null && !openApiJsonString.isBlank()) {
//                    // 4. Parse the fetched JSON
//                    JsonNode serviceRoot = objectMapper.readTree(openApiJsonString);
//                    JsonNode servicePaths = serviceRoot.path("paths");
//
//                    // 5. Merge paths into the main document
//                    if (servicePaths.isObject()) {
//                        Iterator<Map.Entry<String, JsonNode>> pathIterator = servicePaths.fields();
//                        while (pathIterator.hasNext()) {
//                            Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
//                            String originalPath = pathEntry.getKey();
//                            JsonNode pathItem = pathEntry.getValue();
//
//                            // --- Merging Strategy Implemented ---
//                            // a) Prefix the path with the service ID
//                            String mergedPath = "/" + serviceId + originalPath;
//                            mergedPath = mergedPath.replaceAll("/+", "/"); // Clean up slashes
//
//                            // b) Add/Update operations within the path item
//                            // Use .withObject() which gets or creates the object node
//                            ObjectNode mergedPathItemNode = mergedPathsNode.withObject(mergedPath);
//                            Iterator<Map.Entry<String, JsonNode>> operationIterator = pathItem.fields();
//                            while (operationIterator.hasNext()) {
//                                Map.Entry<String, JsonNode> opEntry = operationIterator.next();
//                                String httpMethod = opEntry.getKey();
//                                // Ensure it's a valid HTTP method node before casting
//                                if (opEntry.getValue().isObject()) {
//                                    ObjectNode operationNode = (ObjectNode) opEntry.getValue().deepCopy(); // Work on a copy
//
//                                    // c) Prefix operationId if it exists
//                                    if (operationNode.has("operationId")) {
//                                        String originalOpId = operationNode.path("operationId").asText("");
//                                        operationNode.put("operationId", serviceId + "_" + originalOpId);
//                                    } else {
//                                        // Generate one if missing (optional but recommended)
//                                        String generatedOpId = serviceId + "_" + httpMethod + mergedPath.replaceAll("[^A-Za-z0-9_]", "_");
//                                        operationNode.put("operationId", generatedOpId);
//                                    }
//
//
//                                    // d) Add service name as the primary tag (replace existing tags)
//                                    operationNode.putArray("tags").removeAll().add(serviceId);
//
//                                    // e) Add the modified operation to the merged path item
//                                    mergedPathItemNode.set(httpMethod, operationNode);
//                                }
//                            }
//                            // --- End Merging Strategy ---
//                        }
//                    }
//                    log.info("Successfully merged docs for {}", serviceId);
//                } else {
//                    log.warn("Received empty response for docs from {}", serviceId);
//                }
//            } catch (RestClientException e) {
//                log.error("Network error fetching docs for service {}: {}", serviceId, e.getMessage());
//                // Optionally add info about the failure to the main spec?
//            } catch (Exception e) {
//                log.error("Error processing/merging docs for service {}: {}", serviceId, e.getMessage(), e);
//                // Optionally add info about the failure to the main spec?
//            }
//        });
//
//        log.info("Finished merging definitions.");
//
//        // 6. Convert the final merged JSON object back to a string
//        try {
//            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mergedRoot);
//        } catch (Exception e) {
//            log.error("Error generating final merged OpenAPI spec: {}", e.getMessage(), e);
//            // Return a minimal valid spec with an error message
//            return "{\"openapi\": \"3.0.0\", \"info\": {\"title\": \"Error Generating Aggregated Spec\", \"version\":\"1.0.0\", \"description\": \"" + e.getMessage().replace("\"", "'") + "\"}, \"paths\": {}}";
//        }
//    }
//}

//
//package com.autodocer.Controller; // Ensure correct package
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.netflix.appinfo.InstanceInfo;
//import com.netflix.discovery.shared.Applications;
//import com.netflix.eureka.registry.InstanceRegistry;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestClientException;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//public class AggregatorDataController {
//
//    private static final Logger log = LoggerFactory.getLogger(AggregatorDataController.class);
//
//    @Autowired
//    private InstanceRegistry instanceRegistry;
//
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper;
//
//    public AggregatorDataController(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//        this.objectMapper = new ObjectMapper();
//    }
//
//    @GetMapping(value = "/autodocer-aggregator/definitions", produces = "application/json")
//    public String getAggregatedDefinitions() {
//        log.info("Fetching and merging API definitions...");
//
//        ObjectNode mergedRoot = objectMapper.createObjectNode();
//        mergedRoot.put("openapi", "3.0.0");
//        ObjectNode infoNode = mergedRoot.putObject("info");
//        infoNode.put("title", "Aggregated API Documentation");
//        infoNode.put("version", "1.0.0");
//        infoNode.put("description", "Combined documentation from all registered services (generated by AutoDocER)");
//        ObjectNode mergedPathsNode = mergedRoot.putObject("paths"); // This is the node we populate
//
//        Applications applications = instanceRegistry.getApplications();
//        log.info("Found {} applications in the registry.", applications.getRegisteredApplications().size());
//
//        applications.getRegisteredApplications().forEach(app -> {
//            String serviceId = app.getName().toLowerCase();
//            log.debug("Processing application: {}", serviceId);
//
//            if ("eureka-server".equalsIgnoreCase(serviceId)) {
//                log.debug("Skipping eureka-server.");
//                return;
//            }
//
//            InstanceInfo instance = app.getInstancesAsIsFromEureka().stream()
//                    .filter(info -> info.getStatus() == InstanceInfo.InstanceStatus.UP)
//                    .findFirst()
//                    .orElse(null);
//
//            if (instance == null) {
//                log.warn("No UP instances found for service: {}", serviceId);
//                return;
//            }
//
//            String serviceUrl = instance.getHomePageUrl();
//            if (serviceUrl == null || serviceUrl.isBlank()) {
//                serviceUrl = instance.getIPAddr() + ":" + instance.getPort();
//                if (!serviceUrl.startsWith("http")) {
//                    serviceUrl = (instance.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https://" : "http://") + serviceUrl;
//                }
//            }
//            if (serviceUrl.endsWith("/")) {
//                serviceUrl = serviceUrl.substring(0, serviceUrl.length() -1);
//            }
//            String docsUrl = serviceUrl + "/autodocer/api-docs";
//
//            try {
//                log.info("Fetching docs for {} from {}", serviceId, docsUrl);
//                String openApiJsonString = restTemplate.getForObject(docsUrl, String.class);
//
//                if (openApiJsonString != null && !openApiJsonString.isBlank()) {
//                    JsonNode serviceRoot = objectMapper.readTree(openApiJsonString);
//                    JsonNode servicePaths = serviceRoot.path("paths");
//
//                    if (servicePaths.isObject()) {
//                        Iterator<Map.Entry<String, JsonNode>> pathIterator = servicePaths.fields();
//                        while (pathIterator.hasNext()) {
//                            Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
//                            String originalPath = pathEntry.getKey();
//                            JsonNode pathItem = pathEntry.getValue();
//
//                            // --- CORRECTED MERGING ---
//                            // a) Create the correct merged path string
//                            String mergedPath = "/" + serviceId + originalPath;
//                            mergedPath = mergedPath.replaceAll("/+", "/");
//
//                            // b) Get or create the ObjectNode for this specific path key
//                            ObjectNode mergedPathItemNode;
//                            if (mergedPathsNode.has(mergedPath)) {
//                                mergedPathItemNode = (ObjectNode) mergedPathsNode.get(mergedPath);
//                            } else {
//                                mergedPathItemNode = mergedPathsNode.putObject(mergedPath); // Use putObject here!
//                            }
//
//                            // c) Iterate through operations (GET, POST, etc.) and add/modify them
//                            Iterator<Map.Entry<String, JsonNode>> operationIterator = pathItem.fields();
//                            while (operationIterator.hasNext()) {
//                                Map.Entry<String, JsonNode> opEntry = operationIterator.next();
//                                String httpMethod = opEntry.getKey();
//                                if (opEntry.getValue().isObject()) {
//                                    ObjectNode operationNode = (ObjectNode) opEntry.getValue().deepCopy();
//
//                                    // Prefix operationId
//                                    if (operationNode.has("operationId")) {
//                                        String originalOpId = operationNode.path("operationId").asText("");
//                                        operationNode.put("operationId", serviceId + "_" + originalOpId);
//                                    } else {
//                                        String generatedOpId = serviceId + "_" + httpMethod + mergedPath.replaceAll("[^A-Za-z0-9_]", "_");
//                                        operationNode.put("operationId", generatedOpId);
//                                    }
//
//                                    // Set tag
//                                    operationNode.putArray("tags").removeAll().add(serviceId);
//
//                                    // Add to the merged path item
//                                    mergedPathItemNode.set(httpMethod, operationNode);
//                                }
//                            }
//                            // --- END CORRECTED MERGING ---
//                        }
//                    }
//                    log.info("Successfully merged docs for {}", serviceId);
//                } else {
//                    log.warn("Received empty response for docs from {}", serviceId);
//                }
//            } catch (RestClientException e) {
//                log.error("Network error fetching docs for service {}: {}", serviceId, e.getMessage());
//            } catch (Exception e) {
//                log.error("Error processing/merging docs for service {}: {}", serviceId, e.getMessage(), e);
//            }
//        });
//
//        log.info("Finished merging definitions.");
//
//        try {
//            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mergedRoot);
//        } catch (Exception e) {
//            log.error("Error generating final merged OpenAPI spec: {}", e.getMessage(), e);
//            return "{\"openapi\": \"3.0.0\", \"info\": {\"title\": \"Error Generating Aggregated Spec\"}, \"paths\": {}}";
//        }
//    }
//}


package com.autodocer.Controller; // Ensure correct package

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.InstanceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // Import ResponseEntity
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RestController
public class AggregatorDataController {

    private static final Logger log = LoggerFactory.getLogger(AggregatorDataController.class);

    @Autowired
    private InstanceRegistry instanceRegistry;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public record AggregatedDefinitionsResult(JsonNode unifiedSpec, Map<String, JsonNode> individualSpecs) {}


    public AggregatorDataController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // UPDATED: Endpoint now returns the combined structure
    @GetMapping(value = "/autodocer-aggregator/definitions", produces = "application/json")
    public ResponseEntity<AggregatedDefinitionsResult> getAggregatedDefinitions() { // Return ResponseEntity
        log.info("Fetching and merging API definitions...");

        // --- Initialize Structures ---
        ObjectNode mergedRoot = objectMapper.createObjectNode();
        mergedRoot.put("openapi", "3.0.0");
        ObjectNode infoNode = mergedRoot.putObject("info");
        infoNode.put("title", "Aggregated API Documentation");
        infoNode.put("version", "1.0.0");
        infoNode.put("description", "Combined documentation from all registered services (generated by AutoDocER)");
        ObjectNode mergedPathsNode = mergedRoot.putObject("paths");

        Map<String, JsonNode> individualSpecs = new HashMap<>(); // Store individual parsed specs
        // --- End Initialization ---


        Applications applications = instanceRegistry.getApplications();
        log.info("Found {} applications in the registry.", applications.getRegisteredApplications().size());

        applications.getRegisteredApplications().forEach(app -> {
            String serviceId = app.getName().toLowerCase();
            log.debug("Processing application: {}", serviceId);

            if ("eureka-server".equalsIgnoreCase(serviceId)) {
                log.debug("Skipping eureka-server.");
                return;
            }

            InstanceInfo instance = app.getInstancesAsIsFromEureka().stream()
                    .filter(info -> info.getStatus() == InstanceInfo.InstanceStatus.UP)
                    .findFirst()
                    .orElse(null);

            if (instance == null) {
                log.warn("No UP instances found for service: {}", serviceId);
                return;
            }

            // Construct Docs URL (Ensure /autodocer/api-docs matches your core library)
            String serviceUrl = instance.getHomePageUrl();
            if (serviceUrl == null || serviceUrl.isBlank()) {
                serviceUrl = instance.getIPAddr() + ":" + instance.getPort();
                if (!serviceUrl.startsWith("http")) {
                    serviceUrl = (instance.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https://" : "http://") + serviceUrl;
                }
            }
            if (serviceUrl.endsWith("/")) {
                serviceUrl = serviceUrl.substring(0, serviceUrl.length() -1);
            }
            String docsUrl = serviceUrl + "/autodocer/api-docs";

            try {
                log.info("Fetching docs for {} from {}", serviceId, docsUrl);
                String openApiJsonString = restTemplate.getForObject(docsUrl, String.class);

                if (openApiJsonString != null && !openApiJsonString.isBlank()) {
                    JsonNode serviceRoot = objectMapper.readTree(openApiJsonString);

                    // Store the individual parsed spec
                    individualSpecs.put(serviceId, serviceRoot);

                    // Merge paths into the main document
                    JsonNode servicePaths = serviceRoot.path("paths");
                    if (servicePaths.isObject()) {
                        Iterator<Map.Entry<String, JsonNode>> pathIterator = servicePaths.fields();
                        while (pathIterator.hasNext()) {
                            Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
                            String originalPath = pathEntry.getKey();
                            JsonNode pathItem = pathEntry.getValue();

                            String mergedPath = "/" + serviceId + originalPath;
                            mergedPath = mergedPath.replaceAll("/+", "/");

                            ObjectNode mergedPathItemNode;
                            if (mergedPathsNode.has(mergedPath)) {
                                mergedPathItemNode = (ObjectNode) mergedPathsNode.get(mergedPath);
                            } else {
                                mergedPathItemNode = mergedPathsNode.putObject(mergedPath);
                            }

                            Iterator<Map.Entry<String, JsonNode>> operationIterator = pathItem.fields();
                            while (operationIterator.hasNext()) {
                                Map.Entry<String, JsonNode> opEntry = operationIterator.next();
                                String httpMethod = opEntry.getKey();
                                if (opEntry.getValue().isObject()) {
                                    ObjectNode operationNode = (ObjectNode) opEntry.getValue().deepCopy();

                                    if (operationNode.has("operationId")) {
                                        String originalOpId = operationNode.path("operationId").asText("");
                                        operationNode.put("operationId", serviceId + "_" + originalOpId);
                                    } else {
                                        String generatedOpId = serviceId + "_" + httpMethod + mergedPath.replaceAll("[^A-Za-z0-9_]", "_");
                                        operationNode.put("operationId", generatedOpId);
                                    }
                                    operationNode.putArray("tags").removeAll().add(serviceId);
                                    mergedPathItemNode.set(httpMethod, operationNode);
                                }
                            }
                        }
                    }
                    log.info("Successfully processed docs for {}", serviceId);
                } else {
                    log.warn("Received empty response for docs from {}", serviceId);
                    individualSpecs.put(serviceId, objectMapper.createObjectNode().put("error", "Received empty response"));
                }
            } catch (RestClientException e) {
                log.error("Network error fetching docs for service {}: {}", serviceId, e.getMessage());
                individualSpecs.put(serviceId, objectMapper.createObjectNode().put("error", "Network error: " + e.getMessage()));
            } catch (JsonProcessingException e) {
                log.error("JSON parsing error for service {}: {}", serviceId, e.getMessage());
                individualSpecs.put(serviceId, objectMapper.createObjectNode().put("error", "JSON Parsing error: " + e.getMessage()));
            } catch (Exception e) {
                log.error("Error processing/merging docs for service {}: {}", serviceId, e.getMessage(), e);
                individualSpecs.put(serviceId, objectMapper.createObjectNode().put("error", "Processing error: " + e.getMessage()));
            }
        });

        log.info("Finished fetching definitions.");

        // Create the combined result object
        AggregatedDefinitionsResult result = new AggregatedDefinitionsResult(mergedRoot, individualSpecs);
        return ResponseEntity.ok(result); // Return the result object
    }
}