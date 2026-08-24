package rs.ac.uns.ftn.sitapp.dto;

import rs.ac.uns.ftn.sitapp.domain.Message;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Long conversationId,
        UserResponse sender,
        String content,
        Instant sentAt
) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                UserResponse.from(message.getSender()),
                message.getContent(),
                message.getSentAt()
        );
    }
}
