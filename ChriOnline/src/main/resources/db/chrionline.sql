-- =============================================================
--  ChriOnline — Script MySQL (12/4/2026 18:00)
--  Mini-Projet 1 · Architecture TCP/UDP
-- =============================================================

CREATE DATABASE chrionline
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chrionline;

-- =============================================================
--  TABLE : admins
--  Separate from users — admins manage the platform
-- =============================================================
CREATE TABLE admins (
                        admin_id      INT          NOT NULL AUTO_INCREMENT,
                        username      VARCHAR(50)  NOT NULL,
                        email         VARCHAR(150) NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        first_name    VARCHAR(80)  NOT NULL,
                        last_name     VARCHAR(80)  NOT NULL,
                        is_active     TINYINT(1)   NOT NULL DEFAULT 1,
                        is_super      TINYINT(1)   NOT NULL DEFAULT 0,
                        created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT pk_admins          PRIMARY KEY (admin_id),
                        CONSTRAINT uq_admins_username UNIQUE (username),
                        CONSTRAINT uq_admins_email    UNIQUE (email)
) ENGINE=InnoDB;

INSERT INTO admins (admin_id, username, email, password_hash, first_name, last_name, is_active, is_super, created_at) VALUES
                                                                                                                          (1, 'superadmin', 'super@chrionline.com', '$argon2id$v=19$m=65536,t=3,p=1$AMLD/tGQNnu4Q/q9pWbV/Q$OnR4IEiBMTmLJ2N1bolG/Pz3Bdsm3I5ADnWfgxqW73I', 'Super',  'Admin', 1, 1, '2026-03-29 15:16:50'),
                                                                                                                          (2, 'admin',      'admin@chrionline.com', '$argon2id$v=19$m=65536,t=3,p=1$thfCAtqBaKOrOS1ABlUnkQ$Ib26FKpVzxrhKIPF5m7O0uK6f9Omab8vOGSBUhNjY4E', 'Normal', 'Admin', 1, 0, '2026-03-29 17:04:54');


-- =============================================================
--  TABLE : users
--  Clients only — role is always CLIENT
-- =============================================================
CREATE TABLE users (
                       user_id         INT          NOT NULL AUTO_INCREMENT,
                       username        VARCHAR(50)  NOT NULL,
                       email           VARCHAR(150) NOT NULL,
                       password_hash   VARCHAR(255) NOT NULL,
                       first_name      VARCHAR(80)  NOT NULL,
                       last_name       VARCHAR(80)  NOT NULL,
                       address         TEXT,
                       phone           VARCHAR(20),
                       role            ENUM('CLIENT') NOT NULL DEFAULT 'CLIENT',
                       is_active       TINYINT(1)   NOT NULL DEFAULT 1,
                       is_verified     TINYINT(1)   NOT NULL DEFAULT 0,
                       verify_code     VARCHAR(10)           DEFAULT NULL,
                       verify_expires  DATETIME              DEFAULT NULL,
                       failed_attempts INT          NOT NULL DEFAULT 0,
                       locked_until    DATETIME              DEFAULT NULL,
                       created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT pk_users          PRIMARY KEY (user_id),
                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT uq_users_email    UNIQUE (email)
) ENGINE=InnoDB;

