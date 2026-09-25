# SecureRank — Secure QR-Based Cloud File Sharing Using Visual Cryptography & Lossless Data Transformation

<p align="center">
  <a href="https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System">
    <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
  </a>
  <a href="https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System">
    <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  </a>
  <a href="https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System">
    <img src="https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
  </a>
  <a href="https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System">
    <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  </a>
  <a href="https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System">
    <img src="https://img.shields.io/badge/Visual%20Cryptography-2--out--of--2-16C7C9?style=for-the-badge&logo=databricks&logoColor=white" alt="Visual Cryptography" />
  </a>
  <a href="https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System">
    <img src="https://img.shields.io/badge/Lossless%20Benchmark-6%20Algorithms-3B82F6?style=for-the-badge&logo=speedtest&logoColor=white" alt="Lossless Benchmark" />
  </a>
  <a href="https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System">
    <img src="https://img.shields.io/badge/Bootstrap-5.3-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white" alt="Bootstrap" />
  </a>
  <a href="https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System/blob/develop/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
  </a>
</p>

---

## 📌 Executive Summary & Abstract

**SecureRank** is a zero-trust, cloud-based secure file sharing framework integrating **Quick Response (QR) code technology**, **Visual Cryptography (VC)**, and **lossless data transformation algorithms** on binary bit streams alongside **AES-256 cloud encryption** and **TF-IDF multi-keyword ranked search**.

### 🔬 Research Abstract
In distributed cloud storage systems, transmitting sensitive documents and their associated secret keys creates critical privacy and access-control vulnerabilities. The SecureRank architecture addresses this challenge:
1. **Document Protection:** Sensitive files are encrypted using **AES-256-CBC/GCM** with unique document master keys.
2. **Encrypted Search:** An inverted index with **Term Frequency - Inverse Document Frequency (TF-IDF)** relevance vectors enables multi-keyword search directly over encrypted metadata without revealing query terms or file contents.
3. **Visual Cryptography (VC):** Each file's access token and authentication metadata is encoded into an ISO/IEC 18004 QR code and mathematically decomposed into a **2-out-of-2 Visual Cryptographic share scheme**. Neither individual share leaks any visual or statistical information about the secret QR code.
4. **Binary Bit Stream Transformation:** The generated VC share is flattened into a raw binary bit stream ($65,536\text{ bits}$ from $256 \times 256$ subpixels) with near-maximal Shannon entropy ($H \approx 1.0$).
5. **Lossless Evaluation:** The bit stream is systematically evaluated across six distinct lossless compression and transformation pipelines:
   - **Run Length Encoding (RLE)**
   - **Huffman Coding** (Prefix-tree frequency encoding)
   - **Lempel-Ziv-Welch (LZW)** (Dictionary-based compression)
   - **Binary-to-Integer Conversion**
   - **Base64 Encoding** (Chunked 6-bit transformation — optimal payload reduction)
   - **Combined BWT + MTF + Huffman** (Burrows-Wheeler Transform + Move-To-Front + Huffman)

All six transformation pipelines guarantee **100% bit-level lossless reconstruction fidelity**, enabling reliable share transmission and optical scanning upon superimposition.

---

