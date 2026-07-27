package com.vinit.bankingclient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DirectOpenAiService {

    private static final Logger log = LoggerFactory.getLogger(DirectOpenAiService.class);

    private final RestClient restClient;
    private final String model;
    private final String chatCompletionsUrl;

    public DirectOpenAiService(
            @Value("${opencode.zen.chat-completions-url:https://opencode.ai/zen/v1/chat/completions}") String chatCompletionsUrl,
            @Value("${opencode.zen.model:deepseek-v4-flash-free}") String model,
            @Value("${opencode.zen.api-key:}") String configuredApiKey,
            @Value("${opencode.zen.connect-timeout:5s}") Duration connectTimeout,
            @Value("${opencode.zen.request-timeout:30s}") Duration requestTimeout) {
        this.chatCompletionsUrl = chatCompletionsUrl;
        this.model = model;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(connectTimeout)
                        .build()
        );
        requestFactory.setReadTimeout(requestTimeout);

        String apiKey = resolveApiKey(configuredApiKey);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> {
                    if (!apiKey.isBlank()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .build();
    }

    public String chat(String prompt) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 800,
                    "stream", false
            );

            Map response = restClient.post()
                    .uri(chatCompletionsUrl)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            log.info("OpenAI request completed in {} ms", System.currentTimeMillis() - start);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");
                return content != null && !content.isBlank() ? content : (String) message.get("reasoning_content");
            }
            return "No response";
        } catch (Exception e) {
            log.error("OpenAI request failed after {} ms: {}", System.currentTimeMillis() - start, e.getMessage());
            throw new RuntimeException("OpenAI API call failed", e);
        }
    }

    private String resolveApiKey(String configuredApiKey) {
        return cleanApiKey(Optional.ofNullable(configuredApiKey)
                .filter(key -> !key.isBlank())
                .or(() -> Optional.ofNullable(System.getenv("OPENCODE_API_KEY")))
                .filter(key -> !key.isBlank())
                .or(this::readApiKeyFromDotEnv)
                .orElse(""));
    }

    private Optional<String> readApiKeyFromDotEnv() {
        Path dotenv = Path.of(".env");
        if (!Files.isRegularFile(dotenv)) {
            return Optional.empty();
        }

        try {
            return Files.readAllLines(dotenv).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("OPENCODE_API_KEY"))
                    .map(line -> line.substring(line.indexOf('=') + 1))
                    .findFirst();
        } catch (IOException e) {
            log.warn("Could not read .env for OPENCODE_API_KEY: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String cleanApiKey(String apiKey) {
        String cleaned = apiKey.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }
}
