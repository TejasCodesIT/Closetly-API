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

    /**
     * Send a chat message.
     * The senderId will default to the authenticated user if not provided.
     */
    @PostMapping("/message")
    public ResponseEntity<MessageDTO> send(@Valid @RequestBody MessageDTO dto, Authentication authentication) {
        return ResponseEntity.ok(chatService.sendMessage(dto, authentication.getName()));
    }

    /**
     * Create a chat room or return existing.
     * Handles both booking-based and order-based chat rooms.
     */
    @PostMapping("/room")
    public ResponseEntity<ChatRoomDTO> createRoom(@Valid @RequestBody CreateChatRoomRequestDTO request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(chatService.createOrGetRoom(request, authentication.getName()));
    }

    /**
     * Get all chat rooms for the authenticated user (buyer or seller).
     */
    @GetMapping("/user")
    public ResponseEntity<List<ChatRoomDTO>> myRooms(Authentication authentication) {
        return ResponseEntity.ok(chatService.getMyRooms(authentication.getName()));
    }

    /**
     * Get all messages for a specific chat room.
     * Access is verified by ChatService (must be participant).
     */
    @GetMapping("/room/{chatRoomId}")
    public ResponseEntity<List<MessageDTO>> messages(@PathVariable Long chatRoomId,
                                                     Authentication authentication) {
        return ResponseEntity.ok(chatService.getMessages(chatRoomId, authentication.getName()));
    }
}