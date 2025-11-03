package com.autodocer; // Ensure correct package

import com.autodocer.Controller.AggregatorDataController;
import com.autodocer.Controller.AggregatorUiController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AggregatorAutoConfiguration {

    @Bean
    public AggregatorDataController aggregatorDataController(RestTemplate restTemplate) {
        return new AggregatorDataController(restTemplate);
    }

    // Bean to create the controller that serves the UI HTML page (no change)
    @Bean
    public AggregatorUiController aggregatorUiController() {
        return new AggregatorUiController();
    }

    // Bean for the RestTemplate (no change)
    @Bean
    public RestTemplate autodocerRestTemplate() {
        return new RestTemplate();
    }
}