package com.cefet_rj.projeto_final.reports.services;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementRequestDTO;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementUpdateDTO;
import com.cefet_rj.projeto_final.announcements.entities.Announcement;
import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.announcements.services.AnnouncementService;
import com.cefet_rj.projeto_final.auth.security.CachedUserAuthService;
import com.cefet_rj.projeto_final.billing.dtos.BillingPlanRequestDTO;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.CondominiumSubscription;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.BillingCycle;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.enums.SubscriptionStatus;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.CondominiumSubscriptionRepository;
import com.cefet_rj.projeto_final.billing.services.BillingPlanService;
import com.cefet_rj.projeto_final.billing.services.CondominiumSubscriptionService;
import com.cefet_rj.projeto_final.community.entities.CondominiumInfraction;
import com.cefet_rj.projeto_final.community.entities.OccurrenceTicket;
import com.cefet_rj.projeto_final.community.repositories.CondominiumInfractionRepository;
import com.cefet_rj.projeto_final.community.repositories.OccurrenceTicketRepository;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.CondominiumInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.MembershipInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.ResidentInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.SectorInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.UnitInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.CondominiumUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.MembershipUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.ResidentUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.SectorUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.UnitUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.services.CondominiumService;
import com.cefet_rj.projeto_final.condominiums.services.MembershipService;
import com.cefet_rj.projeto_final.condominiums.services.ResidentService;
import com.cefet_rj.projeto_final.condominiums.services.SectorService;
import com.cefet_rj.projeto_final.condominiums.services.UnitService;
import com.cefet_rj.projeto_final.financial.entities.CondominiumExpense;
import com.cefet_rj.projeto_final.financial.repositories.CondominiumExpenseRepository;
import com.cefet_rj.projeto_final.packages.dtos.PackageInsertDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageUpdateDTO;
import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.packages.services.PackageService;
import com.cefet_rj.projeto_final.reports.dtos.AdminDataSchemaDTO.*;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaRequestDTO;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaUpdateDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.services.CommonAreaService;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.shared.observability.AuditLogService;
import com.cefet_rj.projeto_final.shared.validation.UrlSanitizer;
import com.cefet_rj.projeto_final.users.dtos.RoleDTO;
import com.cefet_rj.projeto_final.users.dtos.insert.UserInsertDTO;
import com.cefet_rj.projeto_final.users.dtos.update.UserUpdateDTO;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.cefet_rj.projeto_final.users.services.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Governed Admin DB Record Editor service: supplies typed schemas, enforces protected/immutable
 * field policies, delegates mutations to domain services (including {@link CondominiumSubscriptionService}),
 * and persists structured before/after diffs with mandatory reason into {@code tb_audit_log}.
 */
@Service
@RequiredArgsConstructor
public class AdminDataManagementService {

    private static final Set<String> ALWAYS_PROTECTED_FIELDS = Set.of(
            "id", "version", "passwordHash", "googleSubject", "createdAt", "updatedAt",
            "paidAt", "digitableLine", "correlationId", "timestamp", "principal", "action"
    );

    private static final Set<String> IMMUTABLE_LEDGER_RESOURCES = Set.of(
            "audit-logs", "subscription-history"
    );

    private final UserService userService;
    private final UserRepository userRepository;
    private final CondominiumService condominiumService;
    private final CondominiumRepository condominiumRepository;
    private final CondominiumSubscriptionService subscriptionService;
    private final CondominiumSubscriptionRepository subscriptionRepository;
    private final BillingPlanService billingPlanService;
    private final BillingPlanRepository billingPlanRepository;
    private final SectorService sectorService;
    private final SectorRepository sectorRepository;
    private final UnitService unitService;
    private final UnitRepository unitRepository;
    private final ResidentService residentService;
    private final ResidentRepository residentRepository;
    private final MembershipService membershipService;
    private final MembershipRepository membershipRepository;
    private final CommonAreaService commonAreaService;
    private final CommonAreaRepository commonAreaRepository;
    private final AnnouncementService announcementService;
    private final AnnouncementRepository announcementRepository;
    private final PackageService packageService;
    private final PackageRepository packageRepository;
    private final CondominiumExpenseRepository expenseRepository;
    private final CondominiumInfractionRepository infractionRepository;
    private final OccurrenceTicketRepository ticketRepository;
    private final CachedUserAuthService cachedUserAuthService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public List<AdminResourceMetaDTO> listResourcesMetadata() {
        return List.of(
                new AdminResourceMetaDTO("users", "Usuários", "Identidade", true, true, true, false,
                        "Contas de usuários, perfis globais e dados de contato."),
                new AdminResourceMetaDTO("condominiums", "Condomínios", "Estrutura", true, true, false, false,
                        "Cadastro mestre de condomínios, plano contratado e status ativo."),
                new AdminResourceMetaDTO("condominium-subscriptions", "Assinaturas & Planos", "Comercial", false, true, false, false,
                        "Status operacional (TRIAL/ACTIVE/PAST_DUE/SUSPENDED/CANCELLED), ciclo e troca de plano."),
                new AdminResourceMetaDTO("memberships", "Vínculos (Papéis)", "Identidade", true, true, true, false,
                        "Vínculos entre usuário, condomínio e papel operacional."),
                new AdminResourceMetaDTO("sectors", "Setores / Blocos", "Estrutura", true, true, true, false,
                        "Blocos, ruas, torres ou áreas estruturais do condomínio."),
                new AdminResourceMetaDTO("units", "Unidades", "Estrutura", true, true, true, false,
                        "Apartamentos, salas e casas vinculados a setores."),
                new AdminResourceMetaDTO("residents", "Moradores por Unidade", "Estrutura", true, true, true, false,
                        "Relação de proprietários, inquilinos e dependentes nas unidades."),
                new AdminResourceMetaDTO("common-areas", "Áreas Comuns", "Operação", true, true, false, false,
                        "Espaços reserváveis, capacidade e regras de aprovação."),
                new AdminResourceMetaDTO("billing-plans", "Planos Comerciais", "Comercial", true, true, false, false,
                        "Catálogo SaaS de planos, mensalidade e matriz de 17 recursos."),
                new AdminResourceMetaDTO("packages", "Encomendas", "Portaria", true, true, true, false,
                        "Registros de recebimento e destinatário de encomendas."),
                new AdminResourceMetaDTO("announcements", "Comunicados", "Comunicação", true, true, false, false,
                        "Avisos gerais ou segmentados publicados para o condomínio."),
                new AdminResourceMetaDTO("tickets", "Chamados & Ocorrências", "Comunidade", false, true, false, false,
                        "Triagem administrativa e encerramento de ocorrências."),
                new AdminResourceMetaDTO("expenses", "Despesas", "Financeiro", true, true, false, false,
                        "Lançamentos financeiros de despesas do condomínio."),
                new AdminResourceMetaDTO("infractions", "Notificações & Multas", "Comunidade", false, true, false, false,
                        "Registros disciplinares e julgamento administrativo."),
                new AdminResourceMetaDTO("audit-logs", "Logs de Auditoria", "Governança", false, false, false, true,
                        "Ledger imutável de auditoria do sistema."),
                new AdminResourceMetaDTO("subscription-history", "Histórico de Assinaturas", "Governança", false, false, false, true,
                        "Ledger imutável de transições de planos e status.")
        );
    }

