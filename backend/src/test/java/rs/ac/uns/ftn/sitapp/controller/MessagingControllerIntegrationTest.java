package rs.ac.uns.ftn.sitapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class MessagingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private MessageRepository messageRepository;

    private User currentUser;
    private User otherUser;
    private User outsideUser;

    @BeforeEach
    void setUp() {
        currentUser = userRepository.save(
                new User("messaging.current", "Current", "User", "+381661000001")
        );
        otherUser = userRepository.save(
                new User("messaging.other", "Other", "User", "+381661000002")
        );
        outsideUser = userRepository.save(
                new User("messaging.outside", "Outside", "User", "+381661000003")
        );
        userRepository.flush();
    }

    @Test
    void sendsTrimmedMessagesAndLoadsThemChronologicallyForParticipants() throws Exception {
        Conversation conversation = saveConversation(
                ConversationType.DIRECT,
                null,
                currentUser,
                otherUser
        );

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageRequest(currentUser.getId(), "  Hello\\nworld  ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversation.getId()))
                .andExpect(jsonPath("$.sender.id").value(currentUser.getId()))
                .andExpect(jsonPath("$.content").value("Hello\nworld"))
                .andExpect(jsonPath("$.sentAt").isString());

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageRequest(otherUser.getId(), "Second")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId())
                        .param("currentUserId", currentUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Hello\nworld"))
                .andExpect(jsonPath("$[1].content").value("Second"));

        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId())
                        .param("currentUserId", outsideUser.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageRequest(outsideUser.getId(), "Forbidden")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidMessagePayloads() throws Exception {
        Conversation conversation = saveConversation(
                ConversationType.DIRECT,
                null,
                currentUser,
                otherUser
        );

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageRequest(currentUser.getId(), "   ")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageRequest(currentUser.getId(), "x".repeat(4001))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageRequest(0L, "Hello")))
                .andExpect(status().isBadRequest());

        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void listsDirectAndGroupConversationsByActivityWithLastMessageAndUnreadCount()
            throws Exception {
        Conversation olderDirect = saveConversation(
                ConversationType.DIRECT,
                null,
                currentUser,
                otherUser
        );
        Conversation newerDirect = saveConversation(
                ConversationType.DIRECT,
                null,
                currentUser,
                outsideUser
        );
        Conversation group = saveConversation(
                ConversationType.GROUP,
                "Project team",
                currentUser,
                otherUser,
                outsideUser
        );
        saveMessage(
                olderDirect,
                otherUser,
                "Incoming direct",
                Instant.parse("2026-08-24T10:00:00Z")
        );
        saveMessage(
                olderDirect,
                currentUser,
                "Own follow-up",
                Instant.parse("2026-08-24T10:30:00Z")
        );
        saveMessage(
                newerDirect,
                outsideUser,
                "Newer direct",
                Instant.parse("2026-08-24T11:00:00Z")
        );
        saveMessage(
                group,
                otherUser,
                "Group activity",
                Instant.parse("2026-08-24T12:00:00Z")
        );

        mockMvc.perform(get("/api/conversations")
                        .param("currentUserId", currentUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(group.getId()))
                .andExpect(jsonPath("$[0].type").value("GROUP"))
                .andExpect(jsonPath("$[0].title").value("Project team"))
                .andExpect(jsonPath("$[0].otherUser").doesNotExist())
                .andExpect(jsonPath("$[0].unreadCount").value(1))
                .andExpect(jsonPath("$[1].id").value(newerDirect.getId()))
                .andExpect(jsonPath("$[1].title").value("Outside User"))
                .andExpect(jsonPath("$[1].otherUser.id").value(outsideUser.getId()))
                .andExpect(jsonPath("$[1].unreadCount").value(1))
                .andExpect(jsonPath("$[2].id").value(olderDirect.getId()))
                .andExpect(jsonPath("$[2].title").value("Other User"))
                .andExpect(jsonPath("$[2].lastMessage.content").value("Own follow-up"))
                .andExpect(jsonPath("$[2].unreadCount").value(1));
    }

    @Test
    void marksOnlyClientLastSeenMessageAndNeverMovesMarkerBackward() throws Exception {
        Conversation conversation = saveConversation(
                ConversationType.DIRECT,
                null,
                currentUser,
                otherUser
        );
        Message first = saveMessage(
                conversation,
                otherUser,
                "First",
                Instant.parse("2026-08-24T10:00:00Z")
        );
        Message lastSeen = saveMessage(
                conversation,
                otherUser,
                "Last seen",
                Instant.parse("2026-08-24T11:00:00Z")
        );

        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId())
                        .param("currentUserId", currentUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        Message arrivedAfterGet = saveMessage(
                conversation,
                otherUser,
                "Arrived after GET",
                Instant.parse("2026-08-24T12:00:00Z")
        );
        mockMvc.perform(put("/api/conversations/{id}/read", conversation.getId())
                        .param("currentUserId", currentUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(readRequest(lastSeen.getId())))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversation.getId(), currentUser.getId())
                .orElseThrow();
        assertThat(participant.getLastReadMessageId()).isEqualTo(lastSeen.getId());
        assertThat(participant.getLastReadAt()).isEqualTo(lastSeen.getSentAt());
        assertThat(messageRepository.countUnreadMessages(
                conversation.getId(),
                currentUser.getId()
        )).isEqualTo(1);

        mockMvc.perform(put("/api/conversations/{id}/read", conversation.getId())
                        .param("currentUserId", currentUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(readRequest(first.getId())))
                .andExpect(status().isNoContent());
        assertThat(participant.getLastReadMessageId()).isEqualTo(lastSeen.getId());

        mockMvc.perform(put("/api/conversations/{id}/read", conversation.getId())
                        .param("currentUserId", currentUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(readRequest(arrivedAfterGet.getId())))
                .andExpect(status().isNoContent());
        assertThat(messageRepository.countUnreadMessages(
                conversation.getId(),
                currentUser.getId()
        )).isZero();
    }

    @Test
    void rejectsReadMarkerFromAnotherConversationAndNonParticipant() throws Exception {
        Conversation target = saveConversation(
                ConversationType.DIRECT,
                null,
                currentUser,
                otherUser
        );
        Conversation another = saveConversation(
                ConversationType.DIRECT,
                null,
                otherUser,
                outsideUser
        );
        Message wrongConversationMessage = saveMessage(
                another,
                otherUser,
                "Wrong conversation",
                Instant.parse("2026-08-24T10:00:00Z")
        );
        Message targetMessage = saveMessage(
                target,
                otherUser,
                "Target",
                Instant.parse("2026-08-24T11:00:00Z")
        );

        mockMvc.perform(put("/api/conversations/{id}/read", target.getId())
                        .param("currentUserId", currentUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(readRequest(wrongConversationMessage.getId())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/conversations/{id}/read", target.getId())
                        .param("currentUserId", outsideUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(readRequest(targetMessage.getId())))
                .andExpect(status().isForbidden());
    }

    private Conversation saveConversation(
            ConversationType type,
            String title,
            User... users
    ) {
        Conversation conversation = conversationRepository.save(new Conversation(type, title));
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
            String messageContent,
            Instant sentAt
    ) {
        Message message = new Message(conversation, sender, messageContent);
        ReflectionTestUtils.setField(message, "sentAt", sentAt);
        return messageRepository.saveAndFlush(message);
    }

    private String messageRequest(Long senderId, String messageContent) {
        return """
                {
                  "senderId": %d,
                  "content": "%s"
                }
                """.formatted(senderId, messageContent);
    }

    private String readRequest(Long lastSeenMessageId) {
        return """
                {
                  "lastSeenMessageId": %d
                }
                """.formatted(lastSeenMessageId);
    }
}
