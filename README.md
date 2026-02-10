# 🚀 Financial Market Data Aggregation & Calculation System

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green) ![Docker](https://img.shields.io/badge/Docker-Enabled-blue) ![Kafka](https://img.shields.io/badge/Kafka-Streaming-black) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue)

**[🇬🇧 English](#-english)** | **[🇹🇷 Türkçe](#-türkçe)**

---

## 🇬🇧 English

### 📖 Overview
This project is a comprehensive **internship project** designed to simulate a real-time financial exchange system. It demonstrates a scalable, modular, and containerized architecture that collects financial data (rates) from various simulators, streams them through Kafka, and persists them into a database.

The system is engineered to handle **dynamic loading of data collectors**, ensuring that new data sources can be added without modifying the core application code.

### 🏗 Architecture
The system consists of independent micro-modules orchestrated via Docker Compose:

1.  **Simulators**:
    *   `pf1-simulator`: Generates random financial rates via **TCP** protocol.
    *   `pf2-simulator`: Generates random financial rates via **REST API**.
2.  **Main Application (`main-app`)**:
    *   Connects to simulators using **Dynamic Class Loading** (Plugin architecture).
    *   Collects data in real-time.
    *   Publishes data to a **Kafka** topic.
    *   Uses **Redis** for configuration and caching.
3.  **Consumer (`db-consumer`)**:
    *   Listens to the Kafka topic.
    *   Persists data into **PostgreSQL**.
    *   Logs operations to **OpenSearch**.

### ✨ Key Features
*   **Modular Design**: Core logic is decoupled into a `common-lib` shared module.
*   **Dynamic Plugin System**: Collector implementations are loaded at runtime using Java Reflection based on configuration (`config.properties`).
*   **Event-Driven Architecture**: High-throughput data streaming using Apache Kafka.
*   **Containerization**: Fully Dockerized environment with `docker-compose`.
*   **Externalized Configuration**: Adaptable to any environment via Environment Variables.

### 🛠 Tech Stack
*   **Language**: Java 17
*   **Framework**: Spring Boot
*   **Streaming**: Apache Kafka, Zookeeper
*   **Caching**: Redis
*   **Database**: PostgreSQL
*   **Logging**: Log4j2 with OpenSearch integration
*   **DevOps**: Docker, Docker Compose

### 🚀 How to Run

#### Prerequisites
*   Docker & Docker Compose
*   Java 17 & Maven (for local development)

#### Steps
1.  **Clone the repository**:
    ```bash
    git clone <repo-url>
    cd exchange-project
    ```
2.  **Build and Run**:
    ```bash
    docker-compose up -d --build
    ```
3.  **Verify**:
    *   Check logs:
        ```bash
        docker logs -f main-app
        ```
    *   Access Database (PostgreSQL):
        *   **Host**: `localhost`
        *   **Port**: `5433`
        *   **User/Pass**: `postgres` / `postgres`
        *   **DB**: `exchange_db`

### 📚 Class Descriptions

#### `common-lib` Module
*   **`IRateCollector`**: Interface defining the contract for rate collectors (connect, subscribe, disconnect).
*   **`RateListener`**: Interface for callback methods when a rate update occurs.
*   **`RateFields`**: DTO (Data Transfer Object) holding rate data (Bid, Ask, Timestamp).

#### `main-app` Module
*   **`Main`**: Entry point. Sets up subscribers, Redis, and Kafka publisher. Reads dynamic configuration.
*   **`Coordinator`**: Central hub. Implements `RateListener`. Receives data from collectors and pushes it to `KafkaPublisher`.
*   **`SubscriberFactory`**: **Dynamic Loader**. Reads `config.properties` and loads collector classes (plugins) using Java Reflection (`Class.forName`).
*   **`KafkaPublisher`**: Wrapper for Kafka Producer. Sends rate data to the `rates-topic` topic.
*   **`RedisClient`**: Wrapper for Jedis. Manages connection to Redis for caching/config.
*   **`PF1RateCollector` / `PF2RateCollector`**: Implementations of `IRateCollector`. connect to respective simulators. loaded dynamically.
*   **`UniversalRateCalculator`**: Legacy logic for rate processing (if needed).

#### `db-consumer` Module
*   **`DbConsumerApplication`**: Spring Boot entry point for the consumer service.
*   **`KafkaDbConsumer`**: Listens to `rates-topic`. Parses messages and saves them to PostgreSQL via `RateRepository`.
*   **`RateEntity`**: JPA Entity representing the `tbl_rates` table.
*   **`RateRepository`**: Spring Data JPA repository for database operations.
*   **`OpenSearchAppender`**: Custom Log4j2 appender that sends logs to OpenSearch.

---

## 🇹🇷 Türkçe

### 📖 Proje Özeti
Bu proje, gerçek zamanlı bir **finansal veri borsası simülasyonu** olarak tasarlanmış kapsamlı bir **staj projesidir**. Ölçeklenebilir, modüler ve konteynerize edilmiş (Dockerized) bir mimari ile finansal verileri (kurlar) çeşitli simülatörlerden toplar, Kafka üzerinden işler ve veritabanına kaydeder.

Sistem, **dinamik sınıf yükleme (dynamic loading)** prensibiyle tasarlanmıştır; bu sayede yeni veri kaynakları eklendiğinde ana uygulama kodunda değişiklik yapılmasına gerek kalmaz.

### 🏗 Mimari
Sistem, Docker Compose ile yönetilen bağımsız modüllerden oluşur:

1.  **Simülatörler**:
    *   `pf1-simulator`: **TCP** protokolü üzerinden rastgele kur verisi üretir.
    *   `pf2-simulator`: **REST API** üzerinden rastgele kur verisi üretir.
2.  **Ana Uygulama (`main-app`)**:
    *   Simülatörlere **Dinamik Sınıf Yükleme** (Plugin mimarisi) ile bağlanır.
    *   Verileri gerçek zamanlı toplar.
    *   Verileri **Kafka** konusuna (topic) yazar.
    *   Önbellekleme ve konfigürasyon için **Redis** kullanır.
3.  **Tüketici (`db-consumer`)**:
    *   Kafka üzerindeki verileri dinler.
    *   Verileri **PostgreSQL** veritabanına kaydeder.
    *   Logları **OpenSearch** üzerine yazar.

### ✨ Temel Özellikler
*   **Modüler Tasarım**: Ortak mantık `common-lib` modülünde toplanmıştır.
*   **Dinamik Plugin Sistemi**: Veri toplayıcı sınıflar, `config.properties` üzerinden Java Reflection kullanılarak çalışma zamanında (runtime) yüklenir. **Hardcoded bağımlılık yoktur.**
*   **Olay Güdümlü Mimari (Event-Driven)**: Apache Kafka ile yüksek performanslı veri akışı.
*   **Konteynerizasyon**: Tüm servisler `Dockerfile` ile paketlenmiş ve `docker-compose` ile tek komutla çalıştırılabilir hale getirilmiştir.
*   **Dışsallaştırılmış Konfigürasyon**: Ortam değişkenleri (Environment Variables) sayesinde Docker veya lokal ortamlara tam uyum sağlar.

### 🛠 Kullanılan Teknolojiler
*   **Dil**: Java 17
*   **Framework**: Spring Boot
*   **Veri Akışı**: Apache Kafka, Zookeeper
*   **Önbellek**: Redis
*   **Veritabanı**: PostgreSQL
*   **Loglama**: Log4j2 ve OpenSearch entegrasyonu
*   **DevOps**: Docker, Docker Compose

### 🚀 Nasıl Çalıştırılır?

#### Gereksinimler
*   Docker & Docker Compose
*   Java 17 & Maven (Kodu derlemek isterseniz)

#### Adımlar
1.  **Projeyi indirin**:
    ```bash
    git clone <repo-url>
    cd exchange-project
    ```
2.  **Derleyin ve Başlatın**:
    ```bash
    docker-compose up -d --build
    ```
    *(Bu komut projeyi derler, Docker imajlarını oluşturur ve veritabanı dahil tüm sistemi başlatır.)*

3.  **Kontrol Edin**:
    *   Uygulama loglarını izlemek için:
        ```bash
        docker logs -f main-app
        ```
    *   Veritabanına Bağlanmak için (DataGrip/DBeaver):
        *   **Host**: `localhost`
        *   **Port**: `5433` *(Yerel çakışmaları önlemek için 5432 yerine 5433'e map edilmiştir)*
        *   **Kullanıcı/Şifre**: `postgres` / `postgres`
        *   **Veritabanı**: `exchange_db`

### 📚 Sınıf Açıklamaları (Class Descriptions)

#### `common-lib` Modülü
*   **`IRateCollector`**: Veri toplayıcılar için sözleşmeyi (interface) belirler (bağlan, abone ol, bağlantıyı kes).
*   **`RateListener`**: Kur güncellemesi geldiğinde tetiklenecek metodları tanımlayan arayüz.
*   **`RateFields`**: Kur verisini (Alış, Satış, Zaman Damgası) taşıyan veri transfer nesnesi (DTO).

#### `main-app` Modülü
*   **`Main`**: Giriş noktası. Aboneleri, Redis'i ve Kafka yayıncısını (publisher) başlatır. Dinamik konfigürasyonu okur.
*   **`Coordinator`**: Merkezi yönetim birimi. `RateListener`'ı uygular. Toplayıcılardan gelen veriyi alır ve `KafkaPublisher`'a iletir.
*   **`SubscriberFactory`**: **Dinamik Yükleyici**. `config.properties` dosyasını okur ve toplayıcı sınıflarını (plugin) Java Reflection (`Class.forName`) kullanarak yükler.
*   **`KafkaPublisher`**: Kafka Producer için sarmalayıcı (wrapper) sınıf. Kur verilerini `rates-topic` başlığına gönderir.
*   **`RedisClient`**: Jedis sarmalayıcısı. Önbellek/konfigürasyon için Redis bağlantısını yönetir.
*   **`PF1RateCollector` / `PF2RateCollector`**: `IRateCollector` arayüzünün uygulamalarıdır. İlgili simülatörlere bağlanırlar ve dinamik olarak yüklenirler.
*   **`UniversalRateCalculator`**: Kur işleme için eski mantık (gerekirse kullanılır).

#### `db-consumer` Modülü
*   **`DbConsumerApplication`**: Tüketici servisi için Spring Boot giriş noktası.
*   **`KafkaDbConsumer`**: `rates-topic` başlığını dinler. Mesajları ayrıştırır (parse) ve `RateRepository` aracılığıyla PostgreSQL'e kaydeder.
*   **`RateEntity`**: `tbl_rates` tablosunu temsil eden JPA varlığı (Entity).
*   **`RateRepository`**: Veritabanı işlemleri için Spring Data JPA deposu.
*   **`OpenSearchAppender`**: Logları OpenSearch'e gönderen özel Log4j2 eklentisi.

---
*Developed as part of an Computer Engineering Internship Project.*
*Bilgisayar Mühendisliği Staj Projesi kapsamında geliştirilmiştir.*
