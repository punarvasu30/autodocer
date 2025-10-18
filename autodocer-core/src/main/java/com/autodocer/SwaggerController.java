package com.autodocer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerController {

    /**
     * This endpoint now manually loads the index.html file from the classpath
     * and serves it directly. This is a robust method that does not rely on
     * Spring's view resolver.
     *
     * The file must be located at: 'src/main/resources/static/autodocer-ui/index.html'
     */
    @GetMapping("/autodocer/ui")
    public ResponseEntity<Resource> getSwaggerUi() {
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

