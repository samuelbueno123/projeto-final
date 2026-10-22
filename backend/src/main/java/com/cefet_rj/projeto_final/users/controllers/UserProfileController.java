package com.cefet_rj.projeto_final.users.controllers;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.auth.security.GoogleIdentityTokenService;
import com.cefet_rj.projeto_final.users.dtos.UserProfileDTO;
import com.cefet_rj.projeto_final.users.dtos.update.UserPasswordUpdateDTO;
import com.cefet_rj.projeto_final.users.dtos.update.UserProfileUpdateDTO;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserProfileController {
    private final AuthenticatedUserService authenticatedUser;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final GoogleIdentityTokenService googleTokens;

    @GetMapping
    public UserProfileDTO getProfile() {
        return new UserProfileDTO(authenticatedUser.getAuthenticatedUser());
    }

    @PutMapping("/profile")
    public UserProfileDTO updateProfile(@Valid @RequestBody UserProfileUpdateDTO request) {
        User user = authenticatedUser.getAuthenticatedUser();
        String email = request.email().trim().toLowerCase();
        boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());
        if (emailChanged) {
            boolean passwordProof = request.currentPassword() != null && !request.currentPassword().isBlank()
                    && passwordEncoder.matches(request.currentPassword(), user.getPassword());
            boolean googleProof = false;
            if (request.googleIdToken() != null && !request.googleIdToken().isBlank()) {
                try {
                    googleProof = user.getGoogleSubject() != null
                            && user.getGoogleSubject().equals(googleTokens.verifyRecent(request.googleIdToken()).getSubject());
                } catch (RuntimeException ignored) {
                    googleProof = false;
                }
            }
            if (passwordProof == googleProof) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Confirme sua senha atual para alterar o email.");
            }
            if (users.existsByEmailIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email já está em uso.");
            }
        }
        String image = request.profileImage();
        if (image != null && !image.isBlank() && !image.matches("^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+={0,2}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem deve ser PNG, JPEG ou WebP válida.");
        }
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setProfileImage(image == null || image.isBlank() ? null : image);
        return new UserProfileDTO(users.save(user));
    }

    @PostMapping("/password")
    public void updatePassword(@Valid @RequestBody UserPasswordUpdateDTO request) {
        User user = authenticatedUser.getAuthenticatedUser();
        boolean hasPasswordProof = request.currentPassword() != null && !request.currentPassword().isBlank();
        boolean hasGoogleProof = request.googleIdToken() != null && !request.googleIdToken().isBlank();
        if (hasPasswordProof == hasGoogleProof) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe exatamente um método de reautenticação.");
        }
        boolean authenticated = hasPasswordProof
                ? passwordEncoder.matches(request.currentPassword(), user.getPassword())
                : user.getGoogleSubject() != null
                    && user.getGoogleSubject().equals(googleTokens.verifyRecent(request.googleIdToken()).getSubject());
        if (!authenticated) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A reautenticação falhou.");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
    }
}
