package com.cefet_rj.projeto_final.condominiums.dtos;

public record MembershipInvitationDTO(
        Long id,
        Long condominiumId,
        String condominiumName,
        String authority,
        String status,
        Long notificationId
) {
}