## 📖 Table of Contents
1. [Key Features](#-key-features)
2. [Lossless Transformation & Entropy Evaluation](#-lossless-transformation--entropy-evaluation)
3. [System Architecture & Workflow](#-system-architecture--workflow)
4. [Technology Stack](#-technology-stack)
5. [Role-Based Access Control (RBAC)](#-role-based-access-control-rbac)
6. [API Endpoints Reference](#-api-endpoints-reference)
7. [Installation & Local Deployment](#-installation--local-deployment)
8. [Database Schema & Seed Accounts](#-database-schema--seed-accounts)
9. [UI Design & Theme System](#-ui-design--theme-system)
10. [Author & Contact](#-author--contact)

---

## ⚡ Key Features

* 🔐 **Client-Side & Cloud AES-256 Encryption:** Files are safeguarded with AES-256 encryption. Secret keys are never stored in plaintext.
* 🔍 **Ranked Multi-Keyword Search (TF-IDF):** Cloud server calculates relevance scores ($Score(Q, F_d) = \sum_{t \in Q} TF(t, d) \times IDF(t)$) over encrypted vectors without learning query terms.
* 🖼️ **2-out-of-2 Visual Cryptography Decomposition:**
  - Decomposes each pixel of the generated QR code into two $2 \times 2$ subpixel shares.
  - White pixels ($0$) $\rightarrow$ identical subpixel patterns across Share 1 and Share 2.
  - Black pixels ($1$) $\rightarrow$ complementary subpixel patterns.
  - Perfect secrecy: $I(QR; Share_1) = 0$ and $I(QR; Share_2) = 0$.
* 🧬 **Bit Stream Flattening & Lossless Benchmark:**
  - Converts $256 \times 256$ raster shares into $65,536$-character bit streams.
  - Evaluates compression ratio, space savings, compression/decompression latency, and bit-level fidelity across 6 algorithms.
* 🛡️ **Zero-Trust Access Request Workflow:**
  - Data Consumers search encrypted files and submit key access requests with explicit rationale.
  - Cloud Admin / PKG reviews requests and issues decryption keys.
* 🌓 **Responsive High-Contrast UI:**
  - Single Page Application (SPA) dashboard with WCAG AAA compliant Dark and Light modes.
  - Interactive modals for QR/VC share inspection and real-time transformation benchmarks.

---

## 📊 Lossless Transformation & Entropy Evaluation

Visual Cryptographic shares exhibit **near-maximal Shannon entropy** ($H \approx 1.0$), resembling white noise:

$$H(X) = - \sum_{i} P(x_i) \log_2 P(x_i) \approx - \left( 0.5 \log_2 0.5 + 0.5 \log_2 0.5 \right) = 1.0\text{ bit/symbol}$$

Due to the lack of repeated patterns, traditional entropy coders (RLE, Huffman, LZW) experience little to negative compression. In contrast, **Base64 Encoding** groups consecutive 6-bit tuples into single ASCII characters, achieving optimal payload reduction for network transport.

### 🧪 Empirical Benchmark Results (65,536-bit stream)

| Algorithm | Category | Input Chars | Output Chars | Compression Ratio | Space Savings | Compression Time | Decompression Time | Lossless Fidelity |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Base64 Encoding** | Binary Chunking | 65,536 | **10,924** | **16.67%** | **+83.33%** | < 1 ms | < 1 ms | ✅ 100% Match |
| **Binary-to-Integer** | Base Conversion | 65,536 | 19,728 | 30.10% | +69.90% | ~ 2 ms | ~ 3 ms | ✅ 100% Match |
| **Huffman Coding** | Statistical Prefix | 65,536 | 65,536 | 100.00% | 0.00% | ~ 4 ms | ~ 5 ms | ✅ 100% Match |
| **BWT + MTF + Huffman**| Compound Pipeline | 65,536 | 65,536 | 100.00% | 0.00% | ~ 12 ms | ~ 10 ms | ✅ 100% Match |
| **LZW Dictionary** | Dictionary Coding | 65,536 | 85,190 | 130.00% | -30.00% | ~ 6 ms | ~ 4 ms | ✅ 100% Match |
| **Run Length (RLE)** | Repetition Coding | 65,536 | 131,072 | 200.00% | -100.00% | ~ 2 ms | ~ 2 ms | ✅ 100% Match |

> **Conclusion:** Base64 Encoding yields the optimal character reduction (~83.33%) for QR payload transport over HTTP/cloud networks while preserving 100% bit-exact reconstruction fidelity.

---

## 🏗️ System Architecture & Workflow

```mermaid
flowchart TD
    subgraph DO[Data Owner]
        F[Plaintext Document] --> ENC[AES-256 Encryption]
        F --> TFIDF[TF-IDF Index Generation]
        ENC --> CIPHER[Encrypted File Ciphertext]
        ENC --> QRGEN[Generate Authentication QR Code]
        QRGEN --> VC[2-out-of-2 Visual Cryptography Engine]
        VC --> S1[Share 1 Matrix]
        VC --> S2[Share 2 Matrix]
        S1 --> FLAT[Flatten to 65,536-bit Stream]
        FLAT --> BENCH[Lossless Transformation Suite<br/>RLE | Huffman | LZW | Bin2Int | Base64 | BWT+MTF]
    end

    subgraph CLOUD[SecureRank Cloud Storage & Server]
        CIPHER --> STORAGE[(Cloud File Storage)]
        TFIDF --> IDX[(Encrypted Inverted Index)]
        S1 --> SHARE_STORE[(Stored VC Shares)]
    end

    subgraph DC[Data Consumer]
        QUERY[Search Query Keywords] --> TRAP[Generate Trapdoor Query Vector]
        TRAP --> SEARCH[Ranked Relevance Matching]
        SEARCH <--> IDX
        SEARCH --> RESULTS[Ranked Results: Relevance Scores]
        RESULTS --> REQ[Request Decryption Key + State Access Reason]
    end

    subgraph ADMIN[Cloud Admin / PKG Authority]
        REQ --> REVIEW[Review Access Reason & Identity]
        REVIEW --> APPROVE[Issue Secret Master Key]
    end

    APPROVE --> DC
    DC --> STACK[Superimpose VC Shares & Scan QR Code]
    STACK --> DECRYPT[AES-256 Decrypt Document]
```

---

## 💻 Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | **Java 17 (LTS)**, **Spring Boot 2.7.18** |
| **Security & Auth** | **Spring Security**, **Stateless JWT Tokens**, BCrypt Password Hashing |
| **Cryptographic Engines** | AES-256 (GCM/CBC), Visual Cryptography 2-out-of-2 Decomposition, ZXing QR Code Engine |
| **Transformation Algorithms** | Base64 Chunking, Huffman Coding, LZW Dictionary, Run Length Encoding (RLE), BWT, MTF |
| **Database & Persistence** | **MySQL 8.0**, Spring Data JPA, Hibernate ORM |
| **Frontend Architecture** | Single Page Application (SPA), HTML5, JavaScript (ES6+), CSS3 Design System |
| **UI Components & Icons** | **Bootstrap 5.3**, FontAwesome 6.4, Inter & Poppins Typography |
| **Build & Packaging** | **Apache Maven**, Executable Spring Boot JAR |

---

## 👥 Role-Based Access Control (RBAC)

The platform enforces strict zero-trust role segregation:

| Role | Permissions & Capabilities |
| :--- | :--- |
| **`ROLE_OWNER`** (Data Owner) | Upload documents with keyword extraction, encrypt with AES-256, view generated QR & VC shares, and run 6-algorithm lossless compression benchmarks. |
| **`ROLE_CONSUMER`** (Data Consumer) | Perform TF-IDF ranked searches across encrypted indexes, request file decryption keys with audit justification, and download/decrypt approved documents. |
| **`ROLE_ADMIN`** (System Admin / PKG) | Global metrics overview, approve pending user account registrations, audit and approve decryption key requests with access reasons. |

---

## 📡 API Endpoints Reference

### Authentication Controller (`/api/auth`)
* `POST /api/auth/register` — Register a new user account (`ROLE_OWNER` or `ROLE_CONSUMER`).
* `POST /api/auth/login` — Authenticate and receive a signed JWT bearer token.

### File Controller (`/api/files`)
* `POST /api/files/upload` — Upload and encrypt a document with auto-extracted TF-IDF keywords.
* `GET /api/files/my-files` — Fetch documents uploaded by the authenticated Data Owner.
* `GET /api/files/search?query={keywords}` — Perform ranked relevance search over encrypted index vectors.
* `POST /api/files/request-key` — Submit a decryption key access request with access rationale.
* `GET /api/files/my-requests` — View statuses, approved master keys, and download actions.
* `GET /api/files/download/{id}` — Securely download and decrypt the authorized document.

### Visual Cryptography & Lossless Benchmark Controllers
* `GET /api/vc/shares/{fileId}` — Retrieve QR code, Share 1, Share 2, superimposed image, and sample bit stream.
* `GET /api/lossless/benchmark/{fileId}` — Execute real-time benchmark across all 6 lossless algorithms.

### Admin Controller (`/api/admin`)
* `GET /api/admin/metrics` — Dashboard counts: total users, files, pending registrations, pending keys.
* `GET /api/admin/pending-users` — List pending user registrations awaiting approval.
* `POST /api/admin/approve-user/{id}` — Approve user registration.
* `GET /api/admin/pending-keys` — List pending decryption key requests with access reasons.
* `POST /api/admin/approve-key/{requestId}` — Authorize and issue master decryption key.

---

## 🚀 Installation & Local Deployment

### Prerequisites
* **Java Development Kit (JDK):** Version 17+ installed (`java -version`).
* **Apache Maven:** Version 3.8+ installed (`mvn -version`).
* **MySQL Server:** Version 8.0+ running on port 3306.

### 1. Clone the Repository
```bash
git clone https://github.com/shyamsunderreddypolu/Secure-Ranked-Multi-Keyword-Search-System.git
cd Secure-Ranked-Multi-Keyword-Search-System
```

### 2. Configure Database Connection
Open `src/main/resources/application.properties` and verify your MySQL credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/securerank_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
spring.jpa.hibernate.ddl-auto=update
```

### 3. Build & Package Executable JAR
```bash
# Set Java 17 environment (if multiple JDKs installed)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

mvn clean compile package -DskipTests
```

### 4. Run the Application
```bash
java -jar target/SecureRank-0.0.1-SNAPSHOT.jar
```
The embedded Tomcat server starts automatically on port **8080**.

### 5. Access the Platform
Navigate to:
```
http://localhost:8080/
```

---

## 🔑 Database Schema & Seed Accounts

When the application boots, `DataInitializer` seeds default roles and demo accounts:

| Role | Email | Password | Access Scope |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin@securerank.com` | `admin123` | System metrics, user approvals, key issuance |
| **Data Owner** | `owner@securerank.com` | `owner123` | File encryption, QR/VC shares, benchmark modal |
| **Data Consumer** | `consumer@securerank.com` | `consumer123` | Encrypted ranked search, key requests, file downloads |

---

## 🎨 UI Design & Theme System

* **Glassmorphism & Color Palette:**
  - Primary Cloud Navy: `#081B33` / Elevated Cards: `#0C1E38`
  - Accent Cyan/Teal: `#16C7C9`
  - Secondary Electric Blue: `#3B82F6`
  - Emerald Success: `#22C55E` / Amber Warning: `#F59E0B` / Ruby Danger: `#EF4444`
* **WCAG AAA Contrast Compliance:**
  - High-contrast slate typography (`#F8FAFC`, `#CBD5E1`, `#94A3B8`) ensures clear readability across all cards and table cells.
* **Theme Persistence:** Toggle between Dark and Light mode via the navbar switch; preferences persist via `localStorage`.

---

## 👨‍💻 Author & Contact

**POLU SHYAM SUNDER REDDY**
* **Role:** Full Stack Java & Cloud Security Developer
* **GitHub:** [@shyamsunderreddypolu](https://github.com/shyamsunderreddypolu)
* **LinkedIn:** [Polu Shyam Sunder Reddy](https://www.linkedin.com/in/polushyamsunderreddy)
* **Email:** [polushyamsunderreddy@gmail.com](mailto:polushyamsunderreddy@gmail.com)
* **Portfolio:** [Portfolio Website](https://shyamsunderpolu.github.io)

---

## 📄 License
This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.
