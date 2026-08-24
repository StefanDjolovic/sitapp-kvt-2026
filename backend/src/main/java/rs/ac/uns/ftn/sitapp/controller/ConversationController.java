package rs.ac.uns.ftn.sitapp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.sitapp.dto.CreateDirectConversationRequest;
import rs.ac.uns.ftn.sitapp.dto.DirectConversationResponse;
import rs.ac.uns.ftn.sitapp.service.ConversationService;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct")
    public DirectConversationResponse getOrCreateDirectConversation(
            @Valid @RequestBody CreateDirectConversationRequest request
    ) {
        return conversationService.getOrCreateDirectConversation(
                request.currentUserId(),
                request.otherUserId()
        );
    }

    @GetMapping("/{conversationId}")
    public DirectConversationResponse getDirectConversation(
            @PathVariable @Positive Long conversationId,
            @RequestParam @Positive Long currentUserId
    ) {
        return conversationService.getDirectConversation(conversationId, currentUserId);
    }
}
