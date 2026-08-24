package rs.ac.uns.ftn.sitapp.dto;

import rs.ac.uns.ftn.sitapp.domain.User;

public record UserResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String phoneNumber
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber()
        );
    }
}
