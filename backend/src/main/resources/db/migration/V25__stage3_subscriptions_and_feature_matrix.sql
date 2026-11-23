-- ============================================================================
-- V25: Stage 3 Subscription Lifecycle, Plans & Granular Feature Matrix
-- ============================================================================

-- 1. Metadados comerciais nos planos
ALTER TABLE tb_billing_plan ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE tb_billing_plan ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE tb_billing_plan ADD COLUMN IF NOT EXISTS max_units INTEGER;
ALTER TABLE tb_billing_plan ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;

-- 2. Garantir catálogo completo de FeatureCode (17 módulos granulares, ANSI SQL)
INSERT INTO tb_feature (code, name, description)
SELECT 'PACKAGES', 'Encomendas', 'Recebimento, registro e retirada de encomendas na portaria'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'PACKAGES');

INSERT INTO tb_feature (code, name, description)
SELECT 'NOTIFICATIONS', 'Notificações e Multas', 'Notificações em tempo real e infrações/advertências'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'NOTIFICATIONS');

INSERT INTO tb_feature (code, name, description)
SELECT 'ANNOUNCEMENTS', 'Comunicados', 'Publicação e leitura de comunicados por condomínio, bloco ou unidade'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'ANNOUNCEMENTS');

INSERT INTO tb_feature (code, name, description)
SELECT 'RESERVATIONS', 'Reservas de Áreas Comuns', 'Agendamento, fila de espera e aprovação de áreas comuns'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'RESERVATIONS');

INSERT INTO tb_feature (code, name, description)
SELECT 'REPORTS', 'Relatórios Gerenciais', 'KPIs executivos e exportação CSV/PDF'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'REPORTS');

INSERT INTO tb_feature (code, name, description)
SELECT 'MARKETPLACE_INTEGRATION', 'Classificados e Vagas', 'Mural de classificados internos e gestão de garagem'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'MARKETPLACE_INTEGRATION');

INSERT INTO tb_feature (code, name, description)
SELECT 'VISITORS', 'Visitantes e QR Code', 'Pré-autorização de visitantes, lista de convidados e passe digital'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'VISITORS');

INSERT INTO tb_feature (code, name, description)
SELECT 'DOORMAN_CHAT', 'Chat com Portaria', 'Mensagens em tempo real entre moradores, unidades e portaria'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'DOORMAN_CHAT');

INSERT INTO tb_feature (code, name, description)
SELECT 'COMMUNITY_TICKETS', 'Chamados e Comunidade', 'Ocorrências, achados e perdidos, mudanças, veículos, pets e documentos'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'COMMUNITY_TICKETS');

INSERT INTO tb_feature (code, name, description)
SELECT 'POLLS', 'Enquetes Rápidas', 'Votações e consultas rápidas aos moradores'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'POLLS');

INSERT INTO tb_feature (code, name, description)
SELECT 'FINANCIAL', 'Gestão Financeira', 'Cobranças por unidade, despesas, leitura de hidrômetros e orçamento'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'FINANCIAL');

INSERT INTO tb_feature (code, name, description)
SELECT 'MAINTENANCE', 'Manutenção Predial', 'Ordens de serviço, equipamentos preventivos, checklists e contratos'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'MAINTENANCE');

INSERT INTO tb_feature (code, name, description)
SELECT 'ASSEMBLIES', 'Assembleias Virtuais', 'Assembleias condominiais, pautas e votação com verificação de adimplência'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'ASSEMBLIES');

INSERT INTO tb_feature (code, name, description)
SELECT 'SMART_LOCKERS', 'Armários Inteligentes', 'Gestão de compartimentos Smart Locker com código PIN'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'SMART_LOCKERS');

INSERT INTO tb_feature (code, name, description)
SELECT 'CONCIERGE_OPERATIONS', 'Operações de Portaria', 'Livro de plantão digital e controle de claviculário/chaves'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'CONCIERGE_OPERATIONS');

INSERT INTO tb_feature (code, name, description)
SELECT 'EMERGENCY_SOS', 'Alertas de Emergência SOS', 'Botão de pânico e atendimento imediato pela portaria'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'EMERGENCY_SOS');

INSERT INTO tb_feature (code, name, description)
SELECT 'AI_ASSISTANT', 'Assistente IA Âncora', 'Assistente inteligente para convenção, finanças e dúvidas do condomínio'
WHERE NOT EXISTS (SELECT 1 FROM tb_feature WHERE code = 'AI_ASSISTANT');

-- 3. Padronizar os 3 tiers SaaS comerciais (Essencial, Profissional, Enterprise)
UPDATE tb_billing_plan
SET name = 'Essencial', code = 'ESSENCIAL', monthly_fee = 199.90, max_units = 60, display_order = 1,
    description = 'Operação essencial: encomendas, comunicados, visitantes, enquetes e chamados.'
WHERE LOWER(name) IN ('básico', 'basico')
  AND NOT EXISTS (SELECT 1 FROM tb_billing_plan WHERE LOWER(name) = 'essencial');

UPDATE tb_billing_plan
SET code = 'PROFISSIONAL', monthly_fee = 349.90, max_units = 250, display_order = 2,
    description = 'Operação completa com áreas comuns, chat da portaria, financeiro, manutenção, assembleias e SOS.'
WHERE LOWER(name) = 'profissional';

