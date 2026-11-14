package com.cefet_rj.projeto_final.community.controllers;

import com.cefet_rj.projeto_final.community.services.CondominiumCommunityService;
import com.cefet_rj.projeto_final.community.services.CondominiumCommunityService.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Condominium Community & Operations", description = "Ocorrências, visitantes, enquetes, achados e perdidos, classificados, veículos, vagas, pets, mudanças e documentos")
public class CondominiumCommunityController {

    private final CondominiumCommunityService communityService;

    // 1. OCORRÊNCIAS E CHAMADOS
    @GetMapping("/condominiums/{condominiumId}/tickets")
    public List<OccurrenceTicketDTO> listTickets(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine
    ) {
        return communityService.listTickets(condominiumId, mine);
    }

    @PostMapping("/condominiums/{condominiumId}/tickets")
    public ResponseEntity<OccurrenceTicketDTO> createTicket(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createTicket(condominiumId, body));
    }

    @PatchMapping("/tickets/{id}/status")
    public OccurrenceTicketDTO updateTicketStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return communityService.updateTicketStatus(id, body);
    }

    // 2. VISITANTES, CONVITES QR CODE E PORTARIA
    @GetMapping("/condominiums/{condominiumId}/visitors")
    public Object listVisitors(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        if (page != null) {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, Math.min(size, 100));
            return communityService.listVisitorsPaged(condominiumId, mine, pageable);
        }
        return communityService.listVisitors(condominiumId, mine);
    }

    @PostMapping("/condominiums/{condominiumId}/visitors")
    public ResponseEntity<VisitorAccessDTO> createVisitor(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createVisitorAccess(condominiumId, body));
    }

    @PostMapping("/condominiums/{condominiumId}/visitors/validate-qr")
    public VisitorAccessDTO validateVisitorQr(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return communityService.validateVisitorQrCode(condominiumId, body);
    }

    @PatchMapping("/visitors/{id}/approve")
    public VisitorAccessDTO approveVisitor(@PathVariable Long id) {
        return communityService.updateVisitorStatus(id, "AUTHORIZED");
    }

    @PatchMapping("/visitors/{id}/deny")
    public VisitorAccessDTO denyVisitor(@PathVariable Long id) {
        return communityService.updateVisitorStatus(id, "DENIED");
    }

    @PatchMapping("/visitors/{id}/check-in")
    public VisitorAccessDTO checkInVisitor(@PathVariable Long id) {
        return communityService.updateVisitorStatus(id, "CHECKED_IN");
    }

    @PatchMapping("/visitors/{id}/check-out")
    public VisitorAccessDTO checkOutVisitor(@PathVariable Long id) {
        return communityService.updateVisitorStatus(id, "CHECKED_OUT");
    }

    // 3. ENQUETES RÁPIDAS
    @GetMapping("/condominiums/{condominiumId}/polls")
    public List<PollDTO> listPolls(@PathVariable Long condominiumId) {
        return communityService.listPolls(condominiumId);
    }

    @PostMapping("/condominiums/{condominiumId}/polls")
    public ResponseEntity<PollDTO> createPoll(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createPoll(condominiumId, body));
    }

    @PostMapping("/polls/{pollId}/vote")
    public PollDTO voteOnPoll(
            @PathVariable Long pollId,
            @RequestBody Map<String, Object> body
    ) {
        Long optionId = body.get("optionId") instanceof Number n
                ? n.longValue()
                : Long.parseLong(body.get("optionId").toString());
        return communityService.voteOnPoll(pollId, optionId);
    }

    @PatchMapping("/polls/{pollId}/close")
    public PollDTO closePoll(@PathVariable Long pollId) {
        return communityService.closePoll(pollId);
    }

    // 4. ACHADOS E PERDIDOS
    @GetMapping("/condominiums/{condominiumId}/lost-found")
    public List<LostFoundItemDTO> listLostFound(@PathVariable Long condominiumId) {
        return communityService.listLostFound(condominiumId);
    }

    @PostMapping("/condominiums/{condominiumId}/lost-found")
    public ResponseEntity<LostFoundItemDTO> createLostFound(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createLostFound(condominiumId, body));
    }

    @PatchMapping("/lost-found/{id}/request-claim")
    public LostFoundItemDTO requestClaim(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return communityService.requestClaimLostFound(id, body);
    }

    @PatchMapping("/lost-found/{id}/return")
    public LostFoundItemDTO returnLostFound(@PathVariable Long id) {
        return communityService.markLostFoundReturned(id);
    }

    @PatchMapping("/lost-found/{id}/close")
    public LostFoundItemDTO closeLostFound(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return communityService.closeLostFound(id, body);
    }

    @GetMapping("/lost-found/{id}/messages")
    public List<LostFoundMessageDTO> listLostFoundMessages(@PathVariable Long id) {
        return communityService.listLostFoundMessages(id);
    }

    @PostMapping("/lost-found/{id}/messages")
    public ResponseEntity<LostFoundMessageDTO> addLostFoundMessage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.addLostFoundMessage(id, body));
    }

    // 5. CLASSIFICADOS E MURAL DE SERVIÇOS
    @GetMapping("/condominiums/{condominiumId}/classifieds")
    public List<ClassifiedAdDTO> listClassifieds(@PathVariable Long condominiumId) {
        return communityService.listClassifieds(condominiumId);
    }

    @PostMapping("/condominiums/{condominiumId}/classifieds")
    public ResponseEntity<ClassifiedAdDTO> createClassified(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createClassified(condominiumId, body));
    }

    @PostMapping("/classifieds/{id}/interest")
    public ClassifiedAdDTO sendInterest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return communityService.sendClassifiedInterest(id, body);
    }

    @PatchMapping("/classifieds/{id}/status")
    public ClassifiedAdDTO updateClassifiedStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        return communityService.updateClassifiedStatus(id, body.get("status"));
    }

    // 6. VEÍCULOS, VAGAS DE GARAGEM E CONSULTA DE PLACAS
    @GetMapping("/condominiums/{condominiumId}/parking-spots")
    public List<ParkingSpotDTO> listParkingSpots(@PathVariable Long condominiumId) {
        return communityService.listParkingSpots(condominiumId);
    }

    @PostMapping("/condominiums/{condominiumId}/parking-spots")
    public ResponseEntity<ParkingSpotDTO> createParkingSpot(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createParkingSpot(condominiumId, body));
    }

    @PutMapping("/parking-spots/{id}")
    public ParkingSpotDTO updateParkingSpot(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return communityService.updateParkingSpot(id, body);
    }

    @DeleteMapping("/parking-spots/{id}")
    public ResponseEntity<Void> deleteParkingSpot(@PathVariable Long id) {
        communityService.deleteParkingSpot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/condominiums/{condominiumId}/vehicles")
    public List<VehicleDTO> listVehicles(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine
    ) {
        return communityService.listVehicles(condominiumId, mine);
    }

    @GetMapping("/condominiums/{condominiumId}/vehicles/check-plate")
    public PlateCheckResultDTO checkPlate(
            @PathVariable Long condominiumId,
            @RequestParam String plate
    ) {
        return communityService.checkVehiclePlate(condominiumId, plate);
    }

    @PostMapping("/condominiums/{condominiumId}/vehicles")
    public ResponseEntity<VehicleDTO> createVehicle(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createVehicle(condominiumId, body));
    }

    @PatchMapping("/vehicles/{id}/approve")
    public VehicleDTO approveVehicle(@PathVariable Long id) {
        return communityService.updateVehicleStatus(id, "APPROVED");
    }

    @PatchMapping("/vehicles/{id}/reject")
    public VehicleDTO rejectVehicle(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = null;
        if (body != null) {
            reason = body.get("reason") != null ? body.get("reason") : body.get("notes");
        }
        return communityService.updateVehicleStatus(id, "REJECTED", reason);
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        communityService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    // 7. GESTÃO DE ANIMAIS DE ESTIMAÇÃO (PETS)
    @GetMapping("/condominiums/{condominiumId}/pets")
    public List<PetDTO> listPets(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine
    ) {
        return communityService.listPets(condominiumId, mine);
    }

    @PostMapping("/condominiums/{condominiumId}/pets")
    public ResponseEntity<PetDTO> createPet(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createPet(condominiumId, body));
    }

    @PutMapping("/pets/{id}")
    public PetDTO updatePet(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return communityService.updatePet(id, body);
    }

    @DeleteMapping("/pets/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        communityService.deletePet(id);
        return ResponseEntity.noContent().build();
    }

    // 8. AGENDAMENTO DE MUDANÇAS E CARGA/DESCARGA
    @GetMapping("/condominiums/{condominiumId}/moving-requests/check-availability")
    public MovingAvailabilityDTO checkMovingAvailability(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return communityService.checkMovingAvailability(condominiumId, date);
    }

    @GetMapping("/condominiums/{condominiumId}/moving-requests")
    public List<MovingRequestDTO> listMovingRequests(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine
    ) {
        return communityService.listMovingRequests(condominiumId, mine);
    }

    @PostMapping("/condominiums/{condominiumId}/moving-requests")
    public ResponseEntity<MovingRequestDTO> createMovingRequest(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createMovingRequest(condominiumId, body));
    }

    @PatchMapping("/moving-requests/{id}/approve")
    public MovingRequestDTO approveMovingRequest(@PathVariable Long id) {
        return communityService.updateMovingStatus(id, "APPROVED", null);
    }

    @PatchMapping("/moving-requests/{id}/reject")
    public MovingRequestDTO rejectMovingRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        return communityService.updateMovingStatus(id, "REJECTED", body != null ? body.get("reason") : null);
    }

    @PatchMapping("/moving-requests/{id}/cancel")
    public MovingRequestDTO cancelMovingRequest(@PathVariable Long id) {
        return communityService.updateMovingStatus(id, "CANCELLED", null);
    }

    // 9. MURAL DE DOCUMENTOS, ATAS E CONTATOS ÚTEIS
    @GetMapping("/condominiums/{condominiumId}/documents")
    public List<CondominiumDocumentDTO> listDocuments(@PathVariable Long condominiumId) {
        return communityService.listDocuments(condominiumId);
    }

    @PostMapping("/condominiums/{condominiumId}/documents")
    public ResponseEntity<CondominiumDocumentDTO> createDocument(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createDocument(condominiumId, body));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        communityService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    // 10. SMART LOCKERS & ARMÁRIOS INTELIGENTES
    @GetMapping("/condominiums/{condominiumId}/lockers")
    public List<SmartLockerDTO> listLockers(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine
    ) {
        return communityService.listLockers(condominiumId, mine);
    }

    @PostMapping("/condominiums/{condominiumId}/lockers")
    public ResponseEntity<SmartLockerDTO> depositLocker(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.depositLocker(condominiumId, body));
    }

    @PatchMapping("/lockers/{id}/retrieve")
    public SmartLockerDTO retrieveLocker(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String pin = body != null ? body.get("pinCode") : null;
        return communityService.retrieveLocker(id, pin);
    }

    // 11. LISTAS DE CONVIDADOS DE FESTAS E EVENTOS
    @GetMapping("/condominiums/{condominiumId}/guest-lists")
    public List<EventGuestListDTO> listGuestLists(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine
    ) {
        return communityService.listGuestLists(condominiumId, mine);
    }

    @PostMapping("/condominiums/{condominiumId}/guest-lists")
    public ResponseEntity<EventGuestListDTO> createGuestList(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createGuestList(condominiumId, body));
    }

    @PostMapping("/guest-lists/{id}/guests")
    public ResponseEntity<EventGuestListDTO> addGuest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.addGuest(id, body));
    }

    @PatchMapping("/guests/{id}/check-in")
    public EventGuestListDTO checkInGuest(@PathVariable Long id) {
        return communityService.checkInGuest(id);
    }

    @PatchMapping("/guests/{id}/check-out")
    public EventGuestListDTO checkOutGuest(@PathVariable Long id) {
        return communityService.checkOutGuest(id);
    }

    @DeleteMapping("/guests/{id}")
    public ResponseEntity<EventGuestListDTO> removeGuest(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.removeGuest(id));
    }

    // 12. LIVRO DE ADVERTÊNCIAS E MULTAS COM DEFESA
    @GetMapping("/condominiums/{condominiumId}/infractions")
    public List<CondominiumInfractionDTO> listInfractions(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine
    ) {
        return communityService.listInfractions(condominiumId, mine);
    }

    @PostMapping("/condominiums/{condominiumId}/infractions")
    public ResponseEntity<CondominiumInfractionDTO> createInfraction(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createInfraction(condominiumId, body));
    }

    @PostMapping("/infractions/{id}/defense")
    public CondominiumInfractionDTO submitDefense(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        return communityService.submitInfractionDefense(id, body.get("defense"));
    }

    @PatchMapping("/infractions/{id}/judge")
    public CondominiumInfractionDTO judgeInfraction(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        return communityService.judgeInfraction(id, body.get("status"), body.get("judgment"));
    }

    // 13. OBRAS E REFORMAS (NBR 16.280)
    @GetMapping("/condominiums/{condominiumId}/renovations")
    public List<RenovationRequestDTO> listRenovations(
            @PathVariable Long condominiumId,
            @RequestParam(defaultValue = "false") boolean mine
    ) {
        return communityService.listRenovations(condominiumId, mine);
    }

    @PostMapping("/condominiums/{condominiumId}/renovations")
    public ResponseEntity<RenovationRequestDTO> createRenovation(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createRenovation(condominiumId, body));
    }

    @PatchMapping("/renovations/{id}/approve")
    public RenovationRequestDTO approveRenovation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return communityService.approveRenovation(id, body != null ? body : Map.of());
    }

    @PatchMapping("/renovations/{id}/reject")
    public RenovationRequestDTO rejectRenovation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return communityService.rejectRenovation(id, body != null ? body : Map.of());
    }

    @PatchMapping("/renovations/{id}/complete")
    public RenovationRequestDTO completeRenovation(@PathVariable Long id) {
        return communityService.completeRenovation(id);
    }

    // 14. LIVRO DIGITAL DE PASSAGEM DE TURNO DA PORTARIA
    @GetMapping("/condominiums/{condominiumId}/shift-logs")
    public List<ConciergeShiftLogDTO> listShiftLogs(@PathVariable Long condominiumId) {
        return communityService.listShiftLogs(condominiumId);
    }

    @PostMapping("/condominiums/{condominiumId}/shift-logs")
    public ResponseEntity<ConciergeShiftLogDTO> createShiftLog(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createShiftLog(condominiumId, body));
    }

    @PatchMapping("/shift-logs/{id}/acknowledge")
    public ConciergeShiftLogDTO acknowledgeShiftLog(@PathVariable Long id) {
        return communityService.acknowledgeShiftLog(id);
    }

    // 15. CONTROLE DE CHAVES E TAGS DA PORTARIA
    @GetMapping("/condominiums/{condominiumId}/keys")
    public List<KeyRegistryEntryDTO> listKeys(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) String status
    ) {
        return communityService.listKeys(condominiumId, status);
    }

    @PostMapping("/condominiums/{condominiumId}/keys")
    public ResponseEntity<KeyRegistryEntryDTO> createKey(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createKey(condominiumId, body));
    }

    @PostMapping("/keys/{id}/checkout")
    public KeyRegistryEntryDTO checkoutKey(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return communityService.checkoutKey(id, body);
    }

    @PostMapping("/keys/{id}/checkin")
    public KeyRegistryEntryDTO checkinKey(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return communityService.checkinKey(id, body != null ? body : Map.of());
    }

    // 16. BOTÃO DE PÂNICO & ALERTA DE EMERGÊNCIA (SOS)
    @GetMapping("/condominiums/{condominiumId}/emergency-alerts")
    public List<EmergencyAlertDTO> listEmergencyAlerts(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) String status
    ) {
        return communityService.listEmergencyAlerts(condominiumId, status);
    }

    @PostMapping("/condominiums/{condominiumId}/emergency-alerts")
    public ResponseEntity<EmergencyAlertDTO> triggerEmergencyAlert(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.triggerEmergencyAlert(condominiumId, body));
    }

    @PatchMapping("/emergency-alerts/{id}/attend")
    public EmergencyAlertDTO attendEmergencyAlert(@PathVariable Long id) {
        return communityService.attendEmergencyAlert(id);
    }

    @PatchMapping("/emergency-alerts/{id}/resolve")
    public EmergencyAlertDTO resolveEmergencyAlert(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return communityService.resolveEmergencyAlert(id, body != null ? body : Map.of());
    }

    // 17. MEDIÇÃO INDIVIDUALIZADA DE ÁGUA E GÁS
    @GetMapping("/condominiums/{condominiumId}/utility-readings")
    public List<UtilityReadingDTO> listUtilityReadings(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long unitId
    ) {
        return communityService.listUtilityReadings(condominiumId, year, month, unitId);
    }

    @PostMapping("/condominiums/{condominiumId}/utility-readings")
    public ResponseEntity<UtilityReadingDTO> createUtilityReading(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createUtilityReading(condominiumId, body));
    }

    // 18. CONVITE DIGITAL WHATSAPP
    @GetMapping("/visitors/{id}/digital-pass")
    public VisitorDigitalPassDTO getVisitorDigitalPass(@PathVariable Long id) {
        return communityService.getVisitorDigitalPass(id);
    }
}
