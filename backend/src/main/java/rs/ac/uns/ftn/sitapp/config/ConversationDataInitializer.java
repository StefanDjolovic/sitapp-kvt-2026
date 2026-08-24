package rs.ac.uns.ftn.sitapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationParticipant;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;
import rs.ac.uns.ftn.sitapp.domain.Message;
import rs.ac.uns.ftn.sitapp.domain.User;
import rs.ac.uns.ftn.sitapp.repository.ConversationParticipantRepository;
import rs.ac.uns.ftn.sitapp.repository.ConversationRepository;
import rs.ac.uns.ftn.sitapp.repository.MessageRepository;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.util.List;

@Component
@Profile("dev")
@Order(2)
public class ConversationDataInitializer implements ApplicationRunner {

    static final String GROUP_TITLE = "KVT grupa";
    static final String INITIAL_MESSAGE = "Dobrodošli u KVT grupu!";
    private static final List<String> PARTICIPANT_USERNAMES = List.of(
            "ana.petrovic",
            "marko.jovanovic",
            "jelena.nikolic"
    );

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;

    public ConversationDataInitializer(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository
    ) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> participants = PARTICIPANT_USERNAMES.stream()
                .map(this::requireUser)
                .toList();
        Conversation conversation = conversationRepository
                .findFirstByTypeAndTitle(ConversationType.GROUP, GROUP_TITLE)
                .orElseGet(() -> conversationRepository.save(
                        new Conversation(ConversationType.GROUP, GROUP_TITLE)
                ));

        List<ConversationParticipant> missingParticipants = participants.stream()
                .filter(user -> participantRepository
                        .findByConversationIdAndUserId(conversation.getId(), user.getId())
                        .isEmpty())
                .map(user -> new ConversationParticipant(conversation, user))
                .toList();
        if (!missingParticipants.isEmpty()) {
            participantRepository.saveAll(missingParticipants);
        }

        User marko = participants.stream()
                .filter(user -> "marko.jovanovic".equals(user.getUsername()))
                .findFirst()
                .orElseThrow();
        if (!messageRepository.existsByConversationIdAndSenderIdAndContent(
                conversation.getId(),
                marko.getId(),
                INITIAL_MESSAGE
        )) {
            messageRepository.save(new Message(conversation, marko, INITIAL_MESSAGE));
        }
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Predefined user " + username + " was not initialized"
                ));
    }
}
