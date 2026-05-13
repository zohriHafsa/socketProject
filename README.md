# 🛒 ChriOnline - E-Commerce Socket Platform

[![Java Version](https://img.shields.io/badge/Java-23-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)]()
[![JavaFX Version](https://img.shields.io/badge/JavaFX-23-000000?style=for-the-badge&logo=java&logoColor=white)]()
[![MySQL Version](https://img.shields.io/badge/MySQL-8.3-4479A1?style=for-the-badge&logo=mysql&logoColor=white)]()
[![Maven Build](https://img.shields.io/badge/Maven-Build-C71A22?style=for-the-badge&logo=apache-maven&logoColor=white)]()

ChriOnline is a high-performance, strictly Client-Server E-Commerce application engineered with **Java 23**, **JavaFX**, and **Native TCP/UDP Sockets**. Purpose-built to bypass traditional HTTP REST frameworks, ChriOnline utilizes a custom persistent network protocol layered over sockets to deliver real-time shopping functionality combined with robust, enterpise-grade security mechanisms.

---

## ✨ Key Features

### 🔌 Custom Network Architecture
- **TCP Sockets:** Reliable, JSON-based payload transmission (via Gson) for strict state-based communication including authentication, catalog browsing, and cart operations.
- **UDP Sockets:** Lightweight, low-latency datagrams reserved exclusively for active client heartbeats and real-time push notifications (e.g., immediate order status updates).
- **Multithreaded Server:** Highly capable threaded request routing structure (`RequestRouter`) maintaining concurrent client sessions cleanly and efficiently.

### 🛡️ Built-in Security Middleware
Instead of relying solely on external firewalls, the server features an internal Application Security layer covering major vectors:
- **Rate-Limiting & Flood Protection:** Built-in SYN Flood protection and intelligent UDP Flood limits (e.g., 20 packets/10s per IP) to aggressively mitigate DoS attack patterns.
- **Anti-Replay Filter:** Defends against packet sniffing and transaction replay attacks.
- **Input Sanitization & Exploit Detection:** Guards against SQL injections and known CVE payloads (such as Log4j signatures) during JSON parsing.
- **Argon2 Password Hashing:** Industry-standard secure cryptographic hashing for all user and administrator credentials.

### 💳 E-commerce & Payments Engine
- **Extensive Catalog & Orders:** Deep category filtering, multi-item cart management, and secure order generation with dynamic JSON product snapshotting to preserve precise line-item billing history.
- **Local Bank Validation:** Payment validation algorithms linked directly to Moroccan Bank Identification Numbers (BIN) seamlessly handled via database mapping.
- **2FA Payment Flow:** Secure transaction pipeline requiring direct Email Two-Factor Authentication (via Jakarta Mail) prior to validating payments.

### 👥 Role-Based Access Control (RBAC)
- **Clients:** Standard shoppers tracking their personal orders, active carts, and account profiles.
- **Admins & Superadmin:** Specialized database structures and system flags enabling platform oversight and operational administration logic.

---

## 🏗️ Project Architecture

The codebase adheres to a modular `client`, `server`, and `shared` domain paradigm:

- **`com.chrionline.client`**: Contains the stateful JavaFX GUI application, client-side packet routers, TCP listeners, and decoupled UDP receiver threads.
- **`com.chrionline.server`**: Hosts the core multi-threaded Socket Server, DAOs mapping cleanly to MySQL over JDBC, and customized Security middlewares. Employs dedicated `ScheduledExecutorService` workers for background tasks like idle-session purging and heartbeat validation.
- **`com.chrionline.shared`**: Centralizes structural Data Transfer Objects (DTOs), network payload definitions, and Enums, functioning as the shared schema between Client and Server memory.

### 📂 Directory Tree

```text
ChriOnline/
├── src/
│   └── main/
│       ├── java/com/chrionline/
│       │   ├── client/              # JavaFX UI and Client Network logic
│       │   │   ├── controller/      # UI event handlers
│       │   │   ├── network/         # TCP/UDP client-side sockets
│       │   │   └── ui/              # JavaFX Views and Launcher
│       │   ├── server/              # Backend Core
│       │   │   ├── attack/          # Simulated attacks/tests
│       │   │   ├── core/            # Socket servers (TCPServer, UDPNotifier, etc.)
│       │   │   ├── dao/             # Database access layers
│       │   │   ├── security/        # Security Middlewares (AntiReplay, etc.)
│       │   │   ├── service/         # Business logic & Session managers
│       │   │   └── ServerMain.java  # Server Entry Point
│       │   └── shared/              # Shared Codebase
│       │       ├── dto/             # Data Transfer Objects
│       │       ├── enums/           # System enumerations
│       │       └── models/          # Business Entities (User, Order, etc.)
│       └── resources/
│           ├── db/
│           │   └── chrionline.sql   # Database Initialization script
│           └── ...                  # FXML and CSS files
├── pom.xml                          # Maven build descriptors
├── server.log                       # Server runtime logs
├── README.md                        # Documentation
└── .gitignore                       # Git ignored files configuration
```

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| **Language** | Core Java 23 |
| **Frontend GUI** | JavaFX 23 (with CSS) |
| **Database** | MySQL 8.3.0 |
| **Serialization** | Google Gson 2.10.1 |
| **Authentication Hash** | Argon2-jvm 2.11 |
| **Emails/Notifications** | Jakarta Mail 2.0.1 |
| **Build System** | Maven |

---

## 🚦 Getting Started

### Prerequisites
- **JDK 23** or higher
- **MySQL 8** Instance
- **Maven** Plugin installed via IDE or CLI

### Installation & System Setup

1. **Database Configuration:**
   - Execute the provided SQL dump located at `src/main/resources/db/chrionline.sql` within your MySQL environment to construct the database schema and seed the initial catalog.
   - Keep the default DB name `chrionline` and verify your access credentials match the environment properties in the Data Access Objects (DAOs).

2. **Build the Project:**
   ```bash
   mvn clean install
   ```

3. **Initialize the Server:**
   - Execute the main method in `com.chrionline.server.ServerMain` to launch the multi-threaded backend. 
   - A real-time console logger will confirm active port listeners (**TCP:** `9090`, **UDP:** `9091`) and daemon status.

4. **Launch the Client:**
   - Start the JavaFX graphic interface by executing the `main` application class: `com.chrionline.client.ui.Launcher`.
