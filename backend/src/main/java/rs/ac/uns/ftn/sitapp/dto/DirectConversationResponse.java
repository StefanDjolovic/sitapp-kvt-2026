package rs.ac.uns.ftn.sitapp.dto;

import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.User;

import java.time.Instant;

public record DirectConversationResponse(
        Long id,
        UserResponse otherUser,
        Instant createdAt
) {

    public static DirectConversationResponse from(Conversation conversation, User otherUser) {
        return new DirectConversationResponse(
                conversation.getId(),
                UserResponse.from(otherUser),
                conversation.getCreatedAt()
        );
    }
}
