package rs.ac.uns.ftn.sitapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.sitapp.domain.User;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("dev")
@Order(1)
public class UserDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;

    public UserDataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<User> existingUsers = userRepository.findAll();
        Set<String> existingUsernames = existingUsers.stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());
        Set<String> existingPhoneNumbers = existingUsers.stream()
                .map(User::getPhoneNumber)
                .collect(Collectors.toSet());

        List<User> missingUsers = predefinedUsers().stream()
                .filter(user -> !existingUsernames.contains(user.getUsername()))
                .filter(user -> !existingPhoneNumbers.contains(user.getPhoneNumber()))
                .toList();

        if (!missingUsers.isEmpty()) {
            userRepository.saveAll(missingUsers);
        }
    }

    static List<User> predefinedUsers() {
        return List.of(
                new User("ana.petrovic", "Ana", "Petrović", "+381601111111"),
                new User("marko.jovanovic", "Marko", "Jovanović", "+381602222222"),
                new User("jelena.nikolic", "Jelena", "Nikolić", "+381603333333"),
                new User("nikola.ilic", "Nikola", "Ilić", "+381604444444"),
                new User("milica.stojanovic", "Milica", "Stojanović", "+381605555555"),
                new User("luka.popovic", "Luka", "Popović", "+381606666666"),
                new User("sara.mitrovic", "Sara", "Mitrović", "+381607777777"),
                new User("stefan.pavlovic", "Stefan", "Pavlović", "+381608888888")
        );
    }
}
