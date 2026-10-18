# Rakt Sanchar — Repository Audit

**Date:** 18 October 2026  
**Repository:** `https://github.com/Harshh-itt/rakt-sanchar`  
**Audit Branch:** `feature/zero-cost-modernization` (branched from `main` at `a7701a0`)

---

## 1. Repository Tree (Baseline)

Excluding `.git`, `target`, `node_modules`, and `.idea`:

```text
.
├── .gitignore
├── README.md
├── blood-app.iml
├── pom.xml
└── src
    └── main
        ├── java
        │   └── com
        │       └── example
        │           └── blood
        │               ├── Application.java
        │               ├── controller
        │               │   ├── BloodRequestController.java
        │               │   └── DonorController.java
        │               ├── model
        │               │   ├── BloodRequest.java
        │               │   └── Donor.java
        │               └── repository
        │                   ├── BloodRequestRepository.java
        │                   └── DonorRepository.java
        └── resources
            ├── Application.properties
            └── static
                ├── images
                │   └── logo.png
                └── index.html
```

---

## 2. Build, Runtime & Configuration Inspection

| Aspect | Baseline Status | Notes / Defects |
| :--- | :--- | :--- |
| **Build Tool** | Maven (`pom.xml`) | No Maven Wrapper (`mvnw` / `mvnw.cmd`) is present in the repo. |
| **Group / Artifact / Version** | `com.example:blood-app:1.0.0` | Uses `<dependencyManagement>` importing `spring-boot-dependencies:3.2.0` rather than `<parent>`. |
| **Java Version** | `<java.version>17</java.version>` (pom.xml); JDK 21.0.8 installed locally | No `maven.compiler.source` / `maven.compiler.target` or `<release>` explicitly configured; defaults to older bytecode target unless configured. |
| **Spring Boot Version** | `3.2.0` | Compatible with Java 17 and Java 21. |
| **Dependencies** | `spring-boot-starter-web`, `spring-boot-starter-data-mongodb`, `jackson-databind`, `spring-boot-starter-test` | `spring-boot-starter-validation` is absent (no Jakarta Bean Validation). |
| **Configuration File** | `src/main/resources/Application.properties` | **Defect:** Capitalised `Application.properties` instead of `application.properties`. Spring Boot ignores `Application.properties` in case-sensitive environments and packaged JARs. |
| **Configured Properties** | `server.port=9090`, `spring.data.mongodb.uri=mongodb://localhost:27017/rakt_sanchar` | Hardcoded values; not externalised with environment variable fallbacks. No `demo` profile exists yet. |
| **Git Hygiene** | `.gitignore` lists `target/`, `.idea/`, `*.iml` | **Defect:** `target/classes/**`, `.idea/**`, and `blood-app.iml` are already tracked in Git index (`git ls-files`). No `.env` ignore rule or `.env.example` exists. |

---

## 3. Backend Code & Database Touchpoints

### Existing Classes in `com.example.blood`

1. **`Application.java`**
   - Standard `@SpringBootApplication` entry point.
2. **`model/Donor.java`** *(Touches MongoDB annotations)*
   - Annotated with `@Document(collection = "donors")` and `@Id`.
   - Fields: `id` (`String`), `name` (`String`), `bloodGroup` (`String`), `city` (`String`), `phone` (`String`), `age` (`Integer`).
   - No validation annotations.
3. **`model/BloodRequest.java`** *(Touches MongoDB annotations)*
   - Annotated with `@Document(collection = "blood_requests")` and `@Id`.
   - Fields: `id` (`String`), `patientName` (`String`), `bloodGroup` (`String`), `hospital` (`String`), `city` (`String`), `urgency` (`String`).
   - No validation annotations.
4. **`repository/DonorRepository.java`** *(Touches Spring Data MongoDB)*
   - Extends `MongoRepository<Donor, String>`.
   - Derived query: `findByBloodGroupIgnoreCaseAndCityIgnoreCase(String bloodGroup, String city)`.
5. **`repository/BloodRequestRepository.java`** *(Touches Spring Data MongoDB)*
   - Extends `MongoRepository<BloodRequest, String>`.
6. **`controller/DonorController.java`**
   - `@RestController`, `@RequestMapping("/api/donors")`, `@CrossOrigin("*")`.
   - Injects `DonorRepository` directly (no service layer, no DTOs).
7. **`controller/BloodRequestController.java`**
   - `@RestController`, `@RequestMapping("/api/requests")`, `@CrossOrigin("*")`.
   - Injects `BloodRequestRepository` directly (no service layer, no DTOs).
8. **Services / DTOs / Exception Handling / Tests**
   - None exist (`src/test` is completely absent).

---

## 4. Frontend Inspection

- **Location:** `src/main/resources/static/index.html` (served as static content by Spring Boot). No `/frontend` React app exists yet.
- **Tech Stack:** Single-file vanilla HTML5 + inline CSS + inline JavaScript (`fetch`).
- **Assets:** `src/main/resources/static/images/logo.png` — **1.7 MB** (`1,778,510` bytes) PNG image displayed at `38x38` px in the header.
- **API Calls Made by `index.html`:**
  - `GET /api/donors` and `GET /api/donors?q=<search>`
  - `POST /api/donors` with `{ name, age, bloodGroup, phone, city }`
  - `GET /api/requests`
  - `POST /api/requests` with `{ patientName, bloodGroup, hospital, city, urgency }`

