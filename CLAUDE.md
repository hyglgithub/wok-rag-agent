# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=RagPipelineTest

# Run a single test method
mvn test -Dtest=RagPipelineTest#testExecute

# Run the application
mvn spring-boot:run

# Run with dev profile (DEBUG logging)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Integration tests (`RagIntegrationTest`, `EndToEndTest`) require `SILICONFLOW_API_KEY` env var and a running Milvus instance. They use `@ActiveProfiles("dev")`.

## Required Environment

- **Java 17**
- **Maven** (wrapper not included, use system `mvn`)
- **SILICONFLOW_API_KEY** — required for all LLM/embedding/rerank API calls
- **Docker** — for Milvus infrastructure (`docker-compose.yml` starts Milvus + etcd + RustFS + Attu)

## Architecture

Agentic RAG platform built on Spring Boot 3.2.5. Answers questions by retrieving relevant document chunks from Milvus and generating answers via SiliconFlow-hosted Qwen models.

Base package: `com.wokrag.agent`. Runs on port 8080. Uses Lombok (`@Data`, `@RequiredArgsConstructor`, `@Slf4j`) throughout — no manual getters/setters/constructors. JSON serialization via Gson (not Jackson).

### Core Pipeline (`RagPipeline.execute()`)

1. **Session memory** — load conversation history (sliding window)
2. **Intent classification** — `IntentClassifier` routes to one of four paths (see below)
3. **Knowledge path**: Query rewrite → Embed → Hybrid search (dense + BM25 sparse, RRF fusion) → Rerank → Generate with function calling → Save to memory
4. **Tool path**: Skip RAG, call `functionCallService.chatWithTools()` directly
5. **Chitchat path**: Use LLM reply from classification call (no second LLM call); rule-based fast-path for short greetings
6. **Clarification path**: Use LLM reply from classification call; do NOT save to session memory

Intent classification uses a hybrid approach: rule-based chitchat keywords (short queries <=6 chars) + LLM fallback via `SiliconFlowClient`. The classifier prompt also generates reply content for chitchat/clarification intents in the same API call.

### Key Service Boundaries

- **`SiliconFlowClient`** — sole gateway to all SiliconFlow APIs (embed, chat, stream, rerank, tool calling). All external HTTP calls go through here via OkHttp.
- **`MilvusClientWrapper`** — sole gateway to Milvus. Auto-creates collection with HNSW (dense) + SPARSE_INVERTED_INDEX (BM25) indexes on startup. Provides both `search()` (dense vector) and `bm25Search()` (sparse BM25 via Milvus built-in function).
- **`SessionMemoryService`** — sessions and messages persisted in SQLite (`data/wok-rag.db`, tables: `sessions`, `messages`, `documents`). `SummaryMemoryService` implements token-threshold compression: when total tokens exceed `memory.tokenThreshold`, older messages are summarized via LLM and evicted, keeping only recent rounds.
- **`FunctionCallService`** — 2-round tool calling: send tool definitions → execute returned calls → send results back to LLM.

### Tool Calling Pattern

Tools implement `ToolHandler` interface (`getDefinition()` + `execute()`), registered via `ToolRegistry` via `@PostConstruct`. Three tools registered: `SearchKnowledgeBaseTool` (calls `RagPipeline.executeWithoutTools()` to avoid recursion), `GetUserAnnualLeaveTool` (mock HR data), `GetOrderStatusTool` (mock logistics data).

`FunctionCallService.chatWithTools()` checks `!config.isEnabled()` — when `enabled` is `false`, tool calling is skipped (falls back to plain chat). With the default `tool.enabled: true`, tool calling is active.

### Document Chunking

`ChunkService` dispatches between two chunking strategies based on MIME type:
- **Markdown** (`text/markdown`, `text/x-markdown`, `text/x-web-markdown`): `TextUtil.markdownChunk()` — tree-based hierarchical splitting by heading level (h1→h2→h3→h4). Each chunk carries all parent headings as prefix. Content under a heading is never split mid-sentence.
- **All other types**: `TextUtil.recursiveChunk()` — recursive character splitting with separators (`\n\n`, `\n`, `。`, `.`, `!`, `?`).

### Exception Model

`RagException` is the root with typed subtypes: `DocumentParseException`, `EmbeddingException`, `RetrievalException`, `GenerationException`. Global handler in `GlobalExceptionHandler`.

## API Endpoints

### Auth (`AuthController`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Validate token, returns 200 or 401 |
| GET | `/api/auth/status` | Check if `X-API-Key` header is valid |

