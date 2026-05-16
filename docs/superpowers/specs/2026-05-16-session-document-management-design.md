# 会话历史 + 知识库文档管理 设计文档

## Context

前端已有 5 个页面（Chat、History、Knowledge、Status、Settings），但后端只实现了聊天接口（`POST /api/rag/query`、`POST /api/rag/stream`、`GET /api/rag/health`）。History 页面和 Knowledge 页面的前端代码（API 函数、Store、页面组件）已全部就绪，但后端没有对应接口。

本次开发目标：补齐后端接口，使前端 History 和 Knowledge 页面功能完整可用。

### 当前状态

| 前端页面 | 前端 API | 后端接口 | 状态 |
|---------|---------|---------|------|
| ChatPage | `POST /api/rag/stream` | StreamController | 已实现 |
| StatusPage | `GET /api/rag/health` + `GET /actuator/health` | RagController + Actuator | 已实现 |
| SettingsPage | 无（纯前端 localStorage） | — | 无需后端 |
| HistoryPage | `GET /api/rag/sessions` | 无 | **缺失** |
| HistoryPage | `DELETE /api/rag/sessions/{id}` | 无 | **缺失** |
| HistoryPage | `GET /api/rag/sessions/{id}/messages` | 无 | **缺失**（函数已声明未调用） |
| KnowledgePage | `GET /api/documents` | 无 | **缺失** |
| KnowledgePage | `POST /api/documents/upload` | 无 | **缺失** |
| KnowledgePage | `DELETE /api/documents/{id}` | 无 | **缺失** |

## 设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 会话存储 | SQLite | 轻量无额外依赖，适合单机部署，重启不丢失 |
| 文档元数据 | SQLite + Milvus | SQLite 存文档元数据，Milvus 存 chunks，各司其职 |
| SQLite 集成方式 | JdbcTemplate | 轻量简洁，与项目现有风格一致（Gson 而非 Jackson，无 JPA） |
| 实施顺序 | Session 优先 | 功能更简单，可先端到端验证 SQLite 集成 |

## 数据库 Schema

SQLite 数据库文件：`data/wok-rag.db`

### sessions 表

```sql
CREATE TABLE sessions (
    session_id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    last_active_at TEXT NOT NULL DEFAULT (datetime('now')),
    message_count INTEGER NOT NULL DEFAULT 0
);
```

### messages 表

```sql
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    citations TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (session_id) REFERENCES sessions(session_id) ON DELETE CASCADE
);
```

### documents 表

```sql
CREATE TABLE documents (
    doc_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    source TEXT,
    upload_time TEXT NOT NULL DEFAULT (datetime('now')),
    chunk_count INTEGER NOT NULL DEFAULT 0
);
```

设计要点：
- `title` 取自会话第一条用户消息（截取前 50 字符）
- `citations` 用 JSON 字符串存储，读取时用 Gson 反序列化
- 需在连接时执行 `PRAGMA foreign_keys = ON` 启用外键
- 时间统一用 ISO 8601 格式

## 后端架构

### 新增 Maven 依赖

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

### 配置变更

`application.yml` 新增：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:data/wok-rag.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
```

### 新建文件

| 文件 | 说明 |
|------|------|
| `config/SQLiteConfig.java` | 配置 `PRAGMA foreign_keys=ON`，处理 SQLite 方言兼容 |
| `repository/SessionRepository.java` | Session + Message 的 JdbcTemplate CRUD |
| `repository/DocumentRepository.java` | Document 的 JdbcTemplate CRUD |
| `controller/SessionController.java` | `/api/rag/sessions` REST 接口 |
| `controller/DocumentController.java` | `/api/documents` REST 接口 |
| `resources/schema.sql` | 建表 SQL（自动执行） |

### 修改文件

| 文件 | 变更内容 |
|------|---------|
| `pom.xml` | 添加 SQLite JDBC + Spring JDBC 依赖 |
| `application.yml` | 添加 datasource 配置 |
| `RagPipeline.java` | 聊天完成后调用 SessionRepository 持久化消息 |
| `MilvusClientWrapper.java` | 新增 `deleteByDocId()` 方法 |

### SessionController

```
GET    /api/rag/sessions                → { sessions: Session[] }
GET    /api/rag/sessions/{id}/messages   → { messages: SessionMessage[] }
DELETE /api/rag/sessions/{id}            → { status: "ok" }
```

Session 响应格式（匹配前端 `Session` 接口）：
```json
{
  "sessionId": "session-xxx",
  "title": "用户的第一条消息...",
  "lastMessage": "最后一条消息内容...",
  "lastTime": "2026-05-16T10:30:00",
  "messageCount": 6
}
```

### DocumentController

```
GET    /api/documents                   → { documents: DocumentInfo[] }
POST   /api/documents/upload            → DocumentInfo  (multipart: file + source)
DELETE /api/documents/{docId}            → { status: "ok" }
```

上传流程：
1. `DocumentService.parseFile(file)` — Tika 提取文本
2. `ChunkService.chunkText(text, chunkSize, overlap, source)` — 分块
3. `EmbeddingService.embedBatch(texts)` — 批量向量化
4. `MilvusService.insertChunks(rows)` — 写入 Milvus
5. `DocumentRepository.save(docId, name, source, chunkCount)` — 保存元数据到 SQLite

删除流程：
1. `MilvusClientWrapper.deleteByDocId(docId)` — 删除 Milvus 中的 chunks
2. `DocumentRepository.delete(docId)` — 删除 SQLite 中的元数据

## 前端改动

### ChatPage 加载会话历史

`frontend/src/pages/ChatPage.tsx`：

当路由为 `/chat/:sessionId` 时，调用 `getSessionMessages(sessionId)` 获取历史消息，转换格式后通过 `chatStore.loadSession()` 填充。

### 无需修改的文件

| 文件 | 原因 |
|------|------|
| `api/rag.ts` | API 函数已声明 |
| `api/document.ts` | API 函数已声明 |
| `stores/sessionStore.ts` | Store 方法已实现 |
| `pages/HistoryPage.tsx` | UI 和 API 调用已完整 |
| `pages/KnowledgePage.tsx` | UI 和 API 调用已完整 |

## 实施顺序

```
Phase 1: SQLite 集成 + Session 管理
  1.1 添加 Maven 依赖 + 数据库配置
  1.2 创建 schema.sql + SQLiteConfig
  1.3 实现 SessionRepository
  1.4 实现 SessionController
  1.5 修改 RagPipeline 持久化消息
  1.6 前端 ChatPage 加载会话历史

Phase 2: 文档管理
  2.1 实现 DocumentRepository
  2.2 扩展 MilvusClientWrapper (deleteByDocId)
  2.3 实现 DocumentController
```

## 验证方式

1. 启动后端 `mvn spring-boot:run`，确认 `data/wok-rag.db` 自动创建
2. 发送聊天消息，检查 SQLite 中 sessions 和 messages 表有数据
3. 刷新 History 页面，确认会话出现
4. 点击会话跳转，确认历史消息加载
5. 删除会话，确认从列表和数据库中消失
6. 上传文档到 Knowledge 页面，确认出现在列表中
7. 删除文档，确认 Milvus chunks 和 SQLite 记录均被清除
8. 重启后端，确认会话和文档数据仍然存在
