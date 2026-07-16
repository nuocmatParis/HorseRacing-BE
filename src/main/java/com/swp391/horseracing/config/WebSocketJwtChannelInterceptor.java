package com.swp391.horseracing.config;

import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceRepository;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {
    private static final Pattern PUBLIC_RACE_TOPIC = Pattern.compile("^/topic/races/[0-9a-fA-F-]{36}/live$");
    private static final Pattern PRIVATE_RACE_QUEUE = Pattern.compile("^/user/queue/races/([0-9a-fA-F-]{36})/control$");

    JwtDecoder jwtDecoder;
    UserRepository userRepository;
    RefereeRepository refereeRepository;
    RaceRefereeRepository raceRefereeRepository;
    RaceRepository raceRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                // Anonymous connections are valid for public live-race topics.
                return message;
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
        if (destination != null && PUBLIC_RACE_TOPIC.matcher(destination).matches()) {
            return;
        }
        if (accessor.getUser() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Authentication is required for private WebSocket destinations");
        }
        if ("/user/queue/notifications".equals(destination)) {
            return;
        }
        Matcher matcher = PRIVATE_RACE_QUEUE.matcher(destination == null ? "" : destination);
        if (!matcher.matches() || !(accessor.getUser() instanceof Authentication authentication)
                || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_REFEREE".equals(authority.getAuthority()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This WebSocket destination is not allowed");
        }
        UUID raceId = UUID.fromString(matcher.group(1));
        UUID userId = UUID.fromString(accessor.getUser().getName());
        var referee = refereeRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Referee profile not found"));
        boolean assigned = raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                raceId, referee.getRefereeId());
        boolean headReferee = raceRepository.existsByRaceIdAndRound_HeadReferee_User_UserId(raceId, userId);
        if (!assigned && !headReferee) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Referee is not assigned to this race");
        }
    }
}
