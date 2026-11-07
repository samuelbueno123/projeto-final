-- V15: Add sector image_url and hub unit tracking columns
ALTER TABLE tb_sector ADD COLUMN IF NOT EXISTS image_url TEXT;

ALTER TABLE tb_common_area ALTER COLUMN image_url TYPE TEXT;

ALTER TABLE tb_event_guest_list ADD COLUMN IF NOT EXISTS unit_id BIGINT REFERENCES tb_unit(id) ON DELETE SET NULL;

ALTER TABLE tb_maintenance_order ADD COLUMN IF NOT EXISTS unit_id BIGINT REFERENCES tb_unit(id) ON DELETE SET NULL;

ALTER TABLE tb_condominium_document ADD COLUMN IF NOT EXISTS unit_id BIGINT REFERENCES tb_unit(id) ON DELETE SET NULL;
