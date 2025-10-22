package com.autodocer;

import com.autodocer.Controller.AggregatorDataController;
import com.autodocer.Controller.AggregatorUiController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for the AutoDocER Aggregator.
 * Creates the necessary beans to discover services and serve the aggregated UI.
 */
@AutoConfiguration
public class AggregatorAutoConfiguration {

    // Bean to create the controller that provides the aggregated data
    @Bean
    public AggregatorDataController aggregatorDataController(DiscoveryClient discoveryClient) {
        // We'll also need a RestTemplate to make HTTP calls
        RestTemplate restTemplate = new RestTemplate();
        return new AggregatorDataController(discoveryClient, restTemplate);
    }

    // Bean to create the controller that serves the UI HTML page
    @Bean
    public AggregatorUiController aggregatorUiController() {
        return new AggregatorUiController();
    }

    // We can add the RestTemplate as a bean too if needed elsewhere,
    // but creating it directly in the controller bean is fine for now.
    // @Bean
    // public RestTemplate restTemplate() {
    //     return new RestTemplate();
    // }
}
