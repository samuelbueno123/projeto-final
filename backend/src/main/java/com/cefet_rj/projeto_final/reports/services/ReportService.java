package com.cefet_rj.projeto_final.reports.services;

import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.community.entities.LostFoundItem;
import com.cefet_rj.projeto_final.community.entities.OccurrenceTicket;
import com.cefet_rj.projeto_final.community.entities.ParkingSpot;
import com.cefet_rj.projeto_final.community.entities.Vehicle;
import com.cefet_rj.projeto_final.community.entities.VisitorAccess;
import com.cefet_rj.projeto_final.community.repositories.LostFoundItemRepository;
import com.cefet_rj.projeto_final.community.repositories.OccurrenceTicketRepository;
import com.cefet_rj.projeto_final.community.repositories.ParkingSpotRepository;
import com.cefet_rj.projeto_final.community.repositories.PetRepository;
import com.cefet_rj.projeto_final.community.repositories.VehicleRepository;
import com.cefet_rj.projeto_final.community.repositories.VisitorAccessRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceEquipment;
import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceOrder;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceEquipmentRepository;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceOrderRepository;
import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.reports.dtos.DashboardReportDTO;
import com.cefet_rj.projeto_final.reports.dtos.PackageReportDTO;
import com.cefet_rj.projeto_final.reports.dtos.ReservationReportDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final PackageRepository packageRepository;
    private final AnnouncementRepository announcementRepository;
    private final ReservationRepository reservationRepository;
    private final CommonAreaRepository commonAreaRepository;
    private final CondominiumRepository condominiumRepository;
    private final UnitRepository unitRepository;
    private final VisitorAccessRepository visitorAccessRepository;
    private final OccurrenceTicketRepository ticketRepository;
    private final MaintenanceOrderRepository maintenanceOrderRepository;
    private final MaintenanceEquipmentRepository equipmentRepository;
    private final UnitPaymentRepository unitPaymentRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final PetRepository petRepository;
    private final LostFoundItemRepository lostFoundItemRepository;

    private final CondominiumAccessService condominiumAccessService;
    private final CondominiumFeatureAccessService featureAccessService;

    public DashboardReportDTO getDashboard(Long condominiumId) {
        return getDashboard(condominiumId, null, null);
    }

    public DashboardReportDTO getDashboard(
            Long condominiumId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.REPORTS
        );

        condominiumAccessService.validateManagerAccess(condominiumId);

        Condominium condominium = condominiumRepository.findById(condominiumId).orElse(null);
        String condominiumName = condominium != null ? condominium.getName() : "Condomínio #" + condominiumId;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime toDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        PackageReportDTO packages = buildPackageReport(condominiumId, fromDateTime, toDateTime);
        long activeAnnouncements = announcementRepository.countActiveByCondominium(condominiumId, now);
        ReservationReportDTO reservations = buildReservationReport(condominiumId, now, fromDateTime, toDateTime);

        DashboardReportDTO.VisitorReportDTO visitors = buildVisitorReport(condominiumId, fromDateTime, toDateTime);
        DashboardReportDTO.TicketReportDTO tickets = buildTicketReport(condominiumId, fromDateTime, toDateTime);
        DashboardReportDTO.MaintenanceReportDTO maintenance = buildMaintenanceReport(condominiumId, fromDateTime, toDateTime);
        List<DashboardReportDTO.CommonAreaUtilizationDTO> commonAreaUtilization =
                buildCommonAreaUtilization(condominiumId, fromDateTime, toDateTime);
        DashboardReportDTO.FinancialReportDTO financial = buildFinancialReport(condominiumId, startDate, endDate);
        DashboardReportDTO.CommunitySummaryDTO community = buildCommunitySummary(condominiumId);

        return new DashboardReportDTO(
                condominiumId,
                condominiumName,
                startDate,
                endDate,
                packages,
                activeAnnouncements,
                reservations,
                visitors,
                tickets,
                maintenance,
                commonAreaUtilization,
                financial,
                community
        );
    }

    public byte[] exportCsv(Long condominiumId, LocalDate startDate, LocalDate endDate) {
        DashboardReportDTO report = getDashboard(condominiumId, startDate, endDate);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM for Excel
        sb.append("Relatorio Gerencial e Indicadores Operacionais\n");
        sb.append("Condominio;").append(escapeCsv(report.condominiumName())).append("\n");
        sb.append("Periodo Inicio;").append(report.startDate() != null ? report.startDate() : "Inicio").append("\n");
        sb.append("Periodo Fim;").append(report.endDate() != null ? report.endDate() : "Atual").append("\n\n");

        sb.append("CATEGORIA;INDICADOR;VALOR\n");
        sb.append("Comunicados;Comunicados Ativos;").append(report.activeAnnouncements()).append("\n");

        sb.append("Encomendas;Total de Encomendas;").append(report.packages().total()).append("\n");
        sb.append("Encomendas;Aguardando Chegada;").append(report.packages().expected()).append("\n");
        sb.append("Encomendas;Recebidas na Portaria;").append(report.packages().received()).append("\n");
        sb.append("Encomendas;Notificadas;").append(report.packages().notified()).append("\n");
        sb.append("Encomendas;Retiradas;").append(report.packages().pickedUp()).append("\n");
        sb.append("Encomendas;Devolvidas;").append(report.packages().returned()).append("\n");

        sb.append("Reservas;Total de Reservas;").append(report.reservations().total()).append("\n");
        sb.append("Reservas;Confirmadas;").append(report.reservations().confirmed()).append("\n");
        sb.append("Reservas;Canceladas;").append(report.reservations().cancelled()).append("\n");
        sb.append("Reservas;Confirmadas Futuras;").append(report.reservations().futureConfirmed()).append("\n");

        sb.append("Visitantes;Total de Registros;").append(report.visitors().total()).append("\n");
        sb.append("Visitantes;Pre-Autorizados;").append(report.visitors().authorized()).append("\n");
        sb.append("Visitantes;Em Visita (Check-in);").append(report.visitors().checkedIn()).append("\n");
        sb.append("Visitantes;Saida Registrada (Check-out);").append(report.visitors().checkedOut()).append("\n");
        sb.append("Visitantes;Acesso Negado/Cancelado;").append(report.visitors().denied()).append("\n");

        sb.append("Ocorrencias;Total de Chamados;").append(report.tickets().total()).append("\n");
        sb.append("Ocorrencias;Abertos;").append(report.tickets().open()).append("\n");
        sb.append("Ocorrencias;Em Andamento;").append(report.tickets().inProgress()).append("\n");
        sb.append("Ocorrencias;Resolvidos/Fechados;").append(report.tickets().resolved()).append("\n");

        sb.append("Manutencao;Total de Ordens de Servico;").append(report.maintenance().totalOrders()).append("\n");
        sb.append("Manutencao;Ordens Abertas/Em Execucao;").append(report.maintenance().openOrders()).append("\n");
        sb.append("Manutencao;Ordens Concluidas;").append(report.maintenance().completedOrders()).append("\n");
        sb.append("Manutencao;Manutencoes Preventivas;").append(report.maintenance().preventiveOrders()).append("\n");
        sb.append("Manutencao;Equipamentos Atrasados;").append(report.maintenance().overdueEquipment()).append("\n");
        sb.append("Manutencao;Equipamentos Proximos do Vencimento;").append(report.maintenance().dueSoonEquipment()).append("\n");
        sb.append("Manutencao;Custo Realizado Total (R$);").append(report.maintenance().totalActualCost()).append("\n");

        sb.append("Financeiro;Total Faturado (R$);").append(report.financial().totalInvoiced()).append("\n");
        sb.append("Financeiro;Total Recebido (R$);").append(report.financial().totalCollected()).append("\n");
        sb.append("Financeiro;Total Pendente (R$);").append(report.financial().totalPending()).append("\n");
        sb.append("Financeiro;Unidades Inadimplentes;").append(report.financial().overdueUnits()).append("\n");

        sb.append("Cadastros;Veiculos Cadastrados;").append(report.community().totalVehicles()).append("\n");
        sb.append("Cadastros;Veiculos Pendentes de Aprovacao;").append(report.community().pendingVehicles()).append("\n");
        sb.append("Cadastros;Vagas de Garagem Totais;").append(report.community().totalParkingSpots()).append("\n");
        sb.append("Cadastros;Vagas Ocupadas;").append(report.community().occupiedParkingSpots()).append("\n");
        sb.append("Cadastros;Animais de Estimacao;").append(report.community().totalPets()).append("\n");
        sb.append("Cadastros;Achados e Perdidos em Aberto;").append(report.community().openLostFoundItems()).append("\n\n");

        sb.append("UTILIZACAO DE AREAS COMUNS\n");
        sb.append("Area Comum;Confirmadas;Pendentes;Fila de Espera;Canceladas\n");
        for (DashboardReportDTO.CommonAreaUtilizationDTO area : report.commonAreaUtilization()) {
            sb.append(escapeCsv(area.commonAreaName())).append(";")
                    .append(area.confirmedBookings()).append(";")
                    .append(area.pendingBookings()).append(";")
                    .append(area.waitlistedBookings()).append(";")
                    .append(area.cancelledBookings()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(Long condominiumId, LocalDate startDate, LocalDate endDate) {
        DashboardReportDTO report = getDashboard(condominiumId, startDate, endDate);
        List<String> lines = new ArrayList<>();
        lines.add("RELATORIO GERENCIAL - " + sanitizePdfText(report.condominiumName()));
        lines.add("Periodo: " + (report.startDate() != null ? report.startDate() : "Inicio")
                + " ate " + (report.endDate() != null ? report.endDate() : "Atual"));
        lines.add("----------------------------------------------------------------------");
        lines.add(String.format("ENCOMENDAS: Total=%d | Recebidas=%d | Notificadas=%d | Retiradas=%d",
                report.packages().total(), report.packages().received(),
                report.packages().notified(), report.packages().pickedUp()));
        lines.add(String.format("RESERVAS: Total=%d | Confirmadas=%d | Canceladas=%d | Futuras=%d",
                report.reservations().total(), report.reservations().confirmed(),
                report.reservations().cancelled(), report.reservations().futureConfirmed()));
        lines.add(String.format("VISITANTES: Total=%d | Autorizados=%d | Check-in=%d | Check-out=%d | Negados=%d",
                report.visitors().total(), report.visitors().authorized(),
                report.visitors().checkedIn(), report.visitors().checkedOut(), report.visitors().denied()));
        lines.add(String.format("OCORRENCIAS: Total=%d | Abertas=%d | Em Andamento=%d | Resolvidas=%d",
                report.tickets().total(), report.tickets().open(),
                report.tickets().inProgress(), report.tickets().resolved()));
        lines.add(String.format("MANUTENCAO: OS=%d | Preventivas=%d | Equip. Atrasados=%d | Custo=R$ %s",
                report.maintenance().totalOrders(), report.maintenance().preventiveOrders(),
                report.maintenance().overdueEquipment(), report.maintenance().totalActualCost()));
        lines.add(String.format("FINANCEIRO: Faturado=R$ %s | Recebido=R$ %s | Pendente=R$ %s | Inadimplentes=%d",
                report.financial().totalInvoiced(), report.financial().totalCollected(),
                report.financial().totalPending(), report.financial().overdueUnits()));
        lines.add(String.format("COMUNIDADE: Veiculos=%d | Vagas=%d/%d | Pets=%d | Achados Abertos=%d",
                report.community().totalVehicles(), report.community().occupiedParkingSpots(),
                report.community().totalParkingSpots(), report.community().totalPets(),
                report.community().openLostFoundItems()));
        lines.add("----------------------------------------------------------------------");
        lines.add("UTILIZACAO POR AREA COMUM:");
        for (DashboardReportDTO.CommonAreaUtilizationDTO area : report.commonAreaUtilization()) {
            lines.add(String.format(" - %s: Confirmadas=%d | Pendentes=%d | Fila=%d | Canceladas=%d",
                    sanitizePdfText(area.commonAreaName()),
                    area.confirmedBookings(),
                    area.pendingBookings(),
                    area.waitlistedBookings(),
                    area.cancelledBookings()));
        }
        return buildSimplePdf(lines);
    }

    private PackageReportDTO buildPackageReport(
            Long condominiumId,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
        if (fromDateTime == null && toDateTime == null) {
            long total = packageRepository.countByCondominiumId(condominiumId);
            Map<PackageStatus, Long> counts = new EnumMap<>(PackageStatus.class);
            List<Object[]> results = packageRepository.countByStatusForCondominium(condominiumId);
            for (Object[] row : results) {
                counts.put((PackageStatus) row[0], (Long) row[1]);
            }
            return new PackageReportDTO(
                    total,
                    counts.getOrDefault(PackageStatus.EXPECTED, 0L),
                    counts.getOrDefault(PackageStatus.RECEIVED, 0L),
                    counts.getOrDefault(PackageStatus.NOTIFIED, 0L),
                    counts.getOrDefault(PackageStatus.PICKED_UP, 0L),
                    counts.getOrDefault(PackageStatus.CANCELLED, 0L),
                    counts.getOrDefault(PackageStatus.RETURNED, 0L)
            );
        }

        List<Package> filtered = packageRepository.findByCondominiumId(condominiumId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(p -> isWithinRange(
                        p.getReceivedAt() != null ? p.getReceivedAt() : (p.getNotifiedAt() != null ? p.getNotifiedAt() : p.getPickedUpAt()),
                        fromDateTime,
                        toDateTime
                ))
                .toList();
        Map<PackageStatus, Long> counts = filtered.stream()
                .collect(Collectors.groupingBy(Package::getStatus, Collectors.counting()));

        return new PackageReportDTO(
                filtered.size(),
                counts.getOrDefault(PackageStatus.EXPECTED, 0L),
                counts.getOrDefault(PackageStatus.RECEIVED, 0L),
                counts.getOrDefault(PackageStatus.NOTIFIED, 0L),
                counts.getOrDefault(PackageStatus.PICKED_UP, 0L),
                counts.getOrDefault(PackageStatus.CANCELLED, 0L),
                counts.getOrDefault(PackageStatus.RETURNED, 0L)
        );
    }

    private ReservationReportDTO buildReservationReport(
            Long condominiumId,
            LocalDateTime now,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
        if (fromDateTime == null && toDateTime == null) {
            long total = reservationRepository.countByCondominiumId(condominiumId);
            Map<ReservationStatus, Long> counts = new EnumMap<>(ReservationStatus.class);
            List<Object[]> results = reservationRepository.countByStatusForCondominium(condominiumId);
            for (Object[] row : results) {
                counts.put((ReservationStatus) row[0], (Long) row[1]);
            }
            long futureConfirmed = reservationRepository.countFutureByCondominiumAndStatus(
                    condominiumId, ReservationStatus.CONFIRMED, now
            );
            return new ReservationReportDTO(
                    total,
                    counts.getOrDefault(ReservationStatus.CONFIRMED, 0L),
                    counts.getOrDefault(ReservationStatus.CANCELLED, 0L),
                    futureConfirmed
            );
        }

        List<Reservation> filtered = reservationRepository
                .findByCommonAreaCondominiumIdOrderByStartAtDesc(condominiumId)
                .stream()
                .filter(r -> isWithinRange(r.getStartAt(), fromDateTime, toDateTime))
                .toList();
        long confirmed = filtered.stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
        long cancelled = filtered.stream().filter(r -> r.getStatus() == ReservationStatus.CANCELLED).count();
        long futureConfirmed = filtered.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED && r.getStartAt().isAfter(now))
                .count();
        return new ReservationReportDTO(filtered.size(), confirmed, cancelled, futureConfirmed);
    }

    private DashboardReportDTO.VisitorReportDTO buildVisitorReport(
            Long condominiumId,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
        List<VisitorAccess> visitors = visitorAccessRepository
                .findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream()
                .filter(v -> isWithinRange(v.getExpectedAt() != null ? v.getExpectedAt() : v.getCreatedAt(), fromDateTime, toDateTime))
                .toList();

        long authorized = visitors.stream().filter(v -> "AUTHORIZED".equalsIgnoreCase(v.getStatus())).count();
        long checkedIn = visitors.stream().filter(v -> "CHECKED_IN".equalsIgnoreCase(v.getStatus())).count();
        long checkedOut = visitors.stream().filter(v -> "CHECKED_OUT".equalsIgnoreCase(v.getStatus())).count();
        long denied = visitors.stream()
                .filter(v -> "DENIED".equalsIgnoreCase(v.getStatus()) || "CANCELLED".equalsIgnoreCase(v.getStatus()))
                .count();

        return new DashboardReportDTO.VisitorReportDTO(visitors.size(), authorized, checkedIn, checkedOut, denied);
    }

    private DashboardReportDTO.TicketReportDTO buildTicketReport(
            Long condominiumId,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
        List<OccurrenceTicket> tickets = ticketRepository
                .findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream()
                .filter(t -> isWithinRange(t.getCreatedAt(), fromDateTime, toDateTime))
                .toList();

        long open = tickets.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
        long inProgress = tickets.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long resolved = tickets.stream()
                .filter(t -> "RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus()))
                .count();

        return new DashboardReportDTO.TicketReportDTO(tickets.size(), open, inProgress, resolved);
    }

    private DashboardReportDTO.MaintenanceReportDTO buildMaintenanceReport(
            Long condominiumId,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
        List<MaintenanceOrder> orders = maintenanceOrderRepository
                .findAllByCondominiumIdWithDetails(condominiumId)
                .stream()
                .filter(o -> isWithinRange(o.getCreatedAt(), fromDateTime, toDateTime))
                .toList();

        long open = orders.stream()
                .filter(o -> "OPEN".equalsIgnoreCase(o.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(o.getStatus()))
                .count();
        long completed = orders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        long preventive = orders.stream().filter(o -> "PREVENTIVE".equalsIgnoreCase(o.getType())).count();
        BigDecimal totalCost = orders.stream()
                .map(o -> o.getActualCost() != null
                        ? o.getActualCost()
                        : (o.getEstimatedCost() != null ? o.getEstimatedCost() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        List<MaintenanceEquipment> equipmentList = equipmentRepository.findByCondominiumIdOrderByNextMaintenanceDateAsc(condominiumId);
        long overdueEquipment = equipmentList.stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus())
                        && e.getNextMaintenanceDate() != null
                        && e.getNextMaintenanceDate().isBefore(today))
                .count();
        long dueSoonEquipment = equipmentList.stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus())
                        && e.getNextMaintenanceDate() != null
                        && !e.getNextMaintenanceDate().isBefore(today)
                        && !e.getNextMaintenanceDate().isAfter(today.plusDays(7)))
                .count();

        return new DashboardReportDTO.MaintenanceReportDTO(
                orders.size(), open, completed, preventive, overdueEquipment, dueSoonEquipment, totalCost
        );
    }

    private List<DashboardReportDTO.CommonAreaUtilizationDTO> buildCommonAreaUtilization(
            Long condominiumId,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
        List<CommonArea> areas = commonAreaRepository.findByCondominiumIdOrderByNameAsc(condominiumId);
        List<Reservation> reservations = reservationRepository
                .findByCommonAreaCondominiumIdOrderByStartAtDesc(condominiumId)
                .stream()
                .filter(r -> isWithinRange(r.getStartAt(), fromDateTime, toDateTime))
                .toList();

        Map<Long, List<Reservation>> byArea = reservations.stream()
                .collect(Collectors.groupingBy(r -> r.getCommonArea().getId()));

        return areas.stream()
                .map(area -> {
                    List<Reservation> list = byArea.getOrDefault(area.getId(), List.of());
                    long confirmed = list.stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
                    long pending = list.stream()
                            .filter(r -> r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.COUNTER_PROPOSED)
                            .count();
                    long waitlisted = list.stream().filter(r -> r.getStatus() == ReservationStatus.WAITLISTED).count();
                    long cancelled = list.stream()
                            .filter(r -> r.getStatus() == ReservationStatus.CANCELLED || r.getStatus() == ReservationStatus.REJECTED)
                            .count();
                    return new DashboardReportDTO.CommonAreaUtilizationDTO(
                            area.getId(), area.getName(), confirmed, pending, waitlisted, cancelled
                    );
                })
                .toList();
    }

    private DashboardReportDTO.FinancialReportDTO buildFinancialReport(
            Long condominiumId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Unit> units = unitRepository.findAllForCondominium(condominiumId);
        List<Long> unitIds = units.stream().map(Unit::getId).toList();
        if (unitIds.isEmpty()) {
            return new DashboardReportDTO.FinancialReportDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        List<UnitPayment> payments = unitPaymentRepository.findAllByUnitIdIn(unitIds)
                .stream()
                .filter(p -> {
                    if (startDate != null && p.getDueDate() != null && p.getDueDate().isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && p.getDueDate() != null && p.getDueDate().isAfter(endDate)) {
                        return false;
                    }
                    return true;
                })
                .toList();

        BigDecimal totalInvoiced = payments.stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = payments.stream()
                .filter(p -> p.getStatus() == UnitPaymentStatus.PAID)
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPending = totalInvoiced.subtract(totalCollected);

        long overdueUnits = payments.stream()
                .filter(p -> p.getStatus() == UnitPaymentStatus.OVERDUE)
                .count();

        return new DashboardReportDTO.FinancialReportDTO(totalInvoiced, totalCollected, totalPending, overdueUnits);
    }

    private DashboardReportDTO.CommunitySummaryDTO buildCommunitySummary(Long condominiumId) {
        List<Vehicle> vehicles = vehicleRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        long pendingVehicles = vehicles.stream().filter(v -> "PENDING".equalsIgnoreCase(v.getStatus())).count();
        List<ParkingSpot> spots = parkingSpotRepository.findByCondominiumIdOrderByCodeAsc(condominiumId);
        long occupiedSpots = spots.stream().filter(s -> s.getUnit() != null || "OCCUPIED".equalsIgnoreCase(s.getStatus())).count();
        long pets = petRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId).size();
        List<LostFoundItem> lostFound = lostFoundItemRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        long openLostFound = lostFound.stream().filter(i -> "OPEN".equalsIgnoreCase(i.getStatus())).count();

        return new DashboardReportDTO.CommunitySummaryDTO(
                vehicles.size(), pendingVehicles, spots.size(), occupiedSpots, pets, openLostFound
        );
    }

    private boolean isWithinRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) return true;
        if (from != null && value.isBefore(from)) return false;
        if (to != null && !value.isBefore(to)) return false;
        return true;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String sanitized = value.replace(";", ",").replace("\r", " ").replace("\n", " ").trim();
        if (sanitized.startsWith("=") || sanitized.startsWith("+") || sanitized.startsWith("-") || sanitized.startsWith("@") || sanitized.startsWith("\t")) {
            sanitized = "'" + sanitized;
        }
        return sanitized;
    }

    private String sanitizePdfText(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("(", "[")
                .replace(")", "]")
                .replace("\\", "/");
    }

    private byte[] buildSimplePdf(List<String> lines) {
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 10 Tf\n40 790 Td\n14 TL\n");
        for (String line : lines) {
            stream.append("(").append(sanitizePdfText(line)).append(") Tj\nT*\n");
        }
        stream.append("ET\n");
        byte[] contentBytes = stream.toString().getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        writeAscii(out, "%PDF-1.4\n");

        offsets.add(out.size());
        writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "4 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
        out.writeBytes(contentBytes);
        writeAscii(out, "endstream\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        int xrefOffset = out.size();
        writeAscii(out, "xref\n0 6\n0000000000 65535 f \n");
        for (int offset : offsets) {
            writeAscii(out, String.format("%010d 00000 n \n", offset));
        }
        writeAscii(out, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");
        return out.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.US_ASCII));
    }
}
