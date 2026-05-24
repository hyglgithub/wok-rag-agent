# Open-Source Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare wok-rag-agent for open-source release: README, VitePress docs site, Dockerfile with frontend bundled, LICENSE, CONTRIBUTING, GitHub Pages deployment.

**Architecture:** Single-repo approach. VitePress docs site lives in `docs-site/`. Dockerfile becomes 3-stage (frontend build → backend build → runtime). A Spring MVC forwarding config serves the React SPA for client-side routes.

**Tech Stack:** VitePress, Node.js 20, Maven, Spring Boot 3.2.5, GitHub Pages, GitHub Actions

---

### Task 1: Add LICENSE and update .gitignore

**Files:**
- Create: `LICENSE`
- Modify: `.gitignore`

- [ ] **Step 1: Create MIT LICENSE file**

Create `LICENSE` with MIT license text:

```
MIT License

Copyright (c) 2026 hyglgithub

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

- [ ] **Step 2: Update .gitignore**

Append to `.gitignore`:

```
### VitePress ###
docs-site/node_modules/
docs-site/.vitepress/dist/
docs-site/.vitepress/cache/
```

- [ ] **Step 3: Commit**

```bash
git add LICENSE .gitignore
git commit -m "chore: add MIT license and update gitignore for docs-site"
```

---

### Task 2: Create CONTRIBUTING.md

**Files:**
- Create: `CONTRIBUTING.md`

- [ ] **Step 1: Create CONTRIBUTING.md**

Create `CONTRIBUTING.md`:

```markdown
# 贡献指南

感谢你对 Wok RAG Agent 的关注！

## 如何贡献

1. Fork 本仓库
2. 创建你的特性分支：`git checkout -b feature/amazing-feature`
3. 提交你的改动：`git commit -m 'feat: add amazing feature'`
4. 推送到分支：`git push origin feature/amazing-feature`
5. 提交 Pull Request

## 开发环境

### 后端

- Java 17
- Maven
- Docker（用于 Milvus）

```bash
# 启动 Milvus 基础设施
docker-compose up -d standalone

# 运行应用
export SILICONFLOW_API_KEY=your-key
mvn spring-boot:run
```

### 前端

- Node.js 20+

```bash
cd frontend
npm install
npm run dev
```

## 代码规范

- 后端：使用 Lombok（`@Data`, `@RequiredArgsConstructor`, `@Slf4j`），JSON 序列化用 Gson
- 前端：遵循 ESLint 规则，使用 TypeScript
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范

## Pull Request 规范

- 确保 PR 只解决一个问题
- 提供清晰的 PR 描述，说明改动原因
- 确保代码通过 `mvn test` 和 `npm run lint`
- 更新相关文档（如有必要）
```

- [ ] **Step 2: Commit**

```bash
git add CONTRIBUTING.md
git commit -m "docs: add contributing guide"
```

---

### Task 3: Rewrite README.md

**Files:**
- Modify: `README.md` (replace frontend/README.md content is separate; this is root README)

Note: There is no root `README.md` currently. The `frontend/README.md` is the Vite template boilerplate and should be replaced with a minimal pointer.

- [ ] **Step 1: Create root README.md**

Create `README.md` at project root:

```markdown
# Wok RAG Agent

一个基于 Java 的 Agentic RAG 智能问答平台，支持文档上传、混合检索、多轮对话和工具调用。

> 用 Java 做 RAG，而不是 Python——因为大多数要落地 AI 应用的公司，技术栈是 Java。

<!-- TODO: 添加项目截图 -->

## 功能特性

- **混合检索**：BM25 关键词检索 + 向量语义检索，RRF 融合 + Reranker 精排
- **多轮对话记忆**：基于 Token 阈值的历史摘要 + 最近 N 轮保留策略
- **意图识别**：规则 + LLM 混合意图分类，支持知识检索、工具调用、闲聊、澄清四路路由
- **问题重写**：指代消解、上下文补全、口语化转标准句式
- **工具调用**：Function Calling 支持自定义工具扩展
- **Markdown 感知分块**：按标题层级树形分割，保留上下文前缀
- **文档管理**：支持 PDF、Word 等格式上传，分块可视化编辑
- **SSE 流式输出**：打字机效果的实时回答

## 快速开始

### 环境要求

