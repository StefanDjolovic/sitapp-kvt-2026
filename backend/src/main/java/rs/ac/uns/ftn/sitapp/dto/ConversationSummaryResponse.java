package rs.ac.uns.ftn.sitapp.dto;

import rs.ac.uns.ftn.sitapp.domain.ConversationType;

public record ConversationSummaryResponse(
        Long id,
        ConversationType type,
        String title,
        UserResponse otherUser,
        MessageResponse lastMessage,
        long unreadCount
) {
}