UPDATE tb_billing_plan
SET name = 'Enterprise', code = 'ENTERPRISE', monthly_fee = 499.90, max_units = NULL, display_order = 3,
    description = 'Plataforma corporativa completa: todos os módulos, Smart Lockers, relatórios BI e Assistente IA.'
WHERE LOWER(name) = 'premium'
  AND NOT EXISTS (SELECT 1 FROM tb_billing_plan WHERE LOWER(name) = 'enterprise');

INSERT INTO tb_billing_plan (name, code, monthly_fee, max_units, display_order, description, active)
SELECT 'Essencial', 'ESSENCIAL', 199.90, 60, 1,
       'Operação essencial: encomendas, comunicados, visitantes, enquetes e chamados.', true
WHERE NOT EXISTS (SELECT 1 FROM tb_billing_plan WHERE LOWER(name) = 'essencial');

INSERT INTO tb_billing_plan (name, code, monthly_fee, max_units, display_order, description, active)
SELECT 'Profissional', 'PROFISSIONAL', 349.90, 250, 2,
       'Operação completa com áreas comuns, chat da portaria, financeiro, manutenção, assembleias e SOS.', true
WHERE NOT EXISTS (SELECT 1 FROM tb_billing_plan WHERE LOWER(name) = 'profissional');

INSERT INTO tb_billing_plan (name, code, monthly_fee, max_units, display_order, description, active)
SELECT 'Enterprise', 'ENTERPRISE', 499.90, NULL, 3,
       'Plataforma corporativa completa: todos os módulos, Smart Lockers, relatórios BI e Assistente IA.', true
WHERE NOT EXISTS (SELECT 1 FROM tb_billing_plan WHERE LOWER(name) = 'enterprise');

-- 4. Matriz diferenciada de features para os 3 planos comerciais padrão
DELETE FROM tb_billing_plan_feature
WHERE billing_plan_id IN (
    SELECT id FROM tb_billing_plan WHERE LOWER(name) IN ('essencial', 'profissional', 'enterprise')
);

INSERT INTO tb_billing_plan_feature (billing_plan_id, feature_id)
SELECT bp.id, f.id
FROM tb_billing_plan bp
JOIN tb_feature f ON f.code IN ('PACKAGES', 'ANNOUNCEMENTS', 'NOTIFICATIONS', 'COMMUNITY_TICKETS', 'POLLS', 'VISITORS')
WHERE LOWER(bp.name) = 'essencial';

INSERT INTO tb_billing_plan_feature (billing_plan_id, feature_id)
SELECT bp.id, f.id
FROM tb_billing_plan bp
JOIN tb_feature f ON f.code IN (
    'PACKAGES', 'ANNOUNCEMENTS', 'NOTIFICATIONS', 'COMMUNITY_TICKETS', 'POLLS', 'VISITORS',
    'RESERVATIONS', 'DOORMAN_CHAT', 'FINANCIAL', 'MAINTENANCE', 'ASSEMBLIES', 'EMERGENCY_SOS'
)
WHERE LOWER(bp.name) = 'profissional';

INSERT INTO tb_billing_plan_feature (billing_plan_id, feature_id)
SELECT bp.id, f.id
FROM tb_billing_plan bp
CROSS JOIN tb_feature f
WHERE LOWER(bp.name) = 'enterprise';

-- 5. Entidade de assinatura e ciclo de vida por condomínio
CREATE TABLE IF NOT EXISTS tb_condominium_subscription (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    condominium_id BIGINT NOT NULL UNIQUE REFERENCES tb_condominium(id) ON DELETE CASCADE,
    billing_plan_id BIGINT NOT NULL REFERENCES tb_billing_plan(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_period_start TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_period_end TIMESTAMP(6),
    trial_ends_at TIMESTAMP(6),
    cancelled_at TIMESTAMP(6),
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_condo_sub_status CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED')),
    CONSTRAINT ck_condo_sub_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL'))
);

CREATE INDEX IF NOT EXISTS idx_condo_sub_plan ON tb_condominium_subscription (billing_plan_id);
CREATE INDEX IF NOT EXISTS idx_condo_sub_status ON tb_condominium_subscription (status);
CREATE INDEX IF NOT EXISTS idx_condo_sub_period_end ON tb_condominium_subscription (current_period_end);

CREATE TABLE IF NOT EXISTS tb_subscription_history (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    condominium_id BIGINT NOT NULL REFERENCES tb_condominium(id) ON DELETE CASCADE,
    subscription_id BIGINT REFERENCES tb_condominium_subscription(id) ON DELETE SET NULL,
    previous_plan_id BIGINT REFERENCES tb_billing_plan(id),
    new_plan_id BIGINT REFERENCES tb_billing_plan(id),
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    event_type VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    changed_by_id BIGINT REFERENCES tb_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sub_history_condo_created ON tb_subscription_history (condominium_id, created_at DESC);

-- Backfill portável (H2 + PostgreSQL) para condomínios existentes
INSERT INTO tb_condominium_subscription (
    condominium_id, billing_plan_id, status, billing_cycle,
    started_at, current_period_start, auto_renew, updated_at, version
)
SELECT c.id,
       c.billing_plan_id,
       CASE WHEN c.is_active THEN 'ACTIVE' ELSE 'SUSPENDED' END,
       'MONTHLY',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       TRUE,
       CURRENT_TIMESTAMP,
       0
FROM tb_condominium c
WHERE NOT EXISTS (
    SELECT 1 FROM tb_condominium_subscription s WHERE s.condominium_id = c.id
);