INSERT INTO users (user_id, username, email, password_hash, first_name, last_name, address, phone, is_active, is_verified, failed_attempts, created_at, updated_at) VALUES
                                                                                                                                                                        (2, 'jdupont',      'jean.dupont@mail.com',    '$argon2id$v=19$m=65536,t=3,p=1$wW23gnSPzmlQ59gD0vPT8A$wHjOLMhmL9kFrIek4d1FD8O8sxL6X0LhOLKNM1aqcv8', 'Jean',          'Dupont',      '12 Avenue Victor Hugo, Lyon',          '0611111111', 1, 1, 0, '2026-03-15 23:41:05', '2026-04-12 17:47:00'),
                                                                                                                                                                        (3, 'mleroy',       'marie.leroy@mail.com',    '$argon2id$v=19$m=65536,t=3,p=1$wW23gnSPzmlQ59gD0vPT8A$wHjOLMhmL9kFrIek4d1FD8O8sxL6X0LhOLKNM1aqcv8', 'Marie',         'Leroy',       '8 Boulevard Pasteur, Marseille',       '0622222222', 1, 1, 0, '2026-03-15 23:41:05', '2026-04-12 17:47:02'),
                                                                                                                                                                        (4, 'cblanc',       'claire.blanc@mail.com',   '$argon2id$v=19$m=65536,t=3,p=1$wW23gnSPzmlQ59gD0vPT8A$wHjOLMhmL9kFrIek4d1FD8O8sxL6X0LhOLKNM1aqcv8', 'Claire',        'Blanc',       '5 Rue du Moulin, Bordeaux',            '0633333333', 1, 1, 0, '2026-03-15 23:41:05', '2026-04-12 17:47:04'),
                                                                                                                                                                        (5, 'tnoir',        'thomas.noir@mail.com',    '$argon2id$v=19$m=65536,t=3,p=1$wW23gnSPzmlQ59gD0vPT8A$wHjOLMhmL9kFrIek4d1FD8O8sxL6X0LhOLKNM1aqcv8', 'Thomas',        'Noir',        '22 Rue de Rivoli, Paris',              '0644444444', 1, 1, 0, '2026-03-15 23:41:05', '2026-04-12 17:50:50'),
                                                                                                                                                                        (6, 'mohamed_amine','mohamedamine@gmail.com',  '$argon2id$v=19$m=65536,t=3,p=1$UkK1ElrOLyBe+hoEDgcLFg$07jzI2dXQtXU1EqxPW5IdLlPrA0aOoEdwsICj9u+F8E', 'Mohamed Amine', 'Ait Jaakike', '15 Rue Mouassine, Marrakech',          '0622334412', 1, 1, 0, '2026-03-16 11:50:51', '2026-04-12 18:01:35'),
                                                                                                                                                                        (7, 'hafsa',        'hafsa@gmail.com',         '$argon2id$v=19$m=65536,t=3,p=1$bohgW3nfW0eo5I0txmzTPA$ji/LNS6OwjDIbsdQ0GJVYtlacidIBXc5Lk8vV7asXdU', 'Hafsa',         'Zohri',       '22 Avenue Moulay El Hassan, Martil',   '0699887766', 1, 1, 0, '2026-03-25 14:44:46', '2026-04-12 17:50:05'),
                                                                                                                                                                        (8, 'amine',        'amine@gmail.com',         '$argon2id$v=19$m=65536,t=3,p=1$oSkgWXCpX1vnE5pC0W7x1w$ghUHzGPO8qiNvQnlIENq54ITgqZxx/wguriu1uwF05E', 'Amine',         'El Hauari',   '12 Av. des FAR, Tetouan',              '0612312345', 1, 1, 0, '2026-03-27 09:17:49', '2026-04-12 18:04:38');


