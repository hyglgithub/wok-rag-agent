# Auto-Token Authentication Design

## Context

The RAG Agent bundles frontend and backend in a single JAR. `ApiKeyAuthFilter` requires `X-API-Key` header on all API requests in production, but the frontend has zero auth logic — all API calls are blocked. The security system is non-functional.

**Project type**: Personal/small team internal tool
**Security goal**: Simple access control — prevent unauthorized access
**Deployment modes**: Bundled (same JAR, same origin) + Separate (frontend on different port)

## Approach: Stateless Auto-Token

On every startup, the backend generates a random token, persists it to a file, and logs it. The frontend presents a login page where the user enters this token. The token is stored in localStorage and sent with every API request via `X-API-Key` header. No sessions, no environment variables.

## Token Lifecycle

**Generation**: `UUID.randomUUID().toString()` — 36-character random string, generated in `@PostConstruct` of `AuthTokenService`.

**Persistence**: Written to `data/auth-token.txt` (same directory as SQLite DB). Survives container restarts if volume is mounted. Overwritten on every application start.

**Logging**: Printed to console and log file at startup:
```
========================================
  API Token: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
========================================
```

**Refresh**: New token on every restart. Users must re-login. This is intentional — restart = invalidation.

## Authentication Filter

`ApiKeyAuthFilter` logic (when `security.api-key.enabled = true`):

```
Request
  │
  ├─ security.api-key.enabled = false → pass through (dev mode)
  │
  ├─ Static resources → pass through
  │   Matches: GET /, GET /index.html, GET /assets/**, GET /favicon.*
  │
  ├─ Auth endpoints → pass through
  │   Matches: /api/auth/**
  │
  ├─ Exempt paths → pass through
  │   Matches: /actuator/health, /actuator/info, /actuator/prometheus
  │            /swagger-ui/**, /v3/api-docs/**
  │
  └─ Other paths → check X-API-Key header
          │
          ├─ matches AuthTokenService.getToken() → pass through
          └─ otherwise → 401 JSON error
```

Changes from current implementation:
- Add static resource bypass (SPA must load before login)
- Add `/api/auth/**` bypass (login/status endpoints)
- Remove session logic (stateless approach)
- Remove environment variable key check (single token source)

## Auth API Endpoints

New `AuthController.java` at `/api/auth`:

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/api/auth/login` | `{"token": "xxx"}` | 200 `{"message": "OK"}` or 401 |
| GET | `/api/auth/status` | header `X-API-Key` | 200 `{"authenticated": true}` or 401 |

`POST /api/auth/login` — validates token against `AuthTokenService.getToken()`. Returns 200 if match, 401 if not. Does not create a session.

`GET /api/auth/status` — checks if the request's `X-API-Key` header matches current token. Used by frontend on startup to verify stored token is still valid.

New `LoginRequest.java` model: `{ String token }`

## AuthTokenService

New `AuthTokenService.java` in `service/` package:

- `@PostConstruct init()` — generate UUID, write to `data/auth-token.txt`, log to console
- `String getToken()` — return current token
- `boolean validate(String token)` — constant-time comparison with current token

Injected into both `AuthController` and `ApiKeyAuthFilter`.

## Frontend Changes

### New Files

| File | Purpose |
|------|---------|
| `frontend/src/api/auth.ts` | `login(token)`, `checkAuth()`, `logout()` |
| `frontend/src/pages/LoginPage.tsx` | Token input + login button |
| `frontend/src/stores/authStore.ts` | Zustand store: `token`, `isAuthenticated`, `setToken()`, `clearToken()` |

### Auth Flow

```
App starts
    │
    ▼
  checkAuth() → GET /api/auth/status (with token from localStorage)
    │
    ├─ 200 → show main app
    │
    └─ 401
         │
         ├─ localStorage has token → try login(token)
         │   ├─ 200 → show main app
         │   └─ 401 → clear token, show LoginPage
         │
         └─ no token → show LoginPage
```

### API Layer Changes

Modify `frontend/src/api/client.ts` (`apiFetch`):
- Read token from `authStore`
- Add `X-API-Key` header to all requests
- On 401 response: clear token, redirect to login page

Modify `frontend/src/api/rag.ts` (`streamRag`):
- Add `X-API-Key` header to fetch call

### LoginPage

Simple form: token input field + submit button. On submit, call `POST /api/auth/login`. On success, store token in `authStore` + localStorage, navigate to main page.

### Settings Page

- Keep existing API Key input (for separate deployment scenarios)
- Add "Logout" button that calls `authStore.clearToken()` and navigates to login page

## Config Cleanup

`ApiKeyConfig.java`:
- Remove `key` field (no longer needed, token comes from `AuthTokenService`)
- Keep `enabled` field (controls whether auth is active)

`application.yml` / `application-prod.yml`:
- Remove `security.api-key.key` entries

## Files to Modify

| File | Action |
|------|--------|
| `src/.../service/AuthTokenService.java` | **New** — token generation, persistence, validation |
| `src/.../controller/AuthController.java` | **New** — login/status endpoints |
| `src/.../model/LoginRequest.java` | **New** — login request body |
| `src/.../filter/ApiKeyAuthFilter.java` | **Modify** — static bypass + token validation |
| `src/.../config/ApiKeyConfig.java` | **Modify** — remove `key` field |
| `src/main/resources/application.yml` | **Modify** — remove `security.api-key.key` |
| `src/main/resources/application-prod.yml` | **Modify** — remove `security.api-key.key` |
| `frontend/src/api/auth.ts` | **New** — auth API functions |
| `frontend/src/api/client.ts` | **Modify** — add X-API-Key header + 401 handling |
| `frontend/src/api/rag.ts` | **Modify** — add X-API-Key to streamRag |
| `frontend/src/stores/authStore.ts` | **New** — auth state management |
| `frontend/src/pages/LoginPage.tsx` | **New** — login page |
| `frontend/src/App.tsx` | **Modify** — auth guard |

## Security Properties

| Property | Detail |
|----------|--------|
| Token exposure | Logged at startup, persisted in file. Not in HTML or static assets. |
| Frontend storage | localStorage. Cleared on 401 or explicit logout. |
| Token rotation | New token on every restart. Old tokens become invalid. |
| Constant-time comparison | `AuthTokenService.validate()` uses `MessageDigest.isEqual()` to prevent timing attacks. |
| No session | Stateless. No server-side session to hijack or expire. |
| Static resources | Fully accessible without auth. SPA loads before login prompt. |

## Verification

```bash
# Build + test
mvn clean compile && mvn test

# Manual verification
# 1. Start app (prod profile)
# 2. Open browser → should show login page
# 3. Check logs for generated token
# 4. Enter correct token → should enter main app
# 5. Refresh page → should stay authenticated (token in localStorage)
# 6. Enter wrong token → should show error
# 7. Click logout → should return to login page
# 8. Restart app → old token invalid, must login again
# 9. curl with X-API-Key header → external access works
```
