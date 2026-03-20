package com.closetly.closetly_backend.security.websocket;

import com.closetly.closetly_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private static final String AUTH_CACHE_KEY = "WS_AUTHENTICATION";

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    // STOMP sessionId -> Authentication
    private final ConcurrentMap<String, Authentication> sessionAuthCache = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            // `simpSessionId` is the stable identifier across all STOMP frames.
            Object headerSessionId = message.getHeaders().get(SimpMessageHeaderAccessor.SESSION_ID_HEADER);
            if (headerSessionId instanceof String s) {
                sessionId = s;
            }
        }

        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            String token = extractBearerTokenFromConnect(accessor);
            log.info("WS CONNECT received. sessionId={}", sessionId);

            if (token == null || token.isBlank()) {
                throw new AccessDeniedException("Missing Authorization bearer token in STOMP CONNECT headers");
            }
            if (!tokenProvider.validateToken(token)) {
                throw new AccessDeniedException("Invalid JWT in STOMP CONNECT headers");
            }

            String username = tokenProvider.getUsernameFromJWT(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            accessor.setUser(authentication);
            if (sessionId != null) {
                sessionAuthCache.put(sessionId, authentication);
            }

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put(AUTH_CACHE_KEY, authentication);
            }
            log.info("WS CONNECT authenticated user={}", username);
            return message;
        }

        // For all subsequent frames, re-attach principal based on sessionId.
        java.security.Principal existingPrincipal = accessor.getUser();
        String existingName = existingPrincipal != null ? existingPrincipal.getName() : null;

        boolean principalMissingOrBlank = existingPrincipal == null
                || existingName == null
                || existingName.isBlank();

        Authentication resolved = null;
        if (principalMissingOrBlank && sessionId != null) {
            resolved = sessionAuthCache.get(sessionId);
        }

        if (resolved == null) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Object cached = sessionAttributes.get(AUTH_CACHE_KEY);
                if (cached instanceof Authentication auth) {
                    resolved = auth;
                }
            }
        }

        if (resolved != null) {
            accessor.setUser(resolved);
            SecurityContextHolder.getContext().setAuthentication(resolved);
        }

        java.security.Principal finalPrincipal = accessor.getUser();
        String finalName = finalPrincipal != null ? finalPrincipal.getName() : null;
        log.info("WS {} sessionId={} principalName={}", command, sessionId, finalName);

        // Cleanup on disconnect.
        if (StompCommand.DISCONNECT.equals(command) && sessionId != null) {
            sessionAuthCache.remove(sessionId);
            log.info("WS DISCONNECT sessionId={}", sessionId);
        }

        return message;
    }

    private String extractBearerTokenFromConnect(StompHeaderAccessor accessor) {
        // Native STOMP headers are case-sensitive sometimes; check both.
        String header = Optional.ofNullable(accessor.getFirstNativeHeader("Authorization"))
                .orElseGet(() -> accessor.getFirstNativeHeader("authorization"));

        if (header == null || header.isBlank()) {
            return null;
        }

        String value = header.trim();
        if (value.startsWith("Bearer ")) {
            return value.substring(7);
        }
        return null;
    }
}