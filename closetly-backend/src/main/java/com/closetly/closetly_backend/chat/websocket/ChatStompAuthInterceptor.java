package com.closetly.closetly_backend.chat.websocket;

import com.closetly.closetly_backend.chat.service.ChatService;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class ChatStompAuthInterceptor implements ChannelInterceptor {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/chat/")) {
                Long chatRoomId = parseRoomId(destination);

                Principal principal = accessor.getUser();
                if (principal == null) {
                    throw new AccessDeniedException("Unauthorized");
                }

                String email = principal.getName();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new AccessDeniedException("Invalid user"));

                chatService.assertUserCanAccessRoom(chatRoomId, user.getId());
            }
        }

        return message;
    }

    private Long parseRoomId(String destination) {
        // Expected: /topic/chat/{chatRoomId}
        String[] parts = destination.split("/");
        String last = parts[parts.length - 1];
        return Long.parseLong(last);
    }
}

