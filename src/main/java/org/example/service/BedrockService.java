package org.example.service;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BedrockService implements Resource {

    private static final Logger LOG = LoggerFactory.getLogger(BedrockService.class);
    private BedrockRuntimeClient bedrockClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    // Using Claude 3 Haiku as requested
    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";

    public BedrockService() {
        // Register this class as a CRaC resource to handle checkpoint/restore events
        Core.getGlobalContext().register(this);
    }

    private void initClient() {
        if (bedrockClient == null) {
            // Use UrlConnectionHttpClient for faster startup (lighter than Netty/Apache)
            bedrockClient = BedrockRuntimeClient.builder()
                    .region(Region.US_EAST_1) // Defaulting to US_EAST_1, can be parameterized
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .build();
        }
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        // Pre-initialize the client before the snapshot is taken
        initClient();
        LOG.info("CRaC: BedrockClient initialized before checkpoint.");
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        LOG.info("CRaC: Restored from checkpoint.");
    }

    public String askBedrock(String question) {
        initClient(); // Ensure initialized if not using SnapStart or first run

        try {
            // Construct Claude 3 JSON payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("anthropic_version", "bedrock-2023-05-31");
            payload.put("max_tokens", 1000);

            ArrayNode messages = payload.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            ArrayNode content = message.putArray("content");
            ObjectNode textContent = content.addObject();
            textContent.put("type", "text");
            textContent.put("text", question);

            String jsonBody = objectMapper.writeValueAsString(payload);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .body(SdkBytes.fromString(jsonBody, StandardCharsets.UTF_8))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);

            String responseBody = response.body().asUtf8String();
            ObjectNode responseJson = (ObjectNode) objectMapper.readTree(responseBody);

            // Parse Claude 3 response structure to get the text
            if (responseJson.has("content") && responseJson.get("content").isArray()) {
                return responseJson.get("content").get(0).get("text").asText();
            } else {
                return "Error: Unexpected response format from Bedrock.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error invoking Bedrock: " + e.getMessage();
        }
    }
}

