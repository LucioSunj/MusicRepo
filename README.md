# 🎶 CPT202 Music Platform Backend

## 📋 Overview

This repository contains the backend codebase for the CPT202 Music Platform. It provides the RESTful API services required by the frontend application for music sharing, discovery, user management, and administrative tasks.

The backend is built using Java and the Spring Boot framework, leveraging a layered architecture for maintainability and scalability. It handles business logic, data persistence, and integration with external services like cloud storage.

## ✨ Features

The backend exposes a comprehensive set of APIs covering:

### 🔐 User Authentication & Management (`UserController`)
- User registration with email verification
- Secure login using JWT or session-based authentication (needs verification)
- User profile retrieval and updates
- Password management (change/reset)
- User role management (admin vs. regular user)

### 🎵 Music File Management (`MusicFileController`, `FileController`)
- **Upload**: Secure upload of music files (MP3, WAV, FLAC, etc.) to Tencent Cloud COS.
- **Metadata Extraction**: Automatic extraction of audio metadata using JAudioTagger.
- **Listing & Searching**: Paginated listing of music files with filtering by name, category, tags, user, etc.
- **Details**: Retrieval of detailed information for a specific music file.
- **Management**: Deletion of music files.
- **Review System**: Endpoints for admin review (approve/reject) of uploaded music, including storing review status and messages.
- **Tagging/Categorization**: Management of music categories and tags.

### 📧 Email Service (`EmailController`)
- Sending verification emails for registration.
- Potentially used for password resets or notifications.

### 🛠️ Administration
- Endpoints within controllers likely restricted to admin roles for:
  - Music review and approval
  - User management (viewing users, changing roles)
  - System configuration overview (if applicable)

### ☁️ Cloud Storage Integration (`CosManager`)
- Abstraction layer for interacting with Tencent Cloud Object Storage (COS).
- Handles file uploads, deletions, and URL generation.

## 🛠️ Technology Stack

- **Core Framework**: Spring Boot 2.7.6
- **Language**: Java 17
- **Build Tool**: Apache Maven
- **Database**: MySQL
- **ORM**: MyBatis Plus
- **Caching**: Redis
- **File Storage**: Tencent Cloud COS (Cloud Object Storage)
- **API Documentation**: Knife4j (Swagger/OpenAPI enhancement)
- **Utilities**: Lombok, Hutool, Apache Commons Lang3
- **Audio Metadata**: JAudioTagger
- **Email**: Spring Boot Mail Starter
- **Containerization**: Docker (Dockerfile included)
- **Testing**: Spring Boot Test, JUnit 5

## 🚀 Getting Started

### Prerequisites

- JDK 17 or later
- Maven 3.6+ or use the included Maven Wrapper (`mvnw`)
- MySQL Server 8.x
- Redis Server
- Tencent Cloud COS account and credentials (Bucket Name, Secret ID, Secret Key, Region)

### Environment Setup

1.  **Clone the repository**:
    ```bash
    git clone [repository-url]
    cd MusicRepo
    ```
2.  **Configure `application.yml`**:
    - Navigate to `src/main/resources/application.yml`.
    - Update the following sections with your credentials:
      - `spring.datasource` (MySQL URL, username, password)
      - `spring.redis` (host, port, password if applicable)
      - `tencent.cos` (secret-id, secret-key, region, bucket)
      - `spring.mail` (host, port, username, password)
    - *Recommendation*: Use environment variables or Spring profiles for sensitive credentials instead of hardcoding in `application.yml` for production environments.

