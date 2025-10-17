package com.autodocer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class SwaggerController {

    /**
     * This endpoint serves the Swagger UI.
     * It redirects the user to the main index.html file provided by the webjar.
     * The crucial part is the query parameter `?url=/autodocer/api-docs`. This tells
     * the Swagger UI to automatically load the JSON specification from your
     * existing endpoint.
     */
    @GetMapping("/autodocer/ui")
    public String getSwaggerUi() {

        System.out.println("Heloooooooooooooooooooooooooooooooooooooooo Hooooooooooooooooo" );
        return "autodocer-ui/index.html";
    }
}