-- =============================================================
--  TABLE : sessions
--  Active TCP sessions per user
-- =============================================================
CREATE TABLE sessions (
                          session_id     VARCHAR(64) NOT NULL,
                          user_id        INT         NOT NULL,
                          client_address VARCHAR(45)          DEFAULT NULL,
                          created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          expires_at     DATETIME    NOT NULL,
                          is_active      TINYINT(1)  NOT NULL DEFAULT 1,

                          CONSTRAINT pk_sessions      PRIMARY KEY (session_id),
                          CONSTRAINT fk_sessions_user FOREIGN KEY (user_id)
                              REFERENCES users(user_id)
                              ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

INSERT INTO sessions (session_id, user_id, client_address, created_at, expires_at, is_active) VALUES
                                                                                                  ('sess-0001-abcd-efgh-ijkl', 2, '192.168.1.10', '2026-03-15 23:41:05', '2026-03-16 00:41:05', 1),
                                                                                                  ('sess-0002-abcd-efgh-ijkl', 3, '192.168.1.11', '2026-03-15 23:41:05', '2026-03-16 00:41:05', 1);


-- =============================================================
--  TABLE : categories
--  Flat list — no parent/child hierarchy
-- =============================================================
CREATE TABLE categories (
                            category_id INT          NOT NULL AUTO_INCREMENT,
                            name        VARCHAR(100) NOT NULL,
                            description TEXT                  DEFAULT NULL,
                            created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT pk_categories      PRIMARY KEY (category_id),
                            CONSTRAINT uq_categories_name UNIQUE (name)
) ENGINE=InnoDB;

INSERT INTO categories (category_id, name, description, created_at) VALUES
                                                                        (1, 'Electronique', 'Appareils électroniques et accessoires',  '2026-03-15 23:41:05'),
                                                                        (2, 'Vetements',    'Mode homme, femme et enfant',             '2026-03-15 23:41:05'),
                                                                        (3, 'Livres',       'Romans, sciences, technique',             '2026-03-15 23:41:05'),
                                                                        (4, 'Sport',        'Équipement et vêtements de sport',        '2026-03-15 23:41:05'),
                                                                        (5, 'Informatique', 'Livres techniques et programmation',      '2026-03-15 23:41:05'),
                                                                        (6, 'Smartphones',  'Téléphones portables',                    '2026-03-15 23:41:05'),
                                                                        (7, 'Ordinateurs',  'Laptops et desktops',                     '2026-03-15 23:41:05');


-- =============================================================
--  TABLE : products
--  Full catalog with stock management
-- =============================================================
CREATE TABLE products (
                          product_id     INT            NOT NULL AUTO_INCREMENT,
                          name           VARCHAR(200)   NOT NULL,
                          description    TEXT                    DEFAULT NULL,
                          price          DECIMAL(10, 2) NOT NULL,
                          stock_quantity INT            NOT NULL DEFAULT 0,
                          category_id    INT                     DEFAULT NULL,
                          image_url      VARCHAR(500)            DEFAULT NULL,
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

INSERT INTO products (product_id, name, description, price, stock_quantity, category_id, image_url, is_available, created_at, updated_at) VALUES
                                                                                                                                              (1,  'iPhone 15 Pro',              '...', 1199.99, 27,  6, '/images/products/1774567072937_iphone15pro.jpeg',      1, '2026-03-15 23:41:05', '2026-04-12 17:28:52'),
                                                                                                                                              (2,  'Samsung Galaxy S24',         '...', 899.99,  44,  6, '/images/products/1774567088487_galaxy-s24.png',        1, '2026-03-15 23:41:05', '2026-04-12 17:34:40'),
                                                                                                                                              (3,  'Google Pixel 8',             '...', 699.99,  25,  6, '/images/products/1774567095833_pixel8.jpg',            1, '2026-03-15 23:41:05', '2026-04-12 17:29:05'),
                                                                                                                                              (4,  'MacBook Pro M3',             '...', 2199.99, 15,  7, '/images/products/1774567103856_macbook-pro-m3.png',    1, '2026-03-15 23:41:05', '2026-04-12 17:29:12'),
                                                                                                                                              (5,  'Dell XPS 15',                '...', 1749.99, 20,  7, '/images/products/1774567111976_dell-xps-15.jpg',       1, '2026-03-15 23:41:05', '2026-04-12 17:29:19'),
                                                                                                                                              (6,  'Lenovo ThinkPad X1',         '...', 1299.99, 18,  7, '/images/products/1774567121605_lenovo-thinkpad-x1.jpg',1, '2026-03-15 23:41:05', '2026-04-12 17:29:27'),
                                                                                                                                              (7,  'T-Shirt Coton Bio',          '...', 24.99,   197, 2, '/images/products/1774567129129_T-Shirt_CotonBio.jpeg', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:34'),
                                                                                                                                              (8,  'T-Shirt Nike Dri-FIT',       '...', 34.99,   149, 2, '/images/products/1774567136127_T-ShirtNike-DriFit.jpg',1, '2026-03-15 23:41:05', '2026-04-12 17:29:42'),
                                                                                                                                              (9,  'Jean Slim Noir',             '...', 59.99,   120, 2, '/images/products/1774567184697_Jean_Noirs.jpeg',       1, '2026-03-15 23:41:05', '2026-04-12 17:29:48'),
                                                                                                                                              (10, 'Chino Beige',                '...', 49.99,   80,  2, '/images/products/1774567177168_beige_chino.jpg',       1, '2026-03-15 23:41:05', '2026-04-12 17:29:55'),
                                                                                                                                              (11, 'Clean Code',                 '...', 35.00,   60,  5, '/images/products/1776011995753_clean_code.jpg',        1, '2026-03-15 23:41:05', '2026-04-12 17:39:56'),
                                                                                                                                              (12, 'Design Patterns',            '...', 42.00,   40,  5, '/images/products/1776012004602_Design_Patterns.jpg',   1, '2026-03-15 23:41:05', '2026-04-12 17:40:05'),
                                                                                                                                              (13, 'Java : The Complete Reference','...',55.00,  35,  5, '/images/products/1776012014101_Java_book.jpg',         1, '2026-03-15 23:41:05', '2026-04-12 17:40:14'),
                                                                                                                                              (14, 'Le Petit Prince',            '...', 12.99,   90,  3, '/images/products/1776012298534_le_petit_prince.jpg',   1, '2026-03-15 23:41:05', '2026-04-12 17:44:59'),
                                                                                                                                              (15, '1984',                       '...', 14.99,   70,  3, NULL,                                                   1, '2026-03-15 23:41:05', '2026-04-12 17:35:22');


-- =============================================================
--  TABLE : carts
--  One cart per user (enforced by UNIQUE on user_id)
-- =============================================================
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

INSERT INTO carts (cart_id, user_id, last_modified) VALUES
                                                        (1, 2, '2026-03-15 23:41:05'),
                                                        (2, 3, '2026-03-15 23:41:05'),
                                                        (3, 4, '2026-03-15 23:41:05'),
                                                        (4, 5, '2026-03-15 23:41:05'),
                                                        (5, 7, '2026-03-25 14:53:02');


-- =============================================================
--  TABLE : cart_items
--  Products in a cart with quantity and price snapshot
-- =============================================================
CREATE TABLE cart_items (
                            cart_item_id INT           NOT NULL AUTO_INCREMENT,
                            cart_id      INT           NOT NULL,
                            product_id   INT           NOT NULL,
                            quantity     INT           NOT NULL DEFAULT 1,
                            unit_price   DECIMAL(10,2) NOT NULL,

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

INSERT INTO cart_items (cart_item_id, cart_id, product_id, quantity, unit_price) VALUES
                                                                                     (1, 1, 1,  1, 1199.99),
                                                                                     (2, 1, 11, 2,   35.00);


-- =============================================================
--  TABLE : orders
--  Customer orders with status tracking
-- =============================================================
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
                            ON UPDATE CASCADE,
                        CONSTRAINT ck_orders_total CHECK (total_amount >= 0)
) ENGINE=InnoDB;

INSERT INTO orders (order_id, user_id, status, total_amount, shipping_address, created_at, updated_at) VALUES
                                                                                                           ('ORD-0001-2026', 2, 'LIVREE',     899.99,  '12 Avenue Victor Hugo, Lyon',    '2026-03-15 23:41:06', '2026-03-15 23:41:06'),
                                                                                                           ('ORD-0002-2026', 2, 'EXPEDIEE',    70.00,  '12 Avenue Victor Hugo, Lyon',    '2026-03-15 23:41:06', '2026-03-15 23:41:06'),
                                                                                                           ('ORD-0003-2026', 3, 'VALIDEE',   2199.99,  '8 Boulevard Pasteur, Marseille', '2026-03-15 23:41:06', '2026-03-15 23:41:06'),
                                                                                                           ('ORD-0004-2026', 4, 'VALIDEE',     74.97,  '5 Rue du Moulin, Bordeaux',      '2026-03-15 23:41:06', '2026-03-16 01:12:15'),
                                                                                                           ('ORD-0005-2026', 5, 'ANNULEE',   1299.00,  '22 Rue de Rivoli, Paris',        '2026-03-15 23:41:06', '2026-03-15 23:41:06'),
                                                                                                           ('ORD-7C9E12F4',  4, 'VALIDEE',     74.97,  '5 Rue du Moulin, Bordeaux',      '2026-03-16 01:12:15', '2026-03-16 01:12:15'),
                                                                                                           ('ORD-EC8F6535',  7, 'VALIDEE',   1199.99,  '99 Rue X, Teouan',               '2026-03-25 14:53:51', '2026-04-12 18:00:32');


-- =============================================================
--  TABLE : order_items
--  Lines of an order — product snapshot preserves data if
--  product is later modified or deleted
-- =============================================================
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

INSERT INTO order_items (order_item_id, order_id, product_id, product_snapshot, quantity, unit_price) VALUES
                                                                                                          (1, 'ORD-0001-2026', 2,  '{"name":"Samsung Galaxy S24","price":899.99}',   1,  899.99),
                                                                                                          (2, 'ORD-0002-2026', 11, '{"name":"Clean Code","price":35.00}',            2,   35.00),
                                                                                                          (3, 'ORD-0003-2026', 4,  '{"name":"MacBook Pro M3","price":2199.99}',      1, 2199.99),
                                                                                                          (4, 'ORD-0004-2026', 7,  '{"name":"T-Shirt Coton Bio","price":24.99}',     3,   24.99),
                                                                                                          (5, 'ORD-7C9E12F4',  7,  '{"name":"T-Shirt Coton Bio","price":24.99}',     3,   24.99),
                                                                                                          (6, 'ORD-EC8F6535',  1,  '{"name":"iPhone 15 Pro","price":1199.99}',       1, 1199.99);


-- =============================================================
--  TABLE : payments
--  One payment per order — simulated (no real gateway)
-- =============================================================
CREATE TABLE payments (
                          payment_id      VARCHAR(36)    NOT NULL,
                          order_id        VARCHAR(36)    NOT NULL,
                          method          ENUM('CARTE_BANCAIRE','PAIEMENT_FICTIF','VIREMENT') NOT NULL,
                          status          ENUM('EN_ATTENTE','ACCEPTE','REFUSE','REMBOURSE')   NOT NULL DEFAULT 'EN_ATTENTE',
                          amount          DECIMAL(10,2)  NOT NULL,
                          card_last_four  CHAR(4)                DEFAULT NULL,
                          transaction_ref VARCHAR(100)           DEFAULT NULL,
                          paid_at         DATETIME               DEFAULT NULL,

                          CONSTRAINT pk_payments        PRIMARY KEY (payment_id),
                          CONSTRAINT uq_payments_order  UNIQUE (order_id),
                          CONSTRAINT fk_payments_order  FOREIGN KEY (order_id)
                              REFERENCES orders(order_id)
                              ON DELETE CASCADE ON UPDATE CASCADE,
                          CONSTRAINT ck_payments_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

INSERT INTO payments (payment_id, order_id, method, status, amount, card_last_four, transaction_ref, paid_at) VALUES
                                                                                                                  ('PAY-0001-2026', 'ORD-0001-2026', 'CARTE_BANCAIRE',  'ACCEPTE',   899.99,  '4242', 'TXN-A1B2C3D4', '2026-01-15 10:30:00'),
                                                                                                                  ('PAY-0002-2026', 'ORD-0002-2026', 'PAIEMENT_FICTIF', 'ACCEPTE',    70.00,  NULL,   'TXN-E5F6G7H8', '2026-02-01 14:00:00'),
                                                                                                                  ('PAY-0003-2026', 'ORD-0003-2026', 'CARTE_BANCAIRE',  'ACCEPTE',  2199.99,  '1234', 'TXN-I9J0K1L2', '2026-02-20 09:15:00'),
                                                                                                                  ('PAY-0004-2026', 'ORD-0004-2026', 'CARTE_BANCAIRE',  'ACCEPTE',    74.97,  NULL,   'TXN-3E4D3E60', '2026-03-16 01:12:15'),
                                                                                                                  ('PAY-0005-2026', 'ORD-0005-2026', 'VIREMENT',        'REMBOURSE', 1299.00, NULL,   'TXN-M3N4O5P6', '2026-03-01 11:45:00'),
                                                                                                                  ('PAY-16251479',  'ORD-7C9E12F4',  'CARTE_BANCAIRE',  'ACCEPTE',    74.97,  '9999', 'TXN-D6D357D8', '2026-03-16 01:12:15'),
                                                                                                                  ('PAY-E8FB2C08',  'ORD-EC8F6535',  'PAIEMENT_FICTIF', 'ACCEPTE',  1199.99,  '7890', 'TXN-FC667164', '2026-03-25 14:53:51');


-- =============================================================
--  TABLE : notifications
--  Sent via UDP — stored for history and unread tracking
-- =============================================================
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

INSERT INTO notifications (notification_id, user_id, message, type, is_read, protocol, created_at) VALUES
                                                                                                       (1, 2, 'Votre commande #ORD-0001-2026 est maintenant : LIVREE',    'ORDER',   1, 'UDP', '2026-03-15 23:41:06'),
                                                                                                       (2, 2, 'Votre commande #ORD-0002-2026 est maintenant : EXPEDIEE',  'ORDER',   0, 'UDP', '2026-03-15 23:41:06'),
                                                                                                       (3, 3, 'Votre commande #ORD-0003-2026 est maintenant : VALIDEE',   'ORDER',   0, 'UDP', '2026-03-15 23:41:06'),
                                                                                                       (4, 3, 'Paiement accepte pour la commande #ORD-0003-2026',         'PAYMENT', 1, 'UDP', '2026-03-15 23:41:06'),
                                                                                                       (5, 5, 'Votre commande #ORD-0005-2026 a ete annulee.',             'ORDER',   0, 'UDP', '2026-03-15 23:41:06'),
                                                                                                       (7, 7, 'Commande ORD-EC8F6535 confirmée ! Total : 1199.99 €',      'ORDER',   1, 'UDP', '2026-03-25 14:53:51');

-- =============================================================
--  FIN DU SCRIPT
-- =============================================================