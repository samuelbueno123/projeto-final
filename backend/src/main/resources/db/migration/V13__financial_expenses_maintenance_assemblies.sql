-- V13: Gestão Financeira de Despesas, Manutenção Predial e Assembleias Condominiais

-- 1. Despesas Operacionais do Condomínio
CREATE TABLE IF NOT EXISTS tb_condominium_expense (
    id BIGSERIAL PRIMARY KEY,
    condominium_id BIGINT NOT NULL REFERENCES tb_condominium(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    due_date DATE NOT NULL,
    paid_at TIMESTAMP,
    supplier VARCHAR(150),
    document_number VARCHAR(100),
    receipt_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_by_id BIGINT REFERENCES tb_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_expense_condo_due ON tb_condominium_expense(condominium_id, due_date DESC);
CREATE INDEX IF NOT EXISTS idx_expense_condo_status ON tb_condominium_expense(condominium_id, status);

-- 2. Manutenção Predial e Ordens de Serviço
CREATE TABLE IF NOT EXISTS tb_maintenance_order (
    id BIGSERIAL PRIMARY KEY,
    condominium_id BIGINT NOT NULL REFERENCES tb_condominium(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'CORRECTIVE',
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    contractor_name VARCHAR(150),
    contractor_contact VARCHAR(100),
    scheduled_date DATE,
    completed_at TIMESTAMP,
    estimated_cost NUMERIC(12,2),
    actual_cost NUMERIC(12,2),
    notes TEXT,
    created_by_id BIGINT REFERENCES tb_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_maint_condo_status ON tb_maintenance_order(condominium_id, status);
CREATE INDEX IF NOT EXISTS idx_maint_condo_sched ON tb_maintenance_order(condominium_id, scheduled_date);

-- 3. Assembleias Condominiais Gerais (Ordinárias e Extraordinárias)
CREATE TABLE IF NOT EXISTS tb_condominium_assembly (
    id BIGSERIAL PRIMARY KEY,
    condominium_id BIGINT NOT NULL REFERENCES tb_condominium(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(30) NOT NULL DEFAULT 'ORDINARY',
    meeting_date TIMESTAMP NOT NULL,
    second_call_date TIMESTAMP,
    location_or_link VARCHAR(500),
    call_notice_url VARCHAR(500),
    minutes_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_by_id BIGINT REFERENCES tb_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assembly_condo_date ON tb_condominium_assembly(condominium_id, meeting_date DESC);
CREATE INDEX IF NOT EXISTS idx_assembly_condo_status ON tb_condominium_assembly(condominium_id, status);

-- 4. Pautas em Deliberação da Assembleia
CREATE TABLE IF NOT EXISTS tb_assembly_topic (
    id BIGSERIAL PRIMARY KEY,
    assembly_id BIGINT NOT NULL REFERENCES tb_condominium_assembly(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    voting_open BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 1,
    in_favor_count INT NOT NULL DEFAULT 0,
    against_count INT NOT NULL DEFAULT 0,
    abstain_count INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_topic_assembly ON tb_assembly_topic(assembly_id);

-- 5. Votos das Unidades nas Pautas da Assembleia
CREATE TABLE IF NOT EXISTS tb_assembly_vote (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES tb_assembly_topic(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    unit_id BIGINT REFERENCES tb_unit(id) ON DELETE SET NULL,
    vote_choice VARCHAR(20) NOT NULL,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_assembly_topic_user UNIQUE (topic_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_vote_topic ON tb_assembly_vote(topic_id);