### RAG (`RagController`, `StreamController`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/rag/query` | Synchronous RAG query (accepts `sessionId`) |
| POST | `/api/rag/stream` | SSE streaming RAG query |
| GET | `/api/rag/health` | Health check |

### Documents (`DocumentController`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/documents` | List all documents |
| POST | `/api/documents/upload` | Upload document (multipart, triggers chunking + embedding) |
| GET | `/api/documents/{docId}/download` | Download original file |
| GET | `/api/documents/{docId}/preview` | Inline preview (PDF) |
| GET | `/api/documents/{docId}/chunks` | List chunks for a document |
| POST | `/api/documents/{docId}/chunks` | Add a new chunk (text + auto-embed) |
| PUT | `/api/documents/chunks/{milvusId}` | Update chunk text (re-embeds) |
| DELETE | `/api/documents/chunks/{milvusId}` | Delete single chunk (requires `docId` query param) |
| DELETE | `/api/documents/{docId}` | Delete document + all its chunks |

### Sessions (`SessionController`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/sessions` | List all sessions |
| GET | `/api/sessions/{sessionId}/messages` | Get messages for a session |
| GET | `/api/sessions/search?q=...` | Search chat history |
| DELETE | `/api/sessions/{sessionId}` | Delete a session |

## Infrastructure

`docker-compose.yml` starts: Milvus 2.6.6 (port 19530), etcd, RustFS (S3-compatible object storage, replacing MinIO, ports 9000/9001), Attu web UI (port 8000), and the application itself (port 8080, `prod` profile). Milvus config references `MINIO_*` env vars pointing to RustFS (S3-compatible API). The `API_KEY` env var in docker-compose.yml is dead — auth token is auto-generated by `AuthTokenService` on startup.

```bash
# Full stack (Milvus + app)
docker-compose up -d

# App requires env var
export SILICONFLOW_API_KEY=your-key
docker-compose up -d
```

### Milvus Collection Schema

Auto-created on startup (`MilvusClientWrapper`): fields are `id` (Int64 PK, auto), `chunk_text` (VarChar 8192, analyzer enabled), `text_dense` (FloatVector 4096-dim, HNSW/COSINE), `text_sparse` (SparseFloatVector, SPARSE_INVERTED_INDEX/BM25 via built-in `bm25_fn` function), `doc_id` (VarChar 64), `source` (VarChar 256), `source_url` (VarChar 512).

### Hybrid Search Pipeline

`HybridSearchService` performs dual recall:
1. **Dense recall** — `milvusService.search()` with embedding vector, top-k = `rag.denseRecallTopK` (default 20)
2. **Sparse BM25 recall** — `milvusService.bm25Search()` with raw query text, top-k = `rag.sparseRecallTopK` (default 20)
3. **RRF fusion** — Reciprocal Rank Fusion with `rag.rrfK` (default 60)
4. **Rerank** — `RerankerService.rerank()` via SiliconFlow reranker API, returns top `rag.topK` (default 8) results

## Production Features

### Profiles
- **default/dev**: Auth disabled, console logging, all actuator endpoints open
- **prod**: Auto-token auth enabled (generated on startup, logged to console), JSON structured logging, restricted actuator, CORS locked down

### Monitoring
- **Actuator**: `/actuator/health` (with Milvus + SiliconFlow dependency checks), `/actuator/prometheus`, `/actuator/info`
- **Custom health indicators**: `MilvusHealthIndicator` checks collection accessibility, `SiliconFlowHealthIndicator` does a lightweight embed call
- **Structured logging**: `logback-spring.xml` — JSON file output in prod (`logs/wok-rag-agent.json`), console in dev. Log rotation: 100MB/30 days for JSON, 50MB/14 days for plain.

### Security
- **Auto-token auth**: On startup, `AuthTokenService` generates a random UUID token, persists to `data/auth-token.txt`, and logs it to console. `ApiKeyAuthFilter` validates `X-API-Key` header against this token when `security.api-key.enabled=true`. Token rotates on every restart.
- **Frontend auth flow**: `AuthGuard` in `App.tsx` checks stored token on startup via `GET /api/auth/status`. If invalid, shows `LoginPage` where user enters the token. Token stored in localStorage, sent as `X-API-Key` header on all requests. 401 responses clear the token and redirect to login.
- **Filter bypass**: Static resources (`/`, `/index.html`, `/assets/**`, `/favicon*`), auth endpoints (`/api/auth/login`, `/api/auth/status`), and actuator/swagger paths bypass auth.
- **CORS**: `SecurityConfig` — configurable allowed origins per profile. Default: `*`. Prod: `${CORS_ALLOWED_ORIGINS:http://localhost:5173}`.
- **Request logging**: `RequestLoggingFilter` — generates/propagates `X-Correlation-Id`, logs request/response timing.

