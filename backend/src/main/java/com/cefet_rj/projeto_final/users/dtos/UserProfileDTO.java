package com.cefet_rj.projeto_final.users.dtos;

import com.cefet_rj.projeto_final.users.entities.User;

public record UserProfileDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String profileImage,
        String themePreference,
        String phone
) {
    public UserProfileDTO(Long id, String firstName, String lastName, String email, String profileImage) {
        this(id, firstName, lastName, email, profileImage, "SYSTEM", null);
    }

    public UserProfileDTO(Long id, String firstName, String lastName, String email,
                          String profileImage, String themePreference) {
        this(id, firstName, lastName, email, profileImage, themePreference, null);
    }

    public UserProfileDTO(User user) {
        this(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getProfileImage(),
                user.getThemePreference() != null ? user.getThemePreference() : "SYSTEM",
                user.getPhone()
        );
    }
}
