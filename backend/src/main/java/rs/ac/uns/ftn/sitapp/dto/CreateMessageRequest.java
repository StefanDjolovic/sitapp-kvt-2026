package rs.ac.uns.ftn.sitapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
        @NotNull @Positive Long senderId,
        @NotBlank @Size(max = 4000) String content
) {
}
