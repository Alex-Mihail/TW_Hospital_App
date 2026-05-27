package com.example.proiecttw.hospital.controller;

import com.example.proiecttw.hospital.dto.ChatRequest;
import com.example.proiecttw.hospital.dto.ChatResponse;
import com.example.proiecttw.hospital.service.AiChatService;
import com.example.proiecttw.hospital.service.AllContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:3000"})
public class ChatController {

    private final AiChatService ai;
    private final AllContextService contextService;

    public ChatController(AiChatService ai, AllContextService contextService) {
        this.ai = ai;
        this.contextService = contextService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req) {
        if (req == null || req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Mesaj lipsă."));
        }

        String role = req.role() != null ? req.role() : "PATIENT";
        Long userId = req.userId();
        var ui = req.uiContext();

        String context = contextService.buildContext(role, userId, ui);
        String answer = ai.ask(req.message().trim(), context);

        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
