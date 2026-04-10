-- =============================================================
--  ChriOnline E-Commerce — Tables + Données de test
--  Mini-Projet 1 · MySQL
-- =============================================================

CREATE DATABASE chrionline
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chrionline;

-- -------------------------------------------------------------
--  TABLE : users
-- -------------------------------------------------------------
CREATE TABLE users (
                       user_id       INT            NOT NULL AUTO_INCREMENT,
                       username      VARCHAR(50)    NOT NULL,
                       email         VARCHAR(150)   NOT NULL,
                       password_hash VARCHAR(255)   NOT NULL,
                       first_name    VARCHAR(80)    NOT NULL,
                       last_name     VARCHAR(80)    NOT NULL,
                       address       TEXT,
                       phone         VARCHAR(20),
                       role          ENUM('CLIENT', 'ADMIN') NOT NULL DEFAULT 'CLIENT',
                       is_active     TINYINT(1)     NOT NULL DEFAULT 1,
                       created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT pk_users          PRIMARY KEY (user_id),
                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT uq_users_email    UNIQUE (email)
) ENGINE=InnoDB;

INSERT INTO users (username, email, password_hash, first_name, last_name, role, address, phone) VALUES
                                                                                                    ('admin',   'admin@chrionline.com',  SHA2('adminpass123', 256), 'Alice',  'Martin', 'ADMIN',  '1 Rue de la Paix, Paris',       '0600000001'),
                                                                                                    ('jdupont', 'jean.dupont@mail.com',  SHA2('password123',  256), 'Jean',   'Dupont', 'CLIENT', '12 Avenue Victor Hugo, Lyon',   '0611111111'),
                                                                                                    ('mleroy',  'marie.leroy@mail.com',  SHA2('password123',  256), 'Marie',  'Leroy',  'CLIENT', '8 Boulevard Pasteur, Marseille','0622222222'),
                                                                                                    ('cblanc',  'claire.blanc@mail.com', SHA2('password123',  256), 'Claire', 'Blanc',  'CLIENT', '5 Rue du Moulin, Bordeaux',     '0633333333'),
                                                                                                    ('tnoir',   'thomas.noir@mail.com',  SHA2('password123',  256), 'Thomas', 'Noir',   'CLIENT', '22 Rue de Rivoli, Paris',       '0644444444');


