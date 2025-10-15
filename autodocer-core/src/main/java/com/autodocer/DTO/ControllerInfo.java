package com.autodocer.DTO;

import java.util.List;

/**
 * Holds all the information about a single REST controller.
 */

public record ControllerInfo(
        String className,
        String basePath,
        List<EndpointInfo> endpoints
) {}
