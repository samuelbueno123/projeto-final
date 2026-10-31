-- Comunicados direcionados a uma unidade específica (nulo = comunicado para todo o condomínio)
alter table tb_announcement add column unit_id bigint;
alter table tb_announcement add constraint fk_announcement_unit
    foreign key (unit_id) references tb_unit (id) on delete cascade;

-- Telefone de contato informado no agendamento de áreas comuns (somente dígitos)
alter table tb_reservation add column contact_phone varchar(20);
