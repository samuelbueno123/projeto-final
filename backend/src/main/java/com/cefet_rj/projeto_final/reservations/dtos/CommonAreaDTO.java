package com.cefet_rj.projeto_final.reservations.dtos;

import com.cefet_rj.projeto_final.reservations.entities.CommonArea;

public record CommonAreaDTO(

        Long id,
        String name,
        String description,
        Integer capacity,
        Boolean active,
        Boolean requiresApproval,
        String rules,
        Integer openingHour,
        Integer closingHour,
        String imageUrl,
        Long condominiumId,
        String condominiumName,
        Integer minAdvanceHours,
        Integer maxAdvanceDays,
        Integer maxDurationHours,
        Integer maxWeeklyBookingsPerUser,
        Boolean allowWaitlist

) {

    public CommonAreaDTO(
            Long id,
            String name,
            String description,
            Integer capacity,
            Boolean active,
            Boolean requiresApproval,
            String rules,
            Integer openingHour,
            Integer closingHour,
            Long condominiumId,
            String condominiumName
    ) {
        this(id, name, description, capacity, active, requiresApproval, rules, openingHour, closingHour, null,
                condominiumId, condominiumName, 1, 60, 8, 3, true);
    }

    public CommonAreaDTO(
            Long id,
            String name,
            String description,
            Integer capacity,
            Boolean active,
            Boolean requiresApproval,
            String rules,
            Integer openingHour,
            Integer closingHour,
            String imageUrl,
            Long condominiumId,
            String condominiumName
    ) {
        this(id, name, description, capacity, active, requiresApproval, rules, openingHour, closingHour, imageUrl,
                condominiumId, condominiumName, 1, 60, 8, 3, true);
    }

    public CommonAreaDTO(CommonArea entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCapacity(),
                entity.getActive(),
                entity.getRequiresApproval() != null ? entity.getRequiresApproval() : true,
                entity.getRules(),
                entity.getOpeningHour() != null ? entity.getOpeningHour() : 8,
                entity.getClosingHour() != null ? entity.getClosingHour() : 22,
                entity.getImageUrl(),
                entity.getCondominium().getId(),
                entity.getCondominium().getName(),
                entity.getMinAdvanceHours() != null ? entity.getMinAdvanceHours() : 1,
                entity.getMaxAdvanceDays() != null ? entity.getMaxAdvanceDays() : 60,
                entity.getMaxDurationHours() != null ? entity.getMaxDurationHours() : 8,
                entity.getMaxWeeklyBookingsPerUser() != null ? entity.getMaxWeeklyBookingsPerUser() : 3,
                entity.getAllowWaitlist() != null ? entity.getAllowWaitlist() : true
        );
    }
}
