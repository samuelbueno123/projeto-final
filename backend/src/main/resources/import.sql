-- ROLES
INSERT INTO tb_role (authority) VALUES ('ROLE_ADMIN');
INSERT INTO tb_role (authority) VALUES ('ROLE_MANAGER');
INSERT INTO tb_role (authority) VALUES ('ROLE_RESIDENT');
INSERT INTO tb_role (authority) VALUES ('ROLE_DOORMAN');

-- CONDOMINIOS
INSERT INTO tb_condominium (name, address, billing_plan, is_active) VALUES ('Vila Angela', 'Rua Principal, 100', 'BASIC', true);
INSERT INTO tb_condominium (name, address, billing_plan, is_active) VALUES ('Agora Nova Iguacu', 'Av Central, 200', 'PREMIUM', true);

-- SETORES
INSERT INTO tb_sector (name, structural_type, condominium_id) VALUES ('Rua 1', 'STREET', 1);
INSERT INTO tb_sector (name, structural_type, condominium_id) VALUES ('Rua 2', 'STREET', 1);
INSERT INTO tb_sector (name, structural_type, condominium_id) VALUES ('Bloco 1', 'BLOCK', 2);
INSERT INTO tb_sector (name, structural_type, condominium_id) VALUES ('Bloco 2', 'BLOCK', 2);

-- UNIDADES
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 1', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 2', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 3', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 4', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 5', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 6', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 7', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 8', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 9', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 10', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 11', 1);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 12', 1);

INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 1', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 2', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 3', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 4', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 5', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 6', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 7', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 8', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 9', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 10', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 11', 2);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('Casa 12', 2);

INSERT INTO tb_unit (identifier, sector_id) VALUES ('101', 3);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('102', 3);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('103', 3);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('104', 3);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('105', 3);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('106', 3);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('107', 3);
INSERT INTO tb_unit (identifier, sector_id) VALUES ('108', 3);

-- USERS
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Marcus', 'Vinicius', 'admin@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Carlos', 'Ferro','sindico@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Joao', 'Pedro','morador1@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Ana', 'Paula','morador2@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Pedro', 'Lima','porteiro1@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Lucas','Augusto', 'porteiro2@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Fernanda', 'Souza', 'morador3@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Ricardo', 'Almeida', 'morador4@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Juliana', 'Rocha', 'morador5@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Marcos', 'Silva', 'morador6@email.com', '$2a$10$7X.JEwyz7/XA64D/8uC1auUrRRRc/nrVFG63fUF/YenN9Td9JTDvW');

-- USER_ROLE
INSERT INTO tb_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO tb_user_role (user_id, role_id) VALUES (2, 2);
INSERT INTO tb_user_role (user_id, role_id) VALUES (3, 3);
INSERT INTO tb_user_role (user_id, role_id) VALUES (4, 3);
INSERT INTO tb_user_role (user_id, role_id) VALUES (5, 4);
INSERT INTO tb_user_role (user_id, role_id) VALUES (6, 4);
INSERT INTO tb_user_role (user_id, role_id) VALUES (7, 3);
INSERT INTO tb_user_role (user_id, role_id) VALUES (8, 3);
INSERT INTO tb_user_role (user_id, role_id) VALUES (9, 3);
INSERT INTO tb_user_role (user_id, role_id) VALUES (10, 3);

-- RESIDENT
INSERT INTO tb_resident (user_id, unit_id, type) VALUES (3, 3, 'OWNER');
INSERT INTO tb_resident (user_id, unit_id, type) VALUES (4, 27, 'TENANT');
INSERT INTO tb_resident (user_id, unit_id, type) VALUES (7, 5, 'OWNER');
INSERT INTO tb_resident (user_id, unit_id, type) VALUES (8, 14, 'TENANT');
INSERT INTO tb_resident (user_id, unit_id, type) VALUES (9, 29, 'OWNER');
INSERT INTO tb_resident (user_id, unit_id, type) VALUES (10, 31, 'TENANT');

-- PACKAGE
INSERT INTO tb_package (description, status, recipient_id, unit_id, arrival_at) VALUES ('Amazon - TV 50"', 'PENDING', 3, 3, CURRENT_TIMESTAMP);
INSERT INTO tb_package (description, status, recipient_id, unit_id, arrival_at) VALUES ('Mercado Livre - Notebook', 'PENDING', 4,27, CURRENT_TIMESTAMP);
INSERT INTO tb_package (description, status, recipient_id, unit_id, arrival_at) VALUES ('Shopee - Headset Gamer', 'PENDING', 7, 5, CURRENT_TIMESTAMP);
INSERT INTO tb_package (description, status, recipient_id, unit_id, arrival_at) VALUES ('Amazon - Livro Clean Architecture', 'PENDING', 8, 14, CURRENT_TIMESTAMP);
INSERT INTO tb_package (description, status, recipient_id, unit_id, arrival_at) VALUES ('Magazine Luiza - Geladeira', 'PENDING', 9, 29, CURRENT_TIMESTAMP);
INSERT INTO tb_package (description, status, recipient_id, unit_id, arrival_at) VALUES ('Mercado Livre - Monitor 27"', 'PENDING', 10, 31, CURRENT_TIMESTAMP);

-- CONDOMINIUM_USER
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (2, 1, 2);
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (5, 1, 4);
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (6, 2, 4);
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (3, 1, 3);
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (4, 2, 3);
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (7, 1, 3);
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (8, 1, 3);
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (9, 2, 3);
INSERT INTO tb_condominium_user (user_id, condominium_id, role_id) VALUES (10, 2, 3);