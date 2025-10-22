package com.autodocer.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve the main HTML page for the Aggregator UI.
 */
@Controller
public class AggregatorUiController {

    /**
     * Serves the index.html page for the aggregator dashboard.
     * Spring Boot will look for this file in 'src/main/resources/static/aggregator-ui/index.html'.
     * @return The path to the HTML view file.
     */

    @GetMapping("/autodocer-aggregator/ui")
    public ResponseEntity<Resource> getAggregatorUi() {
        // Load the HTML file as a resource from the classpath.
        Resource resource = new ClassPathResource("static/autodocer-ui/index.html");

        // If the resource doesn't exist, return a 404.
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Serve the resource with the correct content type.
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }
}
