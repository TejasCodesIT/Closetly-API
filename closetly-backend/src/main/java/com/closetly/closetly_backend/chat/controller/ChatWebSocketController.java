package com.closetly.closetly_backend.chat.controller;

import com.closetly.closetly_backend.chat.dto.MessageDTO;
import com.closetly.closetly_backend.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/send")
    public void sendMessage(@Valid MessageDTO message, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthorized");
        }

        String username = authentication.getName();

        log.info("🔥 FINAL USER: {}", username);

        MessageDTO saved = chatService.sendMessage(message, username);

        messagingTemplate.convertAndSend(
                "/topic/chat/" + saved.getChatRoomId(),
                saved);
    }
}