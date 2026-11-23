-- ============================================================================
-- V24: Stage 2 Performance Indexes, OAuth2 Cleanup Indexes & Duplicate Cleanup
-- ============================================================================

-- 1. Índices seguros em oauth2_authorization (apenas colunas limitadas/timestamps,
--    evitando exceder o limite B-Tree de 2704 bytes em colunas TEXT de tokens).
CREATE INDEX IF NOT EXISTS idx_oauth2_auth_principal
    ON oauth2_authorization (principal_name);

CREATE INDEX IF NOT EXISTS idx_oauth2_auth_access_exp
    ON oauth2_authorization (access_token_expires_at);

CREATE INDEX IF NOT EXISTS idx_oauth2_auth_refresh_exp
    ON oauth2_authorization (refresh_token_expires_at);

CREATE INDEX IF NOT EXISTS idx_oauth2_auth_state
    ON oauth2_authorization (state);

-- 2. Índices de FKs e filtros multi-tenant de alta frequência ausentes
CREATE INDEX IF NOT EXISTS idx_sector_condominium
    ON tb_sector (condominium_id);

CREATE INDEX IF NOT EXISTS idx_condominium_billing_plan
    ON tb_condominium (billing_plan_id);

CREATE INDEX IF NOT EXISTS idx_condo_user_condo_status
    ON tb_condominium_user (condominium_id, status);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_condo_created
    ON tb_notification (recipient_id, condominium_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_invitation
    ON tb_notification (invitation_id);

CREATE INDEX IF NOT EXISTS idx_poll_option_poll
    ON tb_poll_option (poll_id);

CREATE INDEX IF NOT EXISTS idx_poll_vote_voter
    ON tb_poll_vote (voter_id);

CREATE INDEX IF NOT EXISTS idx_checklist_condo
    ON tb_maintenance_checklist (condominium_id);

CREATE INDEX IF NOT EXISTS idx_checklist_inspector
    ON tb_maintenance_checklist (inspector_id);

CREATE INDEX IF NOT EXISTS idx_checklist_item_equipment
    ON tb_maintenance_checklist_item (equipment_id);

CREATE INDEX IF NOT EXISTS idx_unit_payment_status
    ON tb_unit_payment_status (status);

CREATE INDEX IF NOT EXISTS idx_unit_payment_due_date
    ON tb_unit_payment_status (due_date);

CREATE INDEX IF NOT EXISTS idx_audit_log_correlation
    ON tb_audit_log (correlation_id);

-- 3. Remoção de índices redundantes/subsumidos que oneram INSERT/UPDATE/VACUUM
DROP INDEX IF EXISTS idx_package_recipient;
DROP INDEX IF EXISTS idx_package_unit;
DROP INDEX IF EXISTS idx_reservation_conflict_search;
DROP INDEX IF EXISTS idx_resident_user;
DROP INDEX IF EXISTS idx_condo_user_user;
DROP INDEX IF EXISTS idx_expense_condo_due_date;
DROP INDEX IF EXISTS idx_emergency_condo;
DROP INDEX IF EXISTS idx_pkg_history_pkg;
DROP INDEX IF EXISTS idx_chat_condo;
