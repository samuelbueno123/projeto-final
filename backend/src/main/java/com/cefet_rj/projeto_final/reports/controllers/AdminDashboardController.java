package com.cefet_rj.projeto_final.reports.controllers;

import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementDTO;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.billing.dtos.BillingPlanDTO;
import com.cefet_rj.projeto_final.billing.dtos.FeatureDTO;
import com.cefet_rj.projeto_final.community.repositories.*;
import com.cefet_rj.projeto_final.community.services.CondominiumCommunityService.*;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.data.jpa.repository.JpaRepository;
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
    private final OccurrenceTicketRepository tickets;
    private final VisitorAccessRepository visitors;
    private final LostFoundItemRepository lostFound;
    private final ClassifiedAdRepository classifieds;
    private final VehicleRepository vehicles;
    private final PetRepository pets;
    private final MovingRequestRepository movingRequests;
    private final CondominiumDocumentRepository documents;
    private final SmartLockerCompartmentRepository lockers;
    private final EventGuestListRepository guestLists;
    private final CondominiumInfractionRepository infractions;

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
    public Page<?> data(
            @PathVariable String resource,
            @RequestParam(required = false) @Size(max = 100) String query,
            Pageable pageable
    ) {
        return switch (resource) {
            case "users" -> queryPage(users, UserDTO::new, query, pageable);
            case "condominiums" -> queryPage(condominiums, CondominiumDTO::new, query, pageable);
            case "memberships" -> queryPage(memberships, MembershipDTO::new, query, pageable);
            case "sectors" -> queryPage(sectors, SectorDTO::new, query, pageable);
            case "units" -> queryPage(units, UnitDTO::new, query, pageable);
            case "residents" -> queryPage(residents, ResidentDTO::new, query, pageable);
            case "packages" -> queryPage(packages, PackageDTO::new, query, pageable);
            case "announcements" -> queryPage(announcements, AnnouncementDTO::new, query, pageable);
            case "reservations" -> queryPage(reservations, ReservationDTO::new, query, pageable);
            case "common-areas" -> queryPage(commonAreas, CommonAreaDTO::new, query, pageable);
            case "notifications" -> queryPage(notifications, NotificationDTO::new, query, pageable);
            case "billing-plans" -> queryPage(billingPlans, BillingPlanDTO::new, query, pageable);
            case "features" -> queryPage(features, FeatureDTO::new, query, pageable);
            case "tickets" -> queryPage(tickets, OccurrenceTicketDTO::new, query, pageable);
            case "visitors" -> queryPage(visitors, VisitorAccessDTO::new, query, pageable);
            case "lost-found" -> queryPage(lostFound, LostFoundItemDTO::new, query, pageable);
            case "classifieds" -> queryPage(classifieds, ClassifiedAdDTO::new, query, pageable);
            case "vehicles" -> queryPage(vehicles, VehicleDTO::new, query, pageable);
            case "pets" -> queryPage(pets, PetDTO::new, query, pageable);
            case "moving-requests" -> queryPage(movingRequests, MovingRequestDTO::new, query, pageable);
            case "documents" -> queryPage(documents, CondominiumDocumentDTO::new, query, pageable);
            case "lockers" -> queryPage(lockers, SmartLockerDTO::new, query, pageable);
            case "guest-lists" -> queryPage(guestLists, EventGuestListDTO::new, query, pageable);
            case "infractions" -> queryPage(infractions, CondominiumInfractionDTO::new, query, pageable);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown admin resource");
        };
    }

    private <E, D> Page<D> queryPage(
            JpaRepository<E, ?> repository,
            Function<E, D> mapper,
            String query,
            Pageable pageable
    ) {
        if (query == null || query.isBlank()) {
            return repository.findAll(pageable).map(mapper);
        }

        List<String> terms = Arrays.stream(query.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();

        if (terms.isEmpty()) {
            return repository.findAll(pageable).map(mapper);
        }

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");

        List<D> filtered = repository.findAll(sort).stream()
                .map(mapper)
                .filter(dto -> matchesTerms(dto, terms))
                .toList();

        if (pageable.isUnpaged()) {
            return new PageImpl<>(filtered);
        }

        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    private boolean matchesTerms(Object dto, List<String> terms) {
        String searchable = extractSearchableText(dto).toLowerCase(Locale.ROOT);
        return terms.stream().allMatch(searchable::contains);
    }

    private String extractSearchableText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Record record) {
            StringBuilder sb = new StringBuilder();
            for (RecordComponent component : record.getClass().getRecordComponents()) {
                try {
                    Object fieldValue = component.getAccessor().invoke(record);
                    sb.append(extractSearchableText(fieldValue)).append(' ');
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return sb.toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder sb = new StringBuilder();
            for (Object item : collection) {
                sb.append(extractSearchableText(item)).append(' ');
            }
            return sb.toString();
        }
        return value.toString();
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
