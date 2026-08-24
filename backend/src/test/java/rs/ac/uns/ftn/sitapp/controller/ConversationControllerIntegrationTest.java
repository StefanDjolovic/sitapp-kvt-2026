package rs.ac.uns.ftn.sitapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationParticipant;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;
import rs.ac.uns.ftn.sitapp.domain.User;
import rs.ac.uns.ftn.sitapp.repository.ConversationParticipantRepository;
import rs.ac.uns.ftn.sitapp.repository.ConversationRepository;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class ConversationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    private User currentUser;
    private User otherUser;
    private User outsideUser;

    @BeforeEach
    void setUp() {
        currentUser = userRepository.save(
                new User("conversation.current", "Current", "User", "+381641000001")
        );
        otherUser = userRepository.save(
                new User("conversation.other", "Other", "User", "+381641000002")
        );
        outsideUser = userRepository.save(
                new User("conversation.outside", "Outside", "User", "+381641000003")
        );
        userRepository.flush();
    }

    @Test
    void createsDirectConversationOnceAndReturnsExistingOneForSamePair() throws Exception {
        String requestBody = directRequest(currentUser.getId(), otherUser.getId());

        mockMvc.perform(post("/api/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.otherUser.id").value(otherUser.getId()))
                .andExpect(jsonPath("$.otherUser.username").value("conversation.other"))
                .andExpect(jsonPath("$.createdAt").isString());

        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(participantRepository.count()).isEqualTo(2);
        Long conversationId = conversationRepository.findAll().getFirst().getId();

        mockMvc.perform(post("/api/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(directRequest(otherUser.getId(), currentUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId))
                .andExpect(jsonPath("$.otherUser.id").value(currentUser.getId()));

        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(participantRepository.count()).isEqualTo(2);
    }

    @Test
    void returnsConversationFromParticipantPerspectiveAndForbidsOutsider() throws Exception {
        mockMvc.perform(post("/api/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(directRequest(currentUser.getId(), otherUser.getId())))
                .andExpect(status().isOk());
        Long conversationId = conversationRepository.findAll().getFirst().getId();

        mockMvc.perform(get("/api/conversations/{id}", conversationId)
                        .param("currentUserId", currentUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId))
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andExpect(jsonPath("$.title").value("Other User"))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.otherUser.id").value(otherUser.getId()));

        mockMvc.perform(get("/api/conversations/{id}", conversationId)
                        .param("currentUserId", otherUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherUser.id").value(currentUser.getId()));

        mockMvc.perform(get("/api/conversations/{id}", conversationId)
                        .param("currentUserId", outsideUser.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsGroupDetailsWithParticipantsAndForbidsOutsider() throws Exception {
        User thirdMember = userRepository.save(
                new User("conversation.third", "Third", "Member", "+381641000004")
        );
        Conversation group = conversationRepository.save(
                new Conversation(ConversationType.GROUP, " ")
        );
        participantRepository.saveAllAndFlush(List.of(
                new ConversationParticipant(group, currentUser),
                new ConversationParticipant(group, otherUser),
                new ConversationParticipant(group, thirdMember)
        ));

        mockMvc.perform(get("/api/conversations/{id}", group.getId())
                        .param("currentUserId", currentUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.getId()))
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.title").value("Grupni razgovor"))
                .andExpect(jsonPath("$.otherUser").doesNotExist())
                .andExpect(jsonPath("$.participants.length()").value(3))
                .andExpect(jsonPath("$.participants[0].id").value(currentUser.getId()))
                .andExpect(jsonPath("$.participants[1].id").value(otherUser.getId()))
                .andExpect(jsonPath("$.participants[2].id").value(thirdMember.getId()))
                .andExpect(jsonPath("$.createdAt").isString());

        mockMvc.perform(get("/api/conversations/{id}", group.getId())
                        .param("currentUserId", outsideUser.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void mapsValidationAndMissingResourcesToRequestedStatuses() throws Exception {
        mockMvc.perform(post("/api/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(directRequest(currentUser.getId(), currentUser.getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(directRequest(0L, otherUser.getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(directRequest(currentUser.getId(), 999999L)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/conversations/{id}", 999999L)
                        .param("currentUserId", currentUser.getId().toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/conversations/{id}", 999999L)
                        .param("currentUserId", "0"))
                .andExpect(status().isBadRequest());
    }

    private String directRequest(Long currentUserId, Long otherUserId) {
        return """
                {
                  "currentUserId": %d,
                  "otherUserId": %d
                }
                """.formatted(currentUserId, otherUserId);
    }
}
