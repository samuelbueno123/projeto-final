alter table tb_user add column profile_image text;
alter table tb_user add column google_subject varchar(255);
create unique index uk_user_google_subject on tb_user(google_subject);
