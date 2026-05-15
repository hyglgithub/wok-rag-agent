# 第三阶段：生产化功能测试步骤

## 前置条件

| 条件 | 说明 |
|------|------|
| Java 17 | `java -version` 确认 |
| Maven 3.8+ | `mvn -version` 确认 |
| Docker | `docker --version` 确认 |
| SILICONFLOW_API_KEY | 环境变量已设置 |
| Milvus 运行中 | `docker-compose up -d standalone` |

---

## 1. 结构化日志

### 1.1 Dev 模式 — 控制台日志

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**验证点：**
- [ ] 控制台输出格式为 `HH:mm:ss.SSS [thread] LEVEL logger - message`
- [ ] `com.wokrag` 包日志级别为 DEBUG
- [ ] `io.milvus` 包日志级别为 DEBUG

### 1.2 Prod 模式 — JSON 文件日志

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**验证点：**
- [ ] `logs/wok-rag-agent.json` 文件生成，内容为 JSON 格式
- [ ] `logs/wok-rag-agent.log` 文件生成，内容为可读文本格式
- [ ] JSON 日志包含 `timestamp`、`level`、`logger`、`thread`、`message` 字段
- [ ] `com.wokrag` 包日志级别为 INFO（DEBUG 日志不出现）
- [ ] `io.milvus` 包日志级别为 WARN

---

## 2. Actuator 监控端点

启动应用后依次验证：

### 2.1 健康检查（含依赖检查）

```bash
curl http://localhost:8080/actuator/health | jq
```

**验证点：**
- [ ] 返回 `status` 字段（`UP` 或 `DOWN`）
- [ ] 包含 `components.milvusHealthIndicator`（数据库连接状态）
- [ ] 包含 `components.siliconFlowHealthIndicator`（API 连接状态）
- [ ] Milvus 连接正常时状态为 `UP`
- [ ] 无 SILICONFLOW_API_KEY 时 SiliconFlow 状态为 `DOWN`

### 2.2 Prometheus 指标

```bash
curl http://localhost:8080/actuator/prometheus
```

**验证点：**
- [ ] 返回 200 状态码
- [ ] 返回 Prometheus 格式指标数据（`# HELP`、`# TYPE` 开头）
- [ ] 包含 JVM 指标（`jvm_memory_used_bytes` 等）
- [ ] 包含 HTTP 指标（`http_server_requests_seconds` 等）

### 2.3 Info 端点

```bash
curl http://localhost:8080/actuator/info
```

**验证点：**
- [ ] 返回 200 状态码
- [ ] 返回 JSON 对象

---

## 3. API Key 认证

### 3.1 Dev 模式（认证关闭）

```bash
# 无 API Key 应正常访问
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "你好"}'
```

**验证点：**
- [ ] 返回 200，正常响应
- [ ] 不需要 `X-API-Key` 头

### 3.2 Prod 模式（认证开启）

```bash
# 启动 prod 模式
export API_KEY=test-key-123
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

#### 测试 3.2.1：无 API Key

```bash
curl -w "\nHTTP_CODE:%{http_code}\n" \
  -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "你好"}'
```

**验证点：**
- [ ] 返回 HTTP 401
- [ ] 响应体包含 `"Missing API key"`
- [ ] 响应体 JSON 格式：`{"errorCode":"UNAUTHORIZED","errorMessage":"..."}`

#### 测试 3.2.2：错误 API Key

```bash
curl -w "\nHTTP_CODE:%{http_code}\n" \
  -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -H "X-API-Key: wrong-key" \
  -d '{"question": "你好"}'
```

**验证点：**
- [ ] 返回 HTTP 403
- [ ] 响应体包含 `"Invalid API key"`

#### 测试 3.2.3：正确 API Key

```bash
curl -w "\nHTTP_CODE:%{http_code}\n" \
  -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -H "X-API-Key: test-key-123" \
  -d '{"question": "你好"}'