    public ResourceSchemaDTO getSchema(String resource) {
        String key = normalizeResource(resource);
        return switch (key) {
            case "users" -> schema("users", "Usuários", true, true, true, true, false, List.of(
                    idField(),
                    strField("firstName", "Nome", "STRING", true, true, false, 80, null),
                    strField("lastName", "Sobrenome", "STRING", false, true, false, 80, null),
                    strField("email", "E-mail", "EMAIL", true, true, false, 254, "Alterar o e-mail revoga tokens OAuth2 ativos."),
                    strField("phone", "Telefone", "STRING", false, true, false, 40, null),
                    passwordCreateField(),
                    multiEnumField("roles", "Perfis Globais", true, true, false, List.of(
                            new EnumOption("ROLE_RESIDENT", "Morador"),
                            new EnumOption("ROLE_DOORMAN", "Porteiro"),
                            new EnumOption("ROLE_MANAGER", "Síndico"),
                            new EnumOption("ROLE_ADMIN", "Administrador")
                    )),
                    enumField("themePreference", "Preferência de Tema", false, true, false, List.of(
                            new EnumOption("SYSTEM", "Sistema"),
                            new EnumOption("LIGHT", "Claro"),
                            new EnumOption("DARK", "Escuro")
                    ), "SYSTEM"),
                    readOnlyField("googleAccountLinked", "Conta Google Vinculada", "BOOLEAN")
            ));
            case "condominiums" -> schema("condominiums", "Condomínios", true, true, false, true, false, List.of(
                    idField(),
                    strField("name", "Nome do Condomínio", "STRING", true, true, false, 150, null),
                    strField("address", "Endereço", "STRING", true, true, false, 255, null),
                    relationField("billingPlanId", "Plano Comercial", true, true, false, "billing-plans", "name",
                            "Trocas de plano passam automaticamente pelo ciclo de assinatura."),
                    boolField("isActive", "Condomínio Ativo", false, true, false, true),
                    boolField("planSelected", "Plano Selecionado", false, true, false, true),
                    strField("imageUrl", "URL da Fachada", "STRING", false, true, false, 500, null)
            ));
            case "condominium-subscriptions" -> schema("condominium-subscriptions", "Assinaturas & Planos", false, true, false, true, false, List.of(
                    idField(),
                    relationField("condominiumId", "Condomínio", true, false, true, "condominiums", "name",
                            "Vínculo contratual imutável."),
                    relationField("billingPlanId", "Plano Comercial", true, true, false, "billing-plans", "name", null),
                    enumField("status", "Status Operacional", true, true, false, List.of(
                            new EnumOption("ACTIVE", "ACTIVE (Liberado)"),
                            new EnumOption("TRIAL", "TRIAL (Avaliação)"),
                            new EnumOption("PAST_DUE", "PAST_DUE (Fatura Pendente)"),
                            new EnumOption("SUSPENDED", "SUSPENDED (Bloqueado Fail-Closed)"),
                            new EnumOption("CANCELLED", "CANCELLED (Cancelado Fail-Closed)")
                    ), "ACTIVE"),
                    enumField("billingCycle", "Ciclo de Cobrança", true, true, false, List.of(
                            new EnumOption("MONTHLY", "Mensal"),
                            new EnumOption("ANNUAL", "Anual")
                    ), "MONTHLY"),
                    boolField("autoRenew", "Renovação Automática", false, true, false, true),
                    readOnlyField("currentPeriodEnd", "Fim do Período Atual", "DATETIME")
            ));
            case "memberships" -> schema("memberships", "Vínculos (Papéis)", true, true, true, true, false, List.of(
                    idField(),
                    relationField("userId", "Usuário", true, true, true, "users", "email", null),
                    relationField("condominiumId", "Condomínio", true, true, true, "condominiums", "name", null),
                    enumField("authority", "Papel no Condomínio", true, true, false, List.of(
                            new EnumOption("ROLE_RESIDENT", "Morador"),
                            new EnumOption("ROLE_DOORMAN", "Porteiro"),
                            new EnumOption("ROLE_MANAGER", "Síndico")
                    ), "ROLE_RESIDENT"),
                    enumField("status", "Status do Vínculo", false, true, false, List.of(
                            new EnumOption("ACTIVE", "Ativo"),
                            new EnumOption("PENDING", "Pendente"),
                            new EnumOption("REJECTED", "Recusado"),
                            new EnumOption("SUSPENDED", "Suspenso")
                    ), "ACTIVE")
            ));
            case "sectors" -> schema("sectors", "Setores / Blocos", true, true, true, false, false, List.of(
                    idField(),
                    strField("name", "Nome do Setor", "STRING", true, true, false, 100, null),
                    enumField("structuralType", "Tipo Estrutural", true, true, false, List.of(
                            new EnumOption("BLOCK", "Bloco"),
                            new EnumOption("STREET", "Rua"),
                            new EnumOption("COURT", "Quadra"),
                            new EnumOption("GRILL", "Área Gourmet")
                    ), "BLOCK"),
                    relationField("condominiumId", "Condomínio", true, true, true, "condominiums", "name", null),
                    strField("imageUrl", "Imagem do Setor", "STRING", false, true, false, 500, null)
            ));
            case "units" -> schema("units", "Unidades", true, true, true, false, false, List.of(
                    idField(),
                    strField("identifier", "Identificador (ex.: 101)", "STRING", true, true, false, 60, null),
                    relationField("sectorId", "Setor / Bloco", true, true, false, "sectors", "name", null)
            ));
            case "residents" -> schema("residents", "Moradores por Unidade", true, true, true, true, false, List.of(
                    idField(),
                    relationField("userId", "Usuário", true, true, true, "users", "email", null),
                    relationField("unitId", "Unidade", true, true, false, "units", "identifier", null),
                    enumField("type", "Tipo de Vínculo", true, true, false, List.of(
                            new EnumOption("OWNER", "Proprietário"),
                            new EnumOption("TENANT", "Inquilino"),
                            new EnumOption("DEPENDENT", "Dependente")
                    ), "OWNER")
            ));
            case "common-areas" -> schema("common-areas", "Áreas Comuns", true, true, false, false, false, List.of(
                    idField(),
                    relationField("condominiumId", "Condomínio", true, true, true, "condominiums", "name", null),
                    strField("name", "Nome da Área", "STRING", true, true, false, 120, null),
                    strField("description", "Descrição", "TEXT", false, true, false, 1000, null),
                    intField("capacity", "Capacidade (pessoas)", true, true, 10),
                    boolField("requiresApproval", "Requer Aprovação", false, true, false, true),
                    intField("openingHour", "Hora de Abertura (0-23)", false, true, 8),
                    intField("closingHour", "Hora de Fechamento (1-24)", false, true, 22),
                    boolField("active", "Área Ativa", false, true, false, true),
                    strField("imageUrl", "Foto da Área", "STRING", false, true, false, 500, null)
            ));
            case "billing-plans" -> schema("billing-plans", "Planos Comerciais", true, true, false, true, false, List.of(
                    idField(),
                    strField("name", "Nome do Plano", "STRING", true, true, false, 100, null),
                    strField("code", "Código", "STRING", false, true, false, 40, null),
                    strField("description", "Descrição Comercial", "TEXT", false, true, false, 500, null),
                    decimalField("monthlyFee", "Mensalidade (R$)", true, true, 0),
                    intField("maxUnits", "Limite de Unidades (opcional)", false, true, null),
                    intField("displayOrder", "Ordem de Exibição", false, true, 0),
                    boolField("active", "Plano Ativo", false, true, false, true),
                    multiEnumField("features", "Recursos Liberados", true, true, false, Arrays.stream(FeatureCode.values())
                            .map(f -> new EnumOption(f.name(), f.name()))
                            .toList())
            ));
            case "packages" -> schema("packages", "Encomendas", true, true, true, true, false, List.of(
                    idField(),
                    strField("description", "Descrição da Encomenda", "STRING", true, true, false, 255, null),
                    relationField("unitId", "Unidade", true, true, false, "units", "identifier", null),
                    relationField("recipientId", "Destinatário (opcional)", false, true, false, "users", "email", null),
                    readOnlyField("status", "Status Operacional", "STRING")
            ));
            case "announcements" -> schema("announcements", "Comunicados", true, true, false, true, false, List.of(
                    idField(),
                    relationField("condominiumId", "Condomínio", true, true, true, "condominiums", "name", null),
                    strField("title", "Título", "STRING", true, true, false, 150, null),
                    strField("content", "Conteúdo", "TEXT", true, true, false, 4000, null),
                    relationField("unitId", "Unidade Destinatária (opcional)", false, true, true, "units", "identifier", null),
                    boolField("active", "Ativo", false, true, false, true)
            ));
            case "tickets" -> schema("tickets", "Chamados & Ocorrências", false, true, false, true, false, List.of(
                    idField(),
                    strField("title", "Título", "STRING", true, true, false, 160, null),
                    strField("description", "Descrição", "TEXT", true, true, false, 3000, null),
                    enumField("status", "Status do Chamado", true, true, false, List.of(
                            new EnumOption("OPEN", "Aberto"),
                            new EnumOption("IN_PROGRESS", "Em Análise"),
                            new EnumOption("RESOLVED", "Resolvido"),
                            new EnumOption("CLOSED", "Fechado")
                    ), "OPEN"),
                    strField("adminResponse", "Resposta Administrativa", "TEXT", false, true, false, 1500, null)
            ));
            case "expenses" -> schema("expenses", "Despesas", true, true, false, true, false, List.of(
                    idField(),
                    relationField("condominiumId", "Condomínio", true, true, true, "condominiums", "name", null),
                    enumField("category", "Categoria", true, true, false, List.of(
                            new EnumOption("CONSUMO", "Consumo"),
                            new EnumOption("MANUTENCAO", "Manutenção"),
                            new EnumOption("PESSOAL", "Pessoal"),
                            new EnumOption("ADMINISTRATIVO", "Administrativo"),
                            new EnumOption("SEGURO", "Seguro"),
                            new EnumOption("REFORMA", "Reforma"),
                            new EnumOption("OUTROS", "Outros")
                    ), "ADMINISTRATIVO"),
                    strField("description", "Descrição da Despesa", "STRING", true, true, false, 500, null),
                    decimalField("amount", "Valor (R$)", true, true, 0),
                    dateField("dueDate", "Data de Vencimento", true, true),
                    enumField("status", "Status", true, true, false, List.of(
                            new EnumOption("PENDING", "Pendente"),
                            new EnumOption("PAID", "Paga"),
                            new EnumOption("CANCELLED", "Cancelada")
                    ), "PENDING"),
                    strField("supplier", "Fornecedor", "STRING", false, true, false, 160, null)
            ));
            case "infractions" -> schema("infractions", "Notificações & Multas", false, true, false, true, false, List.of(
                    idField(),
                    strField("title", "Título", "STRING", true, true, false, 180, null),
                    strField("description", "Descrição", "TEXT", true, true, false, 2000, null),
                    decimalField("penaltyAmount", "Valor da Penalidade (R$)", false, true, 0),
                    enumField("status", "Status Disciplinar", true, true, false, List.of(
                            new EnumOption("ISSUED", "Emitida"),
                            new EnumOption("CONTESTED", "Contestada"),
                            new EnumOption("UPHELD", "Mantida"),
                            new EnumOption("DISMISSED", "Cancelada")
                    ), "ISSUED"),
                    strField("managerJudgment", "Parecer Administrativo", "TEXT", false, true, false, 1500, null)
            ));
            case "audit-logs" -> schema("audit-logs", "Logs de Auditoria", false, false, false, false, true, List.of(
                    idField(),
                    readOnlyField("timestamp", "Carimbo de Data/Hora", "DATETIME"),
                    readOnlyField("principal", "Operador", "STRING"),
                    readOnlyField("action", "Ação", "STRING"),
                    readOnlyField("resourceType", "Recurso", "STRING"),
                    readOnlyField("reason", "Motivo", "STRING")
            ));
            case "subscription-history" -> schema("subscription-history", "Histórico de Assinaturas", false, false, false, false, true, List.of(
                    idField(),
                    readOnlyField("eventType", "Evento", "STRING"),
                    readOnlyField("previousStatus", "Status Anterior", "STRING"),
                    readOnlyField("newStatus", "Novo Status", "STRING"),
                    readOnlyField("reason", "Motivo", "STRING")
            ));
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso administrativo desconhecido: " + resource);
        };
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRecord(String resource, Long id) {
        String key = normalizeResource(resource);
        return switch (key) {
            case "users" -> userToMap(userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id)));
            case "condominiums" -> condoToMap(condominiumRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + id)));
            case "condominium-subscriptions" -> subscriptionToMap(subscriptionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada: " + id)));
            case "memberships" -> membershipToMap(membershipRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Vínculo não encontrado: " + id)));
            case "sectors" -> sectorToMap(sectorRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado: " + id)));
            case "units" -> unitToMap(unitRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + id)));
            case "residents" -> residentToMap(residentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + id)));
            case "common-areas" -> commonAreaToMap(commonAreaRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Área comum não encontrada: " + id)));
            case "billing-plans" -> billingPlanToMap(billingPlanRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id)));
            case "packages" -> packageToMap(packageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Encomenda não encontrada: " + id)));
            case "announcements" -> announcementToMap(announcementRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado: " + id)));
            case "tickets" -> ticketToMap(ticketRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado: " + id)));
            case "expenses" -> expenseToMap(expenseRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada: " + id)));
            case "infractions" -> infractionToMap(infractionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Infração não encontrada: " + id)));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Visualização individual de edição não suportada para o ledger: " + key);
        };
    }

    @Transactional
    public AdminRecordMutationResponse createRecord(String resource, AdminRecordMutationRequest request) {
        String key = normalizeResource(resource);
        ResourceSchemaDTO schema = getSchema(key);
        if (!schema.creatable() || schema.immutableLedger()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Criação direta bloqueada para o recurso: " + key);
        }
        Map<String, Object> data = request != null && request.data() != null ? request.data() : Map.of();
        validateNoForbiddenFields(schema, data, true);

        Map<String, Object> createdMap = executeCreate(key, data);
        Long createdId = asLong(createdMap.get("id"));
        Long condoId = asLong(createdMap.get("condominiumId"));
        String condoName = stringOrNull(createdMap.get("condominiumName"));
        String reason = normalizeReason(request != null ? request.reason() : null, "Criação administrativa via Console de Dados");

        auditLogService.logAdminDataMutation(
                null,
                "ADMIN_DATA_CREATE_" + key.toUpperCase(Locale.ROOT).replace('-', '_'),
                key.toUpperCase(Locale.ROOT),
                createdId != null ? String.valueOf(createdId) : "NEW",
                condoId,
                condoName,
                "Registro criado via Editor Estruturado (" + key + ")",
                null,
                toJson(createdMap),
                reason
        );

        return new AdminRecordMutationResponse(key, createdId, "CREATE", reason, createdMap, null, createdMap);
    }

    @Transactional
    public AdminRecordMutationResponse updateRecord(String resource, Long id, AdminRecordMutationRequest request) {
        String key = normalizeResource(resource);
        ResourceSchemaDTO schema = getSchema(key);
        if (!schema.editable() || schema.immutableLedger()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Edição bloqueada: '" + key + "' é um ledger imutável ou somente leitura.");
        }
        Map<String, Object> data = request != null && request.data() != null ? request.data() : Map.of();
        if (data.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum campo enviado para atualização.");
        }
        validateNoForbiddenFields(schema, data, false);

        String reason = request != null && request.reason() != null ? request.reason().trim() : "";
        if (schema.requiresReason() && reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o motivo operacional desta alteração (obrigatório para auditoria).");
        }
        String effectiveReason = reason.isBlank() ? "Alteração administrativa via Editor Estruturado" : reason;

        Map<String, Object> beforeMap = new LinkedHashMap<>(getRecord(key, id));
        Map<String, Object> afterMap = executeUpdate(key, id, data, effectiveReason);

        Long condoId = asLong(afterMap.get("condominiumId"));
        String condoName = stringOrNull(afterMap.get("condominiumName"));

        auditLogService.logAdminDataMutation(
                null,
                "ADMIN_DATA_UPDATE_" + key.toUpperCase(Locale.ROOT).replace('-', '_'),
                key.toUpperCase(Locale.ROOT),
                String.valueOf(id),
                condoId,
                condoName,
                "Registro #" + id + " atualizado em " + key,
                toJson(beforeMap),
                toJson(afterMap),
                effectiveReason
        );

        return new AdminRecordMutationResponse(key, id, "UPDATE", effectiveReason, afterMap, beforeMap, afterMap);
    }

    @Transactional
    public AdminRecordMutationResponse deleteRecord(String resource, Long id, String rawReason) {
        String key = normalizeResource(resource);
        ResourceSchemaDTO schema = getSchema(key);
        if (!schema.deletable() || schema.immutableLedger()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Exclusão direta bloqueada pela política de integridade para '" + key + "'.");
        }
        String reason = rawReason != null ? rawReason.trim() : "";
        if (reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o motivo da exclusão para registro imutável na auditoria.");
        }

        Map<String, Object> beforeMap = new LinkedHashMap<>(getRecord(key, id));
        executeDelete(key, id);

        Long condoId = asLong(beforeMap.get("condominiumId"));
        String condoName = stringOrNull(beforeMap.get("condominiumName"));

        auditLogService.logAdminDataMutation(
                null,
                "ADMIN_DATA_DELETE_" + key.toUpperCase(Locale.ROOT).replace('-', '_'),
                key.toUpperCase(Locale.ROOT),
                String.valueOf(id),
                condoId,
                condoName,
                "Registro #" + id + " removido em " + key,
                toJson(beforeMap),
                null,
                reason
        );

        return new AdminRecordMutationResponse(key, id, "DELETE", reason, null, beforeMap, null);
    }

    private Map<String, Object> executeCreate(String key, Map<String, Object> data) {
        return switch (key) {
            case "users" -> {
                String password = requiredString(data, "password", "Senha é obrigatória na criação de usuário.");
                Set<RoleDTO> roles = extractRoleDTOs(data.get("roles"));
                UserInsertDTO dto = new UserInsertDTO(
                        requiredString(data, "firstName", "Nome é obrigatório."),
                        stringOrNull(data.get("lastName")),
                        requiredString(data, "email", "E-mail é obrigatório."),
                        password,
                        roles
                );
                var created = userService.insert(dto);
                User entity = userRepository.findById(created.id())
                        .orElseThrow(() -> new ResourceNotFoundException("Usuário recém-criado não localizado."));
                if (data.containsKey("phone")) entity.setPhone(stringOrNull(data.get("phone")));
                if (data.containsKey("themePreference")) entity.setThemePreference(normalizeTheme(stringOrNull(data.get("themePreference"))));
                userRepository.save(entity);
                yield userToMap(entity);
            }
            case "condominiums" -> {
                Long planId = asLong(data.get("billingPlanId"));
                String planName = planId != null
                        ? billingPlanService.findById(planId).getName()
                        : requiredString(data, "billingPlan", "Selecione um plano comercial.");
                CondominiumInsertDTO dto = new CondominiumInsertDTO(
                        requiredString(data, "name", "Nome é obrigatório."),
                        requiredString(data, "address", "Endereço é obrigatório."),
                        planName,
                        asBoolean(data.get("isActive"), true),
                        UrlSanitizer.sanitizeMediaUrl(stringOrNull(data.get("imageUrl")), "imageUrl")
                );
                var created = condominiumService.insert(dto);
                yield condoToMap(condominiumRepository.findById(created.id()).orElseThrow());
            }
            case "memberships" -> {
                MembershipInsertDTO dto = new MembershipInsertDTO(
                        requiredLong(data, "userId", "Usuário é obrigatório."),
                        requiredLong(data, "condominiumId", "Condomínio é obrigatório."),
                        requiredString(data, "authority", "Papel é obrigatório.")
                );
                var created = membershipService.insert(dto);
                Membership entity = membershipRepository.findById(created.id()).orElseThrow();
                if (data.containsKey("status")) {
                    entity.setStatus(parseEnum(MembershipStatus.class, data.get("status"), MembershipStatus.ACTIVE));
                    membershipRepository.save(entity);
                }
                yield membershipToMap(entity);
            }
            case "sectors" -> {
                SectorInsertDTO dto = new SectorInsertDTO(
                        requiredString(data, "name", "Nome do setor é obrigatório."),
                        requiredString(data, "structuralType", "Tipo estrutural é obrigatório."),
                        requiredLong(data, "condominiumId", "Condomínio é obrigatório."),
                        UrlSanitizer.sanitizeMediaUrl(stringOrNull(data.get("imageUrl")), "imageUrl")
                );
                var created = sectorService.insert(dto);
                yield sectorToMap(sectorRepository.findById(created.id()).orElseThrow());
            }
            case "units" -> {
                UnitInsertDTO dto = new UnitInsertDTO(
                        requiredString(data, "identifier", "Identificador da unidade é obrigatório."),
                        requiredLong(data, "sectorId", "Setor é obrigatório.")
                );
                var created = unitService.insert(dto);
                yield unitToMap(unitRepository.findById(created.id()).orElseThrow());
            }
            case "residents" -> {
                ResidentInsertDTO dto = new ResidentInsertDTO(
                        requiredLong(data, "userId", "Usuário é obrigatório."),
                        requiredLong(data, "unitId", "Unidade é obrigatória."),
                        requiredString(data, "type", "Tipo de morador é obrigatório.")
                );
                var created = residentService.insert(dto);
                yield residentToMap(residentRepository.findById(created.id()).orElseThrow());
            }
            case "common-areas" -> {
                Long condoId = requiredLong(data, "condominiumId", "Condomínio é obrigatório.");
                CommonAreaRequestDTO dto = new CommonAreaRequestDTO(
                        requiredString(data, "name", "Nome da área é obrigatório."),
                        stringOrNull(data.get("description")),
                        asInt(data.get("capacity"), 10),
                        asBoolean(data.get("requiresApproval"), true),
                        null,
                        asInt(data.get("openingHour"), 8),
                        asInt(data.get("closingHour"), 22),
                        UrlSanitizer.sanitizeMediaUrl(stringOrNull(data.get("imageUrl")), "imageUrl")
                );
                var created = commonAreaService.create(condoId, dto);
                yield commonAreaToMap(commonAreaRepository.findById(created.id()).orElseThrow());
            }
            case "billing-plans" -> {
                BillingPlanRequestDTO dto = new BillingPlanRequestDTO(
                        requiredString(data, "name", "Nome do plano é obrigatório."),
                        requiredDecimal(data, "monthlyFee", "Mensalidade é obrigatória."),
                        extractFeatureCodes(data.get("features"))
                );
                var created = billingPlanService.create(dto);
                BillingPlan plan = billingPlanRepository.findById(created.id()).orElseThrow();
                applyExtendedPlanFields(plan, data);
                billingPlanRepository.save(plan);
                yield billingPlanToMap(plan);
            }
            case "packages" -> {
                PackageInsertDTO dto = new PackageInsertDTO(
                        requiredString(data, "description", "Descrição é obrigatória."),
                        asLong(data.get("recipientId")),
                        requiredLong(data, "unitId", "Unidade é obrigatória.")
                );
                var created = packageService.registerReceived(dto);
                yield packageToMap(packageRepository.findById(created.id()).orElseThrow());
            }
            case "announcements" -> {
                Long condoId = requiredLong(data, "condominiumId", "Condomínio é obrigatório.");
                AnnouncementRequestDTO dto = new AnnouncementRequestDTO(
                        requiredString(data, "title", "Título é obrigatório."),
                        requiredString(data, "content", "Conteúdo é obrigatório."),
                        null,
                        null,
                        asLong(data.get("unitId")),
                        null,
                        null
                );
                var created = announcementService.create(condoId, dto);
                yield announcementToMap(announcementRepository.findById(created.id()).orElseThrow());
            }
            case "expenses" -> {
                Long condoId = requiredLong(data, "condominiumId", "Condomínio é obrigatório.");
                Condominium condo = condominiumRepository.findById(condoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condoId));
                CondominiumExpense exp = new CondominiumExpense();
                exp.setCondominium(condo);
                exp.setCategory(requiredString(data, "category", "Categoria é obrigatória."));
                exp.setDescription(requiredString(data, "description", "Descrição é obrigatória."));
                exp.setAmount(requiredDecimal(data, "amount", "Valor é obrigatório."));
                exp.setDueDate(parseDate(data.get("dueDate"), LocalDate.now().plusDays(10)));
                exp.setStatus(stringOrDefault(data.get("status"), "PENDING"));
                exp.setSupplier(stringOrNull(data.get("supplier")));
                exp = expenseRepository.save(exp);
                yield expenseToMap(exp);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Criação não suportada para: " + key);
        };
    }

    private Map<String, Object> executeUpdate(String key, Long id, Map<String, Object> data, String reason) {
        return switch (key) {
            case "users" -> {
                User current = userRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
                Set<RoleDTO> roles = data.containsKey("roles")
                        ? extractRoleDTOs(data.get("roles"))
                        : current.getRoles().stream().map(RoleDTO::new).collect(java.util.stream.Collectors.toSet());
                UserUpdateDTO dto = new UserUpdateDTO(
                        stringOrDefault(data.get("firstName"), current.getFirstName()),
                        data.containsKey("lastName") ? stringOrNull(data.get("lastName")) : current.getLastName(),
                        stringOrDefault(data.get("email"), current.getEmail()),
                        roles
                );
                userService.update(id, dto);
                User updated = userRepository.findById(id).orElseThrow();
                if (data.containsKey("phone")) updated.setPhone(stringOrNull(data.get("phone")));
                if (data.containsKey("themePreference")) {
                    updated.setThemePreference(normalizeTheme(stringOrNull(data.get("themePreference"))));
                }
                updated = userRepository.save(updated);
                cachedUserAuthService.evictUserAuth(updated.getEmail());
                yield userToMap(updated);
            }
            case "condominiums" -> {
                Condominium current = condominiumRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + id));
                Long newPlanId = asLong(data.get("billingPlanId"));
                String targetPlanName = current.getBillingPlan() != null ? current.getBillingPlan().getName() : "Essencial";
                if (newPlanId != null && (current.getBillingPlan() == null || !newPlanId.equals(current.getBillingPlan().getId()))) {
                    subscriptionService.changePlan(id, newPlanId, null, reason);
                    targetPlanName = billingPlanService.findById(newPlanId).getName();
                }
                CondominiumUpdateDTO dto = new CondominiumUpdateDTO(
                        stringOrDefault(data.get("name"), current.getName()),
                        stringOrDefault(data.get("address"), current.getAddress()),
                        targetPlanName,
                        asBoolean(data.get("isActive"), Boolean.TRUE.equals(current.getIsActive())),
                        data.containsKey("imageUrl")
                                ? UrlSanitizer.sanitizeMediaUrl(stringOrNull(data.get("imageUrl")), "imageUrl")
                                : current.getImageUrl()
                );
                condominiumService.update(id, dto);
                Condominium refreshed = condominiumRepository.findById(id).orElseThrow();
                if (data.containsKey("planSelected")) {
                    refreshed.setPlanSelected(asBoolean(data.get("planSelected"), Boolean.TRUE.equals(refreshed.getPlanSelected())));
                    refreshed = condominiumRepository.save(refreshed);
                }
                yield condoToMap(refreshed);
            }
            case "condominium-subscriptions" -> {
                CondominiumSubscription sub = subscriptionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada: " + id));
                Long condoId = sub.getCondominium().getId();
                if (data.containsKey("billingPlanId") || data.containsKey("billingCycle")) {
                    Long planId = data.containsKey("billingPlanId")
                            ? requiredLong(data, "billingPlanId", "Plano inválido.")
                            : sub.getBillingPlan().getId();
                    BillingCycle cycle = data.containsKey("billingCycle")
                            ? parseEnum(BillingCycle.class, data.get("billingCycle"), sub.getBillingCycle())
                            : sub.getBillingCycle();
                    subscriptionService.changePlan(condoId, planId, cycle, reason);
                }
                if (data.containsKey("status")) {
                    SubscriptionStatus status = parseEnum(SubscriptionStatus.class, data.get("status"), sub.getStatus());
                    subscriptionService.updateStatus(condoId, status, reason);
                }
                if (data.containsKey("autoRenew")) {
                    CondominiumSubscription reloaded = subscriptionRepository.findById(id).orElseThrow();
                    reloaded.setAutoRenew(asBoolean(data.get("autoRenew"), Boolean.TRUE.equals(reloaded.getAutoRenew())));
                    reloaded.setUpdatedAt(LocalDateTime.now());
                    subscriptionRepository.save(reloaded);
                }
                yield subscriptionToMap(subscriptionRepository.findById(id).orElseThrow());
            }
            case "memberships" -> {
                Membership current = membershipRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Vínculo não encontrado: " + id));
                if (data.containsKey("authority")) {
                    membershipService.update(id, new MembershipUpdateDTO(
                            current.getUser().getId(),
                            current.getCondominium().getId(),
                            stringOrDefault(data.get("authority"), current.getRole().getAuthority())
                    ));
                }
                Membership refreshed = membershipRepository.findById(id).orElseThrow();
                if (data.containsKey("status")) {
                    refreshed.setStatus(parseEnum(MembershipStatus.class, data.get("status"), refreshed.getStatus()));
                    refreshed = membershipRepository.save(refreshed);
                    cachedUserAuthService.evictUserAuth(refreshed.getUser().getEmail());
                }
                yield membershipToMap(refreshed);
            }
            case "sectors" -> {
                Sector current = sectorRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado: " + id));
                sectorService.update(id, new SectorUpdateDTO(
                        stringOrDefault(data.get("name"), current.getName()),
                        stringOrDefault(data.get("structuralType"), current.getStructuralType().name()),
                        current.getCondominium().getId(),
                        data.containsKey("imageUrl")
                                ? UrlSanitizer.sanitizeMediaUrl(stringOrNull(data.get("imageUrl")), "imageUrl")
                                : current.getImageUrl()
                ));
                yield sectorToMap(sectorRepository.findById(id).orElseThrow());
            }
            case "units" -> {
                Unit current = unitRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + id));
                unitService.update(id, new UnitUpdateDTO(
                        stringOrDefault(data.get("identifier"), current.getIdentifier()),
                        data.containsKey("sectorId") ? asLong(data.get("sectorId")) : current.getSector().getId()
                ));
                yield unitToMap(unitRepository.findById(id).orElseThrow());
            }
            case "residents" -> {
                Resident current = residentRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + id));
                residentService.update(id, new ResidentUpdateDTO(
                        current.getUser().getId(),
                        data.containsKey("unitId") ? asLong(data.get("unitId")) : current.getUnit().getId(),
                        stringOrDefault(data.get("type"), current.getType().name())
                ));
                yield residentToMap(residentRepository.findById(id).orElseThrow());
            }
            case "common-areas" -> {
                CommonArea current = commonAreaRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Área comum não encontrada: " + id));
                commonAreaService.update(id, new CommonAreaUpdateDTO(
                        stringOrDefault(data.get("name"), current.getName()),
                        data.containsKey("description") ? stringOrNull(data.get("description")) : current.getDescription(),
                        data.containsKey("capacity") ? asInt(data.get("capacity"), current.getCapacity()) : current.getCapacity(),
                        asBoolean(data.get("requiresApproval"), Boolean.TRUE.equals(current.getRequiresApproval())),
                        current.getRules(),
                        data.containsKey("openingHour") ? asInt(data.get("openingHour"), current.getOpeningHour()) : current.getOpeningHour(),
                        data.containsKey("closingHour") ? asInt(data.get("closingHour"), current.getClosingHour()) : current.getClosingHour(),
                        data.containsKey("imageUrl") ? UrlSanitizer.sanitizeMediaUrl(stringOrNull(data.get("imageUrl")), "imageUrl") : current.getImageUrl()
                ));
                if (data.containsKey("active")) {
                    if (asBoolean(data.get("active"), true)) commonAreaService.activate(id);
                    else commonAreaService.deactivate(id);
                }
                yield commonAreaToMap(commonAreaRepository.findById(id).orElseThrow());
            }
            case "billing-plans" -> {
                BillingPlan current = billingPlanRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));
                Set<FeatureCode> features = data.containsKey("features")
                        ? extractFeatureCodes(data.get("features"))
                        : current.getFeatures().stream()
                                .map(f -> FeatureCode.valueOf(f.getCode()))
                                .collect(java.util.stream.Collectors.toSet());
                billingPlanService.update(id, new BillingPlanRequestDTO(
                        stringOrDefault(data.get("name"), current.getName()),
                        data.containsKey("monthlyFee") ? requiredDecimal(data, "monthlyFee", "Mensalidade inválida.") : current.getMonthlyFee(),
                        features
                ));
                BillingPlan refreshed = billingPlanRepository.findById(id).orElseThrow();
                applyExtendedPlanFields(refreshed, data);
                if (data.containsKey("active")) {
                    if (asBoolean(data.get("active"), true)) billingPlanService.activate(id);
                    else billingPlanService.deactivate(id);
                    refreshed = billingPlanRepository.findById(id).orElseThrow();
                } else {
                    refreshed = billingPlanRepository.save(refreshed);
                }
                yield billingPlanToMap(refreshed);
            }
            case "packages" -> {
                Package current = packageRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Encomenda não encontrada: " + id));
                packageService.update(id, new PackageUpdateDTO(
                        stringOrDefault(data.get("description"), current.getDescription()),
                        data.containsKey("recipientId") ? asLong(data.get("recipientId"))
                                : (current.getRecipient() != null ? current.getRecipient().getId() : null),
                        data.containsKey("unitId") ? asLong(data.get("unitId")) : current.getUnit().getId()
                ));
                yield packageToMap(packageRepository.findById(id).orElseThrow());
            }
            case "announcements" -> {
                Announcement current = announcementRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado: " + id));
                announcementService.update(id, new AnnouncementUpdateDTO(
                        stringOrDefault(data.get("title"), current.getTitle()),
                        stringOrDefault(data.get("content"), current.getContent()),
                        current.getExpiresAt()
                ));
                if (data.containsKey("active")) {
                    if (asBoolean(data.get("active"), true)) announcementService.activate(id);
                    else announcementService.deactivate(id);
                }
                yield announcementToMap(announcementRepository.findById(id).orElseThrow());
            }
            case "tickets" -> {
                OccurrenceTicket ticket = ticketRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado: " + id));
                if (data.containsKey("title")) ticket.setTitle(requiredString(data, "title", "Título obrigatório."));
                if (data.containsKey("description")) ticket.setDescription(requiredString(data, "description", "Descrição obrigatória."));
                if (data.containsKey("status")) ticket.setStatus(requiredString(data, "status", "Status obrigatório."));
                if (data.containsKey("adminResponse")) ticket.setAdminResponse(stringOrNull(data.get("adminResponse")));
                ticket.setUpdatedAt(LocalDateTime.now());
                if ("RESOLVED".equals(ticket.getStatus()) && ticket.getResolvedAt() == null) {
                    ticket.setResolvedAt(LocalDateTime.now());
                }
                yield ticketToMap(ticketRepository.save(ticket));
            }
            case "expenses" -> {
                CondominiumExpense exp = expenseRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada: " + id));
                if (data.containsKey("category")) exp.setCategory(requiredString(data, "category", "Categoria obrigatória."));
                if (data.containsKey("description")) exp.setDescription(requiredString(data, "description", "Descrição obrigatória."));
                if (data.containsKey("amount")) exp.setAmount(requiredDecimal(data, "amount", "Valor inválido."));
                if (data.containsKey("dueDate")) exp.setDueDate(parseDate(data.get("dueDate"), exp.getDueDate()));
                if (data.containsKey("status")) exp.setStatus(requiredString(data, "status", "Status obrigatório."));
                if (data.containsKey("supplier")) exp.setSupplier(stringOrNull(data.get("supplier")));
                yield expenseToMap(expenseRepository.save(exp));
            }
            case "infractions" -> {
                CondominiumInfraction inf = infractionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Infração não encontrada: " + id));
                if (data.containsKey("title")) inf.setTitle(requiredString(data, "title", "Título obrigatório."));
                if (data.containsKey("description")) inf.setDescription(requiredString(data, "description", "Descrição obrigatória."));
                if (data.containsKey("penaltyAmount")) inf.setPenaltyAmount(requiredDecimal(data, "penaltyAmount", "Valor inválido."));
                if (data.containsKey("status")) inf.setStatus(requiredString(data, "status", "Status obrigatório."));
                if (data.containsKey("managerJudgment")) inf.setManagerJudgment(stringOrNull(data.get("managerJudgment")));
                inf.setUpdatedAt(LocalDateTime.now());
                yield infractionToMap(infractionRepository.save(inf));
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Edição não suportada para: " + key);
        };
    }

    private void executeDelete(String key, Long id) {
        switch (key) {
            case "users" -> userService.delete(id);
            case "memberships" -> membershipService.delete(id);
            case "sectors" -> sectorService.delete(id);
            case "units" -> unitService.delete(id);
            case "residents" -> residentService.delete(id);
            case "packages" -> packageService.delete(id);
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Exclusão direta desabilitada pela política de integridade para: " + key);
        }
    }

    private void applyExtendedPlanFields(BillingPlan plan, Map<String, Object> data) {
        if (data.containsKey("code")) plan.setCode(stringOrNull(data.get("code")));
        if (data.containsKey("description")) plan.setDescription(stringOrNull(data.get("description")));
        if (data.containsKey("maxUnits")) plan.setMaxUnits(asInt(data.get("maxUnits"), null));
        if (data.containsKey("displayOrder")) plan.setDisplayOrder(asInt(data.get("displayOrder"), 0));
    }

    private void validateNoForbiddenFields(ResourceSchemaDTO schema, Map<String, Object> payload, boolean isCreate) {
        Map<String, FieldDescriptor> fieldIndex = new HashMap<>();
        for (FieldDescriptor f : schema.fields()) {
            fieldIndex.put(f.name(), f);
        }
        for (String incomingKey : payload.keySet()) {
            if (ALWAYS_PROTECTED_FIELDS.contains(incomingKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Campo protegido pela política de auditoria não pode ser alterado: " + incomingKey);
            }
            FieldDescriptor fd = fieldIndex.get(incomingKey);
            if (fd == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Campo desconhecido ou não autorizado para '" + schema.resource() + "': " + incomingKey);
            }
            if (!fd.editable() || (!isCreate && fd.immutableOnUpdate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Campo somente leitura/imutável para '" + schema.resource() + "': " + incomingKey);
            }
        }
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("firstName", u.getFirstName());
        m.put("lastName", u.getLastName());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("roles", u.getRoles().stream().map(r -> r.getAuthority()).sorted().toList());
        m.put("themePreference", u.getThemePreference() != null ? u.getThemePreference() : "SYSTEM");
        m.put("googleAccountLinked", u.getGoogleSubject() != null && !u.getGoogleSubject().isBlank());
        return m;
    }

    private Map<String, Object> condoToMap(Condominium c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("condominiumId", c.getId());
        m.put("condominiumName", c.getName());
        m.put("name", c.getName());
        m.put("address", c.getAddress());
        m.put("billingPlanId", c.getBillingPlan() != null ? c.getBillingPlan().getId() : null);
        m.put("billingPlanName", c.getBillingPlan() != null ? c.getBillingPlan().getName() : null);
        m.put("isActive", c.getIsActive());
        m.put("planSelected", c.getPlanSelected());
        m.put("imageUrl", c.getImageUrl());
        return m;
    }

    private Map<String, Object> subscriptionToMap(CondominiumSubscription s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("condominiumId", s.getCondominium().getId());
        m.put("condominiumName", s.getCondominium().getName());
        m.put("billingPlanId", s.getBillingPlan().getId());
        m.put("billingPlanName", s.getBillingPlan().getName());
        m.put("status", s.getStatus().name());
        m.put("billingCycle", s.getBillingCycle().name());
        m.put("autoRenew", s.getAutoRenew());
        m.put("currentPeriodEnd", s.getCurrentPeriodEnd() != null ? s.getCurrentPeriodEnd().toString() : null);
        return m;
    }

    private Map<String, Object> membershipToMap(Membership mem) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mem.getId());
        m.put("userId", mem.getUser().getId());
        m.put("userEmail", mem.getUser().getEmail());
        m.put("condominiumId", mem.getCondominium().getId());
        m.put("condominiumName", mem.getCondominium().getName());
        m.put("authority", mem.getRole().getAuthority());
        m.put("status", mem.getStatus().name());
        return m;
    }

    private Map<String, Object> sectorToMap(Sector s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("structuralType", s.getStructuralType().name());
        m.put("condominiumId", s.getCondominium().getId());
        m.put("condominiumName", s.getCondominium().getName());
        m.put("imageUrl", s.getImageUrl());
        return m;
    }

    private Map<String, Object> unitToMap(Unit u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("identifier", u.getIdentifier());
        m.put("sectorId", u.getSector().getId());
        m.put("sectorName", u.getSector().getName());
        m.put("condominiumId", u.getSector().getCondominium().getId());
        m.put("condominiumName", u.getSector().getCondominium().getName());
        return m;
    }

    private Map<String, Object> residentToMap(Resident r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("userId", r.getUser().getId());
        m.put("userEmail", r.getUser().getEmail());
        m.put("unitId", r.getUnit().getId());
        m.put("unitIdentifier", r.getUnit().getIdentifier());
        m.put("condominiumId", r.getUnit().getSector().getCondominium().getId());
        m.put("type", r.getType().name());
        return m;
    }

    private Map<String, Object> commonAreaToMap(CommonArea a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("condominiumId", a.getCondominium().getId());
        m.put("condominiumName", a.getCondominium().getName());
        m.put("name", a.getName());
        m.put("description", a.getDescription());
        m.put("capacity", a.getCapacity());
        m.put("requiresApproval", a.getRequiresApproval());
        m.put("openingHour", a.getOpeningHour());
        m.put("closingHour", a.getClosingHour());
        m.put("active", a.getActive());
        m.put("imageUrl", a.getImageUrl());
        return m;
    }

    private Map<String, Object> billingPlanToMap(BillingPlan p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("code", p.getCode());
        m.put("description", p.getDescription());
        m.put("monthlyFee", p.getMonthlyFee());
        m.put("maxUnits", p.getMaxUnits());
        m.put("displayOrder", p.getDisplayOrder());
        m.put("active", p.getActive());
        m.put("features", p.getFeatures().stream().map(Feature::getCode).sorted().toList());
        return m;
    }

    private Map<String, Object> packageToMap(Package p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("description", p.getDescription());
        m.put("unitId", p.getUnit().getId());
        m.put("recipientId", p.getRecipient() != null ? p.getRecipient().getId() : null);
        m.put("status", p.getStatus().name());
        m.put("condominiumId", p.getUnit().getSector().getCondominium().getId());
        return m;
    }

    private Map<String, Object> announcementToMap(Announcement a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("condominiumId", a.getCondominium().getId());
        m.put("condominiumName", a.getCondominium().getName());
        m.put("title", a.getTitle());
        m.put("content", a.getContent());
        m.put("unitId", a.getUnit() != null ? a.getUnit().getId() : null);
        m.put("active", a.getActive());
        return m;
    }

    private Map<String, Object> ticketToMap(OccurrenceTicket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("condominiumId", t.getCondominium().getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("status", t.getStatus());
        m.put("adminResponse", t.getAdminResponse());
        return m;
    }

    private Map<String, Object> expenseToMap(CondominiumExpense e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("condominiumId", e.getCondominium().getId());
        m.put("category", e.getCategory());
        m.put("description", e.getDescription());
        m.put("amount", e.getAmount());
        m.put("dueDate", e.getDueDate() != null ? e.getDueDate().toString() : null);
        m.put("status", e.getStatus());
        m.put("supplier", e.getSupplier());
        return m;
    }

    private Map<String, Object> infractionToMap(CondominiumInfraction i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("condominiumId", i.getCondominium().getId());
        m.put("title", i.getTitle());
        m.put("description", i.getDescription());
        m.put("penaltyAmount", i.getPenaltyAmount());
        m.put("status", i.getStatus());
        m.put("managerJudgment", i.getManagerJudgment());
        return m;
    }

    private String normalizeResource(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String normalizeReason(String raw, String fallback) {
        return raw != null && !raw.isBlank() ? raw.trim() : fallback;
    }

    private String requiredString(Map<String, Object> data, String field, String msg) {
        String s = stringOrNull(data.get(field));
        if (s == null || s.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        return s.trim();
    }

    private String stringOrNull(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).trim();
        return s.isEmpty() ? null : s;
    }

    private String stringOrDefault(Object raw, String fallback) {
        String s = stringOrNull(raw);
        return s != null ? s : fallback;
    }

    private Long asLong(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number n) return n.longValue();
        try {
            String s = String.valueOf(raw).trim();
            return s.isEmpty() ? null : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long requiredLong(Map<String, Object> data, String field, String msg) {
        Long val = asLong(data.get(field));
        if (val == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        return val;
    }

    private Integer asInt(Object raw, Integer fallback) {
        if (raw == null) return fallback;
        if (raw instanceof Number n) return n.intValue();
        try {
            String s = String.valueOf(raw).trim();
            return s.isEmpty() ? fallback : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private BigDecimal requiredDecimal(Map<String, Object> data, String field, String msg) {
        Object raw = data.get(field);
        if (raw == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        try {
            String s = String.valueOf(raw).replace(',', '.').trim();
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
    }

    private Boolean asBoolean(Object raw, boolean fallback) {
        if (raw == null) return fallback;
        if (raw instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(raw).trim());
    }

    private LocalDate parseDate(Object raw, LocalDate fallback) {
        if (raw == null) return fallback;
        try {
            return LocalDate.parse(String.valueOf(raw).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, Object raw, E fallback) {
        if (raw == null) return fallback;
        try {
            return Enum.valueOf(enumClass, String.valueOf(raw).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valor inválido '" + raw + "' para " + enumClass.getSimpleName());
        }
    }

    private Set<RoleDTO> extractRoleDTOs(Object raw) {
        Set<RoleDTO> roles = new LinkedHashSet<>();
        if (raw instanceof Collection<?> col) {
            for (Object item : col) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    roles.add(new RoleDTO(null, String.valueOf(item).trim().toUpperCase(Locale.ROOT)));
                }
            }
        }
        return roles;
    }

    private Set<FeatureCode> extractFeatureCodes(Object raw) {
        Set<FeatureCode> result = new LinkedHashSet<>();
        if (raw instanceof Collection<?> col) {
            for (Object item : col) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    result.add(FeatureCode.valueOf(String.valueOf(item).trim().toUpperCase(Locale.ROOT)));
                }
            }
        }
        return result;
    }

    private String normalizeTheme(String raw) {
        if (raw == null) return "SYSTEM";
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (upper.equals("LIGHT") || upper.equals("DARK") || upper.equals("SYSTEM")) return upper;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preferência de tema inválida: " + raw);
    }

    private ResourceSchemaDTO schema(
            String resource, String label, boolean creatable, boolean editable, boolean deletable,
            boolean requiresReason, boolean immutableLedger, List<FieldDescriptor> fields
    ) {
        return new ResourceSchemaDTO(
                resource, label, creatable, editable, deletable, requiresReason, immutableLedger,
                ALWAYS_PROTECTED_FIELDS, fields
        );
    }

    private FieldDescriptor idField() {
        return new FieldDescriptor("id", "ID", "INTEGER", false, false, true, false, null, null, null, null, null, null);
    }

    private FieldDescriptor readOnlyField(String name, String label, String type) {
        return new FieldDescriptor(name, label, type, false, false, true, false, null, null, null, null, "Campo protegido (somente leitura).", null);
    }

    private FieldDescriptor passwordCreateField() {
        return new FieldDescriptor("password", "Senha Inicial (apenas na criação)", "PASSWORD", true, true, true, false, 128, null, null, null,
                "Por segurança, senhas nunca são exibidas nem editadas neste formulário.", null);
    }

    private FieldDescriptor strField(String name, String label, String type, boolean required, boolean editable, boolean immutableOnUpdate, int maxLength, String helper) {
        return new FieldDescriptor(name, label, type, required, editable, immutableOnUpdate, false, maxLength, null, null, null, helper, null);
    }

    private FieldDescriptor boolField(String name, String label, boolean required, boolean editable, boolean immutableOnUpdate, boolean defaultVal) {
        return new FieldDescriptor(name, label, "BOOLEAN", required, editable, immutableOnUpdate, false, null, null, null, null, null, defaultVal);
    }

    private FieldDescriptor intField(String name, String label, boolean required, boolean editable, Integer defaultVal) {
        return new FieldDescriptor(name, label, "INTEGER", required, editable, false, false, null, null, null, null, null, defaultVal);
    }

    private FieldDescriptor decimalField(String name, String label, boolean required, boolean editable, Number defaultVal) {
        return new FieldDescriptor(name, label, "DECIMAL", required, editable, false, false, null, null, null, null, null, defaultVal);
    }

    private FieldDescriptor dateField(String name, String label, boolean required, boolean editable) {
        return new FieldDescriptor(name, label, "DATE", required, editable, false, false, null, null, null, null, null, null);
    }

    private FieldDescriptor enumField(String name, String label, boolean required, boolean editable, boolean immutableOnUpdate, List<EnumOption> options, String defaultVal) {
        return new FieldDescriptor(name, label, "ENUM", required, editable, immutableOnUpdate, false, null, options, null, null, null, defaultVal);
    }

    private FieldDescriptor multiEnumField(String name, String label, boolean required, boolean editable, boolean immutableOnUpdate, List<EnumOption> options) {
        return new FieldDescriptor(name, label, "MULTI_ENUM", required, editable, immutableOnUpdate, false, null, options, null, null, null, List.of());
    }

    private FieldDescriptor relationField(String name, String label, boolean required, boolean editable, boolean immutableOnUpdate, String relationResource, String relationDisplayField, String helper) {
        return new FieldDescriptor(name, label, "RELATION", required, editable, immutableOnUpdate, false, null, null, relationResource, relationDisplayField, helper, null);
    }
}