3.  **Database Setup**: See the [Database Setup](#database-setup) section below.

### Installation & Running

1.  **Build the project** (using Maven Wrapper):
    ```bash
    ./mvnw clean package -DskipTests
    ```
    (Use `mvnw.cmd` on Windows)

2.  **Run the application**:
    ```bash
    java -jar target/CPT202Music-0.0.1-SNAPSHOT.jar
    ```
    Alternatively, run directly from your IDE (e.g., IntelliJ IDEA, Eclipse) by running the `Cpt202MusicApplication` main class.

The application should start and be available, typically on port 8080 (check `application.yml` for `server.port`).

### Running with Docker

A `Dockerfile` is provided for containerizing the application.

1.  **Build the Docker image**:
    ```bash
    docker build -t cpt202-music-backend .
    ```

2.  **Run the Docker container** (ensure MySQL and Redis are accessible from the container, and pass configuration via environment variables or volume mounts):
    ```bash
    docker run -p 8080:8080 \
      -e SPRING_DATASOURCE_URL=jdbc:mysql://<db_host>:3306/<db_name> \
      -e SPRING_DATASOURCE_USERNAME=<db_user> \
      -e SPRING_DATASOURCE_PASSWORD=<db_pass> \
      -e SPRING_REDIS_HOST=<redis_host> \
      # Add other necessary environment variables (COS, Mail, etc.)
      cpt202-music-backend
    ```

## 📁 Project Structure

```
MusicRepo/
├── src/
│   ├── main/
│   │   ├── java/org/example/cpt202music/
│   │   │   ├── Cpt202MusicApplication.java  # Main entry point
│   │   │   ├── annotation/     # Custom annotations
│   │   │   ├── aop/            # Aspect-Oriented Programming (logging, auth checks)
│   │   │   ├── common/         # Common classes, enums
│   │   │   ├── config/         # Spring configurations (Beans, Security, CORS, etc.)
│   │   │   ├── constant/       # Application constants
│   │   │   ├── controller/     # API Controllers (REST endpoints)
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   ├── exception/      # Custom exception handling
│   │   │   ├── manager/        # External service interaction (e.g., CosManager)
│   │   │   ├── mapper/         # MyBatis Plus data mappers (DAO layer)
│   │   │   ├── model/          # Data models (Entities, Value Objects)
│   │   │   └── service/        # Business logic layer
│   │   └── resources/
│   │       ├── application.yml # Main application configuration
│   │       └── generator/      # (Potentially MyBatis code generator config)
│   └── test/                   # Unit and integration tests
├── sql/
│   └── create_table.sql    # Database schema setup script
├── target/                 # Build output (JAR file)
├── .mvn/                   # Maven Wrapper configuration
├── Dockerfile              # Docker build instructions
├── mvnw / mvnw.cmd         # Maven Wrapper scripts
├── pom.xml                 # Maven project configuration
└── README.md               # This file
```

## 📖 API Documentation

The project uses **Knife4j** to provide enhanced Swagger/OpenAPI documentation.

- Once the application is running, access the API documentation UI at:
  `http://localhost:8080/doc.html` (or your configured host/port + `/doc.html`)

This interface allows you to explore all available API endpoints, view request/response models, and test the APIs directly from your browser.

## ⚙️ Configuration

Key configuration is managed in `src/main/resources/application.yml`.

- **Server**: Port, context path.
- **Database**: Datasource URL, credentials, connection pool settings.
- **Redis**: Host, port, database index, password.
- **MyBatis Plus**: Mapper locations, type aliases, global configuration.
- **Tencent Cloud COS**: Credentials (Secret ID, Secret Key), region, bucket name.
- **Spring Mail**: SMTP server details, authentication.
- **Logging**: Log levels, output formats.

Consider using Spring Profiles (`application-{profile}.yml`) to manage different configurations for development, testing, and production environments. Environment variables are often preferred for sensitive data in production.

## 💾 Database Setup

1.  Ensure your MySQL server is running.
2.  Create a database for the application (e.g., `cpt202_music`).
3.  Connect to your MySQL server using a client (e.g., MySQL Workbench, `mysql` CLI).
4.  Execute the script `sql/create_table.sql` against the created database to set up the necessary tables.
    ```sql
    USE cpt202_music;
    SOURCE /path/to/MusicRepo/sql/create_table.sql;
    ```
    (Adjust the path as necessary)
5.  Verify that the tables have been created successfully.

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/your-feature-name`).
3.  Make your changes.
4.  Write unit tests for new functionality.
5.  Ensure all tests pass (`./mvnw test`).
6.  Commit your changes (`git commit -m 'Add some feature'`).
7.  Push to the branch (`git push origin feature/your-feature-name`).
8.  Open a Pull Request against the `main` branch.

Please adhere to the existing code style and provide clear commit messages.

## 📄 License

[Specify your license here, e.g., MIT License]

## 🙏 Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis Plus](https://baomidou.com/)
- [Knife4j](https://doc.xiaominfo.com/)
- [Tencent Cloud COS](https://cloud.tencent.com/product/cos)
- [Hutool](https://hutool.cn/)
- [Lombok](https://projectlombok.org/) 
