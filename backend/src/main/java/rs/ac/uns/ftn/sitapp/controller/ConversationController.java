package rs.ac.uns.ftn.sitapp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import rs.ac.uns.ftn.sitapp.dto.ConversationSummaryResponse;
import rs.ac.uns.ftn.sitapp.dto.CreateDirectConversationRequest;
import rs.ac.uns.ftn.sitapp.dto.CreateMessageRequest;
import rs.ac.uns.ftn.sitapp.dto.DirectConversationResponse;
import rs.ac.uns.ftn.sitapp.dto.MarkConversationReadRequest;
import rs.ac.uns.ftn.sitapp.dto.MessageResponse;
import rs.ac.uns.ftn.sitapp.service.ConversationService;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<ConversationSummaryResponse> getConversations(
            @RequestParam @Positive Long currentUserId
    ) {
        return conversationService.getConversations(currentUserId);
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

    @GetMapping("/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable @Positive Long conversationId,
            @RequestParam @Positive Long currentUserId
    ) {
        return conversationService.getMessages(conversationId, currentUserId);
    }

    @PostMapping("/{conversationId}/messages")
    public MessageResponse sendMessage(
            @PathVariable @Positive Long conversationId,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        return conversationService.sendMessage(
                conversationId,
                request.senderId(),
                request.content()
        );
    }

    @PutMapping("/{conversationId}/read")
    public ResponseEntity<Void> markConversationRead(
            @PathVariable @Positive Long conversationId,
            @RequestParam @Positive Long currentUserId,
            @Valid @RequestBody MarkConversationReadRequest request
    ) {
        conversationService.markConversationRead(
                conversationId,
                currentUserId,
                request.lastSeenMessageId()
        );
        return ResponseEntity.noContent().build();
    }
}
