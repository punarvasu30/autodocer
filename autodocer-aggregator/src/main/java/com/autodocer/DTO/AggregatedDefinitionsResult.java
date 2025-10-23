package com.autodocer.DTO;// Add this record definition, either inside AggregatorDataController.java
// or in its own AggregatedDefinitionsResult.java file in the same package.

import com.fasterxml.jackson.databind.JsonNode; // Make sure this is imported
import java.util.Map;                            // Make sure this is imported

public record AggregatedDefinitionsResult(
        JsonNode unifiedSpec,           // The merged OpenAPI spec as a Jackson JSON node
        Map<String, JsonNode> individualSpecs // Map of serviceId to individual OpenAPI specs as Jackson JSON nodes
) {}