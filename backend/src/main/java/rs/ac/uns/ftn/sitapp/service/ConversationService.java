package rs.ac.uns.ftn.sitapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationParticipant;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;
import rs.ac.uns.ftn.sitapp.domain.Message;
import rs.ac.uns.ftn.sitapp.domain.User;
import rs.ac.uns.ftn.sitapp.dto.ConversationDetailsResponse;
import rs.ac.uns.ftn.sitapp.dto.ConversationSummaryResponse;
import rs.ac.uns.ftn.sitapp.dto.DirectConversationResponse;
import rs.ac.uns.ftn.sitapp.dto.MessageResponse;
import rs.ac.uns.ftn.sitapp.dto.UserResponse;
import rs.ac.uns.ftn.sitapp.repository.ConversationParticipantRepository;
import rs.ac.uns.ftn.sitapp.repository.ConversationRepository;
import rs.ac.uns.ftn.sitapp.repository.MessageRepository;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private static final String GROUP_FALLBACK_TITLE = "Grupni razgovor";

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
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
    public ConversationDetailsResponse getConversationDetails(
            Long conversationId,
            Long currentUserId
    ) {
        validatePositiveId(conversationId, "conversationId");
        validatePositiveId(currentUserId, "currentUserId");
        requireUser(currentUserId);
        Conversation conversation = requireConversation(conversationId);

        List<ConversationParticipant> participants = participantRepository
                .findByConversationIdOrderById(conversationId);
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

        List<User> participantUsers = participants.stream()
                .map(ConversationParticipant::getUser)
                .toList();
        User otherUser = null;
        String title = groupTitle(conversation.getTitle());
        if (conversation.getType() == ConversationType.DIRECT) {
            validateDirectParticipants(participantUsers);
            otherUser = participantUsers.stream()
                    .filter(user -> !currentUserId.equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "DIRECT conversation participants must be different"
                    ));
            title = fullName(otherUser);
        }

        return new ConversationDetailsResponse(
                conversation.getId(),
                conversation.getType(),
                title,
                otherUser == null ? null : UserResponse.from(otherUser),
                participantUsers.stream().map(UserResponse::from).toList(),
                conversation.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(Long currentUserId) {
        validatePositiveId(currentUserId, "currentUserId");
        requireUser(currentUserId);

        return conversationRepository.findAllForUserOrderByActivityDesc(currentUserId)
                .stream()
                .map(conversation -> toSummary(conversation, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, Long currentUserId) {
        validatePositiveId(conversationId, "conversationId");
        validatePositiveId(currentUserId, "currentUserId");
        requireUser(currentUserId);
        requireConversation(conversationId);
        requireParticipant(conversationId, currentUserId);

        return messageRepository.findByConversationIdOrderBySentAtAscIdAsc(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, String content) {
        validatePositiveId(conversationId, "conversationId");
        validatePositiveId(senderId, "senderId");
        validateMessageContent(content);
        User sender = requireUser(senderId);
        Conversation conversation = requireConversationForUpdate(conversationId);
        requireParticipant(conversationId, senderId);

        Message message = messageRepository.save(
                new Message(conversation, sender, content.strip())
        );
        return MessageResponse.from(message);
    }

    @Transactional
    public void markConversationRead(
            Long conversationId,
            Long currentUserId,
            Long lastSeenMessageId
    ) {
        validatePositiveId(conversationId, "conversationId");
        validatePositiveId(currentUserId, "currentUserId");
        validatePositiveId(lastSeenMessageId, "lastSeenMessageId");
        requireUser(currentUserId);
        requireConversation(conversationId);
        ConversationParticipant participant = participantRepository
                .findForUpdate(conversationId, currentUserId)
                .orElseThrow(() -> forbiddenParticipant());
        Message lastSeenMessage = messageRepository
                .findByIdAndConversationId(lastSeenMessageId, conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "lastSeenMessageId does not belong to this conversation"
                ));
        Long currentMarker = participant.getLastReadMessageId();
        if (currentMarker == null || lastSeenMessageId > currentMarker) {
            participant.setLastReadMessageId(lastSeenMessageId);
            participant.setLastReadAt(lastSeenMessage.getSentAt());
        }
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

    private ConversationSummaryResponse toSummary(
            Conversation conversation,
            Long currentUserId
    ) {
        User otherUser = null;
        String title = groupTitle(conversation.getTitle());
        if (conversation.getType() == ConversationType.DIRECT) {
            List<ConversationParticipant> participants = participantRepository
                    .findByConversationIdOrderById(conversation.getId());
            List<User> participantUsers = participants.stream()
                    .map(ConversationParticipant::getUser)
                    .toList();
            validateDirectParticipants(participantUsers);
            otherUser = participantUsers.stream()
                    .filter(user -> !currentUserId.equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "DIRECT conversation participants must be different"
                    ));
            title = fullName(otherUser);
        }

        MessageResponse lastMessage = messageRepository
                .findFirstByConversationIdOrderBySentAtDescIdDesc(conversation.getId())
                .map(MessageResponse::from)
                .orElse(null);
        long unreadCount = messageRepository.countUnreadMessages(
                conversation.getId(),
                currentUserId
        );
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getType(),
                title,
                otherUser == null ? null : UserResponse.from(otherUser),
                lastMessage,
                unreadCount
        );
    }

    private void validateDirectParticipants(List<User> participants) {
        long distinctParticipantCount = participants.stream()
                .map(User::getId)
                .distinct()
                .count();
        if (participants.size() != 2 || distinctParticipantCount != 2) {
            throw new IllegalStateException(
                    "DIRECT conversation must have exactly two different participants"
            );
        }
    }

    private String groupTitle(String title) {
        return title == null || title.isBlank() ? GROUP_FALLBACK_TITLE : title;
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> userNotFound(userId));
    }

    private Conversation requireConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation " + conversationId + " not found"
                ));
    }

    private Conversation requireConversationForUpdate(Long conversationId) {
        return conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation " + conversationId + " not found"
                ));
    }

    private ConversationParticipant requireParticipant(Long conversationId, Long userId) {
        return participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> forbiddenParticipant());
    }

    private ResponseStatusException forbiddenParticipant() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "User is not a participant in this conversation"
        );
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

    private void validateMessageContent(String content) {
        if (content == null || content.isBlank() || content.length() > 4000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "content must contain between 1 and 4000 non-blank characters"
            );
        }
    }
}
