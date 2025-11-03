package com.autodocer;

import com.autodocer.AiDescription.AiController;
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

@AutoConfiguration
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

    @Bean
    @ConditionalOnMissingBean(AiDescriptionService.class)
    public AiDescriptionService placeholderAiDescriptionService() {
        System.out.println("--- [AutoDocER] No 'gemini.api.key' found. Creating *Placeholder* AiDescriptionService bean ---");
        return new PlaceholderAiDescriptionService();
    }


    @Bean
    public AiController aiController(AiDescriptionService aiService) {
        // This controller uses the same AiService bean (Gemini or Placeholder)
        System.out.println("--- [AutoDocER] Creating AiController bean ---");
        return new AiController(aiService);
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

    @Bean
    public DocumentationController documentationController(ApplicationContext context, DocumentationParser parser, OpenApiGenerator generator) {
        System.out.println("--- [AutoDocER] Creating DocumentationController bean ---"); // Added debug
        return new DocumentationController(context, parser, generator);
    }

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