-- -------------------------------------------------------------
--  TABLE : sessions
-- -------------------------------------------------------------
CREATE TABLE sessions (
                          session_id     VARCHAR(64) NOT NULL,
                          user_id        INT         NOT NULL,
                          client_address VARCHAR(45),
                          created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          expires_at     DATETIME    NOT NULL,
                          is_active      TINYINT(1)  NOT NULL DEFAULT 1,

                          CONSTRAINT pk_sessions      PRIMARY KEY (session_id),
                          CONSTRAINT fk_sessions_user FOREIGN KEY (user_id)
                              REFERENCES users(user_id)
                              ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

INSERT INTO sessions (session_id, user_id, client_address, expires_at, is_active) VALUES
                                                                                      ('sess-0001-abcd-efgh-ijkl', 2, '192.168.1.10', DATE_ADD(NOW(), INTERVAL 1 HOUR), 1),
                                                                                      ('sess-0002-abcd-efgh-ijkl', 3, '192.168.1.11', DATE_ADD(NOW(), INTERVAL 1 HOUR), 1);


-- -------------------------------------------------------------
--  TABLE : categories
-- -------------------------------------------------------------
CREATE TABLE categories (
                            category_id INT          NOT NULL AUTO_INCREMENT,
                            name        VARCHAR(100) NOT NULL,
                            description TEXT,
                            created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT pk_categories      PRIMARY KEY (category_id),
                            CONSTRAINT uq_categories_name UNIQUE (name)
) ENGINE=InnoDB;

INSERT INTO categories (name, description) VALUES
                                               ('Électronique', 'Appareils électroniques et accessoires'),
                                               ('Vêtements',    'Mode homme, femme et enfant'),
                                               ('Livres',       'Romans, sciences, technique'),
                                               ('Sport',        'Équipement et vêtements de sport'),
                                               ('Informatique', 'Livres techniques et programmation'),
                                               ('Smartphones',  'Téléphones portables'),
                                               ('Ordinateurs',  'Laptops et desktops');


-- -------------------------------------------------------------
--  TABLE : products
-- -------------------------------------------------------------
CREATE TABLE products (
                          product_id     INT            NOT NULL AUTO_INCREMENT,
                          name           VARCHAR(200)   NOT NULL,
                          description    TEXT,
                          price          DECIMAL(10, 2) NOT NULL,
                          stock_quantity INT            NOT NULL DEFAULT 0,
                          category_id    INT                     DEFAULT NULL,
                          image_url      VARCHAR(500),
                          is_available   TINYINT(1)     NOT NULL DEFAULT 1,
                          created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

                          CONSTRAINT pk_products          PRIMARY KEY (product_id),
                          CONSTRAINT fk_products_category FOREIGN KEY (category_id)
                              REFERENCES categories(category_id)
                              ON DELETE SET NULL ON UPDATE CASCADE,
                          CONSTRAINT ck_products_price    CHECK (price >= 0),
                          CONSTRAINT ck_products_stock    CHECK (stock_quantity >= 0)
) ENGINE=InnoDB;

INSERT INTO products (name, description, price, stock_quantity, category_id) VALUES
                           ('iPhone 15 Pro',        '...',  1199.99,  30,  6),  -- Smartphones
                           ('Samsung Galaxy S24',   '...',   899.99,  45,  6),  -- Smartphones
                           ('Google Pixel 8',       '...',   699.99,  25,  6),  -- Smartphones
                           ('MacBook Pro M3',       '...',  2199.99,  15,  7),  -- Ordinateurs
                           ('Dell XPS 15',          '...',  1749.99,  20,  7),  -- Ordinateurs
                           ('Lenovo ThinkPad X1',   '...',  1299.99,  18,  7),  -- Ordinateurs
                           ('T-Shirt Coton Bio',    '...',    24.99, 200,  2),  -- Vêtements
                           ('T-Shirt Nike Dri-FIT', '...',    34.99, 150,  2),  -- Vêtements
                           ('Jean Slim Noir',       '...',    59.99, 120,  2),  -- Vêtements
                           ('Chino Beige',          '...',    49.99,  80,  2),  -- Vêtements
                           ('Clean Code',           '...',    35.00,  60,  5),  -- Informatique
                           ('Design Patterns',      '...',    42.00,  40,  5),  -- Informatique
                           ('Java : The Complete Reference', '...', 55.00, 35, 5), -- Informatique
                           ('Le Petit Prince',      '...',    12.99,  90,  3),  -- Livres
                           ('1984',                 '...',    14.99,  70,  3),  -- Livres
                           ('Vélo de Route Carbon', '...',  1299.00,   8,  4),  -- Sport
                           ('Tapis de Yoga',        '...',    29.90, 150,  4),  -- Sport
                           ('Raquette de Tennis',   '...',   149.99,  30,  4);  -- Sport

-- -------------------------------------------------------------
--  TABLE : carts
-- -------------------------------------------------------------
CREATE TABLE carts (
                       cart_id       INT      NOT NULL AUTO_INCREMENT,
                       user_id       INT      NOT NULL,
                       last_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT pk_carts      PRIMARY KEY (cart_id),
                       CONSTRAINT uq_carts_user UNIQUE (user_id),
                       CONSTRAINT fk_carts_user FOREIGN KEY (user_id)
                           REFERENCES users(user_id)
                           ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

INSERT INTO carts (user_id) VALUES (2), (3), (4), (5);


-- -------------------------------------------------------------
--  TABLE : cart_items
-- -------------------------------------------------------------
CREATE TABLE cart_items (
                            cart_item_id INT            NOT NULL AUTO_INCREMENT,
                            cart_id      INT            NOT NULL,
                            product_id   INT            NOT NULL,
                            quantity     INT            NOT NULL DEFAULT 1,
                            unit_price   DECIMAL(10,2)  NOT NULL,

                            CONSTRAINT pk_cart_items         PRIMARY KEY (cart_item_id),
                            CONSTRAINT uq_cart_items_product UNIQUE (cart_id, product_id),
                            CONSTRAINT fk_cart_items_cart    FOREIGN KEY (cart_id)
                                REFERENCES carts(cart_id)
                                ON DELETE CASCADE ON UPDATE CASCADE,
                            CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id)
                                REFERENCES products(product_id)
                                ON DELETE CASCADE ON UPDATE CASCADE,
                            CONSTRAINT ck_cart_items_qty     CHECK (quantity > 0)
) ENGINE=InnoDB;

INSERT INTO cart_items (cart_id, product_id, quantity, unit_price) VALUES
                                                                       (1, 1,  1, 1199.99),   -- jdupont : iPhone 15 Pro
                                                                       (1, 11, 2,   35.00),   -- jdupont : Clean Code x2
                                                                       (2, 4,  1, 2199.99),   -- mleroy  : MacBook Pro M3
                                                                       (2, 17, 1,   29.90),   -- mleroy  : Tapis de Yoga
                                                                       (3, 7,  3,   24.99),   -- cblanc  : T-Shirt Coton Bio x3
                                                                       (4, 16, 1, 1299.00);   -- tnoir   : Vélo de Route Carbon


-- -------------------------------------------------------------
--  TABLE : orders
-- -------------------------------------------------------------
CREATE TABLE orders (
                        order_id         VARCHAR(36)    NOT NULL,
                        user_id          INT            NOT NULL,
                        status           ENUM('EN_ATTENTE','VALIDEE','EXPEDIEE','LIVREE','ANNULEE')
                                    NOT NULL DEFAULT 'EN_ATTENTE',
                        total_amount     DECIMAL(10,2)  NOT NULL,
                        shipping_address TEXT           NOT NULL,
                        created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

                        CONSTRAINT pk_orders      PRIMARY KEY (order_id),
                        CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
                            REFERENCES users(user_id)
                            ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT ck_orders_total CHECK (total_amount >= 0)
) ENGINE=InnoDB;

INSERT INTO orders (order_id, user_id, status, total_amount, shipping_address) VALUES
                                                                                   ('ORD-0001-2026', 2, 'LIVREE',    899.99, '12 Avenue Victor Hugo, Lyon'),
                                                                                   ('ORD-0002-2026', 2, 'EXPEDIEE',   70.00, '12 Avenue Victor Hugo, Lyon'),
                                                                                   ('ORD-0003-2026', 3, 'VALIDEE',  2199.99, '8 Boulevard Pasteur, Marseille'),
                                                                                   ('ORD-0004-2026', 4, 'EN_ATTENTE', 74.97, '5 Rue du Moulin, Bordeaux'),
                                                                                   ('ORD-0005-2026', 5, 'ANNULEE',  1299.00, '22 Rue de Rivoli, Paris');


-- -------------------------------------------------------------
--  TABLE : order_items
-- -------------------------------------------------------------
CREATE TABLE order_items (
                             order_item_id    INT            NOT NULL AUTO_INCREMENT,
                             order_id         VARCHAR(36)    NOT NULL,
                             product_id       INT                     DEFAULT NULL,
                             product_snapshot JSON           NOT NULL,
                             quantity         INT            NOT NULL,
                             unit_price       DECIMAL(10,2)  NOT NULL,

                             CONSTRAINT pk_order_items         PRIMARY KEY (order_item_id),
                             CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id)
                                 REFERENCES orders(order_id)
                                 ON DELETE CASCADE ON UPDATE CASCADE,
                             CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
                                 REFERENCES products(product_id)
                                 ON DELETE SET NULL ON UPDATE CASCADE,
                             CONSTRAINT ck_order_items_qty     CHECK (quantity > 0)
) ENGINE=InnoDB;

