package com.cefet_rj.projeto_final.reports.services;

import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.reports.dtos.DashboardReportDTO;
import com.cefet_rj.projeto_final.reports.dtos.PackageReportDTO;
import com.cefet_rj.projeto_final.reports.dtos.ReservationReportDTO;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final PackageRepository packageRepository;
    private final AnnouncementRepository announcementRepository;
    private final ReservationRepository reservationRepository;

    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final CondominiumFeatureAccessService featureAccessService;

    public DashboardReportDTO getDashboard(
            Long condominiumId
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.REPORTS
        );

        validateCondominiumAccess(
                condominiumId
        );

        LocalDateTime now =
                LocalDateTime.now();

        PackageReportDTO packages =
                buildPackageReport(
                        condominiumId
                );

        long activeAnnouncements =
                announcementRepository
                        .countActiveByCondominium(
                                condominiumId,
                                now
                        );

        ReservationReportDTO reservations =
                buildReservationReport(
                        condominiumId,
                        now
                );

        return new DashboardReportDTO(
                condominiumId,
                packages,
                activeAnnouncements,
                reservations
        );
    }

    private PackageReportDTO buildPackageReport(
            Long condominiumId
    ) {

        long total =
                packageRepository
                        .countByCondominiumId(
                                condominiumId
                        );

        Map<PackageStatus, Long> counts =
                new EnumMap<>(
                        PackageStatus.class
                );

        List<Object[]> results =
                packageRepository
                        .countByStatusForCondominium(
                                condominiumId
                        );

        for (Object[] row : results) {

            PackageStatus status =
                    (PackageStatus) row[0];

            Long count =
                    (Long) row[1];

            counts.put(
                    status,
                    count
            );
        }

        return new PackageReportDTO(
                total,

                counts.getOrDefault(
                        PackageStatus.EXPECTED,
                        0L
                ),

                counts.getOrDefault(
                        PackageStatus.RECEIVED,
                        0L
                ),

                counts.getOrDefault(
                        PackageStatus.NOTIFIED,
                        0L
                ),

                counts.getOrDefault(
                        PackageStatus.PICKED_UP,
                        0L
                ),

                counts.getOrDefault(
                        PackageStatus.CANCELLED,
                        0L
                ),

                counts.getOrDefault(
                        PackageStatus.RETURNED,
                        0L
                )
        );
    }

    private ReservationReportDTO buildReservationReport(
            Long condominiumId,
            LocalDateTime now
    ) {

        long total =
                reservationRepository
                        .countByCondominiumId(
                                condominiumId
                        );

        Map<ReservationStatus, Long> counts =
                new EnumMap<>(
                        ReservationStatus.class
                );

        List<Object[]> results =
                reservationRepository
                        .countByStatusForCondominium(
                                condominiumId
                        );

        for (Object[] row : results) {

            ReservationStatus status =
                    (ReservationStatus) row[0];

            Long count =
                    (Long) row[1];

            counts.put(
                    status,
                    count
            );
        }

        long futureConfirmed =
                reservationRepository
                        .countFutureByCondominiumAndStatus(
                                condominiumId,
                                ReservationStatus.CONFIRMED,
                                now
                        );

        return new ReservationReportDTO(
                total,

                counts.getOrDefault(
                        ReservationStatus.CONFIRMED,
                        0L
                ),

                counts.getOrDefault(
                        ReservationStatus.CANCELLED,
                        0L
                ),

                futureConfirmed
        );
    }

    private void validateCondominiumAccess(
            Long condominiumId
    ) {

        User user =
                authenticatedUserService
                        .getAuthenticatedUser();

        if (user.hasRole("ROLE_ADMIN")) {
            return;
        }

        condominiumAccessService
                .validateUserBelongsToCondominium(
                        user.getId(),
                        condominiumId
                );
    }
}