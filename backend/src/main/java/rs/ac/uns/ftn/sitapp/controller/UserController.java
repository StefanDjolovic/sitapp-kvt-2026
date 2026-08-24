package rs.ac.uns.ftn.sitapp.controller;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.sitapp.dto.UserResponse;
import rs.ac.uns.ftn.sitapp.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public List<UserResponse> search(
            @RequestParam @Size(max = 100) String query,
            @RequestParam(required = false) @Positive Long currentUserId
    ) {
        return userService.search(query, currentUserId);
    }
}
