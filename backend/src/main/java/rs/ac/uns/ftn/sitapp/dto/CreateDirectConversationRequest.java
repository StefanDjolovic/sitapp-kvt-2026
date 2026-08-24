package rs.ac.uns.ftn.sitapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateDirectConversationRequest(
        @NotNull @Positive Long currentUserId,
        @NotNull @Positive Long otherUserId
) {
}
