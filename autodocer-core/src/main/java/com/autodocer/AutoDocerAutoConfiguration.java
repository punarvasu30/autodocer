package com.autodocer;

import com.autodocer.AiDescription.AiDescriptionService;
import com.autodocer.AiDescription.GeminiAiDescriptionService;
import com.autodocer.AiDescription.PlaceholderAiDescriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext; // Required import
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
// Removed ComponentScan import

/**
 * The main auto-configuration class for the AutoDocER library.
 * It explicitly creates and registers all the necessary beans.
 */
@AutoConfiguration
// REMOVED: @ComponentScan(basePackages = "com.autodocer.Controller")
public class AutoDocerAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "gemini.api.key")
    public AiDescriptionService geminiAiDescriptionService(
            @Value("${gemini.api.key}") String apiKey,
            WebClient.Builder webClientBuilder // Spring Boot provides this
    ) {
        System.out.println("--- [AutoDocER] Found 'gemini.api.key'. Creating *Gemini* AiDescriptionService bean ---");
        return new GeminiAiDescriptionService(apiKey, webClientBuilder);
    }

    // --- BEAN 2: The FALLBACK Placeholder Service ---
    /**
     * This bean is only created if no other bean of type 'AiDescriptionService'
     * (like the one above) has been created.
     */
    @Bean
    @ConditionalOnMissingBean(AiDescriptionService.class)
    public AiDescriptionService placeholderAiDescriptionService() {
        System.out.println("--- [AutoDocER] No 'gemini.api.key' found. Creating *Placeholder* AiDescriptionService bean ---");
        return new PlaceholderAiDescriptionService();
    }

    @Bean
    public DocumentationParser documentationParser(AiDescriptionService aiService) {
        System.out.println("--- [AutoDocER] Creating DocumentationParser bean ---");
        // Inject the service into the parser
        return new DocumentationParser(aiService);
    }

    @Bean
    public OpenApiGenerator openApiGenerator() {
        System.out.println("--- [AutoDocER] Creating OpenApiGenerator bean ---"); // Added debug
        return new OpenApiGenerator();
    }

    /**
     * THE FIX: Explicitly create the DocumentationController as a bean.
     * Spring will automatically provide the other beans this method needs
     * (context, parser, generator) because they are also defined here.
     */
    @Bean
    public DocumentationController documentationController(ApplicationContext context, DocumentationParser parser, OpenApiGenerator generator) {
        System.out.println("--- [AutoDocER] Creating DocumentationController bean ---"); // Added debug
        return new DocumentationController(context, parser, generator);
    }

    /**
     * THE FIX: Explicitly create the SwaggerController as a bean.
     * This ensures the /autodocer/ui endpoint is always registered.
     */
    @Bean
    public SwaggerController swaggerController() {
        System.out.println("--- [AutoDocER] Creating SwaggerController bean ---"); // Added debug
        return new SwaggerController();
    }

    @Bean
    public CustomUiController customUiController() {
        System.out.println("--- [AutoDocER] Creating CustomUiController bean ---"); // Added debug
        return new CustomUiController();
    }
}