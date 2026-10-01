# Skill Evidence Passport (SEP) — Backend Application Core

This directory contains the non-confidential, enterprise Spring Boot core backend service for **Skill Evidence Passport (SEP)**.

## Architectural Overview
- **Framework:** Spring Boot 3.3.3
- **Language:** Java 21 LTS
- **Build System:** Gradle 8.7
- **Security & Integrity:**
  - `CryptoEngine.java`: FIPS-compliant SHA-256 evidence hashing and HMAC digital seals.
  - `RateLimitingFilter.java`: Tiered Token Bucket rate limiting via Redis Lua scripts.
  - `InputSanitizer.java` & `PiiDataMasker.java`: XSS sanitization and student PII redaction.
- **Persistence:** Spring Data JPA with PostgreSQL / Supabase, Liquibase/Flyway schema migrations, and local H2 in-memory mode for standalone testing.
- **Documentation:** OpenAPI 3 / Swagger UI (`/swagger-ui.html`)

## Directory Structure
```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/passport/
│   │   │   ├── audit/        # Tamper-evident ledger & event auditing
│   │   │   ├── auth/         # Role-based JWT authentication & security filters
│   │   │   ├── common/       # CryptoEngine, rate limiting & sanitization
│   │   │   ├── config/       # Spring Security, Redis, MinIO & CORS configurations
│   │   │   ├── evidence/     # Proof-of-work submission & file management
│   │   │   ├── portfolio/    # Public portfolio generation
│   │   │   ├── recruiter/    # Candidate search & role readiness matching
│   │   │   ├── rubric/       # Academic criteria scoring
│   │   │   ├── taxonomy/     # Bloom's Taxonomy competencies
│   │   │   └── verification/ # Verification queue & seal minting
│   │   └── resources/
│   │       ├── db/migration/ # PostgreSQL DDL & migration scripts
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── application-supabase.yml
│   └── test/                 # Comprehensive JUnit 5 & Mockito test suites
├── gradle/wrapper/           # Gradle wrapper binaries
├── build.gradle              # Core dependency & build configuration
├── settings.gradle
├── gradlew / gradlew.bat
├── Dockerfile                # Production container deployment
└── .env.example              # Non-confidential configuration reference
```

## Running the Backend

### Local Testing Mode (Uses In-Memory H2 DB, No External Services Required)
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Production / Supabase Connected Mode
1. Copy `.env.example` to `.env` and fill in your Supabase DB credentials:
```bash
cp .env.example .env
```
2. Run the application:
```bash
./gradlew bootRun
```

### Running Test Suites
```bash
./gradlew test
```
