package com.cefet_rj.projeto_final.users.dtos;

import com.cefet_rj.projeto_final.users.entities.User;

public record UserProfileDTO(Long id, String firstName, String lastName, String email, String profileImage) {
    public UserProfileDTO(User user) {
        this(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getProfileImage());
    }
}
