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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserProfileController {
    private final AuthenticatedUserService authenticatedUser;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final GoogleIdentityTokenService googleTokens;
    private final PictureStorageService pictureStorageService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.cefet_rj.projeto_final.auth.security.CachedUserAuthService cachedUserAuthService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.cefet_rj.projeto_final.auth.security.OAuth2TokenCleanupService oauth2TokenCleanupService;

    @GetMapping
    public UserProfileDTO getProfile() {
        return new UserProfileDTO(authenticatedUser.getAuthenticatedUser());
    }

    @PutMapping("/profile")
    public UserProfileDTO updateProfile(@Valid @RequestBody UserProfileUpdateDTO request) {
        User user = authenticatedUser.getAuthenticatedUser();
        String previousEmail = user.getEmail();
        String email = request.email().trim().toLowerCase();
        boolean emailChanged = !email.equalsIgnoreCase(previousEmail);
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
        String validatedPhone = user.getPhone();
        if (request.phone() != null) {
            String phoneDigits = request.phone().replaceAll("\\D", "");
            if (!phoneDigits.isEmpty()
                    && (phoneDigits.length() < 10 || phoneDigits.length() > 11
                    || phoneDigits.charAt(0) == '0' || phoneDigits.charAt(1) == '0')) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe um telefone válido com DDD (10 ou 11 dígitos).");
            }
            validatedPhone = phoneDigits.isEmpty() ? null : phoneDigits;
        }
        String validatedTheme = user.getThemePreference();
        if (request.themePreference() != null && !request.themePreference().isBlank()) {
            String candidate = request.themePreference().trim().toUpperCase();
            if (!java.util.Set.of("LIGHT", "DARK", "SYSTEM").contains(candidate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tema inválido. Valores permitidos: LIGHT, DARK, SYSTEM.");
            }
            validatedTheme = candidate;
        }
        boolean keepExternalAvatar = user.getProfileImage() != null
                && (user.getProfileImage().startsWith("https://") || user.getProfileImage().startsWith("http://"))
                && (request.profileImage() == null || request.profileImage().trim().equals(user.getProfileImage()));
        String storedPicturePath = keepExternalAvatar
                ? user.getProfileImage()
                : pictureStorageService.resolveOrStoreProfileImage(
                        user.getId(),
                        request.profileImage(),
                        user.getProfileImage()
                );
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName() == null ? "" : request.lastName().trim());
        user.setEmail(email);
        user.setPhone(validatedPhone);
        user.setProfileImage(storedPicturePath);
        user.setThemePreference(validatedTheme);
        User saved = users.save(user);
        if (cachedUserAuthService != null) {
            cachedUserAuthService.evictUserAuth(previousEmail);
            cachedUserAuthService.evictUserAuth(saved.getEmail());
        }
        if (emailChanged && oauth2TokenCleanupService != null) {
            oauth2TokenCleanupService.revokeTokensForPrincipal(previousEmail);
        }
        return new UserProfileDTO(saved);
    }

    @org.springframework.transaction.annotation.Transactional
    @PatchMapping("/theme")
    public UserProfileDTO updateTheme(@RequestBody Map<String, String> body) {
        User user = authenticatedUser.getAuthenticatedUser();
        String theme = body != null ? body.getOrDefault("theme", "SYSTEM") : "SYSTEM";
        if (theme != null && !theme.isBlank()) {
            theme = theme.trim().toUpperCase();
        } else {
            theme = "SYSTEM";
        }
        if (!java.util.Set.of("LIGHT", "DARK", "SYSTEM").contains(theme)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Tema inválido. Valores permitidos: LIGHT, DARK, SYSTEM."
            );
        }
        user.setThemePreference(theme);
        return new UserProfileDTO(users.save(user));
    }

    @PostMapping(value = {"/picture", "/profile/picture"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileDTO uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        User user = authenticatedUser.getAuthenticatedUser();
        String storedPath = pictureStorageService.storeMultipartPicture(
                PictureStorageService.PROFILES_FOLDER,
                "user_" + user.getId(),
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
        if (cachedUserAuthService != null) {
            cachedUserAuthService.evictUserAuth(user.getEmail());
        }
        if (oauth2TokenCleanupService != null) {
            oauth2TokenCleanupService.revokeTokensForPrincipal(user.getEmail());
        }
    }
}
