package com.closetly.closetly_backend.security.websocket;

import com.closetly.closetly_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.security.Principal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;

    
@Override
public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {

    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

    System.out.println("🔥 COMMAND: " + accessor.getCommand());
    System.out.println("🔥 USER BEFORE: " + accessor.getUser());

    if (accessor.getCommand() == null) {
        return message;
    }

    // ✅ If already present → just continue
    if (accessor.getUser() != null) {
        return message;
    }

    // ✅ CONNECT → authenticate
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {

        String rawAuthorization = Optional.ofNullable(accessor.getFirstNativeHeader("Authorization"))
                .orElse(accessor.getFirstNativeHeader("authorization"));

        if (rawAuthorization == null || !rawAuthorization.startsWith("Bearer ")) {
            throw new RuntimeException("Missing Authorization header");
        }

        String token = rawAuthorization.substring(7);

        if (!tokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }

        String username = tokenProvider.getUsernameFromJWT(token);

        Principal user = () -> username;

        accessor.setUser(user);

        // ✅ STORE IN SESSION
        accessor.getSessionAttributes().put("user", user);

        System.out.println("✅ WS CONNECT user = " + username);
    }

    // ✅ SEND → restore user
    else {
        Principal user = (Principal) accessor.getSessionAttributes().get("user");

        if (user != null) {
            accessor.setUser(user);
            System.out.println("🔁 USER RESTORED: " + user.getName());
        }
    }

    // 💥 CRITICAL LINE (THIS FIXES YOUR ISSUE)
    return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
}
}