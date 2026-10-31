alter table tb_condominium_user add column status varchar(30) not null default 'ACTIVE';
alter table tb_condominium_user add column created_at timestamp(6) not null default current_timestamp;
alter table tb_condominium_user add column responded_at timestamp(6);
alter table tb_condominium_user add constraint ck_condominium_user_status check (
    status in ('PENDING', 'ACTIVE', 'DECLINED')
);

alter table tb_notification add column invitation_id bigint;
alter table tb_notification add column condominium_id bigint;
alter table tb_notification add column condominium_name varchar(255);
alter table tb_notification add column invitation_authority varchar(50);
alter table tb_notification add column invitation_status varchar(30);

alter table tb_notification drop constraint ck_notification_type;
alter table tb_notification add constraint ck_notification_type check (
    type in (
        'PACKAGE_RECEIVED',
        'PACKAGE_STATUS_CHANGED',
        'ANNOUNCEMENT',
        'RESERVATION',
        'SYSTEM_ANNOUNCEMENT',
        'MEMBERSHIP_INVITATION'
    )
);
