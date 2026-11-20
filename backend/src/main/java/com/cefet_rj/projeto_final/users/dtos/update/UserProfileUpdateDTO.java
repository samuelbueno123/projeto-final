package com.cefet_rj.projeto_final.users.dtos.update;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateDTO(
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 4_200_000) String profileImage,
        String currentPassword,
        String googleIdToken,
        String themePreference,
        @Size(max = 32) String phone
) {
    public UserProfileUpdateDTO(
            String firstName,
            String lastName,
            String email,
            String profileImage,
            String currentPassword,
            String googleIdToken
    ) {
        this(firstName, lastName, email, profileImage, currentPassword, googleIdToken, null, null);
    }

    public UserProfileUpdateDTO(
            String firstName,
            String lastName,
            String email,
            String profileImage,
            String currentPassword,
            String googleIdToken,
            String themePreference
    ) {
        this(firstName, lastName, email, profileImage, currentPassword, googleIdToken, themePreference, null);
    }
}
