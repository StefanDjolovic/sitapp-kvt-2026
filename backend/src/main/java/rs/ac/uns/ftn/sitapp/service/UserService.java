package rs.ac.uns.ftn.sitapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.sitapp.dto.UserResponse;
import rs.ac.uns.ftn.sitapp.repository.UserRepository;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> search(String query, Long currentUserId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        return userRepository.search(toSearchPattern(query), currentUserId)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    private String toSearchPattern(String query) {
        String escapedQuery = query.trim()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapedQuery + "%";
    }
}
