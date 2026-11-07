package com.cefet_rj.projeto_final.condominiums.security;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("condominiumAccess")
@RequiredArgsConstructor
public class CondominiumAccessService {

    private final MembershipRepository membershipRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public void validateAccess(Long condominiumId) {
        User user = authenticatedUserService.getAuthenticatedUser();
        if (user.hasRole("ROLE_ADMIN")) {
            return;
        }
        validateUserBelongsToCondominium(user.getId(), condominiumId);
    }

    public User validateManagerAccess(Long condominiumId) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        User user = authenticatedUserService.getAuthenticatedUser();
        if (user.hasRole("ROLE_ADMIN")) {
            return user;
        }
        if (!membershipRepository.isManagerOf(user.getId(), condominiumId)) {
            throw new ForbiddenException("Apenas o síndico ou administrador deste condomínio pode realizar esta operação.");
        }
        return user;
    }

    public User validateStaffAccess(Long condominiumId) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        User user = authenticatedUserService.getAuthenticatedUser();
        if (user.hasRole("ROLE_ADMIN")) {
            return user;
        }
        if (!membershipRepository.isStaffOf(user.getId(), condominiumId)) {
            throw new ForbiddenException("Apenas a administração ou portaria deste condomínio pode realizar esta operação.");
        }
        return user;
    }

    public boolean isManagerOrAdmin(User user, Long condominiumId) {
        if (user == null) {
            return false;
        }
        return user.hasRole("ROLE_ADMIN") || membershipRepository.isManagerOf(user.getId(), condominiumId);
    }

    public boolean isStaffOrAdmin(User user, Long condominiumId) {
        if (user == null) {
            return false;
        }
        return user.hasRole("ROLE_ADMIN") || membershipRepository.isStaffOf(user.getId(), condominiumId);
    }

    public void validateUserBelongsToCondominium(Long userId, Long condominiumId) {
        boolean belongs = membershipRepository
                .existsByUserIdAndCondominiumId(userId, condominiumId);

        if (!belongs) {
            throw new ForbiddenException("User does not belong to this condominium");
        }
    }

    public boolean userBelongsToCondominium(Long userId, Long condominiumId) {
        return membershipRepository.existsByUserIdAndCondominiumId(userId, condominiumId);
    }
}
