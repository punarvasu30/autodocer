package com.autodocer; // Ensure correct package

import com.autodocer.Controller.AggregatorDataController;
import com.autodocer.Controller.AggregatorUiController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
// Import InstanceRegistry if needed for injection type hint, though @Autowired in controller is usually enough
// import com.netflix.eureka.registry.InstanceRegistry;


@Configuration
public class AggregatorAutoConfiguration {

    // Bean to create the controller that provides the aggregated data
    // We no longer inject DiscoveryClient here, controller uses @Autowired
    @Bean
    public AggregatorDataController aggregatorDataController(RestTemplate restTemplate) {
        // Spring will automatically inject PeerAwareInstanceRegistry via @Autowired in the controller
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