-- =========================================
-- ROLES
-- =========================================
INSERT INTO tb_role (id, authority) VALUES (1, 'ROLE_ADMIN');
INSERT INTO tb_role (id, authority) VALUES (2, 'ROLE_MANAGER');
INSERT INTO tb_role (id, authority) VALUES (3, 'ROLE_RESIDENT');
INSERT INTO tb_role (id, authority) VALUES (4, 'ROLE_DOORMAN');

-- =========================================
-- CONDOMINIOS
-- =========================================
INSERT INTO tb_condominium (id, name, address, billing_plan, is_active) VALUES (1, 'Vila Angela', 'Rua Principal, 100', 'BASIC', true);
INSERT INTO tb_condominium (id, name, address, billing_plan, is_active) VALUES (2, 'Agora Nova Iguacu', 'Av Central, 200', 'PREMIUM', true);

-- =========================================
-- SETORES - VILA ANGELA (RUAS)
-- =========================================
INSERT INTO tb_sector (id, name, structural_type, condominium_id) VALUES (1, 'Rua 1', 'STREET', 1);
INSERT INTO tb_sector (id, name, structural_type, condominium_id) VALUES (2, 'Rua 2', 'STREET', 1);

-- =========================================
-- UNIDADES - VILA ANGELA (CASAS 1-12)
-- =========================================
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (1, 'Casa 1', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (2, 'Casa 2', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (3, 'Casa 3', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (4, 'Casa 4', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (5, 'Casa 5', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (6, 'Casa 6', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (7, 'Casa 7', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (8, 'Casa 8', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (9, 'Casa 9', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (10, 'Casa 10', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (11, 'Casa 11', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (12, 'Casa 12', 1);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (13, 'Casa 1', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (14, 'Casa 2', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (15, 'Casa 3', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (16, 'Casa 4', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (17, 'Casa 5', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (18, 'Casa 6', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (19, 'Casa 7', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (20, 'Casa 8', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (21, 'Casa 9', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (22, 'Casa 10', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (23, 'Casa 11', 2);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (24, 'Casa 12', 2);

-- =========================================
-- SETORES - AGORA NOVA IGUACU (BLOCOS)
-- =========================================
INSERT INTO tb_sector (id, name, structural_type, condominium_id) VALUES (3, 'Bloco 1', 'BLOCK', 2);
INSERT INTO tb_sector (id, name, structural_type, condominium_id) VALUES (4, 'Bloco 2', 'BLOCK', 2);

-- =========================================
-- UNIDADES - NOVA IGUACU (101-108)
-- =========================================
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (25, '101', 3);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (26, '102', 3);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (27, '103', 3);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (28, '104', 3);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (29, '105', 3);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (30, '106', 3);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (31, '107', 3);
INSERT INTO tb_unit (id, identifier, sector_id) VALUES (32, '108', 3);

-- =========================================
-- USERS
-- =========================================
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (1, 'Marcus', 'Vinicius', 'admin@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (2, 'Carlos', 'Ferro','sindico@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (3, 'Joao', 'Pedro','morador1@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (4, 'Ana', 'Paula','morador2@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (5, 'Pedro', 'Lima','porteiro1@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (6, 'Lucas','Augusto', 'porteiro2@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (7, 'Fernanda', 'Souza', 'morador3@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (8, 'Ricardo', 'Almeida', 'morador4@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (9, 'Juliana', 'Rocha', 'morador5@email.com', '123');
INSERT INTO tb_user (id, first_name, last_name, email, password) VALUES (10, 'Marcos', 'Silva', 'morador6@email.com', '123');

-- =========================================
-- USER_ROLE
-- =========================================
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

-- =========================================
-- RESIDENT
-- =========================================
INSERT INTO tb_resident (id, user_id, unit_id, type) VALUES (1, 3, 3, 'OWNER');
INSERT INTO tb_resident (id, user_id, unit_id, type) VALUES (2, 4, 27, 'TENANT');
INSERT INTO tb_resident (id, user_id, unit_id, type) VALUES (3, 7, 5, 'OWNER');
INSERT INTO tb_resident (id, user_id, unit_id, type) VALUES (4, 8, 14, 'TENANT');
INSERT INTO tb_resident (id, user_id, unit_id, type) VALUES (5, 9, 29, 'OWNER');
INSERT INTO tb_resident (id, user_id, unit_id, type) VALUES (6, 10, 31, 'TENANT');

-- =========================================
-- ENCOMENDAS
-- =========================================
INSERT INTO tb_package (id, description, status, recipient_id, unit_id, arrival_at) VALUES (1, 'Amazon - TV 50"', 'PENDING', 3, 3, CURRENT_TIMESTAMP);
INSERT INTO tb_package (id, description, status, recipient_id, unit_id, arrival_at) VALUES (2, 'Mercado Livre - Notebook', 'PENDING', 4,27, CURRENT_TIMESTAMP);
INSERT INTO tb_package (id, description, status, recipient_id, unit_id, arrival_at) VALUES (3, 'Shopee - Headset Gamer', 'PENDING', 7, 5, CURRENT_TIMESTAMP);
INSERT INTO tb_package (id, description, status, recipient_id, unit_id, arrival_at) VALUES (4, 'Amazon - Livro Clean Architecture', 'PENDING', 8, 14, CURRENT_TIMESTAMP);
INSERT INTO tb_package (id, description, status, recipient_id, unit_id, arrival_at) VALUES (5, 'Magazine Luiza - Geladeira', 'PENDING', 9, 29, CURRENT_TIMESTAMP);
INSERT INTO tb_package (id, description, status, recipient_id, unit_id, arrival_at) VALUES (6, 'Mercado Livre - Monitor 27"', 'PENDING', 10, 31, CURRENT_TIMESTAMP);

-- =========================================
-- CONDOMINIUM_USER
-- =========================================
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (1, 2, 1, 2);
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (2, 5, 1, 4);
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (3, 6, 2, 4);
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (4, 3, 1, 3);
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (5, 4, 2, 3);
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (6, 7, 1, 3);
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (7, 8, 1, 3);
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (8, 9, 2, 3);
INSERT INTO tb_condominium_user (id, user_id, condominium_id, role_id) VALUES (9, 10, 2, 3);