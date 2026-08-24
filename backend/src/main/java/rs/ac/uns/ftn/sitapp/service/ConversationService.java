package rs.ac.uns.ftn.sitapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationParticipant;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;
import rs.ac.uns.ftn.sitapp.domain.User;
import rs.ac.uns.ftn.sitapp.dto.DirectConversationResponse;
import rs.ac.uns.ftn.sitapp.repository.ConversationParticipantRepository;
import rs.ac.uns.ftn.sitapp.repository.ConversationRepository;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DirectConversationResponse getOrCreateDirectConversation(
            Long currentUserId,
            Long otherUserId
    ) {
        validatePositiveId(currentUserId, "currentUserId");
        validatePositiveId(otherUserId, "otherUserId");
        if (currentUserId.equals(otherUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Direct conversation requires two different users"
            );
        }

        Map<Long, User> users = userRepository
                .findAllByIdForUpdate(List.of(currentUserId, otherUserId))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        User currentUser = requireUser(users, currentUserId);
        User otherUser = requireUser(users, otherUserId);

        return conversationRepository.findBetweenUsers(
                        ConversationType.DIRECT,
                        currentUserId,
                        otherUserId
                )
                .map(conversation -> DirectConversationResponse.from(conversation, otherUser))
                .orElseGet(() -> createDirectConversation(currentUser, otherUser));
    }

    @Transactional(readOnly = true)
    public DirectConversationResponse getDirectConversation(
            Long conversationId,
            Long currentUserId
    ) {
        validatePositiveId(conversationId, "conversationId");
        validatePositiveId(currentUserId, "currentUserId");
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> userNotFound(currentUserId));
        Conversation conversation = conversationRepository.findByIdAndType(
                        conversationId,
                        ConversationType.DIRECT
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Direct conversation " + conversationId + " not found"
                ));

        List<ConversationParticipant> participants = participantRepository
                .findByConversationIdOrderById(conversationId);
        if (participants.size() != 2) {
            throw new IllegalStateException(
                    "DIRECT conversation must have exactly two participants"
            );
        }
        boolean currentUserParticipates = participants.stream()
                .map(ConversationParticipant::getUser)
                .map(User::getId)
                .anyMatch(currentUserId::equals);
        if (!currentUserParticipates) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not a participant in this conversation"
            );
        }

        User otherUser = participants.stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !currentUser.getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "DIRECT conversation participants must be different"
                ));
        return DirectConversationResponse.from(conversation, otherUser);
    }

    private DirectConversationResponse createDirectConversation(User currentUser, User otherUser) {
        Conversation conversation = conversationRepository.save(
                new Conversation(ConversationType.DIRECT, null)
        );
        participantRepository.saveAll(List.of(
                new ConversationParticipant(conversation, currentUser),
                new ConversationParticipant(conversation, otherUser)
        ));
        return DirectConversationResponse.from(conversation, otherUser);
    }

    private User requireUser(Map<Long, User> users, Long userId) {
        User user = users.get(userId);
        if (user == null) {
            throw userNotFound(userId);
        }
        return user;
    }

    private ResponseStatusException userNotFound(Long userId) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User " + userId + " not found"
        );
    }

    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " must be a positive number"
            );
        }
    }
}