```

**验证点：**
- [ ] 返回 HTTP 200
- [ ] 正常响应

#### 测试 3.2.4：健康端点绕过认证

```bash
curl -w "\nHTTP_CODE:%{http_code}\n" http://localhost:8080/actuator/health
curl -w "\nHTTP_CODE:%{http_code}\n" http://localhost:8080/swagger-ui.html
```

**验证点：**
- [ ] `/actuator/health` 不需要 API Key，返回 200
- [ ] `/swagger-ui.html` 不需要 API Key，返回 200 或 302

---

## 4. 请求日志与 Correlation ID

### 4.1 自动生成 Correlation ID

```bash
curl -v -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "你好"}' 2>&1 | grep -i "X-Correlation-Id"
```

**验证点：**
- [ ] 响应头包含 `X-Correlation-Id`
- [ ] 值为 16 位十六进制字符串
- [ ] 应用日志中出现 `[correlationId] >>> POST /api/rag/query`
- [ ] 应用日志中出现 `[correlationId] <<< POST /api/rag/query -> 200 (xxms)`

### 4.2 传递已有 Correlation ID

```bash
curl -v -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: my-custom-id-001" \
  -d '{"question": "你好"}' 2>&1 | grep -i "X-Correlation-Id"
```

**验证点：**
- [ ] 响应头 `X-Correlation-Id` 值为 `my-custom-id-001`（原样返回）
- [ ] 日志中使用 `my-custom-id-001`

### 4.3 Actuator 端点跳过日志

```bash
curl http://localhost:8080/actuator/health > /dev/null 2>&1
```

**验证点：**
- [ ] 日志中不出现 `/actuator/health` 的 `>>>` / `<<<` 记录

---

## 5. CORS 跨域配置

### 5.1 Dev 模式（允许所有来源）

```bash
curl -v -X OPTIONS http://localhost:8080/api/rag/query \
  -H "Origin: http://evil.com" \
  -H "Access-Control-Request-Method: POST" 2>&1 | grep -i "access-control"
```

**验证点：**
- [ ] 响应头包含 `Access-Control-Allow-Origin: *`

### 5.2 Prod 模式（限制来源）

```bash
export CORS_ALLOWED_ORIGINS=http://localhost:3000
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# 允许的来源
curl -v -X OPTIONS http://localhost:8080/api/rag/query \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" 2>&1 | grep -i "access-control"

# 不允许的来源
curl -v -X OPTIONS http://localhost:8080/api/rag/query \
  -H "Origin: http://evil.com" \
  -H "Access-Control-Request-Method: POST" 2>&1 | grep -i "access-control"
```

**验证点：**
- [ ] `Origin: http://localhost:3000` 返回 `Access-Control-Allow-Origin: http://localhost:3000`
- [ ] `Origin: http://evil.com` 不返回 `Access-Control-Allow-Origin`
- [ ] 暴露 `X-Correlation-Id` 头：`Access-Control-Expose-Headers: X-Correlation-Id`

---

## 6. API 重试机制

### 6.1 Embedding API 重试

模拟 SiliconFlow API 不可用（断网或错误 API Key）：

```bash
export SILICONFLOW_API_KEY=invalid-key
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

发送一个需要 embedding 的查询：

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "退货政策是什么？"}'
```

**验证点：**
- [ ] 日志中出现 3 次重试记录：`embed failed (attempt 1/4), retrying in 1000ms`
- [ ] 日志中出现：`embed failed (attempt 2/4), retrying in 2000ms`
- [ ] 日志中出现：`embed failed (attempt 3/4), retrying in 4000ms`
- [ ] 最终抛出 `EmbeddingException`，返回错误响应

---

## 7. Swagger UI / OpenAPI 文档

### 7.1 Swagger UI 访问

浏览器打开：`http://localhost:8080/swagger-ui.html`

**验证点：**
- [ ] 页面加载成功
- [ ] 显示两个分组：`RAG` 和 `RAG Streaming`
- [ ] `RAG` 分组包含：`POST /api/rag/query`、`GET /api/rag/health`
- [ ] `RAG Streaming` 分组包含：`POST /api/rag/stream`
- [ ] 页面右上角显示锁图标（API Key 安全方案）

### 7.2 OpenAPI JSON 规范

```bash
curl http://localhost:8080/v3/api-docs | jq '.info'
```

