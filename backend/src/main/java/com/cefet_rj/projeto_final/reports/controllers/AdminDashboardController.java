package com.cefet_rj.projeto_final.reports.controllers;

import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementDTO;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.billing.dtos.BillingPlanDTO;
import com.cefet_rj.projeto_final.billing.dtos.FeatureDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.SectorDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.UnitDTO;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.notifications.dtos.NotificationDTO;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.packages.dtos.PackageDTO;
import com.cefet_rj.projeto_final.reports.dtos.AdminDashboardDTO;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaDTO;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationDTO;
import com.cefet_rj.projeto_final.users.dtos.UserDTO;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminDashboardController {

    private final UserRepository users;
    private final CondominiumRepository condominiums;
    private final MembershipRepository memberships;
    private final SectorRepository sectors;
    private final UnitRepository units;
    private final ResidentRepository residents;
    private final PackageRepository packages;
    private final AnnouncementRepository announcements;
    private final ReservationRepository reservations;
    private final CommonAreaRepository commonAreas;
    private final NotificationRepository notifications;
    private final BillingPlanRepository billingPlans;
    private final FeatureRepository features;

    @GetMapping("/dashboard")
    public AdminDashboardDTO dashboard() {
        return new AdminDashboardDTO(
                users.count(),
                condominiums.count(),
                memberships.count(),
                sectors.count(),
                units.count(),
                residents.count(),
                packages.count(),
                announcements.count(),
                reservations.count(),
                commonAreas.count(),
                notifications.count(),
                billingPlans.count(),
                features.count()
        );
    }

    @GetMapping("/data/{resource}")
    @Transactional(readOnly = true)
    public Page<?> data(@PathVariable String resource, Pageable pageable) {
        return switch (resource) {
            case "users" -> users.findAll(pageable).map(UserDTO::new);
            case "condominiums" -> condominiums.findAll(pageable).map(CondominiumDTO::new);
            case "memberships" -> memberships.findAll(pageable).map(MembershipDTO::new);
            case "sectors" -> sectors.findAll(pageable).map(SectorDTO::new);
            case "units" -> units.findAll(pageable).map(UnitDTO::new);
            case "residents" -> residents.findAll(pageable).map(ResidentDTO::new);
            case "packages" -> packages.findAll(pageable).map(PackageDTO::new);
            case "announcements" -> announcements.findAll(pageable).map(AnnouncementDTO::new);
            case "reservations" -> reservations.findAll(pageable).map(ReservationDTO::new);
            case "common-areas" -> commonAreas.findAll(pageable).map(CommonAreaDTO::new);
            case "notifications" -> notifications.findAll(pageable).map(NotificationDTO::new);
            case "billing-plans" -> billingPlans.findAll(pageable).map(BillingPlanDTO::new);
            case "features" -> features.findAll(pageable).map(FeatureDTO::new);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown admin resource");
        };
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public AdminSearchResponse search(@RequestParam @NotBlank @Size(max = 100) String query) {
        String term = query.trim();
        if (term.isEmpty()) return new AdminSearchResponse(List.of(), List.of(), List.of());
        Pageable limit = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        List<AdminSearchResult> userResults = users.searchByName(term, limit).stream()
                .map(user -> new AdminSearchResult(user.getId(), user.getFirstName() + " " + user.getLastName(), user.getEmail()))
                .toList();
        List<AdminSearchResult> condominiumResults = condominiums.findByNameContainingIgnoreCase(term, limit).stream()
                .map(condominium -> new AdminSearchResult(condominium.getId(), condominium.getName(), condominium.getAddress()))
                .toList();
        List<AdminSearchResult> sectorResults = sectors.findByNameContainingIgnoreCase(term, limit).stream()
                .map(sector -> new AdminSearchResult(sector.getId(), sector.getName(), sector.getCondominium().getName()))
                .toList();
        return new AdminSearchResponse(userResults, condominiumResults, sectorResults);
    }

    public record AdminSearchResult(Long id, String name, String details) {}
    public record AdminSearchResponse(List<AdminSearchResult> users,
                                      List<AdminSearchResult> condominiums,
                                      List<AdminSearchResult> sectors) {}
}
