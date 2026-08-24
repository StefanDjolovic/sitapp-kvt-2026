package rs.ac.uns.ftn.sitapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;
import rs.ac.uns.ftn.sitapp.repository.ConversationParticipantRepository;
import rs.ac.uns.ftn.sitapp.repository.ConversationRepository;
import rs.ac.uns.ftn.sitapp.repository.MessageRepository;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Transactional
class ConversationDataInitializerTest {

    @Autowired
    private ConversationDataInitializer initializer;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsPredefinedGroupAndDoesNotDuplicateItOnRestart() {
        Conversation group = conversationRepository
                .findFirstByTypeAndTitle(
                        ConversationType.GROUP,
                        ConversationDataInitializer.GROUP_TITLE
                )
                .orElseThrow();
        long conversationCount = conversationRepository.count();
        long participantCount = participantRepository.count();
        long messageCount = messageRepository.count();

        assertThat(participantRepository.findByConversationIdOrderById(group.getId()))
                .extracting(participant -> participant.getUser().getUsername())
                .containsExactly(
                        "ana.petrovic",
                        "marko.jovanovic",
                        "jelena.nikolic"
                );
        assertThat(messageRepository.findByConversationIdOrderBySentAtAscIdAsc(group.getId()))
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.getSender().getUsername()).isEqualTo("marko.jovanovic");
                    assertThat(message.getContent())
                            .isEqualTo(ConversationDataInitializer.INITIAL_MESSAGE);
                });

        initializer.run(null);

        assertThat(conversationRepository.count()).isEqualTo(conversationCount);
        assertThat(participantRepository.count()).isEqualTo(participantCount);
        assertThat(messageRepository.count()).isEqualTo(messageCount);
        assertThat(userRepository.count()).isEqualTo(8);
    }
}
