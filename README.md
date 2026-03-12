# 🛒 ChriOnline — Application E-Commerce en Java (Sockets TCP/UDP)

> **Département GI — Mod. SSI-AT | UAE — ENSATé | 2026**
>
> Mini-Projet 1 : Application e-commerce client-serveur native basée sur les sockets Java.

---

## 📋 Table des Matières

- [Présentation](#-présentation)
- [Architecture](#-architecture)
- [Technologies](#-technologies)
- [Structure du Projet](#-structure-du-projet)
- [Fonctionnalités Implémentées](#-fonctionnalités-implémentées)
- [Protocole de Communication](#-protocole-de-communication)
- [Base de Données](#-base-de-données)
- [Installation et Lancement](#-installation-et-lancement)
- [Tests](#-tests)
- [Répartition des Tâches](#-répartition-des-tâches)
- [Avancement](#-avancement)
- [Équipe](#-équipe)

---

## 🎯 Présentation

**ChriOnline** est une application e-commerce développée en Java permettant aux utilisateurs de consulter des produits, gérer un panier et effectuer des achats en ligne. L'application repose sur une **architecture client-serveur native** utilisant les **sockets TCP** pour les opérations principales (authentification, gestion du panier, validation des commandes, paiement simulé) et potentiellement **UDP** pour les notifications.

### Objectifs Pédagogiques

- Programmation réseau (sockets TCP/UDP)
- Gestion de connexions multi-clients (threads)
- Communication client-serveur via protocole JSON
- Accès base de données via JDBC
- Architecture logicielle en couches (DAO / Service / Core)

---

## 🏗 Architecture
┌─────────────────┐ TCP (port 9090) ┌─────────────────────┐
│ │ ───── JSON (une ligne) ──────▶ │ │
│ Client Java │ │ Serveur Java │
│ (Console / │ ◀──── JSON Response ───────── │ (Multi-threads) │
│ Telnet) │ │ │
└─────────────────┘ └──────────┬──────────┘
│
┌──────────▼──────────┐
│ MySQL Database │
│ (chrionline) │
└─────────────────────┘



### Couches Serveur

Request JSON
│
▼
ClientHandler ← Reçoit/envoie via Socket (1 thread par client)
│
▼
RequestRouter ← Parse JSON → dispatche vers le bon Service
│
├──▶ AuthService (T05 — en cours)
├──▶ ProductService (T06 — en cours)
├──▶ CartService ✅ (T08)
├──▶ OrderService ✅ (T09)
├──▶ PaymentService ✅ (T09)
└──▶ AdminService (à venir)
│
▼
DAO Layer ← Accès JDBC à MySQL
(CartDAO, OrderDAO, PaymentDAO, ProductDAO, UserDAO)
│
▼
DBConnection ← Singleton, MySQL via JDBC



---

## 🛠 Technologies

| Composant           | Technologie                          |
|---------------------|--------------------------------------|
| **Langage**         | Java 23                              |
| **Build**           | Maven                                |
| **Réseau**          | `Socket` / `ServerSocket` (TCP)      |
| **Sérialisation**   | JSON via Gson 2.10.1                 |
| **Base de données** | MySQL 8.x                            |
| **Driver JDBC**     | mysql-connector-j 8.3.0              |
| **Versioning**      | Git / GitHub                         |

---

## 📁 Structure du Projet



ChriOnline/
├── src/main/java/com/chrionline/
│ ├── client/ # Côté client (à développer)
│ │ ├── controller/
│ │ │ ├── AuthController.java
│ │ │ ├── CartController.java
│ │ │ ├── OrderController.java
│ │ │ └── ProductController.java
│ │ ├── network/
│ │ │ ├── ServerConnection.java
│ │ │ └── UDPListener.java
│ │ ├── ui/
│ │ │ ├── admin/
│ │ │ │ ├── AdminDashboard.java
│ │ │ │ ├── ManageOrdersScreen.java
│ │ │ │ ├── ManageProductsScreen.java
│ │ │ │ └── ManageUsersScreen.java
│ │ │ ├── CartScreen.java
│ │ │ ├── CheckoutScreen.java
│ │ │ ├── LoginScreen.java
│ │ │ ├── ProductDetailScreen.java
│ │ │ ├── ProductListScreen.java
│ │ │ ├── ProfileScreen.java
│ │ │ └── RegisterScreen.java
│ │ └── ClientMain.java
│ │
│ ├── server/ # Côté serveur
│ │ ├── core/
│ │ │ ├── TCPServer.java # ServerSocket + ThreadPool
│ │ │ ├── ClientHandler.java # 1 thread par client connecté
│ │ │ ├── RequestRouter.java # Dispatch action → service
│ │ │ ├── ServerLogger.java # Logger dual (console + fichier)
│ │ │ └── UDPNotifier.java # (à développer)
│ │ ├── dao/
│ │ │ ├── DBConnection.java # Singleton MySQL JDBC
│ │ │ ├── CartDAO.java # ✅ CRUD panier
│ │ │ ├── OrderDAO.java # ✅ CRUD commandes
│ │ │ ├── PaymentDAO.java # ✅ CRUD paiements
│ │ │ ├── ProductDAO.java # ✅ getById + updateStock
│ │ │ ├── UserDAO.java # ⏳ (à compléter — T05)
│ │ │ ├── CategoryDAO.java # ⏳ (à développer)
│ │ │ └── NotificationDAO.java # ⏳ (à développer)
│ │ ├── service/
│ │ │ ├── CartService.java # ✅ 4 endpoints panier
│ │ │ ├── OrderService.java # ✅ 3 endpoints commandes
│ │ │ ├── PaymentService.java # ✅ 1 endpoint paiement
│ │ │ ├── AuthService.java # ⏳ (à compléter — T05)
│ │ │ ├── ProductService.java # ⏳ (à compléter — T06)
│ │ │ └── AdminService.java # ⏳ (à développer)
│ │ └── ServerMain.java # Point d'entrée serveur
│ │
│ └── shared/ # Code partagé client/serveur
│ ├── dto/
│ │ ├── Request.java # action + token + data (Map)
│ │ └── Response.java # success + message + data
│ ├── enums/
│ │ ├── Action.java # 21 actions définies
│ │ ├── OrderStatus.java # EN_ATTENTE → LIVREE
│ │ ├── PaymentMode.java # CARTE_BANCAIRE, FICTIF, VIREMENT
│ │ ├── PaymentStatus.java # EN_ATTENTE → REMBOURSE
│ │ ├── ProtocolType.java # TCP, UDP
│ │ └── Role.java # CLIENT, ADMIN
│ └── models/
│ ├── User.java
│ ├── Session.java
│ ├── Product.java
│ ├── Category.java
│ ├── Cart.java
│ ├── CartItem.java
│ ├── Order.java
│ ├── OrderItem.java
│ ├── Payment.java
│ └── Notification.java
│
├── src/main/resources/db/
│ └── script.sql # Schéma SQL + données initiales
│
├── pom.xml
├── .gitignore
└── README.md



---

## ✅ Fonctionnalités Implémentées

### Infrastructure (Membre 1 — Amine Elhouari)

| ID  | Fonctionnalité                              | Statut |
|-----|---------------------------------------------|--------|
| T01 | Projet Git + structure Maven                | ✅     |
| T02 | Schéma SQL + script init DB + seed data     | ✅     |
| T03 | Serveur TCP multi-clients (ThreadPool)      | ✅     |
| T04 | Protocole JSON (Request / Response / Action) | ✅     |

### Authentification & Produits (Membre 2 — Hafssa)

| ID  | Fonctionnalité                               | Statut |
|-----|----------------------------------------------|--------|
| T05 | Auth : REGISTER + LOGIN + sessions token     | ⏳     |
| T06 | Endpoints : GET_PRODUCTS + GET_DETAIL + SEARCH | ⏳   |
| T07 | LOGOUT + expiration automatique des sessions | ⏳     |

### Panier, Commandes & Logger (Membre 3 — Mohamed Amine Ait Jaakike)

| ID  | Fonctionnalité                               | Statut |
|-----|----------------------------------------------|--------|
| T08 | Endpoints panier : ADD / REMOVE / GET / CLEAR | ✅    |
| T09 | Commande + paiement simulé                   | ✅     |
| T10 | Logger serveur (actions + erreurs)            | ✅     |

---

## 📡 Protocole de Communication

### Format Général

Chaque échange est une **ligne JSON unique** envoyée via TCP.

**Request** (client → serveur) :
```json
{
    "action": "ACTION_NAME",
    "token": "session-token-or-null",
    "data": { "key": "value" }
}


Progression globale : ████████████████░░░░░░░░░ 70%

Membre 1 (Amine Elhouari)            : ████████████████████ 100%  [T01 T02 T03 T04]
Membre 2 (Hafssa)                     : ░░░░░░░░░░░░░░░░░░░░   0%  [T05 T06 T07]
Membre 3 (Mohamed Amine Ait Jaakike) : ████████████████████ 100%  [T08 T09 T10]
