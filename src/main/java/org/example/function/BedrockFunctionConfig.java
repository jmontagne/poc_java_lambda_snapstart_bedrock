package org.example.function;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

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
            int questionLength = question == null ? 0 : question.length();
            LOG.info("Received question", entry("questionLength", questionLength));
            return bedrockService.askBedrock(question);
        };
    }
}

