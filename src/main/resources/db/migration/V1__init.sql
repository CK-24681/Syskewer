-- ========================================================
-- 1. CRIAÇÃO DE TODAS AS TABELAS (ESTRUTURA)
-- ========================================================

-- 1.1 Cardápio e Estoque
CREATE TABLE tb_prep_location (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE tb_category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    parent_id INT,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES tb_category (id)
);

CREATE TABLE tb_product (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    in_stock BOOLEAN NOT NULL DEFAULT TRUE,
    prep_location_id INT, 
    category_id INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_product_prep_location FOREIGN KEY (prep_location_id) REFERENCES tb_prep_location (id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES tb_category (id)
);

-- 1.2 Segurança e Usuários
CREATE TABLE tb_role (
    id SERIAL PRIMARY KEY,
    authority VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE tb_user (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES tb_role (id)
);

CREATE TABLE tb_password_reset_token (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES tb_user (id)
);
CREATE INDEX idx_password_reset_token_token ON tb_password_reset_token(token);
CREATE INDEX idx_password_reset_token_user_id ON tb_password_reset_token(user_id);

-- 1.3 Configurações da Loja
CREATE TABLE tb_store_settings (
    id SERIAL PRIMARY KEY,
    is_open BOOLEAN NOT NULL DEFAULT TRUE
);

-- 1.4 Salão (Mesas e Comandas Baseadas no Diagrama de Classe)
CREATE TABLE tb_table (
    id SERIAL PRIMARY KEY,
    number INT NOT NULL UNIQUE,
    occupied BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tb_tab (
    id SERIAL PRIMARY KEY,
    customer_name VARCHAR(100), 
    table_id INT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    consumption_type VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    delivery_fee DECIMAL(10,2),
    delivery_address VARCHAR(255),
    apply_cover_charge BOOLEAN NOT NULL DEFAULT FALSE,
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deferred_date TIMESTAMP,
    CONSTRAINT fk_tab_table FOREIGN KEY (table_id) REFERENCES tb_table (id)
);

-- 1.5 Pedidos e Itens (O SEU V2 INCORPORADO AQUI)
CREATE TABLE tb_order (
    id BIGSERIAL PRIMARY KEY, 
    tab_id INT NOT NULL,
    waiter_id INT, 
    prep_status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    origin VARCHAR(50) NOT NULL DEFAULT 'WAITER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_tab FOREIGN KEY (tab_id) REFERENCES tb_tab (id),
    CONSTRAINT fk_order_waiter FOREIGN KEY (waiter_id) REFERENCES tb_user (id)
);

CREATE TABLE tb_order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL, 
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    sold_price DECIMAL(10,2) NOT NULL,
    is_to_go BOOLEAN NOT NULL DEFAULT FALSE,
    packaging_instructions VARCHAR(255),
    notes VARCHAR(255), 
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES tb_order (id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES tb_product (id)
);

CREATE TABLE tb_order_item_sides (
    order_item_id BIGINT NOT NULL, 
    side_dish VARCHAR(100) NOT NULL,
    CONSTRAINT fk_side_order_item FOREIGN KEY (order_item_id) REFERENCES tb_order_item (id) ON DELETE CASCADE
);

-- ========================================================
-- 2. DADOS INICIAIS (SEED)
-- ========================================================

-- 2.0 Mesas (As 20 mesas livres que você pediu)
INSERT INTO tb_table (number, occupied) VALUES
(1, false), (2, false), (3, false), (4, false), (5, false),
(6, false), (7, false), (8, false), (9, false), (10, false),
(11, false), (12, false), (13, false), (14, false), (15, false),
(16, false), (17, false), (18, false), (19, false), (20, false);

-- 2.1 Roles (Nomes Limpos)
INSERT INTO tb_role (authority) VALUES ('Administrador'), ('Garçom');

-- 2.2 Configurações da Loja
INSERT INTO tb_store_settings (is_open) VALUES (true);

-- 2.3 Locais de Preparo
INSERT INTO tb_prep_location (name) VALUES ('Churrasqueira'), ('Cozinha');

-- 2.4 Categorias
INSERT INTO tb_category (name) VALUES ('Espetinhos'); -- ID 1
INSERT INTO tb_category (name) VALUES ('Caldinhos');  -- ID 2
INSERT INTO tb_category (name) VALUES ('Petiscos');   -- ID 3
INSERT INTO tb_category (name) VALUES ('Bebidas');    -- ID 4
INSERT INTO tb_category (name, parent_id) VALUES ('Bebidas Alcoólicas', 4);     -- ID 5
INSERT INTO tb_category (name, parent_id) VALUES ('Bebidas Não Alcoólicas', 4); -- ID 6

-- 2.5 Produtos (Espetinhos) - Fiel às imagens
INSERT INTO tb_product (name, price, active, in_stock, category_id, prep_location_id) VALUES 
('Misto', 7.50, true, true, 1, 1),
('Carne', 8.00, true, true, 1, 1),
('Frango', 7.00, true, true, 1, 1),
('Coração', 8.00, true, true, 1, 1),
('Salsichão', 5.00, true, true, 1, 1),
('Queijo Coalho', 8.00, true, true, 1, 1),
('Mistão Especial', 9.00, true, true, 1, 1),
('Pão de Alho Trad. ou Picante', 5.00, true, true, 1, 1),
('Carne c/ Bacon', 10.00, true, true, 1, 1),
('Frango c/ Bacon', 10.00, true, true, 1, 1),
('Carne c/ Queijo', 10.00, true, true, 1, 1),
('Frango c/ Queijo', 10.00, true, true, 1, 1),
('Costela de Carneiro', 10.00, true, true, 1, 1),
('Charque', 12.00, true, true, 1, 1),
('Camarão c/ Bacon', 12.00, true, true, 1, 1);

-- 2.6 Produtos (Caldinhos) - Fiel às imagens
INSERT INTO tb_product (name, price, active, in_stock, category_id, prep_location_id) VALUES 
('Caldinho de Feijão', 8.00, true, true, 2, 2),
('Caldinho de Camarão', 8.00, true, true, 2, 2),
('Caldinho de Mocotó', 8.00, true, true, 2, 2),
('Caldinho de Sururu', 8.00, true, true, 2, 2),
('Caldinho de Costela', 8.00, true, true, 2, 2);

-- 2.7 Produtos (Petiscos) - Fiel às imagens
INSERT INTO tb_product (name, price, active, in_stock, category_id, prep_location_id) VALUES 
('Tira Gosto (Frios)', 18.00, true, true, 3, 2),
('Ovo de Codorna', 10.00, true, true, 3, 2),
('Macaxeira Frita', 15.00, true, true, 3, 2),
('Batatas Fritas', 15.00, true, true, 3, 2),
('Passarinho', 22.00, true, true, 3, 2),
('Queijo e Azeitona', 15.00, true, true, 3, 2),
('Bolinhos de Charque (10 un)', 15.00, true, true, 3, 2),
('Bolinhos de Queijo (10 un)', 15.00, true, true, 3, 2),
('Bolinhos de Bacalhau (10 un)', 18.00, true, true, 3, 2),
('Bolinhos de Macaxeira c/ Charque (10 un)', 15.00, true, true, 3, 2),
('Cebola Empanada', 15.00, true, true, 3, 2),
('Coxinha Empanada', 15.00, true, true, 3, 2),
('Calabresa Acebolada', 20.00, true, true, 3, 2),
('Isca de Frango', 25.00, true, true, 3, 2),
('Isca de Peixe', 30.00, true, true, 3, 2),
('Camarão Alho e Óleo', 35.00, true, true, 3, 2);

-- 2.8 Produtos (Bebidas Alcoólicas) - Fiel às imagens
INSERT INTO tb_product (name, price, active, in_stock, category_id, prep_location_id) VALUES 
('Pitú Quartinho', 4.00, true, true, 5, NULL),
('Pitú Dose', 3.00, true, true, 5, NULL),
('Syn', 5.00, true, true, 5, NULL),
('Alcatrão dose', 4.00, true, true, 5, NULL),
('Alcatrão quartinho', 6.00, true, true, 5, NULL),
('Brahma chopp super latão', 7.50, true, true, 5, NULL),
('Brahma chopp 600ml', 9.00, true, true, 5, NULL),
('Brahma chopp lata zero', 7.00, true, true, 5, NULL),
('Budweiser long', 8.00, true, true, 5, NULL),
('Budweiser 600ml', 12.00, true, true, 5, NULL),
('Corona long', 9.00, true, true, 5, NULL),
('Heineken long', 9.00, true, true, 5, NULL),
('Heineken 600ml', 15.00, true, true, 5, NULL),
('Vodka Smirnoff', 6.00, true, true, 5, NULL),
('Campari', 6.00, true, true, 5, NULL),
('Whisky Black White', 7.00, true, true, 5, NULL),
('Whisky Johnnie Walker', 8.00, true, true, 5, NULL);

-- 2.9 Produtos (Bebidas Não Alcoólicas) - Fiel às imagens
INSERT INTO tb_product (name, price, active, in_stock, category_id, prep_location_id) VALUES 
('Skinka', 4.00, true, true, 6, NULL),
('Suco de caixa', 6.00, true, true, 6, NULL),
('Água sem Gás', 2.50, true, true, 6, NULL),
('Água com Gás', 3.00, true, true, 6, NULL),
('Água Tônica', 5.00, true, true, 6, NULL),
('Coca-cola (lata)', 5.00, true, true, 6, NULL),
('Coca-cola (lata) zero', 5.00, true, true, 6, NULL),
('Guaraná Antártica (lata)', 5.00, true, true, 6, NULL),
('Guaraná Antártica (lata) zero', 5.00, true, true, 6, NULL),
('Pepsi (lata)', 5.00, true, true, 6, NULL),
('Soda / Sukita (lata)', 5.00, true, true, 6, NULL),
('Fanta (lata)', 5.00, true, true, 6, NULL),
('H2O Limão', 6.00, true, true, 6, NULL),
('H2O Limoneto', 6.00, true, true, 6, NULL),
('Energético (Power Bull)', 8.00, true, true, 6, NULL),
('Energético (Red Bull / Monster)', 12.00, true, true, 6, NULL),
('Coca-cola 1L', 9.00, true, true, 6, NULL);