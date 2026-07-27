package com.vinit.bankingclient.controller;

import com.vinit.bankingclient.dto.ChatRequest;
import com.vinit.bankingclient.dto.ChatResponse;
import com.vinit.bankingclient.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final RestClient restClient;

    public ChatController(ChatService chatService, RestClient.Builder builder) {
        this.chatService = chatService;
        this.restClient = builder.build();
    }

    @PostMapping
    public ChatResponse chat(@RequestBody @Valid ChatRequest request) {

        String response = chatService.chat(request.message(), request.model());

        return new ChatResponse(response);

    }



}
