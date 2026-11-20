-- ============================================================================
-- V21: Security Hardening, Performance Indexes & Smart Locker Reuse Fix
-- ============================================================================

-- 1. Allow compartment reuse after RETRIEVED by replacing unconditional unique constraint
--    with a partial unique index covering only OCCUPIED compartments.
ALTER TABLE tb_smart_locker_compartment DROP CONSTRAINT IF EXISTS uk_locker_condo_name_number;
DROP INDEX IF EXISTS uk_locker_condo_name_number;
CREATE UNIQUE INDEX IF NOT EXISTS uk_locker_condo_name_number_occupied
    ON tb_smart_locker_compartment (condominium_id, locker_name, compartment_number)
    WHERE status = 'OCCUPIED';

-- 2. Functional index for case-insensitive user authentication/lookup
CREATE INDEX IF NOT EXISTS idx_user_email_lower ON tb_user (LOWER(email));

-- 3. Composite indexes for hot multi-tenant queries
CREATE INDEX IF NOT EXISTS idx_condo_user_user_condo_status
    ON tb_condominium_user (user_id, condominium_id, status);

CREATE INDEX IF NOT EXISTS idx_expense_condo_due_date
    ON tb_condominium_expense (condominium_id, due_date);

CREATE INDEX IF NOT EXISTS idx_chat_condo_participants_sent
    ON tb_doorman_chat_message (condominium_id, sender_id, recipient_id, sent_at DESC);

CREATE INDEX IF NOT EXISTS idx_emergency_condo_status_created
    ON tb_emergency_alert (condominium_id, status, created_at DESC);
