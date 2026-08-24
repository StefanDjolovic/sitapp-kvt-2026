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
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    private User currentUser;
    private User otherUser;
    private Conversation conversation;
    private ConversationParticipant currentParticipant;

    @BeforeEach
    void setUp() {
        currentUser = userRepository.save(
                new User("message.current", "Current", "User", "+381651000001")
        );
        otherUser = userRepository.save(
                new User("message.other", "Other", "User", "+381651000002")
        );
        conversation = conversationRepository.save(
                new Conversation(ConversationType.DIRECT, null)
        );
        currentParticipant = participantRepository.save(
                new ConversationParticipant(conversation, currentUser)
        );
        participantRepository.save(new ConversationParticipant(conversation, otherUser));
        participantRepository.flush();
    }

    @Test
    void returnsMessagesChronologicallyAndFindsLatestByIdOnTimeTie() {
        Instant earlier = Instant.parse("2026-08-24T10:00:00Z");
        Instant later = Instant.parse("2026-08-24T11:00:00Z");
        Message laterMessage = saveMessage(otherUser, "Later", later);
        Message earlierMessage = saveMessage(currentUser, "Earlier", earlier);
        Message latestOnTie = saveMessage(currentUser, "Latest on tie", later);

        var chronological = messageRepository
                .findByConversationIdOrderBySentAtAscIdAsc(conversation.getId());
        var latest = messageRepository
                .findFirstByConversationIdOrderBySentAtDescIdDesc(conversation.getId());

        assertThat(chronological)
                .extracting(Message::getId)
                .containsExactly(earlierMessage.getId(), laterMessage.getId(), latestOnTie.getId());
        assertThat(latest).map(Message::getId).contains(latestOnTie.getId());
    }

    @Test
    void countsOnlyUnreadMessagesFromOtherUsersAfterMessageMarker() {
        Message alreadyRead = saveMessage(
                otherUser,
                "Already read",
                Instant.parse("2026-08-24T10:00:00Z")
        );
        saveMessage(
                currentUser,
                "Own message",
                Instant.parse("2026-08-24T10:01:00Z")
        );
        Message unread = saveMessage(
                otherUser,
                "Unread",
                Instant.parse("2026-08-24T10:02:00Z")
        );
        currentParticipant.setLastReadMessageId(alreadyRead.getId());
        participantRepository.saveAndFlush(currentParticipant);

        long unreadAfterMarker = messageRepository.countUnreadMessages(
                conversation.getId(),
                currentUser.getId()
        );
        currentParticipant.setLastReadMessageId(unread.getId());
        participantRepository.saveAndFlush(currentParticipant);
        long unreadAfterLatest = messageRepository.countUnreadMessages(
                conversation.getId(),
                currentUser.getId()
        );

        assertThat(unreadAfterMarker).isEqualTo(1);
        assertThat(unreadAfterLatest).isZero();
    }

    private Message saveMessage(User sender, String content, Instant sentAt) {
        Message message = new Message(conversation, sender, content);
        ReflectionTestUtils.setField(message, "sentAt", sentAt);
        return messageRepository.saveAndFlush(message);
    }
}
