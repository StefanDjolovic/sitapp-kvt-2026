package rs.ac.uns.ftn.sitapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationParticipant;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;
import rs.ac.uns.ftn.sitapp.domain.User;
import rs.ac.uns.ftn.sitapp.repository.ConversationParticipantRepository;
import rs.ac.uns.ftn.sitapp.repository.ConversationRepository;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void returnsExistingDirectConversationWithoutCreatingAnotherOne() {
        User currentUser = user(1L);
        User otherUser = user(2L);
        when(otherUser.getUsername()).thenReturn("other");
        when(otherUser.getFirstName()).thenReturn("Other");
        when(otherUser.getLastName()).thenReturn("User");
        when(otherUser.getPhoneNumber()).thenReturn("+381600000002");
        Conversation conversation = mock(Conversation.class);
        Instant createdAt = Instant.parse("2026-08-24T01:00:00Z");
        when(conversation.getId()).thenReturn(10L);
        when(conversation.getCreatedAt()).thenReturn(createdAt);
        when(userRepository.findAllByIdForUpdate(List.of(1L, 2L)))
                .thenReturn(List.of(currentUser, otherUser));
        when(conversationRepository.findBetweenUsers(ConversationType.DIRECT, 1L, 2L))
                .thenReturn(Optional.of(conversation));

        var result = conversationService.getOrCreateDirectConversation(1L, 2L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.otherUser().id()).isEqualTo(2L);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        verify(conversationRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(participantRepository);
    }

    @Test
    void rejectsConversationWithSelfBeforeAccessingDatabase() {
        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> conversationService.getOrCreateDirectConversation(4L, 4L)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository, conversationRepository, participantRepository);
    }

    @Test
    void returnsNotFoundWhenOneOfUsersDoesNotExist() {
        User currentUser = user(1L);
        when(userRepository.findAllByIdForUpdate(List.of(1L, 99L)))
                .thenReturn(List.of(currentUser));

        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> conversationService.getOrCreateDirectConversation(1L, 99L)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(conversationRepository, participantRepository);
    }

    @Test
    void forbidsReadingConversationForUserWhoIsNotParticipant() {
        User currentUser = mock(User.class);
        User otherUser = user(2L);
        User anotherUser = user(3L);
        Conversation conversation = mock(Conversation.class);
        ConversationParticipant firstParticipant = mock(ConversationParticipant.class);
        ConversationParticipant secondParticipant = mock(ConversationParticipant.class);
        when(firstParticipant.getUser()).thenReturn(otherUser);
        when(secondParticipant.getUser()).thenReturn(anotherUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(conversationRepository.findByIdAndType(20L, ConversationType.DIRECT))
                .thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationIdOrderById(20L))
                .thenReturn(List.of(firstParticipant, secondParticipant));

        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> conversationService.getDirectConversation(20L, 1L)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