- Docker & Docker Compose
- [SiliconFlow API Key](https://cloud.siliconflow.cn/)

### 一键部署

```bash
# 1. 克隆项目
git clone https://github.com/hyglgithub/wok-rag-agent.git
cd wok-rag-agent

# 2. 设置环境变量
export SILICONFLOW_API_KEY=your-siliconflow-api-key

# 3. 启动所有服务
docker-compose up -d
```

启动完成后访问 http://localhost:8080 即可使用。

### 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `SILICONFLOW_API_KEY` | 是 | - | SiliconFlow API 密钥 |
| `API_KEY` | 否 | `wokrag-default-key-change-me` | 生产环境 API 认证密钥 |
| `MILVUS_URI` | 否 | `http://localhost:19530` | Milvus 连接地址 |

### 管理界面

- **Attu (Milvus 管理)**: http://localhost:8000
- **Swagger API 文档**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/actuator/health

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.2.5, OkHttp, Gson, Lombok |
| 前端 | React 19, TypeScript, Vite 8, Tailwind CSS v4, Zustand |
| 向量数据库 | Milvus 2.6.6 (HNSW + BM25) |
| 存储 | SQLite (会话), RustFS/S3 (文件) |
| AI 服务 | SiliconFlow (Qwen 系列模型) |
| 部署 | Docker Compose |

## 架构

```
用户 → React SPA → Spring Boot API
                        ├── 意图识别 → 路由调度
                        ├── 知识路径: 问题重写 → Embedding → 混合检索 → Rerank → LLM 生成
                        ├── 工具路径: Function Calling → 工具执行 → LLM 生成
                        ├── 闲聊路径: 直接回复
                        └── 澄清路径: 引导式回复
```

## 文档

详细文档请访问 [Wok RAG Agent 文档站](https://hyglgithub.github.io/wok-rag-agent/)。

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。

## 贡献

欢迎贡献！请查看 [贡献指南](CONTRIBUTING.md)。
```

- [ ] **Step 2: Replace frontend/README.md with minimal content**

Replace `frontend/README.md` content with:

```markdown
# Frontend

Wok RAG Agent 的前端模块，基于 React 19 + TypeScript + Vite。

## 开发

```bash
npm install
npm run dev
```

详见 [项目根目录 README](../README.md)。
```

- [ ] **Step 3: Commit**

```bash
git add README.md frontend/README.md
git commit -m "docs: add project README with quick start guide"
```

---

### Task 4: Create VitePress documentation site

**Files:**
- Create: `docs-site/package.json`
- Create: `docs-site/.vitepress/config.ts`
- Create: `docs-site/index.md`
- Create: `docs-site/guide/quick-start.md`
- Create: `docs-site/guide/deployment.md`
- Create: `docs-site/guide/configuration.md`
- Create: `docs-site/guide/architecture.md`
- Create: `docs-site/api/endpoints.md`

- [ ] **Step 1: Create docs-site/package.json**

```json
{
  "name": "wok-rag-agent-docs",
  "private": true,
  "scripts": {
    "dev": "vitepress dev",
    "build": "vitepress build",
    "preview": "vitepress preview"
  },
  "devDependencies": {
    "vitepress": "^1.6.3"
  }
}
```

- [ ] **Step 2: Create docs-site/.vitepress/config.ts**

```typescript
import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'Wok RAG Agent',
  description: '基于 Java 的 Agentic RAG 智能问答平台',
  base: '/wok-rag-agent/',
  themeConfig: {
    nav: [
      { text: '指南', link: '/guide/quick-start' },
      { text: 'API', link: '/api/endpoints' },
      { text: 'GitHub', link: 'https://github.com/hyglgithub/wok-rag-agent' }
    ],
    sidebar: {
      '/guide/': [
        {
          text: '指南',
          items: [
            { text: '快速开始', link: '/guide/quick-start' },
            { text: '部署指南', link: '/guide/deployment' },
            { text: '配置说明', link: '/guide/configuration' },
            { text: '架构说明', link: '/guide/architecture' }
          ]
        }
      ],
      '/api/': [
        {
          text: 'API 参考',
          items: [
            { text: '接口文档', link: '/api/endpoints' }
          ]
        }
      ]
    },
    outline: {
      level: [2, 3],
      label: '页面导航'
    },
    search: {
      provider: 'local',
      options: {
        translations: {
          button: { buttonText: '搜索文档' },
          modal: {
            noResultsText: '没有找到相关结果',
            footer: { selectText: '选择', navigateText: '切换' }
          }
        }
      }
    },
    editLink: {
      pattern: 'https://github.com/hyglgithub/wok-rag-agent/edit/main/docs-site/:path',
      text: '在 GitHub 上编辑此页面'
    },
    lastUpdated: {
      text: '最后更新'
    }
  }
})
```

- [ ] **Step 3: Create docs-site/index.md (landing page)**

```markdown
---
layout: home
hero:
  name: Wok RAG Agent
  text: Agentic RAG 智能问答平台
  tagline: 基于 Java 的企业级 RAG 解决方案，支持混合检索、多轮对话、工具调用
  actions:
    - theme: brand
      text: 快速开始
      link: /guide/quick-start
    - theme: alt
      text: GitHub
      link: https://github.com/hyglgithub/wok-rag-agent

