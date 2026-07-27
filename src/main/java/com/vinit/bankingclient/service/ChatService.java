package com.vinit.bankingclient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final Map<String, ChatClient> chatClients;
    private final ToolCallbackProvider toolCallbackProvider;

    public ChatService(Map<String, ChatClient> chatClients,
                       ToolCallbackProvider toolCallbackProvider) {
        this.chatClients = chatClients;
        this.toolCallbackProvider = toolCallbackProvider;
        log.info("Available ChatClients: {}", chatClients.keySet());
    }

    public String chat(String prompt, String model) {
        String key = model != null && !model.isBlank() ? model : "ollama";

        ChatClient client = chatClients.get(key + "ChatClient");
        if (client == null) {
            log.warn("Client '{}ChatClient' not found, falling back to first available", key);
            client = chatClients.values().iterator().next();
        }

        long start = System.currentTimeMillis();
        try {
            return client.prompt()
                    .user(prompt)
                    .tools(toolCallbackProvider)
                    .call()
                    .content();
        } finally {
            log.info("{} request completed in {} ms", key, System.currentTimeMillis() - start);
        }
    }

}
