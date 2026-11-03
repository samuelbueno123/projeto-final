-- V12: Fix notification check constraints and add performance indexes

-- 1. Update tb_notification check constraint to support all enum values
ALTER TABLE tb_notification DROP CONSTRAINT IF EXISTS ck_notification_type;
ALTER TABLE tb_notification ADD CONSTRAINT ck_notification_type CHECK (
    type IN (
        'PACKAGE_RECEIVED',
        'PACKAGE_STATUS_CHANGED',
        'ANNOUNCEMENT',
        'RESERVATION',
        'SYSTEM_ANNOUNCEMENT',
        'MEMBERSHIP_INVITATION',
        'DOORMAN_CHAT',
        'RESERVATION_COUNTER_PROPOSAL'
    )
);

-- 2. Performance indexes for frequent queries

-- Notifications (frequently queried by recipient_id ordered by created_at DESC, or filtered by read)
CREATE INDEX IF NOT EXISTS idx_notification_recipient_created ON tb_notification(recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_recipient_read ON tb_notification(recipient_id, read);

-- Packages (queried by recipient_id and unit_id)
CREATE INDEX IF NOT EXISTS idx_package_recipient ON tb_package(recipient_id);
CREATE INDEX IF NOT EXISTS idx_package_unit ON tb_package(unit_id);
CREATE INDEX IF NOT EXISTS idx_package_status ON tb_package(status);

-- Announcements (queried by condominium_id and active status ordered by created_at DESC)
CREATE INDEX IF NOT EXISTS idx_announcement_condo_active ON tb_announcement(condominium_id, active, created_at DESC);

-- Reservations (queried for conflicts by common_area_id, start_at, end_at, and by reserved_by_id)
CREATE INDEX IF NOT EXISTS idx_reservation_common_area_dates ON tb_reservation(common_area_id, start_at, end_at);
CREATE INDEX IF NOT EXISTS idx_reservation_reserved_by ON tb_reservation(reserved_by_id);
CREATE INDEX IF NOT EXISTS idx_reservation_status ON tb_reservation(status);

-- Unit payments (queried by unit_id and status)
CREATE INDEX IF NOT EXISTS idx_unit_payment_unit ON tb_unit_payment_status(unit_id);

-- Residents (queried by user_id and unit_id)
CREATE INDEX IF NOT EXISTS idx_resident_user_unit ON tb_resident(user_id, unit_id);

-- Memberships (queried by user_id and condominium_id)
CREATE INDEX IF NOT EXISTS idx_condo_user_user ON tb_condominium_user(user_id, condominium_id);