features:
  - icon: 🔍
    title: 混合检索
    details: BM25 关键词检索 + 向量语义检索，RRF 融合 + Reranker 精排，精准召回
  - icon: 💬
    title: 多轮对话记忆
    details: 基于 Token 阈值的历史摘要 + 最近 N 轮保留策略，长对话不丢上下文
  - icon: 🎯
    title: 意图识别路由
    details: 规则 + LLM 混合分类，知识检索、工具调用、闲聊、澄清四路智能路由
  - icon: 🔧
    title: 工具调用
    details: Function Calling 支持自定义工具扩展，轻松集成外部服务
  - icon: 📄
    title: Markdown 感知分块
    details: 按标题层级树形分割，保留上下文前缀，结构化文档检索更精准
  - icon: ⚡
    title: SSE 流式输出
    details: 打字机效果的实时回答，支持中断生成，用户体验流畅
```

- [ ] **Step 4: Create docs-site/guide/quick-start.md**

```markdown
# 快速开始

## 环境要求

- Docker & Docker Compose
- [SiliconFlow API Key](https://cloud.siliconflow.cn/)（用于 LLM 和 Embedding 服务）

## Docker 一键部署

```bash
# 1. 克隆项目
git clone https://github.com/hyglgithub/wok-rag-agent.git
cd wok-rag-agent

# 2. 设置环境变量
export SILICONFLOW_API_KEY=your-siliconflow-api-key

# 3. 启动所有服务
docker-compose up -d
```

启动完成后访问 http://localhost:8080 即可使用。

::: tip
首次启动需要下载 Docker 镜像，可能需要几分钟时间。Milvus 健康检查需要约 90 秒启动。
:::

## 服务组件

`docker-compose up -d` 会启动以下服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| wok-rag-agent | 8080 | 主应用（前端 + 后端） |
| Milvus | 19530 | 向量数据库 |
| Attu | 8000 | Milvus 管理界面 |
| RustFS | 9000/9001 | S3 兼容对象存储 |
| etcd | 2379 | Milvus 元数据存储 |

## 本地开发

### 后端

```bash
# 启动 Milvus 基础设施
docker-compose up -d standalone etcd rustfs

# 设置环境变量
export SILICONFLOW_API_KEY=your-key

# 运行应用
mvn spring-boot:run

# 或使用 dev profile（DEBUG 日志）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器运行在 http://localhost:5173，API 请求会自动代理到 http://localhost:8080。

## 获取 SiliconFlow API Key

1. 访问 [SiliconFlow 控制台](https://cloud.siliconflow.cn/)
2. 注册并登录
3. 在 API Keys 页面创建新的密钥
4. 复制密钥并设置为环境变量

## 下一步

- [配置说明](/guide/configuration) - 了解所有可配置参数
- [架构说明](/guide/architecture) - 了解系统设计
- [API 文档](/api/endpoints) - 查看接口详情
```

- [ ] **Step 5: Create docs-site/guide/deployment.md**

```markdown
# 部署指南

## Docker Compose 部署（推荐）

项目根目录的 `docker-compose.yml` 包含完整的服务编排，适合单机部署。

### 基本部署

```bash
git clone https://github.com/hyglgithub/wok-rag-agent.git
cd wok-rag-agent
export SILICONFLOW_API_KEY=your-key
docker-compose up -d
```

### 生产环境配置

生产环境需要修改以下配置：

```bash
# 设置安全的 API Key
export API_KEY=your-secure-api-key

# 启动服务（自动使用 prod profile）
docker-compose up -d
```

生产环境下：
- API 认证默认开启（`X-API-Key` 请求头）
- CORS 限制为配置的域名
- 日志格式为 JSON 结构化输出

### 自定义端口

修改 `docker-compose.yml` 中的端口映射：

```yaml
services:
  app:
    ports:
      - "自定义端口:8080"
  attu:
    ports:
      - "自定义端口:3000"
```

## 反向代理配置

### Nginx

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # SSE 流式响应支持
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }
}
```

### HTTPS（Let's Encrypt）

```bash
# 安装 certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com
```

## 数据备份

重要数据存储在 Docker volume 中：

```bash
# 备份 Milvus 数据
docker run --rm -v milvus-stack_milvus-data:/data -v $(pwd):/backup alpine tar czf /backup/milvus-data.tar.gz /data

# 备份 SQLite 数据库
docker cp wok-rag-agent:/app/data/wok-rag.db ./wok-rag.db

# 备份上传的文档
docker cp wok-rag-agent:/app/data/documents/ ./documents/
```

## 更新版本

```bash
git pull
docker-compose build
docker-compose up -d
```
```

- [ ] **Step 6: Create docs-site/guide/configuration.md**

```markdown
# 配置说明

所有配置项均可通过环境变量覆盖。环境变量名采用大写下划线格式，例如 `siliconflow.api-key` 对应 `SILICONFLOW_API_KEY`。

## SiliconFlow 配置

| 属性 | 环境变量 | 默认值 | 说明 |
|------|----------|--------|------|
| `siliconflow.api-key` | `SILICONFLOW_API_KEY` | - | API 密钥（必填） |
| `siliconflow.base-url` | - | `https://api.siliconflow.cn/v1` | API 基础 URL |
| `siliconflow.embedding-model` | - | `Qwen/Qwen3-Embedding-8B` | Embedding 模型 |
| `siliconflow.chat-model` | - | `Qwen/Qwen3-32B` | 对话模型 |
| `siliconflow.reranker-model` | - | `BAAI/bge-reranker-v2-m3` | Reranker 模型 |

## Milvus 配置

| 属性 | 环境变量 | 默认值 | 说明 |
|------|----------|--------|------|
| `milvus.uri` | `MILVUS_URI` | `http://localhost:19530` | Milvus 连接地址 |
| `milvus.collection-name` | - | `customer_service_chunks` | 集合名称 |
| `milvus.vector-dim` | - | `4096` | 向量维度 |

## RAG 配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `rag.chunk-size` | `500` | 分块大小（字符数） |
| `rag.chunk-overlap` | `50` | 分块重叠字符数 |
| `rag.top-k` | `8` | 最终返回的 Top-K 结果数 |
| `rag.dense-recall-top-k` | `20` | Dense 检索召回数 |
| `rag.sparse-recall-top-k` | `20` | Sparse BM25 检索召回数 |
| `rag.rrf-k` | `60` | RRF 融合参数 K |

## 会话记忆配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `memory.max-rounds` | `5` | 保留最近 N 轮对话 |
| `memory.token-threshold` | `3000` | 触发摘要压缩的 Token 阈值 |
| `memory.session-timeout-minutes` | `30` | 会话超时时间（分钟） |
| `memory.summary-model` | `Qwen/Qwen2.5-7B-Instruct` | 摘要模型 |

## 其他配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `rewrite.enabled` | `true` | 是否启用问题重写 |
| `tool.enabled` | `true` | 是否启用工具调用 |
| `intent.enabled` | `true` | 是否启用意图识别 |
| `intent.confidence-threshold` | `0.5` | 意图分类置信度阈值 |
| `security.api-key.enabled` | `false` | 是否启用 API Key 认证 |
| `security.api-key.key` | - | API Key 值 |
| `rate-limit.enabled` | `false` | 是否启用限流 |
| `rate-limit.requests-per-minute` | `120` | 每分钟请求限制 |
| `cors.allowed-origins` | `*` | CORS 允许的来源 |
```

- [ ] **Step 7: Create docs-site/guide/architecture.md**

```markdown
# 架构说明

## 系统架构

```
┌─────────────────────────────────────────────────────┐
│                    用户浏览器                         │
│              React SPA (Vite + Tailwind)             │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP / SSE
                       ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot Application                 │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │Controller│  │Controller│  │  Controller       │  │
│  │  (RAG)   │  │  (Doc)   │  │  (Session)        │  │
│  └────┬─────┘  └────┬─────┘  └────────┬─────────┘  │
│       │              │                 │            │
│       ▼              ▼                 ▼            │
│  ┌─────────────────────────────────────────────┐   │
│  │            Service Layer                     │   │
│  │  RagPipeline / ChunkService / SessionMemory  │   │
│  └────────┬──────────────┬─────────────────────┘   │
│           │              │                          │
│           ▼              ▼                          │
│  ┌──────────────┐ ┌──────────────┐                 │
│  │SiliconFlow   │ │Milvus Client │                 │
│  │Client        │ │Wrapper       │                 │
│  └──────┬───────┘ └──────┬───────┘                 │
└─────────┼────────────────┼─────────────────────────┘
          │                │
          ▼                ▼
   ┌──────────────┐ ┌──────────────┐
   │ SiliconFlow  │ │   Milvus     │
   │ (LLM API)    │ │ (Vector DB)  │
   └──────────────┘ └──────────────┘
```

## RAG Pipeline 流程

### 知识检索路径

1. **会话记忆加载** — 从 SQLite 加载对话历史，滑动窗口保留最近 N 轮
2. **意图分类** — 规则匹配 + LLM 分类，路由到对应处理路径
3. **问题重写** — 结合对话历史做指代消解、上下文补全
4. **向量化** — 调用 SiliconFlow Embedding API 生成查询向量
5. **混合检索** — Dense 向量检索 + BM25 稀疏检索，RRF 融合
6. **重排序** — Reranker 模型精排，返回 Top-K 结果
7. **答案生成** — 组装 Prompt（上下文 + 历史 + 问题），调用 LLM 生成
8. **保存记忆** — 将问答对存入会话记忆

### 意图路由

| 意图 | 处理方式 |
|------|----------|
| 知识检索 | 完整 RAG Pipeline |
| 工具调用 | 跳过 RAG，直接 Function Calling |
| 闲聊 | 使用分类时的 LLM 回复，不二次调用 |
| 澄清 | 使用分类时的 LLM 回复，不保存记忆 |

## 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 后端框架 | Spring Boot 3.2.5 | Java 生态成熟，企业级首选 |
| 向量数据库 | Milvus 2.6.6 | 支持 HNSW + BM25，混合检索原生支持 |
| HTTP 客户端 | OkHttp | 轻量、稳定、支持流式响应 |
| JSON | Gson | 简单直接，与 Spring Boot 无冲突 |
| 前端框架 | React 19 | 生态丰富，组件化开发 |
| 状态管理 | Zustand | 轻量、简洁、TypeScript 友好 |
| 会话存储 | SQLite | 零配置、单文件、适合单机部署 |
| 文件存储 | RustFS | S3 兼容、轻量、Docker 友好 |
```

- [ ] **Step 8: Create docs-site/api/endpoints.md**

```markdown
# API 接口文档

## RAG 接口

### 同步查询

```
POST /api/rag/query
Content-Type: application/json

{
  "query": "什么是 RAG？",
  "sessionId": "optional-session-id"
}
```

**响应：** 返回 `RagResponse` 对象，包含 `answer`（生成的答案）和 `citations`（引用的文档片段）。

### 流式查询

```
POST /api/rag/stream
Content-Type: application/json

{
  "query": "什么是 RAG？",
  "sessionId": "optional-session-id"
}
```

**响应：** SSE 流，事件类型：
- `token` — 文本片段
- `done` — 完整响应 JSON
- `error` — 错误信息 JSON

### 健康检查

```
GET /api/rag/health
```

## 文档接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/documents` | 获取文档列表 |
| `POST` | `/api/documents/upload` | 上传文档（multipart） |
| `GET` | `/api/documents/{docId}/download` | 下载原始文件 |
| `GET` | `/api/documents/{docId}/preview` | PDF 预览 |
| `GET` | `/api/documents/{docId}/chunks` | 获取文档分块 |
| `POST` | `/api/documents/{docId}/chunks` | 添加新分块 |
| `PUT` | `/api/documents/chunks/{milvusId}` | 更新分块 |
| `DELETE` | `/api/documents/chunks/{milvusId}` | 删除分块 |
| `DELETE` | `/api/documents/{docId}` | 删除文档及所有分块 |

## 会话接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/sessions` | 获取会话列表 |
| `GET` | `/api/sessions/{sessionId}/messages` | 获取会话消息 |
| `GET` | `/api/sessions/search?q=...` | 搜索聊天记录 |
| `DELETE` | `/api/sessions/{sessionId}` | 删除会话 |

## 系统接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/actuator/health` | 健康检查（含依赖检查） |
| `GET` | `/actuator/prometheus` | Prometheus 指标 |
| `GET` | `/swagger-ui.html` | Swagger UI |
| `GET` | `/v3/api-docs` | OpenAPI 规范 |

## 认证

生产环境（`prod` profile）下，所有 `/api/**` 请求需要 `X-API-Key` 请求头：

```
X-API-Key: your-api-key
```

健康检查和 Actuator 端点不需要认证。
```

- [ ] **Step 9: Install dependencies and verify VitePress builds**

```bash
cd docs-site
npm install
npm run build
```

Expected: Build succeeds, output in `docs-site/.vitepress/dist/`.

- [ ] **Step 10: Commit**

```bash
git add docs-site/
git commit -m "docs: add VitePress documentation site"
```

---

### Task 5: Add SPA forwarding config for React Router

**Files:**
- Create: `src/main/java/com/wokrag/agent/config/WebConfig.java`

- [ ] **Step 1: Create WebConfig.java**

```java
package com.wokrag.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
```

This forwards all non-API, non-matching routes to `index.html`, enabling React Router's client-side routing.

- [ ] **Step 2: Verify backend compiles**

```bash
mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wokrag/agent/config/WebConfig.java
git commit -m "feat: add SPA forwarding for React Router client-side routing"
```

---

### Task 6: Modify Dockerfile to bundle frontend

**Files:**
- Modify: `Dockerfile`

- [ ] **Step 1: Replace Dockerfile content**

Replace the entire `Dockerfile` with:

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
RUN mkdir -p /app/logs /app/data/documents && chown -R wokrag:wokrag /app

COPY --from=backend-builder /build/target/*.jar app.jar

USER wokrag

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

- [ ] **Step 2: Add .dockerignore entries if missing**

Ensure `.dockerignore` contains:

```
.git
.idea
*.iml
frontend/node_modules
frontend/dist
target/
data/
logs/
```

- [ ] **Step 3: Commit**

```bash
git add Dockerfile .dockerignore
git commit -m "feat: bundle frontend into Docker image with 3-stage build"
```

---

### Task 7: Add GitHub Actions workflow for docs deployment

**Files:**
- Create: `.github/workflows/docs.yml`

- [ ] **Step 1: Create the workflow file**

```yaml
name: Deploy Docs to GitHub Pages

on:
  push:
    branches: [main]
    paths:
      - 'docs-site/**'
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: false

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: docs-site/package-lock.json

      - name: Install dependencies
        working-directory: docs-site
        run: npm ci

      - name: Build VitePress
        working-directory: docs-site
        run: npm run build

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: docs-site/.vitepress/dist

  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/docs.yml
git commit -m "ci: add GitHub Actions workflow for docs deployment"
```

---

### Task 8: Final verification

- [ ] **Step 1: Verify VitePress dev server works locally**

```bash
cd docs-site
npm run dev
```

Expected: Dev server starts at http://localhost:5173, all pages load correctly.

- [ ] **Step 2: Verify Docker build**

```bash
docker-compose build
```

Expected: Build succeeds with all 3 stages.

- [ ] **Step 3: Verify full stack runs**

```bash
export SILICONFLOW_API_KEY=test-key
docker-compose up -d
# Wait for services to be healthy
docker-compose ps
```

Expected: All services running, http://localhost:8080 serves the React SPA.

- [ ] **Step 4: Verify API still works**

```bash
curl http://localhost:8080/api/rag/health
```

Expected: Health check responds.

- [ ] **Step 5: Push to GitHub and verify Pages deployment**

```bash
git push origin main
```

Go to repo Settings → Pages → Source: GitHub Actions. Verify the workflow runs and deploys.