**验证点：**
- [ ] `title` 为 `"WokRag Agent API"`
- [ ] `version` 为 `"1.0.0"`
- [ ] 包含 `securitySchemes.API-Key`（类型为 `apiKey`，位置为 `header`，名称为 `X-API-Key`）

### 7.3 端点参数文档

```bash
curl http://localhost:8080/v3/api-docs | jq '.paths["/api/rag/query"].post.parameters'
```

**验证点：**
- [ ] `question` 参数有 `description` 和 `example`
- [ ] `sessionId` 参数有 `description` 和 `example`

---

## 8. Docker 部署

### 8.1 构建镜像

```bash
mvn clean package -DskipTests -q
docker build -t wok-rag-agent .
```

**验证点：**
- [ ] 构建成功，无错误
- [ ] 镜像大小合理（< 500MB）

### 8.2 完整栈启动

```bash
export SILICONFLOW_API_KEY=your-key
export API_KEY=your-api-key
docker-compose up -d
```

**验证点：**
- [ ] 所有 5 个容器启动：`rustfs`、`etcd`、`milvus-standalone`、`milvus-attu`、`wok-rag-agent`
- [ ] `docker-compose ps` 显示所有容器状态为 `Up`
- [ ] `wok-rag-agent` 容器健康检查通过（`healthy` 状态）

### 8.3 容器内健康检查

```bash
docker exec wok-rag-agent wget -qO- http://localhost:8080/actuator/health
```

**验证点：**
- [ ] 返回 JSON 健康信息
- [ ] Milvus 状态为 `UP`（容器内通过 Docker 网络连接）

### 8.4 容器日志

```bash
docker logs wok-rag-agent --tail 50
```

**验证点：**
- [ ] 日志格式正确（INFO 级别）
- [ ] 启动日志包含 `Connected to Milvus`
- [ ] 无 ERROR 级别日志

### 8.5 停止和清理

```bash
docker-compose down
```

**验证点：**
- [ ] 所有容器停止并移除
- [ ] 数据卷保留（`docker volume ls` 中仍存在 `milvus-data` 等）

---

## 9. 单元测试验证

```bash
mvn test -q
```

**验证点：**
- [ ] `ApiKeyAuthFilterTest` — 6 个测试全部通过
- [ ] `RequestLoggingFilterTest` — 3 个测试全部通过
- [ ] `HealthIndicatorTest` — 4 个测试全部通过
- [ ] `ProductionFeaturesTest` — 5 个测试全部通过
- [ ] 预期失败：`EndToEndTest`、`TestDataInitializer`（需要运行中的 Milvus）

---

## 测试结果记录表

| 编号 | 测试项 | 预期结果 | 实际结果 | 通过 |
|------|--------|----------|----------|------|
| 1.1 | Dev 控制台日志 | DEBUG 级别，格式正确 | | |
| 1.2 | Prod JSON 日志 | 文件生成，JSON 格式 | | |
| 2.1 | Actuator Health | 包含依赖状态 | | |
| 2.2 | Prometheus 指标 | 返回指标数据 | | |
| 3.1 | Dev 无认证 | 正常访问 | | |
| 3.2.1 | 无 API Key → 401 | 返回 401 | | |
| 3.2.2 | 错误 Key → 403 | 返回 403 | | |
| 3.2.3 | 正确 Key → 200 | 返回 200 | | |
| 3.2.4 | 健康端点绕过 | 不需要 Key | | |
| 4.1 | 自动生成 Correlation ID | 16 位 hex | | |
| 4.2 | 传递已有 ID | 原样返回 | | |
| 5.1 | Dev CORS 允许所有 | `*` | | |
| 5.2 | Prod CORS 限制 | 仅允许指定来源 | | |
| 6.1 | Embedding 重试 | 3 次重试，指数退避 | | |
| 7.1 | Swagger UI | 页面加载，显示端点 | | |
| 7.2 | OpenAPI JSON | 规范正确 | | |
| 8.1 | Docker 构建 | 构建成功 | | |
| 8.2 | 完整栈启动 | 5 容器运行 | | |
| 8.3 | 容器健康检查 | 返回 UP | | |
| 8.4 | 容器日志 | 格式正确 | | |
| 9.1 | 单元测试 | 18 个新测试通过 | | |
