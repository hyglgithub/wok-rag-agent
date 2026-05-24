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
