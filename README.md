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

```
┌─────────────────┐         TCP (port 9090)         ┌─────────────────────┐
│                 │  ───── JSON (une ligne) ──────▶  │                     │
│   Client Java   │                                  │   Serveur Java      │
│   (Console /    │  ◀──── JSON Response ─────────   │   (Multi-threads)   │
│    Telnet)      │                                  │                     │
└─────────────────┘                                  └──────────┬──────────┘
                                                                │
                                                     ┌──────────▼──────────┐
                                                     │   MySQL Database    │
                                                     │   (chrionline)      │
                                                     └─────────────────────┘
```

### Couches Serveur

```
Request JSON
    │
    ▼
ClientHandler          ← Reçoit/envoie via Socket (1 thread par client)
    │
    ▼
RequestRouter          ← Parse JSON → dispatche vers le bon Service
    │
    ├──▶ AuthService        ✅ (T05)
    ├──▶ ProductService     ✅ (T06)
    ├──▶ CartService        ✅ (T08)
    ├──▶ OrderService       ✅ (T09)
    ├──▶ PaymentService     ✅ (T09)
    └──▶ AdminService       (à venir)
            │
            ▼
        DAO Layer          ← Accès JDBC à MySQL
    (CartDAO, OrderDAO, PaymentDAO, ProductDAO, UserDAO)
            │
            ▼
      DBConnection         ← Singleton, MySQL via JDBC
```

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

```
ChriOnline/
├── src/main/java/com/chrionline/
│   ├── client/                          # Côté client (à développer)
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── CartController.java
│   │   │   ├── OrderController.java
│   │   │   └── ProductController.java
│   │   ├── network/
│   │   │   ├── ServerConnection.java
│   │   │   └── UDPListener.java
│   │   ├── ui/
│   │   │   ├── admin/
│   │   │   │   ├── AdminDashboard.java
│   │   │   │   ├── ManageOrdersScreen.java
│   │   │   │   ├── ManageProductsScreen.java
│   │   │   │   └── ManageUsersScreen.java
│   │   │   ├── CartScreen.java
│   │   │   ├── CheckoutScreen.java
│   │   │   ├── LoginScreen.java
│   │   │   ├── ProductDetailScreen.java
│   │   │   ├── ProductListScreen.java
│   │   │   ├── ProfileScreen.java
│   │   │   └── RegisterScreen.java
│   │   └── ClientMain.java
│   │
│   ├── server/                          # Côté serveur
│   │   ├── core/
│   │   │   ├── TCPServer.java           # ServerSocket + ThreadPool
│   │   │   ├── ClientHandler.java       # 1 thread par client connecté
│   │   │   ├── RequestRouter.java       # Dispatch action → service
│   │   │   ├── ServerLogger.java        # Logger dual (console + fichier)
│   │   │   └── UDPNotifier.java         # (à développer)
│   │   ├── dao/
│   │   │   ├── DBConnection.java        # Singleton MySQL JDBC
│   │   │   ├── CartDAO.java             # ✅ CRUD panier
│   │   │   ├── OrderDAO.java            # ✅ CRUD commandes
│   │   │   ├── PaymentDAO.java          # ✅ CRUD paiements
│   │   │   ├── ProductDAO.java          # ✅ getById + updateStock
│   │   │   ├── UserDAO.java             # ✅ (à compléter — T05)
│   │   │   ├── CategoryDAO.java         # ⏳ (à développer)
│   │   │   └── NotificationDAO.java     # ⏳ (à développer)
│   │   ├── service/
│   │   │   ├── CartService.java         # ✅ 4 endpoints panier
│   │   │   ├── OrderService.java        # ✅ 3 endpoints commandes
│   │   │   ├── PaymentService.java      # ✅ 1 endpoint paiement
│   │   │   ├── AuthService.java         # ✅ (à compléter — T05)
│   │   │   ├── ProductService.java      # ✅ (à compléter — T06)
│   │   │   └── AdminService.java        # ⏳ (à développer)
│   │   └── ServerMain.java              # Point d'entrée serveur
│   │
│   └── shared/                          # Code partagé client/serveur
│       ├── dto/
│       │   ├── Request.java             # action + token + data (Map)
│       │   └── Response.java            # success + message + data
│       ├── enums/
│       │   ├── Action.java              # 21 actions définies
│       │   ├── OrderStatus.java         # EN_ATTENTE → LIVREE
│       │   ├── PaymentMode.java         # CARTE_BANCAIRE, FICTIF, VIREMENT
│       │   ├── PaymentStatus.java       # EN_ATTENTE → REMBOURSE
│       │   ├── ProtocolType.java        # TCP, UDP
│       │   └── Role.java               # CLIENT, ADMIN
│       └── models/
│           ├── User.java
│           ├── Session.java
│           ├── Product.java
│           ├── Category.java
│           ├── Cart.java
│           ├── CartItem.java
│           ├── Order.java
│           ├── OrderItem.java
│           ├── Payment.java
│           └── Notification.java
│
├── src/main/resources/db/
│   └── script.sql                       # Schéma SQL + données initiales
│
├── pom.xml
├── .gitignore
└── README.md
```

