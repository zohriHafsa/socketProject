-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : mar. 14 avr. 2026 à 20:23
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `chrionline`
--

-- --------------------------------------------------------

--
-- Structure de la table `admins`
--

CREATE TABLE `admins` (
                          `admin_id` int(11) NOT NULL,
                          `username` varchar(50) NOT NULL,
                          `email` varchar(150) NOT NULL,
                          `password_hash` varchar(255) NOT NULL,
                          `first_name` varchar(80) NOT NULL,
                          `last_name` varchar(80) NOT NULL,
                          `is_active` tinyint(1) NOT NULL DEFAULT 1,
                          `is_super` tinyint(1) NOT NULL DEFAULT 0,
                          `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `admins`
--

INSERT INTO `admins` (`admin_id`, `username`, `email`, `password_hash`, `first_name`, `last_name`, `is_active`, `is_super`, `created_at`) VALUES
                                                                                                                                              (1, 'superadmin', 'super@chrionline.com', '$argon2id$v=19$m=65536,t=3,p=1$AMLD/tGQNnu4Q/q9pWbV/Q$OnR4IEiBMTmLJ2N1bolG/Pz3Bdsm3I5ADnWfgxqW73I', 'Super', 'Admin', 1, 1, '2026-03-29 15:16:50'),
                                                                                                                                              (2, 'admin', 'admin@chrionline.com', '$argon2id$v=19$m=65536,t=3,p=1$thfCAtqBaKOrOS1ABlUnkQ$Ib26FKpVzxrhKIPF5m7O0uK6f9Omab8vOGSBUhNjY4E', 'Normal', 'Admin', 1, 0, '2026-03-29 17:04:54');

-- --------------------------------------------------------

--
-- Structure de la table `carts`
--

CREATE TABLE `carts` (
                         `cart_id` int(11) NOT NULL,
                         `user_id` int(11) NOT NULL,
                         `last_modified` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `carts`
--

INSERT INTO `carts` (`cart_id`, `user_id`, `last_modified`) VALUES
                                                                (1, 2, '2026-03-15 23:41:05'),
                                                                (2, 3, '2026-03-15 23:41:05'),
                                                                (3, 4, '2026-03-15 23:41:05'),
                                                                (4, 5, '2026-03-15 23:41:05'),
                                                                (5, 7, '2026-03-25 14:53:02'),
                                                                (6, 9, '2026-04-14 18:40:09'),
                                                                (7, 10, '2026-04-14 18:50:20');

-- --------------------------------------------------------

--
-- Structure de la table `cart_items`
--

CREATE TABLE `cart_items` (
                              `cart_item_id` int(11) NOT NULL,
                              `cart_id` int(11) NOT NULL,
                              `product_id` int(11) NOT NULL,
                              `quantity` int(11) NOT NULL DEFAULT 1,
                              `unit_price` decimal(10,2) NOT NULL
) ;

--
-- Déchargement des données de la table `cart_items`
--

INSERT INTO `cart_items` (`cart_item_id`, `cart_id`, `product_id`, `quantity`, `unit_price`) VALUES
                                                                                                 (1, 1, 1, 1, 1199.99),
                                                                                                 (2, 1, 11, 2, 35.00),
                                                                                                 (3, 6, 1, 1, 1199.99);

-- --------------------------------------------------------

--
-- Structure de la table `categories`
--

CREATE TABLE `categories` (
                              `category_id` int(11) NOT NULL,
                              `name` varchar(100) NOT NULL,
                              `description` text DEFAULT NULL,
                              `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `categories`
--

INSERT INTO `categories` (`category_id`, `name`, `description`, `created_at`) VALUES
                                                                                  (1, 'Electronique', 'Appareils électroniques et accessoires', '2026-03-15 23:41:05'),
                                                                                  (2, 'Vetements', 'Mode homme, femme et enfant', '2026-03-15 23:41:05'),
                                                                                  (3, 'Livres', 'Romans, sciences, technique', '2026-03-15 23:41:05'),
                                                                                  (4, 'Sport', 'Équipement et vêtements de sport', '2026-03-15 23:41:05'),
                                                                                  (5, 'Informatique', 'Livres techniques et programmation', '2026-03-15 23:41:05'),
                                                                                  (6, 'Smartphones', 'Téléphones portables', '2026-03-15 23:41:05'),
                                                                                  (7, 'Ordinateurs', 'Laptops et desktops', '2026-03-15 23:41:05');

-- --------------------------------------------------------

--
-- Structure de la table `moroccan_banks`
--

CREATE TABLE `moroccan_banks` (
                                  `bank_id` int(11) NOT NULL,
                                  `bank_name` varchar(100) NOT NULL,
                                  `bank_code` varchar(10) NOT NULL COMMENT 'Code banque (3 premiers chiffres du RIB)',
                                  `bin_prefix` varchar(6) NOT NULL COMMENT 'BIN de la carte (6 premiers chiffres)',
                                  `logo_filename` varchar(100) DEFAULT NULL,
                                  `is_active` tinyint(1) NOT NULL DEFAULT 1,
                                  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `moroccan_banks`
--

INSERT INTO `moroccan_banks` (`bank_id`, `bank_name`, `bank_code`, `bin_prefix`, `logo_filename`, `is_active`, `created_at`) VALUES
                                                                                                                                 (1, 'Attijariwafa Bank', '007', '421141', 'attijariwafa.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (2, 'Attijariwafa Bank', '007', '535333', 'attijariwafa.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (3, 'Banque Populaire', '011', '454393', 'banque_populaire.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (4, 'Banque Populaire', '011', '535334', 'banque_populaire.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (5, 'BMCE Bank', '013', '421142', 'bmce.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (6, 'BMCE Bank', '013', '535335', 'bmce.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (7, 'BMCI', '060', '421143', 'bmci.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (8, 'BMCI', '060', '535336', 'bmci.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (9, 'Crédit du Maroc', '175', '421144', 'cdm.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (10, 'Crédit du Maroc', '175', '535337', 'cdm.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (11, 'CIH Bank', '230', '421145', 'cih.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (12, 'CIH Bank', '230', '535338', 'cih.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (13, 'Société Générale Maroc', '022', '421146', 'sgma.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (14, 'Société Générale Maroc', '022', '535339', 'sgma.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (15, 'Al Barid Bank', '130', '421147', 'baridbank.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (16, 'Al Barid Bank', '130', '535340', 'baridbank.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (17, 'Crédit Agricole du Maroc', '017', '421148', 'cam.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (18, 'Crédit Agricole du Maroc', '017', '535341', 'cam.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (19, 'Bank of Africa', '008', '421149', 'boa.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (20, 'Bank of Africa', '008', '535342', 'boa.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (21, 'CFG Bank', '021', '421150', 'cfg.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (22, 'Arab Bank Maroc', '070', '421151', 'arabbank.png', 1, '2026-04-14 18:08:59'),
                                                                                                                                 (23, 'Umnia Bank', '082', '535343', 'umnia.png', 1, '2026-04-14 18:08:59');

-- --------------------------------------------------------

--
-- Structure de la table `notifications`
--

CREATE TABLE `notifications` (
                                 `notification_id` int(11) NOT NULL,
                                 `user_id` int(11) NOT NULL,
                                 `message` text NOT NULL,
                                 `type` varchar(50) NOT NULL DEFAULT 'INFO',
                                 `is_read` tinyint(1) NOT NULL DEFAULT 0,
                                 `protocol` enum('TCP','UDP') NOT NULL DEFAULT 'UDP',
                                 `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `notifications`
--

INSERT INTO `notifications` (`notification_id`, `user_id`, `message`, `type`, `is_read`, `protocol`, `created_at`) VALUES
                                                                                                                       (1, 2, 'Votre commande #ORD-0001-2026 est maintenant : LIVREE', 'ORDER', 1, 'UDP', '2026-03-15 23:41:06'),
                                                                                                                       (2, 2, 'Votre commande #ORD-0002-2026 est maintenant : EXPEDIEE', 'ORDER', 0, 'UDP', '2026-03-15 23:41:06'),
                                                                                                                       (3, 3, 'Votre commande #ORD-0003-2026 est maintenant : VALIDEE', 'ORDER', 0, 'UDP', '2026-03-15 23:41:06'),
                                                                                                                       (4, 3, 'Paiement accepte pour la commande #ORD-0003-2026', 'PAYMENT', 1, 'UDP', '2026-03-15 23:41:06'),
                                                                                                                       (5, 5, 'Votre commande #ORD-0005-2026 a ete annulee.', 'ORDER', 0, 'UDP', '2026-03-15 23:41:06'),
                                                                                                                       (7, 7, 'Commande ORD-EC8F6535 confirmée ! Total : 1199.99 €', 'ORDER', 1, 'UDP', '2026-03-25 14:53:51'),
                                                                                                                       (8, 10, 'Commande ORD-10A9EE80 confirmée ! Total : 899.99 €', 'ORDER', 1, 'UDP', '2026-04-14 19:09:36');

-- --------------------------------------------------------

--
-- Structure de la table `orders`
--

CREATE TABLE `orders` (
                          `order_id` varchar(36) NOT NULL,
                          `user_id` int(11) NOT NULL,
                          `status` enum('EN_ATTENTE','VALIDEE','EXPEDIEE','LIVREE','ANNULEE') NOT NULL DEFAULT 'EN_ATTENTE',
                          `total_amount` decimal(10,2) NOT NULL,
                          `shipping_address` text NOT NULL,
                          `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                          `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

--
-- Déchargement des données de la table `orders`
--

INSERT INTO `orders` (`order_id`, `user_id`, `status`, `total_amount`, `shipping_address`, `created_at`, `updated_at`) VALUES
                                                                                                                           ('ORD-0001-2026', 2, 'LIVREE', 899.99, '12 Avenue Victor Hugo, Lyon', '2026-03-15 23:41:06', '2026-03-15 23:41:06'),
                                                                                                                           ('ORD-0002-2026', 2, 'EXPEDIEE', 70.00, '12 Avenue Victor Hugo, Lyon', '2026-03-15 23:41:06', '2026-03-15 23:41:06'),
                                                                                                                           ('ORD-0003-2026', 3, 'VALIDEE', 2199.99, '8 Boulevard Pasteur, Marseille', '2026-03-15 23:41:06', '2026-03-15 23:41:06'),
                                                                                                                           ('ORD-0004-2026', 4, 'VALIDEE', 74.97, '5 Rue du Moulin, Bordeaux', '2026-03-15 23:41:06', '2026-03-16 01:12:15'),
                                                                                                                           ('ORD-0005-2026', 5, 'ANNULEE', 1299.00, '22 Rue de Rivoli, Paris', '2026-03-15 23:41:06', '2026-03-15 23:41:06'),
                                                                                                                           ('ORD-10A9EE80', 10, 'VALIDEE', 899.99, 'Mhamid Bouaakaz 1504, marrakech 40000', '2026-04-14 19:09:36', '2026-04-14 19:09:36'),
                                                                                                                           ('ORD-7C9E12F4', 4, 'VALIDEE', 74.97, '5 Rue du Moulin, Bordeaux', '2026-03-16 01:12:15', '2026-03-16 01:12:15'),
                                                                                                                           ('ORD-EC8F6535', 7, 'VALIDEE', 1199.99, '99 Rue X, Teouan', '2026-03-25 14:53:51', '2026-04-12 18:00:32');

-- --------------------------------------------------------

--
-- Structure de la table `order_items`
--

CREATE TABLE `order_items` (
                               `order_item_id` int(11) NOT NULL,
                               `order_id` varchar(36) NOT NULL,
                               `product_id` int(11) DEFAULT NULL,
                               `product_snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`product_snapshot`)),
                               `quantity` int(11) NOT NULL,
                               `unit_price` decimal(10,2) NOT NULL
) ;

--
-- Déchargement des données de la table `order_items`
--

INSERT INTO `order_items` (`order_item_id`, `order_id`, `product_id`, `product_snapshot`, `quantity`, `unit_price`) VALUES
                                                                                                                        (1, 'ORD-0001-2026', 2, '{\"name\":\"Samsung Galaxy S24\",\"price\":899.99}', 1, 899.99),
                                                                                                                        (2, 'ORD-0002-2026', 11, '{\"name\":\"Clean Code\",\"price\":35.00}', 2, 35.00),
                                                                                                                        (3, 'ORD-0003-2026', 4, '{\"name\":\"MacBook Pro M3\",\"price\":2199.99}', 1, 2199.99),
                                                                                                                        (4, 'ORD-0004-2026', 7, '{\"name\":\"T-Shirt Coton Bio\",\"price\":24.99}', 3, 24.99),
                                                                                                                        (5, 'ORD-7C9E12F4', 7, '{\"name\":\"T-Shirt Coton Bio\",\"price\":24.99}', 3, 24.99),
                                                                                                                        (6, 'ORD-EC8F6535', 1, '{\"name\":\"iPhone 15 Pro\",\"price\":1199.99}', 1, 1199.99),
                                                                                                                        (7, 'ORD-10A9EE80', 2, '{\"name\":\"Samsung Galaxy S24\",\"price\":899.99}', 1, 899.99);

-- --------------------------------------------------------

--
-- Structure de la table `payments`
--

CREATE TABLE `payments` (
                            `payment_id` varchar(36) NOT NULL,
                            `order_id` varchar(36) NOT NULL,
                            `method` enum('CARTE_BANCAIRE','PAIEMENT_FICTIF','VIREMENT') NOT NULL,
                            `status` enum('EN_ATTENTE','ACCEPTE','REFUSE','REMBOURSE') NOT NULL DEFAULT 'EN_ATTENTE',
                            `amount` decimal(10,2) NOT NULL,
                            `card_last_four` char(4) DEFAULT NULL,
                            `transaction_ref` varchar(100) DEFAULT NULL,
                            `paid_at` datetime DEFAULT NULL
) ;

--
-- Déchargement des données de la table `payments`
--

INSERT INTO `payments` (`payment_id`, `order_id`, `method`, `status`, `amount`, `card_last_four`, `transaction_ref`, `paid_at`) VALUES
                                                                                                                                    ('PAY-0001-2026', 'ORD-0001-2026', 'CARTE_BANCAIRE', 'ACCEPTE', 899.99, '4242', 'TXN-A1B2C3D4', '2026-01-15 10:30:00'),
                                                                                                                                    ('PAY-0002-2026', 'ORD-0002-2026', 'PAIEMENT_FICTIF', 'ACCEPTE', 70.00, NULL, 'TXN-E5F6G7H8', '2026-02-01 14:00:00'),
                                                                                                                                    ('PAY-0003-2026', 'ORD-0003-2026', 'CARTE_BANCAIRE', 'ACCEPTE', 2199.99, '1234', 'TXN-I9J0K1L2', '2026-02-20 09:15:00'),
                                                                                                                                    ('PAY-0004-2026', 'ORD-0004-2026', 'CARTE_BANCAIRE', 'ACCEPTE', 74.97, NULL, 'TXN-3E4D3E60', '2026-03-16 01:12:15'),
                                                                                                                                    ('PAY-0005-2026', 'ORD-0005-2026', 'VIREMENT', 'REMBOURSE', 1299.00, NULL, 'TXN-M3N4O5P6', '2026-03-01 11:45:00'),
                                                                                                                                    ('PAY-16251479', 'ORD-7C9E12F4', 'CARTE_BANCAIRE', 'ACCEPTE', 74.97, '9999', 'TXN-D6D357D8', '2026-03-16 01:12:15'),
                                                                                                                                    ('PAY-29E299B6', 'ORD-10A9EE80', 'CARTE_BANCAIRE', 'ACCEPTE', 899.99, '0003', 'TXN-D88E596B', '2026-04-14 19:09:36'),
                                                                                                                                    ('PAY-E8FB2C08', 'ORD-EC8F6535', 'PAIEMENT_FICTIF', 'ACCEPTE', 1199.99, '7890', 'TXN-FC667164', '2026-03-25 14:53:51');

-- --------------------------------------------------------

--
-- Structure de la table `payment_2fa_codes`
--

CREATE TABLE `payment_2fa_codes` (
                                     `code_id` int(11) NOT NULL,
                                     `user_id` int(11) NOT NULL,
                                     `code` varchar(6) NOT NULL,
                                     `amount` decimal(10,2) NOT NULL,
                                     `order_id` varchar(36) DEFAULT NULL,
                                     `expires_at` datetime NOT NULL,
                                     `is_used` tinyint(1) NOT NULL DEFAULT 0,
                                     `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `payment_2fa_codes`
--

INSERT INTO `payment_2fa_codes` (`code_id`, `user_id`, `code`, `amount`, `order_id`, `expires_at`, `is_used`, `created_at`) VALUES
                                                                                                                                (1, 10, '870965', 899.99, 'TEMP-1776189952785', '2026-04-14 19:10:52', 0, '2026-04-14 19:05:52'),
                                                                                                                                (2, 10, '419979', 899.99, 'TEMP-1776190162282', '2026-04-14 19:14:22', 1, '2026-04-14 19:09:22');

-- --------------------------------------------------------

--
-- Structure de la table `payment_attempts`
--

CREATE TABLE `payment_attempts` (
                                    `attempt_id` int(11) NOT NULL,
                                    `user_id` int(11) NOT NULL,
                                    `card_number` varchar(19) NOT NULL COMMENT 'Masqué : **** **** **** 1234',
                                    `failure_reason` varchar(255) DEFAULT NULL,
                                    `attempted_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `payment_attempts`
--

INSERT INTO `payment_attempts` (`attempt_id`, `user_id`, `card_number`, `failure_reason`, `attempted_at`) VALUES
    (1, 10, '**** **** **** 0003', 'Échec validation Luhn', '2026-04-14 19:05:02');

-- --------------------------------------------------------

--
-- Structure de la table `products`
--

CREATE TABLE `products` (
                            `product_id` int(11) NOT NULL,
                            `name` varchar(200) NOT NULL,
                            `description` text DEFAULT NULL,
                            `price` decimal(10,2) NOT NULL,
                            `stock_quantity` int(11) NOT NULL DEFAULT 0,
                            `category_id` int(11) DEFAULT NULL,
                            `image_url` varchar(500) DEFAULT NULL,
                            `is_available` tinyint(1) NOT NULL DEFAULT 1,
                            `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                            `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

--
-- Déchargement des données de la table `products`
--

INSERT INTO `products` (`product_id`, `name`, `description`, `price`, `stock_quantity`, `category_id`, `image_url`, `is_available`, `created_at`, `updated_at`) VALUES
                                                                                                                                                                    (1, 'iPhone 15 Pro', '...', 1199.99, 27, 6, '/images/products/1774567072937_iphone15pro.jpeg', 1, '2026-03-15 23:41:05', '2026-04-12 17:28:52'),
                                                                                                                                                                    (2, 'Samsung Galaxy S24', '...', 899.99, 43, 6, '/images/products/1774567088487_galaxy-s24.png', 1, '2026-03-15 23:41:05', '2026-04-14 19:09:36'),
                                                                                                                                                                    (3, 'Google Pixel 8', '...', 699.99, 25, 6, '/images/products/1774567095833_pixel8.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:05'),
                                                                                                                                                                    (4, 'MacBook Pro M3', '...', 2199.99, 15, 7, '/images/products/1774567103856_macbook-pro-m3.png', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:12'),
                                                                                                                                                                    (5, 'Dell XPS 15', '...', 1749.99, 20, 7, '/images/products/1774567111976_dell-xps-15.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:19'),
                                                                                                                                                                    (6, 'Lenovo ThinkPad X1', '...', 1299.99, 18, 7, '/images/products/1774567121605_lenovo-thinkpad-x1.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:27'),
                                                                                                                                                                    (7, 'T-Shirt Coton Bio', '...', 24.99, 197, 2, '/images/products/1774567129129_T-Shirt_CotonBio.jpeg', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:34'),
                                                                                                                                                                    (8, 'T-Shirt Nike Dri-FIT', '...', 34.99, 149, 2, '/images/products/1774567136127_T-ShirtNike-DriFit.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:42'),
                                                                                                                                                                    (9, 'Jean Slim Noir', '...', 59.99, 120, 2, '/images/products/1774567184697_Jean_Noirs.jpeg', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:48'),
                                                                                                                                                                    (10, 'Chino Beige', '...', 49.99, 80, 2, '/images/products/1774567177168_beige_chino.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:29:55'),
                                                                                                                                                                    (11, 'Clean Code', '...', 35.00, 60, 5, '/images/products/1776011995753_clean_code.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:39:56'),
                                                                                                                                                                    (12, 'Design Patterns', '...', 42.00, 40, 5, '/images/products/1776012004602_Design_Patterns.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:40:05'),
                                                                                                                                                                    (13, 'Java : The Complete Reference', '...', 55.00, 35, 5, '/images/products/1776012014101_Java_book.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:40:14'),
                                                                                                                                                                    (14, 'Le Petit Prince', '...', 12.99, 90, 3, '/images/products/1776012298534_le_petit_prince.jpg', 1, '2026-03-15 23:41:05', '2026-04-12 17:44:59'),
                                                                                                                                                                    (15, '1984', '...', 14.99, 70, 3, NULL, 1, '2026-03-15 23:41:05', '2026-04-12 17:35:22');

-- --------------------------------------------------------

--
-- Structure de la table `sessions`
--

CREATE TABLE `sessions` (
                            `session_id` varchar(64) NOT NULL,
                            `user_id` int(11) NOT NULL,
                            `client_address` varchar(45) DEFAULT NULL,
                            `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                            `expires_at` datetime NOT NULL,
                            `is_active` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `sessions`
--

INSERT INTO `sessions` (`session_id`, `user_id`, `client_address`, `created_at`, `expires_at`, `is_active`) VALUES
                                                                                                                ('sess-0001-abcd-efgh-ijkl', 2, '192.168.1.10', '2026-03-15 23:41:05', '2026-03-16 00:41:05', 1),
                                                                                                                ('sess-0002-abcd-efgh-ijkl', 3, '192.168.1.11', '2026-03-15 23:41:05', '2026-03-16 00:41:05', 1);

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
                         `user_id` int(11) NOT NULL,
                         `username` varchar(50) NOT NULL,
                         `email` varchar(150) NOT NULL,
                         `password_hash` varchar(255) NOT NULL,
                         `first_name` varchar(80) NOT NULL,
                         `last_name` varchar(80) NOT NULL,
                         `address` text DEFAULT NULL,
                         `phone` varchar(20) DEFAULT NULL,
                         `role` enum('CLIENT') NOT NULL DEFAULT 'CLIENT',
                         `is_active` tinyint(1) NOT NULL DEFAULT 1,
                         `is_verified` tinyint(1) NOT NULL DEFAULT 0,
                         `verify_code` varchar(10) DEFAULT NULL,
                         `verify_expires` datetime DEFAULT NULL,
                         `failed_attempts` int(11) NOT NULL DEFAULT 0,
                         `locked_until` datetime DEFAULT NULL,
                         `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                         `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`user_id`, `username`, `email`, `password_hash`, `first_name`, `last_name`, `address`, `phone`, `role`, `is_active`, `is_verified`, `verify_code`, `verify_expires`, `failed_attempts`, `locked_until`, `created_at`, `updated_at`) VALUES
                                                                                                                                                                                                                                                             (2, 'jdupont', 'jean.dupont@mail.com', '$argon2id$v=19$m=65536,t=3,p=1$wW23gnSPzmlQ59gD0vPT8A$wHjOLMhmL9kFrIek4d1FD8O8sxL6X0LhOLKNM1aqcv8', 'Jean', 'Dupont', '12 Avenue Victor Hugo, Lyon', '0611111111', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-03-15 23:41:05', '2026-04-12 17:47:00'),
                                                                                                                                                                                                                                                             (3, 'mleroy', 'marie.leroy@mail.com', '$argon2id$v=19$m=65536,t=3,p=1$wW23gnSPzmlQ59gD0vPT8A$wHjOLMhmL9kFrIek4d1FD8O8sxL6X0LhOLKNM1aqcv8', 'Marie', 'Leroy', '8 Boulevard Pasteur, Marseille', '0622222222', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-03-15 23:41:05', '2026-04-12 17:47:02'),
                                                                                                                                                                                                                                                             (4, 'cblanc', 'claire.blanc@mail.com', '$argon2id$v=19$m=65536,t=3,p=1$wW23gnSPzmlQ59gD0vPT8A$wHjOLMhmL9kFrIek4d1FD8O8sxL6X0LhOLKNM1aqcv8', 'Claire', 'Blanc', '5 Rue du Moulin, Bordeaux', '0633333333', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-03-15 23:41:05', '2026-04-12 17:47:04'),
                                                                                                                                                                                                                                                             (5, 'tnoir', 'thomas.noir@mail.com', '$argon2id$v=19$m=65536,t=3,p=1$wW23gnSPzmlQ59gD0vPT8A$wHjOLMhmL9kFrIek4d1FD8O8sxL6X0LhOLKNM1aqcv8', 'Thomas', 'Noir', '22 Rue de Rivoli, Paris', '0644444444', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-03-15 23:41:05', '2026-04-12 17:50:50'),
                                                                                                                                                                                                                                                             (6, 'mohamed_amine', 'mohamedamine@gmail.com', '$argon2id$v=19$m=65536,t=3,p=1$UkK1ElrOLyBe+hoEDgcLFg$07jzI2dXQtXU1EqxPW5IdLlPrA0aOoEdwsICj9u+F8E', 'Mohamed Amine', 'Ait Jaakike', '15 Rue Mouassine, Marrakech', '0622334412', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-03-16 11:50:51', '2026-04-12 18:01:35'),
                                                                                                                                                                                                                                                             (7, 'hafsa', 'hafsa@gmail.com', '$argon2id$v=19$m=65536,t=3,p=1$bohgW3nfW0eo5I0txmzTPA$ji/LNS6OwjDIbsdQ0GJVYtlacidIBXc5Lk8vV7asXdU', 'Hafsa', 'Zohri', '22 Avenue Moulay El Hassan, Martil', '0699887766', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-03-25 14:44:46', '2026-04-12 17:50:05'),
                                                                                                                                                                                                                                                             (8, 'amine', 'amine@gmail.com', '$argon2id$v=19$m=65536,t=3,p=1$oSkgWXCpX1vnE5pC0W7x1w$ghUHzGPO8qiNvQnlIENq54ITgqZxx/wguriu1uwF05E', 'Amine', 'El Hauari', '12 Av. des FAR, Tetouan', '0612312345', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-03-27 09:17:49', '2026-04-12 18:04:38'),
                                                                                                                                                                                                                                                             (9, 'amine_jk', 'aitjaakikemohamedamine@gmail.com', '$argon2id$v=19$m=65536,t=3,p=1$mOFBZpRj5QAlpxoudb1Kfw$yLE1Zel8eJC8QcVYEKZiYMQF9uX8X8bkvSspNktsdvU', 'Amine', 'Jaakike', '', '', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-04-14 18:38:45', '2026-04-14 18:39:59'),
                                                                                                                                                                                                                                                             (10, 'amine2_jk', 'mohamedamine.aitjaakike@etu.uae.ac.ma', '$argon2id$v=19$m=65536,t=3,p=1$+6c+AM7kWjVsS5SEFY1L/Q$mfBoo4NKYMnoyHGHwX/RLXAFYWa2/aw0rWwbDyMoxGg', 'Amine', 'Jaakike', '', '', 'CLIENT', 1, 1, NULL, NULL, 0, NULL, '2026-04-14 18:49:29', '2026-04-14 19:08:00');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `admins`
--
ALTER TABLE `admins`
    ADD PRIMARY KEY (`admin_id`),
  ADD UNIQUE KEY `uq_admins_username` (`username`),
  ADD UNIQUE KEY `uq_admins_email` (`email`);

--
-- Index pour la table `carts`
--
ALTER TABLE `carts`
    ADD PRIMARY KEY (`cart_id`),
  ADD UNIQUE KEY `uq_carts_user` (`user_id`);

--
-- Index pour la table `cart_items`
--
ALTER TABLE `cart_items`
    ADD PRIMARY KEY (`cart_item_id`),
  ADD UNIQUE KEY `uq_cart_items_product` (`cart_id`,`product_id`),
  ADD KEY `fk_cart_items_product` (`product_id`);

--
-- Index pour la table `categories`
--
ALTER TABLE `categories`
    ADD PRIMARY KEY (`category_id`),
  ADD UNIQUE KEY `uq_categories_name` (`name`);

--
-- Index pour la table `moroccan_banks`
--
ALTER TABLE `moroccan_banks`
    ADD PRIMARY KEY (`bank_id`),
  ADD UNIQUE KEY `uq_banks_bin` (`bin_prefix`),
  ADD KEY `idx_bin_lookup` (`bin_prefix`,`is_active`),
  ADD KEY `idx_bank_code` (`bank_code`);

--
-- Index pour la table `notifications`
--
ALTER TABLE `notifications`
    ADD PRIMARY KEY (`notification_id`),
  ADD KEY `fk_notifications_user` (`user_id`);

--
-- Index pour la table `orders`
--
ALTER TABLE `orders`
    ADD PRIMARY KEY (`order_id`),
  ADD KEY `fk_orders_user` (`user_id`);

--
-- Index pour la table `order_items`
--
ALTER TABLE `order_items`
    ADD PRIMARY KEY (`order_item_id`),
  ADD KEY `fk_order_items_order` (`order_id`),
  ADD KEY `fk_order_items_product` (`product_id`);

--
-- Index pour la table `payments`
--
ALTER TABLE `payments`
    ADD PRIMARY KEY (`payment_id`),
  ADD UNIQUE KEY `uq_payments_order` (`order_id`);

--
-- Index pour la table `payment_2fa_codes`
--
ALTER TABLE `payment_2fa_codes`
    ADD PRIMARY KEY (`code_id`),
  ADD KEY `idx_code_expires` (`code`,`expires_at`),
  ADD KEY `idx_user_unused` (`user_id`,`is_used`);

--
-- Index pour la table `payment_attempts`
--
ALTER TABLE `payment_attempts`
    ADD PRIMARY KEY (`attempt_id`),
  ADD KEY `idx_user_card` (`user_id`,`card_number`),
  ADD KEY `idx_attempted_at` (`attempted_at`);

--
-- Index pour la table `products`
--
ALTER TABLE `products`
    ADD PRIMARY KEY (`product_id`),
  ADD KEY `fk_products_category` (`category_id`);

--
-- Index pour la table `sessions`
--
ALTER TABLE `sessions`
    ADD PRIMARY KEY (`session_id`),
  ADD KEY `fk_sessions_user` (`user_id`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
    ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `uq_users_username` (`username`),
  ADD UNIQUE KEY `uq_users_email` (`email`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `admins`
--
ALTER TABLE `admins`
    MODIFY `admin_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `carts`
--
ALTER TABLE `carts`
    MODIFY `cart_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `cart_items`
--
ALTER TABLE `cart_items`
    MODIFY `cart_item_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `categories`
--
ALTER TABLE `categories`
    MODIFY `category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `moroccan_banks`
--
ALTER TABLE `moroccan_banks`
    MODIFY `bank_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- AUTO_INCREMENT pour la table `notifications`
--
ALTER TABLE `notifications`
    MODIFY `notification_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT pour la table `order_items`
--
ALTER TABLE `order_items`
    MODIFY `order_item_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `payment_2fa_codes`
--
ALTER TABLE `payment_2fa_codes`
    MODIFY `code_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `payment_attempts`
--
ALTER TABLE `payment_attempts`
    MODIFY `attempt_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `products`
--
ALTER TABLE `products`
    MODIFY `product_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
    MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `carts`
--
ALTER TABLE `carts`
    ADD CONSTRAINT `fk_carts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `cart_items`
--
ALTER TABLE `cart_items`
    ADD CONSTRAINT `fk_cart_items_cart` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`cart_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_cart_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `notifications`
--
ALTER TABLE `notifications`
    ADD CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `orders`
--
ALTER TABLE `orders`
    ADD CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON UPDATE CASCADE;

--
-- Contraintes pour la table `order_items`
--
ALTER TABLE `order_items`
    ADD CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Contraintes pour la table `payments`
--
ALTER TABLE `payments`
    ADD CONSTRAINT `fk_payments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `payment_2fa_codes`
--
ALTER TABLE `payment_2fa_codes`
    ADD CONSTRAINT `fk_payment_2fa_codes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `payment_attempts`
--
ALTER TABLE `payment_attempts`
    ADD CONSTRAINT `fk_payment_attempts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `products`
--
ALTER TABLE `products`
    ADD CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Contraintes pour la table `sessions`
--
ALTER TABLE `sessions`
    ADD CONSTRAINT `fk_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
