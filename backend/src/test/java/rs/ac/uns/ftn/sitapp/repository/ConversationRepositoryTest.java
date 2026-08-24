package rs.ac.uns.ftn.sitapp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationParticipant;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;
import rs.ac.uns.ftn.sitapp.domain.Message;
import rs.ac.uns.ftn.sitapp.domain.User;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ConversationRepositoryTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    private User firstUser;
    private User secondUser;
    private User thirdUser;

    @BeforeEach
    void setUp() {
        firstUser = userRepository.save(
                new User("first.user", "First", "User", "+381611111111")
        );
        secondUser = userRepository.save(
                new User("second.user", "Second", "User", "+381622222222")
        );
        thirdUser = userRepository.save(
                new User("third.user", "Third", "User", "+381633333333")
        );
        userRepository.flush();
    }

    @Test
    void findsDirectConversationForPairInEitherOrder() {
        Conversation conversation = saveConversation(
                ConversationType.DIRECT,
                firstUser,
                secondUser
        );

        var forward = conversationRepository.findBetweenUsers(
                ConversationType.DIRECT,
                firstUser.getId(),
                secondUser.getId()
        );
        var reversed = conversationRepository.findBetweenUsers(
                ConversationType.DIRECT,
                secondUser.getId(),
                firstUser.getId()
        );

        assertThat(forward).map(Conversation::getId).contains(conversation.getId());
        assertThat(reversed).map(Conversation::getId).contains(conversation.getId());
    }

    @Test
    void ignoresGroupsAndConversationsWithAdditionalParticipants() {
        saveConversation(ConversationType.GROUP, firstUser, secondUser);
        saveConversation(ConversationType.DIRECT, firstUser, secondUser, thirdUser);

        var result = conversationRepository.findBetweenUsers(
                ConversationType.DIRECT,
                firstUser.getId(),
                secondUser.getId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void ordersAllUserConversationsByLastMessageOrCreationTime() {
        Conversation activeConversation = saveConversationAt(
                ConversationType.DIRECT,
                Instant.parse("2026-08-24T08:00:00Z"),
                firstUser,
                secondUser
        );
        Conversation conversationWithoutMessages = saveConversationAt(
                ConversationType.DIRECT,
                Instant.parse("2026-08-24T09:00:00Z"),
                firstUser,
                thirdUser
        );
        Conversation unrelatedConversation = saveConversationAt(
                ConversationType.DIRECT,
                Instant.parse("2026-08-24T11:00:00Z"),
                secondUser,
                thirdUser
        );
        Conversation groupConversation = saveConversationAt(
                ConversationType.GROUP,
                Instant.parse("2026-08-24T13:00:00Z"),
                firstUser,
                secondUser,
                thirdUser
        );
        saveMessage(
                activeConversation,
                secondUser,
                "Most recent activity",
                Instant.parse("2026-08-24T10:00:00Z")
        );
        saveMessage(
                unrelatedConversation,
                thirdUser,
                "Not visible to first user",
                Instant.parse("2026-08-24T12:00:00Z")
        );
        saveMessage(
                groupConversation,
                secondUser,
                "Group activity",
                Instant.parse("2026-08-24T14:00:00Z")
        );

        var result = conversationRepository.findAllForUserOrderByActivityDesc(
                firstUser.getId()
        );

        assertThat(result)
                .extracting(Conversation::getId)
                .containsExactly(
                        groupConversation.getId(),
                        activeConversation.getId(),
                        conversationWithoutMessages.getId()
                );
    }

    private Conversation saveConversation(ConversationType type, User... users) {
        return saveConversationAt(type, null, users);
    }

    private Conversation saveConversationAt(
            ConversationType type,
            Instant createdAt,
            User... users
    ) {
        Conversation unsavedConversation = new Conversation(
                type,
                type == ConversationType.GROUP ? "Group" : null
        );
        if (createdAt != null) {
            ReflectionTestUtils.setField(unsavedConversation, "createdAt", createdAt);
        }
        Conversation conversation = conversationRepository.save(
                unsavedConversation
        );
        List<ConversationParticipant> participants = List.of(users)
                .stream()
                .map(user -> new ConversationParticipant(conversation, user))
                .toList();
        participantRepository.saveAllAndFlush(participants);
        return conversation;
    }

    private Message saveMessage(
            Conversation conversation,
            User sender,
            String content,
            Instant sentAt
    ) {
        Message message = new Message(conversation, sender, content);
        ReflectionTestUtils.setField(message, "sentAt", sentAt);
        return messageRepository.saveAndFlush(message);
    }
}
