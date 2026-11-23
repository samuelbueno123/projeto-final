-- Stage 4: Structured diff and reason columns on tb_audit_log for governed admin DB mutations

ALTER TABLE tb_audit_log ADD COLUMN IF NOT EXISTS before_state TEXT;
ALTER TABLE tb_audit_log ADD COLUMN IF NOT EXISTS after_state TEXT;
ALTER TABLE tb_audit_log ADD COLUMN IF NOT EXISTS reason VARCHAR(500);
