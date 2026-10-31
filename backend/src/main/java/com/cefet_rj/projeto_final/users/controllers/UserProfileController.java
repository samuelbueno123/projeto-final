package com.cefet_rj.projeto_final.users.controllers;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.auth.security.GoogleIdentityTokenService;
import com.cefet_rj.projeto_final.users.dtos.UserProfileDTO;
import com.cefet_rj.projeto_final.users.dtos.update.UserPasswordUpdateDTO;
import com.cefet_rj.projeto_final.users.dtos.update.UserProfileUpdateDTO;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.cefet_rj.projeto_final.users.services.PictureStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserProfileController {
    private final AuthenticatedUserService authenticatedUser;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final GoogleIdentityTokenService googleTokens;
    private final PictureStorageService pictureStorageService;

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
        String storedPicturePath = pictureStorageService.resolveOrStoreProfileImage(
                user.getId(),
                request.profileImage(),
                user.getProfileImage()
        );
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName() == null ? "" : request.lastName().trim());
        user.setEmail(email);
        user.setProfileImage(storedPicturePath);
        return new UserProfileDTO(users.save(user));
    }

    @PostMapping(value = {"/picture", "/profile/picture"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileDTO uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        User user = authenticatedUser.getAuthenticatedUser();
        String storedPath = pictureStorageService.storeMultipartPicture(
                user.getId(),
                file,
                user.getProfileImage()
        );
        user.setProfileImage(storedPath);
        return new UserProfileDTO(users.save(user));
    }

    @DeleteMapping({"/picture", "/profile/picture"})
    public UserProfileDTO deleteProfilePicture() {
        User user = authenticatedUser.getAuthenticatedUser();
        pictureStorageService.deleteFileIfPresent(user.getProfileImage());
        user.setProfileImage(null);
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
