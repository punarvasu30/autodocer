package com.autodocer;

// Change 1: Import @Configuration and @ComponentScan
import com.autodocer.Controller.AggregatorDataController;
import com.autodocer.Controller.AggregatorUiController;
import org.springframework.context.annotation.Configuration; // Use this
import org.springframework.context.annotation.ComponentScan; // Use this
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

// Change 2: Use @Configuration instead of @AutoConfiguration
@Configuration
// Change 3: Add a ComponentScan for your controller package
@ComponentScan(basePackages = "com.autodocer.Controller")
public class AggregatorAutoConfiguration {

    // Bean to create the controller that provides the aggregated data
    @Bean
    public AggregatorDataController aggregatorDataController(DiscoveryClient discoveryClient) {
        RestTemplate restTemplate = new RestTemplate();
        return new AggregatorDataController(discoveryClient, restTemplate);
    }

    // Bean to create the controller that serves the UI HTML page
    @Bean
    public AggregatorUiController aggregatorUiController() {
        return new AggregatorUiController();
    }

    // Bean for the RestTemplate
    @Bean
    public RestTemplate autodocerRestTemplate() {
        return new RestTemplate();
    }
}