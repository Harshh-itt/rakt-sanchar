# Deferred Work: Authentication & Database Integration Guide

> [!WARNING]
> **Interim Security & Persistence Notice**  
> This repository currently runs using **two intentional placeholders** defined in Revision 2 of the Zero-Cost Build Plan:
> 1. **In-memory repositories** instead of a persistent database (all data resets when the backend stops).
> 2. **A demo-only stub identity provider** (`DevStubCurrentUserProvider`) instead of real authentication (any caller in the `demo` profile can assert any identity via HTTP headers).
>
> **Do NOT deploy this application publicly or enter real donor/patient personal data until both deferred items below are implemented.**

---

## 1. Deferred Item A: Real Authentication & Authorization

### What Exists Now (Placeholder)
- **`CurrentUser` record** (`com.example.blood.security.CurrentUser`): Holds `userId`, `role` (`Role`), `displayName`, and `preferredLanguage`.
- **`CurrentUserProvider` interface** (`com.example.blood.security.CurrentUserProvider`): Single abstraction for resolving the current caller (`Optional<CurrentUser> getCurrentUser()`, `CurrentUser requireCurrentUser()`).
- **`DevStubCurrentUserProvider`** (`@Profile("demo")`): Reads `X-Demo-User-Id` and `X-Demo-Role` (plus optional `X-Demo-Name`, `X-Demo-Language`) from the current HTTP request and resolves/creates the corresponding in-memory `User`. Outside the `demo` profile, `DisabledCurrentUserProvider` (`@Profile("!demo")`) rejects all protected calls with `IDENTITY_REQUIRED` (`401`).
- **`AccessPolicy` component**: Services pass `CurrentUser` to `AccessPolicy.requireRole(...)` and `AccessPolicy.requireOwner(...)`. Services never read `HttpServletRequest`, headers, or tokens directly.
- **Frontend isolation**: Only `frontend/src/api/client.js` attaches `X-Demo-User-Id` and `X-Demo-Role` headers from `IdentityContext`.

### What the Owner Will Implement Later
1. **User Credentials & Account Lifecycle**
   - Add password hashes (e.g., BCrypt/Argon2) or OAuth2/OIDC subject identifiers to credentials storage.
   - Build `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, and `/api/auth/logout` endpoints.
   - Enforce password policy and rate limiting on authentication routes.
2. **Spring Security & Token Verification**
   - Add `spring-boot-starter-security` and configure `SecurityFilterChain`.
   - Implement a production `CurrentUserProvider` (e.g., `JwtCurrentUserProvider` reading `SecurityContextHolder.getContext().getAuthentication()`) active under `!demo` / `prod` profiles.
   - Remove or restrict `DevStubCurrentUserProvider` exclusively to local automated tests.
3. **Frontend Auth Integration**
   - Replace the demo identity switcher in `frontend/src/context/IdentityContext.jsx` with real login/register state and `/login` + `/register` screens (a placeholder slot is marked in the router).
   - Update `frontend/src/api/client.js` (the single HTTP layer) to attach `Authorization: Bearer <token>` (or `credentials: 'include'` for HttpOnly cookies) and handle `401 Unauthorized` token refresh/redirect.

---

## 2. Deferred Item B: Persistent Database (MongoDB / SQL)

### What Exists Now (Placeholder)
- **Aggregate Repository Interfaces** in `com.example.blood.repository`:
  - `UserRepository`
  - `DonorProfileRepository`
  - `HospitalRepository`
  - `BloodStockRepository`
  - `BloodRequestRepository` / `BloodRequestStoreRepository`
  - `DonationRepository`
  - `NotificationRepository`
- **In-Memory Implementations** (`InMemory*Repository`):
  - Backed by thread-safe `ConcurrentHashMap` stores and atomic/synchronized operations.
  - Enforce domain uniqueness constraints in code (and tested via repository contract tests):
    - One `DonorProfile` per `userId`
    - One `BloodStock` entry per `hospitalId + bloodGroup`
    - Deduplicated `Notification` per `requestId + donorUserId + type`
  - Multi-step atomic operations (such as recording a verified donation, incrementing fulfilled units on a `BloodRequest`, and transitioning status to `FULFILLED`) use explicit locks and are marked with `// TODO(deferred-db): wrap in @Transactional`.
- **Geospatial Calculation**:
  - `GeoUtils.haversineKm(lat1, lon1, lat2, lon2)` computes distances in memory with a guaranteed city-name fallback.
- **Seed Data**:
  - `DemoDataSeeder` (`@Profile("demo")`) populates synthetic fictional hospitals, donors, blood stock, requests, donations, and notifications on startup.
- **Legacy MongoDB Files Preserved**:
  - Existing `Donor.java`, `BloodRequest.java`, `DonorRepository.java`, and `BloodRequestRepository.java` remain in place; MongoDB auto-configuration is excluded in the `demo` profile so the application starts with zero external services.

### What the Owner Will Implement Later
1. **Database Configuration & Connection**
   - Configure `SPRING_DATA_MONGODB_URI` (or chosen database driver) in production profiles.
   - Re-enable Spring Data MongoDB auto-configuration in the production profile.
2. **Persistent Repository Implementations**
   - Implement each aggregate repository interface against MongoDB (or JPA) and run the existing `*RepositoryContractTest` suite against the persistent implementations.
3. **Indexes & Unique Constraints**
   - `donor_profiles`: unique index on `userId`; compound index on `(bloodGroup, availability, city)`; `2dsphere` index on `location` (optional if migrating from `GeoUtils`).
   - `hospitals`: unique index on `registrationNumber`, index on `(status, city)`.
   - `blood_stocks`: unique compound index on `(hospitalId, bloodGroup)`.
   - `blood_requests`: compound index on `(status, urgency, bloodGroup, city)` and `(status, neededBy)` for expiry scheduling.
   - `notifications`: unique partial/compound index on `(userId, dedupKey)` for event deduplication; index on `(userId, read, createdAt)`.
4. **Transactions**
   - Enable multi-document transactions (`MongoTransactionManager`) on methods marked with `// TODO(deferred-db): wrap in @Transactional` (specifically `DonationService.verifyDonation` / request unit fulfillment).
5. **Legacy Donor Data Migration Strategy**
   - Existing legacy `donors` collection documents (`{ id, name, bloodGroup, city, phone, age }`) have no `userId` or `dateOfBirth`.
   - When migrating:
     1. Back up the `donors` and `blood_requests` collections (`mongodump`).
     2. Run a dry-run migration script that creates a placeholder `User` (`role = DONOR`, `onboarded = false`) per legacy donor, maps `name`, `bloodGroup`, `city`, and `phone` into `DonorProfile`, and flags records missing exact `dateOfBirth` to require profile completion upon first login.
     3. Verify counts and provide an idempotent rollback script using the backup tag.
