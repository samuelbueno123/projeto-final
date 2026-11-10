-- V18: Auditoria da Arquitetura de Banco de Dados e Otimizacoes de Persistencia
-- 1. Deduplicacao preventiva e constraints de unicidade essenciais
DELETE FROM tb_condominium_user
WHERE id NOT IN (
    SELECT MIN(id)
    FROM tb_condominium_user
    GROUP BY condominium_id, user_id, role_id
);

ALTER TABLE tb_condominium_user DROP CONSTRAINT IF EXISTS uk_condominium_user_role;
ALTER TABLE tb_condominium_user
    ADD CONSTRAINT uk_condominium_user_role UNIQUE (condominium_id, user_id, role_id);

DELETE FROM tb_resident
WHERE id NOT IN (
    SELECT MIN(id)
    FROM tb_resident
    GROUP BY user_id, unit_id
);

ALTER TABLE tb_resident DROP CONSTRAINT IF EXISTS uk_resident_user_unit;
ALTER TABLE tb_resident
    ADD CONSTRAINT uk_resident_user_unit UNIQUE (user_id, unit_id);

DELETE FROM tb_parking_spot
WHERE id NOT IN (
    SELECT MIN(id)
    FROM tb_parking_spot
    GROUP BY condominium_id, code
);

ALTER TABLE tb_parking_spot DROP CONSTRAINT IF EXISTS uk_parking_spot_condo_code;
ALTER TABLE tb_parking_spot
    ADD CONSTRAINT uk_parking_spot_condo_code UNIQUE (condominium_id, code);

DELETE FROM tb_key_registry
WHERE id NOT IN (
    SELECT MIN(id)
    FROM tb_key_registry
    GROUP BY condominium_id, key_code
);

ALTER TABLE tb_key_registry DROP CONSTRAINT IF EXISTS uk_key_registry_condo_code;
ALTER TABLE tb_key_registry
    ADD CONSTRAINT uk_key_registry_condo_code UNIQUE (condominium_id, key_code);

DELETE FROM tb_smart_locker_compartment
WHERE id NOT IN (
    SELECT MIN(id)
    FROM tb_smart_locker_compartment
    GROUP BY condominium_id, locker_name, compartment_number
);

ALTER TABLE tb_smart_locker_compartment DROP CONSTRAINT IF EXISTS uk_locker_condo_name_number;
ALTER TABLE tb_smart_locker_compartment
    ADD CONSTRAINT uk_locker_condo_name_number UNIQUE (condominium_id, locker_name, compartment_number);

-- 2. Indices de chaves estrangeiras e consultas compostas de alto desempenho
CREATE INDEX IF NOT EXISTS idx_user_role_user ON tb_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_plan_feature_feature ON tb_billing_plan_feature(feature_id);

CREATE INDEX IF NOT EXISTS idx_condo_user_role ON tb_condominium_user(role_id);
CREATE INDEX IF NOT EXISTS idx_condo_user_req_unit ON tb_condominium_user(requested_unit_id);
CREATE INDEX IF NOT EXISTS idx_resident_unit ON tb_resident(unit_id);
CREATE INDEX IF NOT EXISTS idx_resident_user ON tb_resident(user_id);

CREATE INDEX IF NOT EXISTS idx_common_area_condo ON tb_common_area(condominium_id, active);
CREATE INDEX IF NOT EXISTS idx_reservation_area_status_dates ON tb_reservation(common_area_id, status, start_at, end_at);
CREATE INDEX IF NOT EXISTS idx_reservation_reviewed_by ON tb_reservation(reviewed_by_id);

CREATE INDEX IF NOT EXISTS idx_package_unit_status ON tb_package(unit_id, status);
CREATE INDEX IF NOT EXISTS idx_package_recipient_status ON tb_package(recipient_id, status);
CREATE INDEX IF NOT EXISTS idx_pkg_history_pkg ON tb_package_history(package_id, performed_at ASC);
CREATE INDEX IF NOT EXISTS idx_pkg_history_user ON tb_package_history(performed_by_id);

CREATE INDEX IF NOT EXISTS idx_announcement_created_by ON tb_announcement(created_by_id);
CREATE INDEX IF NOT EXISTS idx_announcement_sector ON tb_announcement(sector_id);
CREATE INDEX IF NOT EXISTS idx_announcement_unit ON tb_announcement(unit_id);
CREATE INDEX IF NOT EXISTS idx_guest_entry_list ON tb_guest_entry(guest_list_id);

CREATE INDEX IF NOT EXISTS idx_maint_supplier ON tb_maintenance_order(supplier_id);
CREATE INDEX IF NOT EXISTS idx_maint_equipment ON tb_maintenance_order(equipment_id);
CREATE INDEX IF NOT EXISTS idx_maint_created_by ON tb_maintenance_order(created_by_id);
CREATE INDEX IF NOT EXISTS idx_equipment_supplier ON tb_maintenance_equipment(supplier_id);
CREATE INDEX IF NOT EXISTS idx_checklist_item_list ON tb_maintenance_checklist_item(checklist_id);
CREATE INDEX IF NOT EXISTS idx_contract_supplier ON tb_service_contract(supplier_id);
CREATE INDEX IF NOT EXISTS idx_expense_created_by ON tb_condominium_expense(created_by_id);