---

## ✅ Fonctionnalités Implémentées

### Infrastructure (Membre 1 — Amine Elhouari)

| ID  | Fonctionnalité                               | Statut |
|-----|----------------------------------------------|--------|
| T01 | Projet Git + structure Maven                 | ✅     |
| T02 | Schéma SQL + script init DB + seed data      | ✅     |
| T03 | Serveur TCP multi-clients (ThreadPool)       | ✅     |
| T04 | Protocole JSON (Request / Response / Action) | ✅     |

### Authentification & Produits (Membre 2 — Hafssa)

| ID  | Fonctionnalité                                  | Statut |
|-----|-------------------------------------------------|--------|
| T05 | Auth : REGISTER + LOGIN + sessions token        | ✅     |
| T06 | Endpoints : GET_PRODUCTS + GET_DETAIL + SEARCH  | ✅     |
| T07 | LOGOUT + expiration automatique des sessions    | ✅     |

### Panier, Commandes & Logger (Membre 3 — Mohamed Amine Ait Jaakike)

| ID  | Fonctionnalité                                | Statut |
|-----|-----------------------------------------------|--------|
| T08 | Endpoints panier : ADD / REMOVE / GET / CLEAR | ✅     |
| T09 | Commande + paiement simulé                    | ✅     |
| T10 | Logger serveur (actions + erreurs)             | ✅     |

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
```

**Response** (serveur → client) :

```json
{
    "success": true,
    "message": "Description",
    "data": { ... }
}
```

### Actions Disponibles (21 au total)

#### Authentification (✅ T05/T07)

| Action     | data (entrée)                                                    | Statut |
|------------|------------------------------------------------------------------|--------|
| `REGISTER` | `username, email, password, firstName, lastName, address, phone` | ✅     |
| `LOGIN`    | `username, password`                                             | ✅     |
| `LOGOUT`   | *(token dans le champ token)*                                    | ✅     |

#### Produits (✅ T06)

| Action               | data (entrée) | Statut |
|----------------------|---------------|--------|
| `GET_PRODUCTS`       | *(aucune)*    | ✅     |
| `GET_PRODUCT_DETAIL` | `productId`   | ✅     |
| `SEARCH_PRODUCTS`    | `keyword`     | ✅     |

#### Panier (✅ T08)

| Action             | data (entrée)                 | Statut |
|--------------------|-------------------------------|--------|
| `ADD_TO_CART`      | `userId, productId, quantity` | ✅     |
| `REMOVE_FROM_CART` | `userId, productId`           | ✅     |
| `GET_CART`         | `userId`                      | ✅     |
| `CLEAR_CART`       | `userId`                      | ✅     |

#### Commandes (✅ T09)

| Action              | data (entrée)                                          | Statut |
|---------------------|--------------------------------------------------------|--------|
| `PLACE_ORDER`       | `userId, shippingAddress, paymentMethod, cardLastFour` | ✅     |
| `GET_ORDER_HISTORY` | `userId`                                               | ✅     |
| `GET_ORDER_STATUS`  | `orderId`                                              | ✅     |

#### Paiement (✅ T09)

| Action            | data (entrée)                          | Statut |
|-------------------|----------------------------------------|--------|
| `PROCESS_PAYMENT` | `orderId, paymentMethod, cardLastFour` | ✅     |

#### Admin (⏳ à venir)

| Action                       | Statut |
|------------------------------|--------|
| `ADMIN_ADD_PRODUCT`          | ⏳     |
| `ADMIN_EDIT_PRODUCT`         | ⏳     |
| `ADMIN_DELETE_PRODUCT`       | ⏳     |
| `ADMIN_GET_USERS`            | ⏳     |
| `ADMIN_TOGGLE_USER`          | ⏳     |
| `ADMIN_UPDATE_ORDER_STATUS`  | ⏳     |

---

## 🗄 Base de Données

- **SGBD** : MySQL 8.x
- **Nom de la base** : `chrionline`
- **Script d'initialisation** : `src/main/resources/db/script.sql`
- **Connexion** : `jdbc:mysql://localhost:3306/chrionline`
- **Utilisateur par défaut** : `root` (sans mot de passe)

