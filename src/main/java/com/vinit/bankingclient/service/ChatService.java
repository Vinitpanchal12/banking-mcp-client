package com.vinit.bankingclient.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    public ChatService(ChatClient chatClient,
                       ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClient;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    public String chat(String prompt) {

        return chatClient.prompt()
                .user(prompt)
                .tools(toolCallbackProvider)
                .call()
                .content();
    }
}