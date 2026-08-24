package rs.ac.uns.ftn.sitapp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.ac.uns.ftn.sitapp.domain.User;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void doesNotInsertUsersThatAlreadyExist() {
        when(userRepository.findAll()).thenReturn(UserDataInitializer.predefinedUsers());
        UserDataInitializer initializer = new UserDataInitializer(userRepository);

        initializer.run(null);

        verify(userRepository, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void insertsAllPredefinedUsersIntoAnEmptyDatabase() {
        when(userRepository.findAll()).thenReturn(List.of());
        UserDataInitializer initializer = new UserDataInitializer(userRepository);

        initializer.run(null);

        ArgumentCaptor<Iterable<User>> usersCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(userRepository).saveAll(usersCaptor.capture());

        List<User> savedUsers = StreamSupport.stream(usersCaptor.getValue().spliterator(), false).toList();
        assertThat(savedUsers)
                .hasSize(8)
                .extracting(User::getUsername)
                .containsExactly(
                        "ana.petrovic",
                        "marko.jovanovic",
                        "jelena.nikolic",
                        "nikola.ilic",
                        "milica.stojanovic",
                        "luka.popovic",
                        "sara.mitrovic",
                        "stefan.pavlovic"
                );
    }
}
