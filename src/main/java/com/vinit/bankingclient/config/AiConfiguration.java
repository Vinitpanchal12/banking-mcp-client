package com.vinit.bankingclient.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

@Configuration
public class AiConfiguration {

    @Bean
    ChatClient ollamaChatClient(@Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    @Bean
    ChatModel opencodeZenChatModel(
            ToolCallingManager toolCallingManager,
            @Value("${opencode.zen.base-url}") String baseUrl,
            @Value("${opencode.zen.model}") String model,
            @Value("${opencode.zen.api-key:}") String configuredApiKey,
            @Value("${opencode.zen.request-timeout:30s}") Duration requestTimeout) {

        String apiKey = resolveApiKey(configuredApiKey);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .maxTokens(800)
                .timeout(requestTimeout)
                .maxRetries(1)
                .build();

        return OpenAiChatModel.builder()
                .options(options)
                .toolCallingManager(toolCallingManager)
                .build();
    }

    @Bean
    ChatClient openaiChatClient(@Qualifier("opencodeZenChatModel") ChatModel opencodeZenChatModel) {
        return ChatClient.builder(opencodeZenChatModel).build();
    }

    private String resolveApiKey(String configuredApiKey) {
        return cleanApiKey(Optional.ofNullable(configuredApiKey)
                .filter(key -> !key.isBlank())
                .or(() -> Optional.ofNullable(System.getenv("OPENCODE_API_KEY")))
                .filter(key -> !key.isBlank())
                .or(this::readApiKeyFromDotEnv)
                .orElse("not-required-for-free-models"));
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
