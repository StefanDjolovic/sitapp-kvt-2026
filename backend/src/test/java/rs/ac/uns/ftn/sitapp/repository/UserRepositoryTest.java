package rs.ac.uns.ftn.sitapp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import rs.ac.uns.ftn.sitapp.domain.User;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User expectedUser;

    @BeforeEach
    void setUp() {
        expectedUser = userRepository.saveAndFlush(
                new User("nightowl", "Milica", "Jovanovic", "+381641234567")
        );
        userRepository.saveAndFlush(
                new User("sunrise", "Petar", "Petrovic", "+381659876543")
        );
    }

    @ParameterizedTest
    @CsvSource({
            "OWL, username",
            "LIC, firstName",
            "VAN, lastName",
            "1234, phoneNumber"
    })
    void findsCaseInsensitivePartialMatchInEverySupportedField(
            String query,
            String searchedField
    ) {
        var result = userRepository.search("%" + query.toLowerCase() + "%", null);

        assertThat(result)
                .as("partial match through %s", searchedField)
                .containsExactly(expectedUser);
    }

    @Test
    void treatsLikeWildcardsAsLiteralCharacters() {
        var userWithPercent = userRepository.saveAndFlush(
                new User("percent%user", "Mina", "Simic", "+381631111111")
        );

        var result = userRepository.search("%!%%", null);

        assertThat(result).containsExactly(userWithPercent);
    }

    @Test
    void excludesCurrentUserFromResults() {
        var result = userRepository.search("%owl%", expectedUser.getId());

        assertThat(result).isEmpty();
    }
}
