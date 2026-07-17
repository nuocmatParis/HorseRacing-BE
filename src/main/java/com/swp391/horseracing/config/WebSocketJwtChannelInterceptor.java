package com.swp391.horseracing.config;

import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {
    JwtDecoder jwtDecoder;
    UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Authentication is required for WebSocket connections");
            }
            Jwt jwt = jwtDecoder.decode(authorization.substring(7));
            User user = userRepository.findByUsername(jwt.getSubject())
                    .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("User not found"));
            if (user.getStatus() != AccountStatus.ACTIVE) {
                throw new org.springframework.security.access.AccessDeniedException("User is not active");
            }
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                    "ROLE_" + user.getRole().getRoleName().name());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user.getUserId().toString(), null, List.of(authority));
            accessor.setUser(authentication);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateSubscription(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Sending STOMP messages is not allowed");
        }
        return message;
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (accessor.getUser() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Authentication is required for private WebSocket destinations");
        }
        if ("/user/queue/notifications".equals(destination)) {
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "This WebSocket destination is not allowed");
    }
}
