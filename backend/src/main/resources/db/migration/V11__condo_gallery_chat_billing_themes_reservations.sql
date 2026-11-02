-- V11: Condominium Gallery, Doorman Chat, Billing Plan Selection, User Theme Preference, Boleto Digitable Line, and Reservation Counter Proposals

-- 1. User Theme Preference
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS theme_preference VARCHAR(20) DEFAULT 'SYSTEM';

-- 2. Condominium Plan Selection Flag
ALTER TABLE tb_condominium ADD COLUMN IF NOT EXISTS plan_selected BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Membership Welcome Screen Flag
ALTER TABLE tb_condominium_user ADD COLUMN IF NOT EXISTS has_seen_welcome BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. Common Area Image URL
ALTER TABLE tb_common_area ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

-- 5. Condominium Gallery Images
CREATE TABLE IF NOT EXISTS tb_condominium_gallery (
    id BIGSERIAL PRIMARY KEY,
    condominium_id BIGINT NOT NULL REFERENCES tb_condominium(id) ON DELETE CASCADE,
    common_area_id BIGINT REFERENCES tb_common_area(id) ON DELETE SET NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    image_path VARCHAR(500) NOT NULL,
    uploaded_by_id BIGINT REFERENCES tb_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_condo_gallery_condo ON tb_condominium_gallery(condominium_id);

-- 6. Doorman Real-time Chat Messages
CREATE TABLE IF NOT EXISTS tb_doorman_chat_message (
    id BIGSERIAL PRIMARY KEY,
    condominium_id BIGINT NOT NULL REFERENCES tb_condominium(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    recipient_id BIGINT REFERENCES tb_user(id) ON DELETE SET NULL,
    unit_id BIGINT REFERENCES tb_unit(id) ON DELETE SET NULL,
    sender_role VARCHAR(30) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_chat_condo ON tb_doorman_chat_message(condominium_id);
CREATE INDEX IF NOT EXISTS idx_chat_sender ON tb_doorman_chat_message(sender_id);
CREATE INDEX IF NOT EXISTS idx_chat_recipient ON tb_doorman_chat_message(recipient_id);

-- 7. Boleto and Payment Details in Unit Payment
ALTER TABLE tb_unit_payment_status ADD COLUMN IF NOT EXISTS digitable_line VARCHAR(100);
ALTER TABLE tb_unit_payment_status ADD COLUMN IF NOT EXISTS invoice_title VARCHAR(200);
ALTER TABLE tb_unit_payment_status ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

-- 8. Reservation Counter Proposals
ALTER TABLE tb_reservation ADD COLUMN IF NOT EXISTS counter_proposal_notes VARCHAR(500);
ALTER TABLE tb_reservation ADD COLUMN IF NOT EXISTS proposed_start_at TIMESTAMP;
ALTER TABLE tb_reservation ADD COLUMN IF NOT EXISTS proposed_end_at TIMESTAMP;
ALTER TABLE tb_reservation ADD COLUMN IF NOT EXISTS reviewed_by_id BIGINT REFERENCES tb_user(id) ON DELETE SET NULL;
ALTER TABLE tb_reservation ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