### Tables Principales

```
users ──────────┐
                │ 1:1
categories      ├──── carts ──── cart_items
    │ 1:N       │
products ───────┤ 1:N
                ├──── orders ──── order_items
                │        │
                │        └──── payments
                │
                └──── notifications
```

Le script SQL inclut des **données seed** (utilisateurs, produits, catégories, paniers pré-remplis, commandes de test) permettant de tester immédiatement les endpoints sans nécessiter d'authentification.

---

## 🚀 Installation et Lancement

### Prérequis

- **Java 23** (JDK)
- **Maven 3.8+**
- **MySQL 8.x** en cours d'exécution

### Étapes

```bash
# 1. Cloner le dépôt
git clone https://github.com/<votre-repo>/socketProjectChriOline.git
cd socketProjectChriOline

# 2. Créer la base de données et insérer les données
mysql -u root < src/main/resources/db/script.sql

# 3. Compiler le projet
mvn clean compile

# 4. Lancer le serveur
mvn exec:java -Dexec.mainClass="com.chrionline.server.ServerMain"
```

Le serveur démarre sur le **port 9090** avec un pool de **10 threads**.

### Vérification

```
[LOGGER] Log file: /chemin/absolu/server.log
[2026-03-15 10:00:00.000] [INFO] [main] Starting ChriOnline Server...
[SERVER] Started on port 9090
```

---

## 🧪 Tests

### Méthode 1 : MainTest (sans réseau)

Appel direct à `RequestRouter.route()` dans la même JVM :

```bash
mvn exec:java -Dexec.mainClass="com.chrionline.MainTest"
```

Scénarios couverts (11 tests) :
- **T08** : GET_CART, ADD_TO_CART, REMOVE_FROM_CART, CLEAR_CART
- **T09** : PLACE_ORDER, GET_ORDER_HISTORY, GET_ORDER_STATUS, PROCESS_PAYMENT

### Méthode 2 : Telnet (test réseau réel)

```bash
# Terminal 1 : lancer le serveur
mvn exec:java -Dexec.mainClass="com.chrionline.server.ServerMain"

# Terminal 2 : se connecter en telnet
telnet localhost 9090
```

Exemples de requêtes à copier-coller :

