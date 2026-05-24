# Open-Source Release Plan

Date: 2026-05-24

## Goal

Prepare wok-rag-agent for open-source release on GitHub: write README, set up VitePress documentation site, fix Dockerfile to bundle frontend, add LICENSE and contributing guide.

## Current State

- GitHub remote already configured: `git@github.com:hyglgithub/wok-rag-agent.git`
- No README.md in project root (only frontend/README.md which is the Vite template boilerplate)
- Dockerfile only packages the backend JAR, frontend dist is not included
- docker-compose.yml is functional with Milvus + etcd + RustFS + Attu + app
- No LICENSE file
- No GitHub Actions workflows
- docs/superpowers/ contains internal design specs (keep as-is)

## Design Decisions

- **Static site**: VitePress
- **Language**: Chinese primary
- **Frontend deployment**: Bundle into Spring Boot static resources (single Docker image)
- **License**: MIT
- **Repository structure**: Single repo, docs-site/ directory for VitePress

## Changes

### 1. Rewrite README.md

Root README with:
- Project intro (one paragraph)
- Feature list (6 key features)
- Screenshot placeholders (`<!-- TODO: 添加截图 -->`)
- Quick start with Docker (3 steps: clone, set env var, docker-compose up)
- Environment variables table (SILICONFLOW_API_KEY, API_KEY, MILVUS_URI)
- Tech stack summary
- Architecture overview (Mermaid diagram)
- License badge and link

### 2. Create VitePress Documentation Site

New directory `docs-site/` with:

```
docs-site/
├── .vitepress/
│   └── config.ts          # VitePress config (Chinese locale, nav, sidebar)
├── index.md               # Landing page (hero + features)
├── guide/
│   ├── quick-start.md     # Docker deploy, local dev, env vars
│   ├── deployment.md      # Production config, reverse proxy, HTTPS
│   ├── configuration.md   # All @ConfigurationProperties reference
│   └── architecture.md    # System design, RAG pipeline, tech choices
├── api/
│   └── endpoints.md       # REST API reference tables
└── package.json
```

VitePress config highlights:
- Chinese locale (`lang: 'zh-CN'`)
- Site title: "Wok RAG Agent"
- Sidebar with guide/ and api/ sections
- GitHub link in nav bar
- Search enabled

Content source: distill information from CLAUDE.md and the existing design specs into user-facing documentation.

### 3. Modify Dockerfile

Three-stage build:

```dockerfile
# Stage 1: Build frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
# Copy frontend dist into Spring Boot static resources
COPY --from=frontend-builder /build/frontend/dist/ src/main/resources/static/
RUN mvn clean package -DskipTests -B

# Stage 3: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S wokrag && adduser -S wokrag -G wokrag
RUN mkdir -p /app/logs && chown wokrag:wokrag /app/logs
COPY --from=backend-builder /build/target/*.jar app.jar
USER wokrag
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

Key change: frontend dist is copied into `src/main/resources/static/` before Maven builds the JAR, so Spring Boot serves the SPA directly.

Also need to add a Spring MVC config to forward non-API routes to `index.html` for client-side routing to work.

### 4. Add Forwarding Config for SPA Routing

Add `WebConfig.java` (or modify `SecurityConfig.java`) to forward non-API, non-actuator paths to `/index.html`. This ensures React Router routes work when accessed directly.

### 5. Add GitHub Actions Workflow

`.github/workflows/docs.yml`:
- Trigger: push to main, changes in `docs-site/**`
- Steps: checkout, setup Node, npm install, vitepress build, deploy to GitHub Pages
- Uses `actions/deploy-pages@v4`

### 6. Add LICENSE File

MIT license text with copyright holder: `hyglgithub`

### 7. Add CONTRIBUTING.md

Standard contributing guide:
- Fork → clone → branch → commit → PR
- Code style: Java 17, Lombok, frontend follows ESLint
- Issue/PR templates

### 8. Update .gitignore

Add:
```
docs-site/node_modules/
docs-site/.vitepress/dist/
docs-site/.vitepress/cache/
```

## Files to Create

| File | Action |
|------|--------|
| README.md | Rewrite |
| LICENSE | Create |
| CONTRIBUTING.md | Create |
| docs-site/ (entire directory) | Create |
| .github/workflows/docs.yml | Create |
| Dockerfile | Modify |
| .gitignore | Modify |
| src/main/resources/static/ | Add SPA forwarding config |

## Files NOT Modified

- docker-compose.yml (already functional)
- frontend/ source code (no changes needed)
- src/main/java/ business logic (no changes except SPA routing config)
- docs/superpowers/ (internal design specs, keep as-is)
- CLAUDE.md (developer reference, keep as-is)

## Verification

1. `docker-compose build` succeeds with new Dockerfile
2. `docker-compose up -d` starts all services
3. `http://localhost:8080` serves the frontend SPA
4. `http://localhost:8080/api/rag/health` returns OK
5. VitePress dev server (`cd docs-site && npm run dev`) works locally
6. GitHub Pages deployment succeeds (after push)
