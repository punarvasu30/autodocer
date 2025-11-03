package com.autodocer.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AggregatorUiController {

    @GetMapping("/autodocer-aggregator/ui")
    public ResponseEntity<Resource> getAggregatorUi() {
        // Load the HTML file as a resource from the classpath.
        Resource resource = new ClassPathResource("static/aggreagtor-ui/index.html");

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
