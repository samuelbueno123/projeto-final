-- ==============================================================================
-- V19: Otimizações de Escalabilidade, Alta Concorrência e Performance
-- ==============================================================================

-- 1. Controle de Concorrência Otimista (Colunas de Versão)
ALTER TABLE tb_package ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE tb_reservation ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE tb_visitor_access ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE tb_parking_spot ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE tb_maintenance_order ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- 2. Índices Compostos de Alta Seletividade para Grandes Volumes e Multi-Inquilinos
CREATE INDEX IF NOT EXISTS idx_reservation_conflict_search 
    ON tb_reservation(common_area_id, status, start_at, end_at);

CREATE INDEX IF NOT EXISTS idx_reservation_user_start 
    ON tb_reservation(reserved_by_id, start_at DESC);

CREATE INDEX IF NOT EXISTS idx_package_recipient_status 
    ON tb_package(recipient_id, status);

CREATE INDEX IF NOT EXISTS idx_package_unit_status 
    ON tb_package(unit_id, status);

CREATE INDEX IF NOT EXISTS idx_visitor_condo_status_created 
    ON tb_visitor_access(condominium_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_visitor_condo_plate 
    ON tb_visitor_access(condominium_id, vehicle_plate);

CREATE INDEX IF NOT EXISTS idx_package_history_package_desc 
    ON tb_package_history(package_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_unit_sector_ident 
    ON tb_unit(sector_id, identifier);
