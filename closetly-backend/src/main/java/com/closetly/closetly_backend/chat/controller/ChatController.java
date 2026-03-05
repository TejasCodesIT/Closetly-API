package com.closetly.closetly_backend.chat.controller;

import com.closetly.closetly_backend.chat.dto.MessageDTO;
import com.closetly.closetly_backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<MessageDTO> send(@Valid @RequestBody MessageDTO dto) {
        return ResponseEntity.ok(chatService.sendMessage(dto));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<MessageDTO>> messages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getMessages(roomId));
    }
}
