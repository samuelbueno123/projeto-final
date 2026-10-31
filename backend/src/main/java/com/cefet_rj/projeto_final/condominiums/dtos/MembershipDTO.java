package com.cefet_rj.projeto_final.condominiums.dtos;

import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;

public record MembershipDTO(
        Long id,
        Long userId,
        Long condominiumId,
        String condominiumName,
        String authority,
        String userName,
        String email,
        String status,
        Long unitId,
        String unitLabel,
        String residentType,
        String requestMessage
) {

    public MembershipDTO(
            Long id,
            Long userId,
            Long condominiumId,
            String condominiumName,
            String authority,
            String userName,
            String email,
            String status,
            Long unitId
    ) {
        this(id, userId, condominiumId, condominiumName, authority, userName, email, status, unitId, null, null, null);
    }

    public MembershipDTO(Membership entity) {
        this(
                entity.getId(),
                entity.getUser().getId(),
                entity.getCondominium().getId(),
                entity.getCondominium().getName(),
                entity.getRole().getAuthority(),
                (entity.getUser().getFirstName() + " " + entity.getUser().getLastName()).trim(),
                entity.getUser().getEmail(),
                (entity.getStatus() == null ? MembershipStatus.ACTIVE : entity.getStatus()).name(),
                entity.getRequestedUnit() != null ? entity.getRequestedUnit().getId() : null,
                entity.getRequestedUnit() != null
                        ? (entity.getRequestedUnit().getSector().getName() + " • Unid. " + entity.getRequestedUnit().getIdentifier())
                        : null,
                entity.getRequestedResidentType() != null ? entity.getRequestedResidentType().name() : null,
                entity.getRequestMessage()
        );
    }

    public MembershipDTO withUnitInfo(Long resolvedUnitId, String resolvedUnitLabel, String resolvedResidentType) {
        return new MembershipDTO(
                id,
                userId,
                condominiumId,
                condominiumName,
                authority,
                userName,
                email,
                status,
                resolvedUnitId != null ? resolvedUnitId : unitId,
                resolvedUnitLabel != null ? resolvedUnitLabel : unitLabel,
                resolvedResidentType != null ? resolvedResidentType : residentType,
                requestMessage
        );
    }
}
