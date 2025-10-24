package com.autodocer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller dedicated to serving the custom AutoDocER UI.
 */
@Controller
public class CustomUiController {

    /**
     * Serves the custom-ui.html file from the classpath resources.
     * The file must be located at: 'src/main/resources/static/autodocer-custom-ui/custom-ui.html'
     */
    @GetMapping("/autodocer/custom-ui") // New, distinct path
    public ResponseEntity<Resource> getCustomUi() {
        Resource resource = new ClassPathResource("static/autodocer-ui/custom-ui.html");
        System.out.println("--- [AutoDocER] Attempting to load custom UI from classpath:static/autodocer-custom-ui/custom-ui.html ---");

        if (!resource.exists()) {
            System.err.println("--- [AutoDocER] ERROR: Could not find custom-ui.html at the specified path.");
            return ResponseEntity.notFound().build();
        }
        System.out.println("--- [AutoDocER] Found custom-ui.html successfully.");

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }
}