### Performance
- **Retry logic**: `SiliconFlowClient.executeWithRetry()` — exponential backoff (1s, 2s, 4s) on `embed()`, `chat()`, `rerank()` API calls. Max 3 retries.

### API Documentation
- **Swagger UI**: `/swagger-ui.html`
- **OpenAPI spec**: `/v3/api-docs`
- **Security scheme**: API-Key header defined in OpenAPI spec

### Deployment
- **Dockerfile**: Multi-stage build (Maven builder → Alpine JRE). Non-root user, container-aware JVM.
- **docker-compose.yml**: App service with `depends_on: standalone (service_healthy)`, env vars for SiliconFlow API key, Milvus URI, profile.

## Frontend

React SPA under `frontend/`, built with Vite 8 + TypeScript 6 + Tailwind CSS v4.

```bash
cd frontend
npm install
npm run dev      # Vite dev server (hot reload)
npm run build    # tsc + vite build (outputs to frontend/dist/)
npm run lint     # ESLint
```

### Tech Stack

- **React 19** with react-router-dom v7 for routing
- **Zustand** for state management (stores in `src/stores/`)
- **react-markdown** + **remark-gfm** for AI answer rendering
- **shadcn/ui** components (Tailwind-based)
- **lucide-react** for icons

### Frontend Architecture

```
frontend/src/
├── api/            # HTTP client layer (fetch wrappers)
│   ├── client.ts   # Base fetch config (API_URL, headers, X-API-Key, 401 handling)
│   ├── auth.ts     # login(token), checkAuth(token)
│   ├── rag.ts      # streamRag() — POST /api/rag/stream, returns raw Response
│   └── document.ts # Document CRUD: upload, delete, chunks (get/add/update/delete)
├── components/     # Reusable UI components
│   └── chat/       # MessageBubble, CitationCard, ChatInput
├── lib/
│   ├── sseParser.ts  # Async generator: ReadableStream → SSEEvent objects
│   └── utils.ts      # Tailwind merge helper
├── stores/         # Zustand stores
│   ├── authStore.ts    # Token management, localStorage persistence, isAuthenticated
│   ├── chatStore.ts    # Messages, streaming state, sendMessage()
│   ├── sessionStore.ts # Session list CRUD
│   └── settingsStore.ts # App settings (theme, language, API URL)
├── types/index.ts  # Shared TypeScript interfaces
└── pages/          # Route pages (ChatPage, History, Knowledge, Settings, LoginPage)
```

### SSE Streaming Pattern

The chat uses POST-based SSE (not `EventSource`, which only supports GET). The flow:

1. `chatStore.sendMessage()` calls `streamRag()` which returns a raw `fetch` `Response`
2. `parseSSEStream(response.body)` is an async generator that yields `SSEEvent` objects
3. Three event types from backend: `token` (plain text), `done` (JSON RagResponse), `error` (JSON)
4. Each `token` event appends to the assistant message content via Zustand `set()`
5. `done` event overwrites with final answer + citations (reconciliation)
6. `MessageBubble` shows a blinking cursor (`animate-blink`) while `isStreaming` is true
7. `React.memo` on `MessageBubble` prevents cascade re-renders during streaming

## Config Properties

All `@ConfigurationProperties` classes under `config/` are auto-registered via `@ConfigurationPropertiesScan` on the application class. No `@Component` or `@EnableConfigurationProperties` needed on individual config classes — just `@Data` + `@ConfigurationProperties`.

- `siliconflow.*` — API key, base URL, model names
- `milvus.*` — host, port, collection name, vector dimension
- `rag.*` — chunk size/overlap, top-k, denseRecallTopK, sparseRecallTopK, rrfK
- `memory.*` — maxRounds, tokenThreshold, sessionTimeoutMinutes
- `rewrite.*` — enabled flag, model name
- `tool.*` — enabled flag, model name
- `intent.*` — enabled flag, model name, confidenceThreshold
- `security.api-key.*` — enabled flag only (token comes from `AuthTokenService`, not config)
- `cors.*` — allowed origins list
- `file.storage.*` — document storage path
