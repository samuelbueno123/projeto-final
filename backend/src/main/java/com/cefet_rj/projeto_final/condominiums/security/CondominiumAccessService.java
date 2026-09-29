package com.cefet_rj.projeto_final.condominiums.security;

import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("condominiumAccess")
@RequiredArgsConstructor
public class CondominiumAccessService {

    private final MembershipRepository membershipRepository;

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