INSERT INTO order_items (order_id, product_id, product_snapshot, quantity, unit_price) VALUES
                                                                                           ('ORD-0001-2026', 2,  '{"name":"Samsung Galaxy S24","price":899.99}',  1,  899.99),
                                                                                           ('ORD-0002-2026', 11, '{"name":"Clean Code","price":35.00}',           2,   35.00),
                                                                                           ('ORD-0003-2026', 4,  '{"name":"MacBook Pro M3","price":2199.99}',     1, 2199.99),
                                                                                           ('ORD-0004-2026', 7,  '{"name":"T-Shirt Coton Bio","price":24.99}',    3,   24.99),
                                                                                           ('ORD-0005-2026', 16, '{"name":"Vélo de Route Carbon","price":1299.00}',1, 1299.00);


-- -------------------------------------------------------------
--  TABLE : payments
-- -------------------------------------------------------------
CREATE TABLE payments (
                          payment_id      VARCHAR(36)    NOT NULL,
                          order_id        VARCHAR(36)    NOT NULL,
                          method          ENUM('CARTE_BANCAIRE','PAIEMENT_FICTIF','VIREMENT') NOT NULL,
                          status          ENUM('EN_ATTENTE','ACCEPTE','REFUSE','REMBOURSE')   NOT NULL DEFAULT 'EN_ATTENTE',
                          amount          DECIMAL(10,2)  NOT NULL,
                          card_last_four  CHAR(4)                DEFAULT NULL,
                          transaction_ref VARCHAR(100)           DEFAULT NULL,
                          paid_at         DATETIME               DEFAULT NULL,

                          CONSTRAINT pk_payments       PRIMARY KEY (payment_id),
                          CONSTRAINT uq_payments_order UNIQUE (order_id),
                          CONSTRAINT fk_payments_order FOREIGN KEY (order_id)
                              REFERENCES orders(order_id)
                              ON DELETE CASCADE ON UPDATE CASCADE,
                          CONSTRAINT ck_payments_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

INSERT INTO payments (payment_id, order_id, method, status, amount, card_last_four, transaction_ref, paid_at) VALUES
                                                                                                                  ('PAY-0001-2026', 'ORD-0001-2026', 'CARTE_BANCAIRE',  'ACCEPTE',    899.99, '4242', 'TXN-A1B2C3D4', '2026-01-15 10:30:00'),
                                                                                                                  ('PAY-0002-2026', 'ORD-0002-2026', 'PAIEMENT_FICTIF', 'ACCEPTE',     70.00,  NULL,  'TXN-E5F6G7H8', '2026-02-01 14:00:00'),
                                                                                                                  ('PAY-0003-2026', 'ORD-0003-2026', 'CARTE_BANCAIRE',  'ACCEPTE',   2199.99, '1234', 'TXN-I9J0K1L2', '2026-02-20 09:15:00'),
                                                                                                                  ('PAY-0004-2026', 'ORD-0004-2026', 'CARTE_BANCAIRE',  'EN_ATTENTE',   74.97, NULL,   NULL,            NULL),
                                                                                                                  ('PAY-0005-2026', 'ORD-0005-2026', 'VIREMENT',        'REMBOURSE',  1299.00, NULL,  'TXN-M3N4O5P6', '2026-03-01 11:45:00');


-- -------------------------------------------------------------
--  TABLE : notifications
-- -------------------------------------------------------------
CREATE TABLE notifications (
                               notification_id INT         NOT NULL AUTO_INCREMENT,
                               user_id         INT         NOT NULL,
                               message         TEXT        NOT NULL,
                               type            VARCHAR(50) NOT NULL DEFAULT 'INFO',
                               is_read         TINYINT(1)  NOT NULL DEFAULT 0,
                               protocol        ENUM('TCP','UDP') NOT NULL DEFAULT 'UDP',
                               created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT pk_notifications      PRIMARY KEY (notification_id),
                               CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
                                   REFERENCES users(user_id)
                                   ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

INSERT INTO notifications (user_id, message, type, is_read, protocol) VALUES
                                                                          (2, 'Votre commande #ORD-0001-2026 est maintenant : LIVREE',    'ORDER',   1, 'UDP'),
                                                                          (2, 'Votre commande #ORD-0002-2026 est maintenant : EXPEDIEE',  'ORDER',   0, 'UDP'),
                                                                          (3, 'Votre commande #ORD-0003-2026 est maintenant : VALIDEE',   'ORDER',   0, 'UDP'),
                                                                          (3, 'Paiement accepté pour la commande #ORD-0003-2026',         'PAYMENT', 1, 'UDP'),
                                                                          (5, 'Votre commande #ORD-0005-2026 a été annulée.',             'ORDER',   0, 'UDP'),
                                                                          (5, 'Remboursement effectué pour la commande #ORD-0005-2026.',  'PAYMENT', 0, 'TCP');



-- =============================================================
--  FIN DU SCRIPT
-- =============================================================