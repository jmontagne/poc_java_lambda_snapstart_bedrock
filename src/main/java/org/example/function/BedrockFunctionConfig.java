package org.example.function;

import org.example.service.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class BedrockFunctionConfig {

    private static final Logger LOG = LoggerFactory.getLogger(BedrockFunctionConfig.class);
    private final BedrockService bedrockService;

    public BedrockFunctionConfig(BedrockService bedrockService) {
        this.bedrockService = bedrockService;
    }

    @Bean
    public Function<String, String> askAi() {
        return question -> {
            LOG.info("Received question: {}", question);
            return bedrockService.askBedrock(question);
        };
    }
}