CREATE INDEX IF NOT EXISTS idx_renovation_requester ON tb_renovation_request(requester_id);
CREATE INDEX IF NOT EXISTS idx_renovation_decided_by ON tb_renovation_request(decided_by_id);
CREATE INDEX IF NOT EXISTS idx_shift_outgoing ON tb_concierge_shift_log(outgoing_doorman_id);
CREATE INDEX IF NOT EXISTS idx_shift_incoming ON tb_concierge_shift_log(incoming_doorman_id);
CREATE INDEX IF NOT EXISTS idx_emergency_resident ON tb_emergency_alert(resident_id);
CREATE INDEX IF NOT EXISTS idx_emergency_unit ON tb_emergency_alert(unit_id);
CREATE INDEX IF NOT EXISTS idx_utility_reader ON tb_utility_reading(reader_id);
CREATE INDEX IF NOT EXISTS idx_infraction_unit ON tb_condominium_infraction(unit_id);
CREATE INDEX IF NOT EXISTS idx_infraction_issued_by ON tb_condominium_infraction(issued_by_id);
CREATE INDEX IF NOT EXISTS idx_locker_recipient ON tb_smart_locker_compartment(recipient_id);
CREATE INDEX IF NOT EXISTS idx_locker_unit ON tb_smart_locker_compartment(unit_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_unit ON tb_vehicle(unit_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_owner ON tb_vehicle(owner_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_spot ON tb_vehicle(parking_spot_id);
CREATE INDEX IF NOT EXISTS idx_pet_unit ON tb_pet(unit_id);
CREATE INDEX IF NOT EXISTS idx_pet_owner ON tb_pet(owner_id);
CREATE INDEX IF NOT EXISTS idx_moving_unit ON tb_moving_request(unit_id);
CREATE INDEX IF NOT EXISTS idx_moving_requester ON tb_moving_request(requester_id);
CREATE INDEX IF NOT EXISTS idx_doc_uploaded_by ON tb_condominium_document(uploaded_by_id);
CREATE INDEX IF NOT EXISTS idx_chat_unit ON tb_doorman_chat_message(unit_id);
CREATE INDEX IF NOT EXISTS idx_chat_condo_sent ON tb_doorman_chat_message(condominium_id, sent_at ASC);
CREATE INDEX IF NOT EXISTS idx_poll_vote_option ON tb_poll_vote(option_id);
CREATE INDEX IF NOT EXISTS idx_assembly_part_user ON tb_assembly_participant(user_id);
CREATE INDEX IF NOT EXISTS idx_assembly_vote_user ON tb_assembly_vote(user_id);

-- 3. Constraints de verificacao (CHECK) para garantia de integridade logica
ALTER TABLE tb_condominium_expense DROP CONSTRAINT IF EXISTS ck_expense_amount_positive;
ALTER TABLE tb_condominium_expense
    ADD CONSTRAINT ck_expense_amount_positive CHECK (amount > 0);

ALTER TABLE tb_service_contract DROP CONSTRAINT IF EXISTS ck_contract_monthly_value;
ALTER TABLE tb_service_contract
    ADD CONSTRAINT ck_contract_monthly_value CHECK (monthly_value >= 0);

ALTER TABLE tb_service_contract DROP CONSTRAINT IF EXISTS ck_contract_date_range;
ALTER TABLE tb_service_contract
    ADD CONSTRAINT ck_contract_date_range CHECK (end_date >= start_date);

ALTER TABLE tb_utility_reading DROP CONSTRAINT IF EXISTS ck_utility_consumption;
ALTER TABLE tb_utility_reading
    ADD CONSTRAINT ck_utility_consumption CHECK (consumption >= 0);

ALTER TABLE tb_budget_forecast DROP CONSTRAINT IF EXISTS ck_budget_amount;
ALTER TABLE tb_budget_forecast
    ADD CONSTRAINT ck_budget_amount CHECK (budgeted_amount >= 0);

ALTER TABLE tb_budget_forecast DROP CONSTRAINT IF EXISTS ck_budget_month;
ALTER TABLE tb_budget_forecast
    ADD CONSTRAINT ck_budget_month CHECK (month >= 1 AND month <= 12);

ALTER TABLE tb_budget_forecast DROP CONSTRAINT IF EXISTS ck_budget_year;
ALTER TABLE tb_budget_forecast
    ADD CONSTRAINT ck_budget_year CHECK (year >= 2000);

ALTER TABLE tb_renovation_request DROP CONSTRAINT IF EXISTS ck_renovation_dates;
ALTER TABLE tb_renovation_request
    ADD CONSTRAINT ck_renovation_dates CHECK (expected_end_date >= start_date);

ALTER TABLE tb_reservation DROP CONSTRAINT IF EXISTS ck_reservation_dates;
ALTER TABLE tb_reservation
    ADD CONSTRAINT ck_reservation_dates CHECK (end_at > start_at);

ALTER TABLE tb_common_area DROP CONSTRAINT IF EXISTS ck_common_area_capacity;
ALTER TABLE tb_common_area
    ADD CONSTRAINT ck_common_area_capacity CHECK (capacity > 0);

ALTER TABLE tb_concierge_shift_log DROP CONSTRAINT IF EXISTS ck_shift_packages;
ALTER TABLE tb_concierge_shift_log
    ADD CONSTRAINT ck_shift_packages CHECK (pending_packages_count >= 0);

ALTER TABLE tb_concierge_shift_log DROP CONSTRAINT IF EXISTS ck_shift_keys;
ALTER TABLE tb_concierge_shift_log
    ADD CONSTRAINT ck_shift_keys CHECK (pending_keys_count >= 0);
