package com.closetly.closetly_backend.security.websocket;

import com.closetly.closetly_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;

    // ✅ Static map to store auth by session ID
    private static final ConcurrentHashMap<String, UsernamePasswordAuthenticationToken> authStore = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() == null) {
            return message;
        }

        // ✅ If already authenticated → continue
        if (accessor.getUser() != null) {
            return message;
        }

        // ✅ CONNECT → authenticate and store token
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

            // ✅ Create proper Authentication object
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            accessor.setUser(auth);

            // ✅ Store in static map by session ID
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                authStore.put(sessionId, auth);
            }

            // ✅ Also store in session attributes as backup
            accessor.getSessionAttributes().put("jwt_token", token);
            accessor.getSessionAttributes().put("auth_user", auth);

        }

        // ✅ SEND/SUBSCRIBE → restore from static map
        else if (StompCommand.SEND.equals(accessor.getCommand()) ||
                StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {

            String sessionId = accessor.getSessionId();

            UsernamePasswordAuthenticationToken auth = null;

            // ✅ Try static map first
            if (sessionId != null) {
                auth = authStore.get(sessionId);
            }

            // ✅ Fallback to session attributes
            if (auth == null) {
                auth = (UsernamePasswordAuthenticationToken) accessor.getSessionAttributes().get("auth_user");
            }

            if (auth != null) {
                accessor.setUser(auth);
            } else {
                throw new RuntimeException("Authentication required");
            }
        }

        // ✅ DISCONNECT → cleanup
        else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                authStore.remove(sessionId);
            }
        }

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}