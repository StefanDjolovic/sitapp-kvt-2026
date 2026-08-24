package rs.ac.uns.ftn.sitapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MarkConversationReadRequest(
        @NotNull @Positive Long lastSeenMessageId
) {
}
