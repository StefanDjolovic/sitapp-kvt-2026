package rs.ac.uns.ftn.sitapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.ac.uns.ftn.sitapp.domain.User;
import rs.ac.uns.ftn.sitapp.dto.UserResponse;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void trimsQueryAndMapsUsersToResponses() {
        var user = new User("alice", "Alice", "Andric", "+38160111222");
        when(userRepository.search("%ali%", 5L)).thenReturn(List.of(user));

        var result = userService.search("  Ali  ", 5L);

        assertThat(result).containsExactly(
                new UserResponse(null, "alice", "Alice", "Andric", "+38160111222")
        );
        verify(userRepository).search("%ali%", 5L);
    }

    @Test
    void returnsEmptyListWithoutQueryingDatabaseForBlankQuery() {
        var result = userService.search("   ", null);

        assertThat(result).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void escapesLikeWildcardsBeforeSearching() {
        when(userRepository.search("%!%!_!!%", null)).thenReturn(List.of());

        userService.search("%_!", null);

        verify(userRepository).search("%!%!_!!%", null);
    }
}