```json
{"action":"GET_CART","token":null,"data":{"userId":2}}
{"action":"ADD_TO_CART","token":null,"data":{"userId":2,"productId":5,"quantity":1}}
{"action":"GET_CART","token":null,"data":{"userId":2}}
{"action":"REMOVE_FROM_CART","token":null,"data":{"userId":2,"productId":5}}
{"action":"PLACE_ORDER","token":null,"data":{"userId":4,"shippingAddress":"5 Rue du Moulin, Bordeaux","paymentMethod":"CARTE_BANCAIRE","cardLastFour":"9999"}}
{"action":"GET_ORDER_HISTORY","token":null,"data":{"userId":4}}
{"action":"GET_ORDER_STATUS","token":null,"data":{"orderId":"ORD-0001-2026"}}
{"action":"PROCESS_PAYMENT","token":null,"data":{"orderId":"ORD-0004-2026","paymentMethod":"CARTE_BANCAIRE","cardLastFour":"1111"}}
```

### Journalisation

Toutes les actions et erreurs sont enregistrées dans `server.log` :

```
[2026-03-15 14:30:22.123] [INFO]  [pool-1-thread-1] Client connected: 127.0.0.1
[2026-03-15 14:30:22.456] [INFO]  [pool-1-thread-1] Routing action: ADD_TO_CART
[2026-03-15 14:30:22.789] [INFO]  [pool-1-thread-1] ADD_TO_CART userId=2 productId=5 qty=1
```

---

## 👥 Répartition des Tâches

| ID   | Tâche                                         | Membre                          | Charge |
|------|-----------------------------------------------|---------------------------------|--------|
| T01  | Init projet Git + structure Maven             | Amine Elhouari (Membre 1)      | 0.5j   |
| T02  | Schéma SQL + script init DB + seed data       | Amine Elhouari (Membre 1)      | 1j     |
| T03  | Serveur TCP multi-clients (ThreadPool)        | Amine Elhouari (Membre 1)      | 1.5j   |
| T04  | Protocole JSON (Request/Response/Action)      | Amine Elhouari (Membre 1)      | 1j     |
| T05  | Auth : REGISTER + LOGIN + sessions token      | Hafssa (Membre 2)              | 1.5j   |
| T06  | Endpoints produits (GET/DETAIL/SEARCH)        | Hafssa (Membre 2)              | 2j     |
| T07  | LOGOUT + expiration automatique sessions      | Hafssa (Membre 2)              | 1j     |
| T08  | Endpoints panier (ADD/REMOVE/GET/CLEAR)       | Mohamed Amine Ait Jaakike (M3) | 1.5j   |
| T09  | Commande + paiement simulé                    | Mohamed Amine Ait Jaakike (M3) | 1.5j   |
| T10  | Journalisation serveur (actions + erreurs)    | Mohamed Amine Ait Jaakike (M3) | 1j     |

---

## 📊 Avancement

```
Progression globale : ████████████████████ 100%

Membre 1 (Amine Elhouari)            : ████████████████████ 100%  [T01 T02 T03 T04]
Membre 2 (Hafssa)                    : ████████████████████   100%  [T05 T06 T07]
Membre 3 (Mohamed Amine Ait Jaakike) : ████████████████████ 100%  [T08 T09 T10]
```

### Détail

- ✅ **Fait** : Infrastructure serveur, protocole JSON, schéma DB, endpoints panier, endpoints commandes/paiement, logger serveur
- ✅ **En cours** : Authentification (T05), endpoints produits (T06), gestion sessions (T07)
- 📋 **À venir** : Interface graphique (JavaFX/Swing), interface admin, notifications UDP

---

## 📝 Licence

Projet académique — UAE ENSATé 2026 — Département Génie Informatique.

---

## 👨‍💻 Équipe

| Membre   | Nom                            | Tâches           |
|----------|--------------------------------|------------------|
| Membre 1 | **Amine Elhouari**             | T01 T02 T03 T04  |
| Membre 2 | **Hafssa**                     | T05 T06 T07      |
| Membre 3 | **Mohamed Amine Ait Jaakike**  | T08 T09 T10      |
