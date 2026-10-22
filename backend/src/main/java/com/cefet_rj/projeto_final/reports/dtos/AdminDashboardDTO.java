package com.cefet_rj.projeto_final.reports.dtos;

public record AdminDashboardDTO(
        long users,
        long condominiums,
        long memberships,
        long sectors,
        long units,
        long residents,
        long packages,
        long announcements,
        long reservations,
        long commonAreas,
        long notifications,
        long billingPlans,
        long features
) {}