---

## 5. Current API Contract (Baseline)

| Method & Path | Query Params | Request Body (JSON) | Response Body (JSON) | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `GET /api/donors` | `bloodGroup` (optional), `city` (optional) | None | `List<Donor>`: `[{ "id": "...", "name": "...", "bloodGroup": "A+", "city": "...", "phone": "...", "age": 25 }]` | `200 OK` (or `500` if MongoDB is down) |
| `POST /api/donors` | None | `{ "name": "...", "bloodGroup": "A+", "city": "...", "phone": "...", "age": 25 }` | Saved `Donor` JSON object with generated `id` | `200 OK` (returns 200, not 201; `500` if MongoDB is down) |
| `GET /api/requests` | None | None | `List<BloodRequest>`: `[{ "id": "...", "patientName": "...", "bloodGroup": "A+", "hospital": "...", "city": "...", "urgency": "High" }]` | `200 OK` (or `500` if MongoDB is down) |
| `POST /api/requests` | None | `{ "patientName": "...", "bloodGroup": "A+", "hospital": "...", "city": "...", "urgency": "High" }` | Saved `BloodRequest` JSON object with generated `id` | `200 OK` (`500` if MongoDB is down) |

---

## 6. Known Defects Identified

1. **Misnamed Configuration File:** `src/main/resources/Application.properties` uses an uppercase `A`, causing Spring Boot to ignore it in packaged builds and case-sensitive filesystems.
2. **Broken Single-Parameter Donor Filtering:** `DonorController.getDonors` checks `if (bloodGroup != null && city != null)`. Passing only `bloodGroup` or only `city` silently ignores the filter and returns all donors (`findAll()`).
3. **Frontend Search Parameter Mismatch:** `static/index.html` queries `/api/donors?q=...`, which `DonorController` does not accept, so typing in the search box always returns the unfiltered list.
4. **No Input Validation or Error Handling:** Controllers accept raw domain models without `@Valid`. Missing required fields, negative ages, invalid blood groups, and invalid phone numbers are accepted. Errors produce default Spring/Tomcat stack-trace structures.
5. **Privacy Leak:** Donor phone numbers are returned unmasked to all anonymous callers on `GET /api/donors`.
6. **Wildcard CORS:** Both controllers specify `@CrossOrigin("*")`.
7. **Unbounded Queries:** `findAll()` returns the entire collection with no pagination or maximum page size.
8. **Tracked Build & IDE Artifacts:** `target/classes/**`, `.idea/**`, and `blood-app.iml` are tracked in Git.
9. **Oversized Static Asset:** `src/main/resources/static/images/logo.png` is 1.7 MB for a 38×38 px icon.
10. **Hard Dependency on Running MongoDB:** Even for local demo/testing, the app fails to serve `/api/donors` and `/api/requests` if `localhost:27017` is not running.

---

## 7. Baseline Build, Test & Endpoint Verification

### Commands Run

```bash
java -version
# openjdk version "21.0.8" 2025-07-15

"/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn" -version
# Apache Maven 3.9.16

node -v && npm -v
# v25.9.0 / 11.12.1

"/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn" test
# [INFO] No tests to run.
# [INFO] BUILD SUCCESS
```

### Baseline Endpoint Limitation
- Local MongoDB is not running on `localhost:27017` (`nc -z localhost 27017` -> connection refused), and per Revision 2 of the build plan, MongoDB setup is deferred. Consequently, calling `GET/POST /api/donors` or `GET/POST /api/requests` against the unmodified baseline fails with `MongoTimeoutException` when attempting to connect to `localhost:27017`.
- Under the `demo` profile (introduced in Phase 0/1), in-memory repository implementations will allow full local execution and testing without MongoDB.

---

## 8. Planned File Changes Summary

- **Phase 0 (Hygiene & Baseline):**
  - Rename/provide `src/main/resources/application.properties` and `src/main/resources/application-demo.properties`.
  - Update `.gitignore` and untrack `target/`, `.idea/`, `blood-app.iml` from Git index (`git rm --cached`).
  - Add `.env.example`, Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`), and compress `logo.png`.
- **Phase 1–7 (Backend Modernization in `com.example.blood`):**
  - Add `config`, `dto`, `exception`, `mapper`, `service`, `security` (stub `CurrentUser` / `CurrentUserProvider` / `AccessPolicy`), `event`, `util` (`GeoUtils`), and in-memory repository implementations behind interfaces.
  - Add comprehensive JUnit 5 + MockMvc unit and integration tests under `src/test/java/com/example/blood/`.
- **Phase 8–11 (React Frontend in `/frontend`):**
  - Create `/frontend` (Vite + React + React Router + Tailwind CSS) while keeping `src/main/resources/static/index.html` intact until verified.
  - Implement `IdentityContext` (demo switcher), English/Hindi `i18n`, onboarding, public discovery, request matching, donor dashboard, hospital dashboard, and admin verification panel.
- **Phase 12–13 (Testing, Hardening & Documentation):**
  - Add frontend Vitest tests, `.http` / Postman API collection, optional local `docker-compose.yml`, and updated `README.md` + `docs/DEFERRED_WORK.md`.
