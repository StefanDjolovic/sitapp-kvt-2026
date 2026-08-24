package rs.ac.uns.ftn.sitapp.dto;

import rs.ac.uns.ftn.sitapp.domain.ConversationType;

import java.time.Instant;
import java.util.List;

public record ConversationDetailsResponse(
        Long id,
        ConversationType type,
        String title,
        UserResponse otherUser,
        List<UserResponse> participants,
        Instant createdAt
) {
}
