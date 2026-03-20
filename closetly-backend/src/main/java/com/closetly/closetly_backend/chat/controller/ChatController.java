package com.closetly.closetly_backend.chat.controller;

import com.closetly.closetly_backend.chat.dto.ChatRoomDTO;
import com.closetly.closetly_backend.chat.dto.CreateChatRoomRequestDTO;
import com.closetly.closetly_backend.chat.dto.MessageDTO;
import com.closetly.closetly_backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<MessageDTO> send(@Valid @RequestBody MessageDTO dto, Authentication authentication) {
        return ResponseEntity.ok(chatService.sendMessage(dto, authentication.getName()));
    }

    @PostMapping("/room")
    public ResponseEntity<ChatRoomDTO> createRoom(@Valid @RequestBody CreateChatRoomRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(chatService.createOrGetRoom(request, authentication.getName()));
    }

    @GetMapping("/user")
    public ResponseEntity<List<ChatRoomDTO>> myRooms(Authentication authentication) {
        return ResponseEntity.ok(chatService.getMyRooms(authentication.getName()));
    }

    @GetMapping("/room/{chatRoomId}")
    public ResponseEntity<List<MessageDTO>> messages(@PathVariable Long chatRoomId, Authentication authentication) {
        return ResponseEntity.ok(chatService.getMessages(chatRoomId, authentication.getName()));
    }
}
