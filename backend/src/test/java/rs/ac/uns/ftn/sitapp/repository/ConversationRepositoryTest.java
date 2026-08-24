package rs.ac.uns.ftn.sitapp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationParticipant;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;
import rs.ac.uns.ftn.sitapp.domain.User;

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

    private Conversation saveConversation(ConversationType type, User... users) {
        Conversation conversation = conversationRepository.save(
                new Conversation(type, type == ConversationType.GROUP ? "Group" : null)
        );
        List<ConversationParticipant> participants = List.of(users)
                .stream()
                .map(user -> new ConversationParticipant(conversation, user))
                .toList();
        participantRepository.saveAllAndFlush(participants);
        return conversation;
    }
}
