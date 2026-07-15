package com.vinit.bankingclient.controller;

import com.vinit.bankingclient.dto.ChatRequest;
import com.vinit.bankingclient.dto.ChatResponse;
import com.vinit.bankingclient.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody @Valid ChatRequest request) {

        String response = chatService.chat(request.message());

        return new ChatResponse(response);

    }

}