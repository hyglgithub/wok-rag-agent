# Agentic RAG 智能问答平台设计文档

## 1. 项目概述

### 1.1 项目目标

构建一个基于Java的Agentic RAG（Retrieval-Augmented Generation）智能问答平台，整合多路检索引擎、用户意图识别、问题重写、会话记忆、工具调用等核心模块，输出高精准度的智能问答服务。

### 1.2 技术选型

| 组件 | 技术选型 | 说明 |
|------|----------|------|
| 开发语言 | Java 17+ | 企业级应用首选 |
| Web框架 | Spring Boot 3.x | 快速开发，生态丰富 |
| 构建工具 | Maven | 依赖管理，构建标准化 |
| LLM/Embedding平台 | SiliconFlow | 国内平台，Qwen系列模型 |
| 向量数据库 | Milvus | 开源，支持混合检索 |
| 文档解析 | Apache Tika | 支持多种文档格式 |
| HTTP客户端 | OkHttp | 高性能，支持流式 |
| JSON处理 | Gson | Google开源，简单易用 |

### 1.3 分阶段实施

- **第一阶段**：核心RAG检索链路（2-3周）
- **第二阶段**：高级功能（3-4周）
- **第三阶段**：生产化（2-3周）

---

## 2. 系统架构

### 2.1 整体架构

采用**模块化单体架构**，在单个Spring Boot应用中实现清晰的模块隔离：

```
┌─────────────────────────────────────────────────────────────┐
│                    WokRagAgentApplication                   │
├─────────────────────────────────────────────────────────────┤
│  Controller Layer                                           │
│  ┌─────────────────┐                                        │
│  │  RagController   │                                        │
│  └─────────────────┘                                        │
├─────────────────────────────────────────────────────────────┤
│  Service Layer                                              │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌────────┐│
│  │  Document   │ │  Embedding  │ │  Retrieval  │ │Generate││
│  │  Service    │ │  Service    │ │  Service    │ │ Service││
│  └─────────────┘ └─────────────┘ └─────────────┘ └────────┘│
├─────────────────────────────────────────────────────────────┤
│  Client Layer                                               │
│  ┌─────────────────┐ ┌─────────────────┐                    │
│  │SiliconFlowClient│ │  MilvusClient   │                    │
│  └─────────────────┘ └─────────────────┘                    │
├─────────────────────────────────────────────────────────────┤
│  External Services                                          │
│  ┌─────────────────┐ ┌─────────────────┐                    │
│  │  SiliconFlow    │ │     Milvus      │                    │
│  │  API            │ │  Vector DB      │                    │
│  └─────────────────┘ └─────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 项目结构

```
wok-rag-agent/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/wokrag/agent/
│   │   │   ├── WokRagAgentApplication.java
│   │   │   ├── config/
│   │   │   │   ├── MilvusConfig.java
│   │   │   │   ├── SiliconFlowConfig.java
│   │   │   │   └── AppConfig.java
│   │   │   ├── controller/
│   │   │   │   └── RagController.java
│   │   │   ├── service/
│   │   │   │   ├── rag/
│   │   │   │   │   ├── RagService.java
│   │   │   │   │   └── RagPipeline.java
│   │   │   │   ├── document/
│   │   │   │   │   ├── DocumentService.java
│   │   │   │   │   ├── ChunkService.java
│   │   │   │   │   └── MetadataService.java
│   │   │   │   ├── embedding/
│   │   │   │   │   └── EmbeddingService.java
│   │   │   │   ├── retrieval/
│   │   │   │   │   ├── MilvusService.java
│   │   │   │   │   ├── HybridSearchService.java
│   │   │   │   │   └── RerankerService.java
│   │   │   │   └── generation/
│   │   │   │       ├── LlmService.java
│   │   │   │       └── PromptService.java
│   │   │   ├── model/
│   │   │   │   ├── Document.java
│   │   │   │   ├── Chunk.java
│   │   │   │   ├── Embedding.java
│   │   │   │   ├── SearchResult.java
│   │   │   │   └── RagResponse.java
│   │   │   ├── client/
│   │   │   │   ├── SiliconFlowClient.java
│   │   │   │   └── MilvusClient.java
│   │   │   ├── util/
│   │   │   │   ├── JsonUtil.java
│   │   │   │   ├── TextUtil.java
│   │   │   │   └── VectorUtil.java
│   │   │   └── exception/
│   │   │       ├── RagException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/wokrag/agent/
└── docs/
```

---

## 3. 核心模块设计

### 3.1 文档处理模块

**职责**：

- 使用Apache Tika解析PDF、Word等文档
- 文本清洗和预处理
- 文本分块（支持递归分块策略）
- 元数据提取和管理

**核心接口**：

```java
public interface DocumentService {
    /**
     * 解析文档，提取文本
     */
    ParseResult parseFile(MultipartFile file);
    
    /**
     * 文本分块
     */
    List<Chunk> chunkText(String text, ChunkStrategy strategy);
    
    /**
     * 提取元数据
     */
    Map<String, String> extractMetadata(MultipartFile file);
}

public enum ChunkStrategy {
    RECURSIVE,      // 递归分块
    FIXED_SIZE,     // 固定大小
    SEMANTIC        // 语义分块
}
```

**分块参数配置**：

| 参数 | 推荐范围 | 说明 |
|------|----------|------|
| chunkSize | 200-1000字符 | 问答场景偏小（200-500），摘要场景偏大（500-1000） |
| overlap | chunkSize的10%-25% | 如chunkSize=500时，overlap设50-125 |

### 3.2 向量化模块

**职责**：

- 调用SiliconFlow Embedding API
- 文本向量化
- 批量向量化支持

**核心接口**：

```java
public interface EmbeddingService {
    /**
     * 单文本向量化
     */
    double[] embed(String text);
    
    /**
     * 批量向量化
     */
    List<double[]> embedBatch(List<String> texts);
}
```

**API配置**：

- 模型：Qwen/Qwen3-Embedding-8B
- 维度：4096
- 编码格式：float

### 3.3 检索模块

**职责**：

- Milvus向量数据库操作
- 混合检索（向量 + BM25）
- RRF算法融合
- Reranker重排序

**核心接口**：

```java
public interface RetrievalService {
    /**
     * 向量检索
     */
    List<SearchResult> search(String query, int topK);
    
    /**
     * 混合检索（向量 + BM25）
     */
    List<SearchResult> hybridSearch(String query, int topK);
    
    /**
     * 重排序
     */
    List<SearchResult> rerank(String query, List<SearchResult> candidates, int topN);
}
```

**检索参数配置**：

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| denseRecallTopK | 20 | 向量召回数量 |
| sparseRecallTopK | 20 | BM25召回数量 |
| finalTopK | 8 | 最终返回数量 |
| rrfK | 60 | RRF算法参数 |
| ef | 128 | HNSW检索参数 |

### 3.4 生成模块

**职责**：

- LLM API调用（SiliconFlow）
- Prompt组装和模板管理
- 流式响应支持
- 引用解析

**核心接口**：

```java
public interface GenerationService {
    /**
     * 非流式生成
     */
    String generate(String systemPrompt, String userMessage);
    
    /**
     * 带引用的RAG生成
     */
    RagResponse generateWithCitations(List<Chunk> chunks, String question);
    
    /**
     * 流式生成
     */
    void streamGenerate(String systemPrompt, String userMessage, StreamCallback callback);
}
```

**Prompt模板结构**：

```java
private static final String SYSTEM_PROMPT = """
    # 角色与边界
    你是一个专业的知识库问答助手。你的任务是仅依据【参考资料】回答【用户问题】。
    
    # 回答规则
    1. 只能使用参考资料中的信息进行陈述
    2. 参考资料不足以支持结论时，提出澄清问题
    3. 不要编造任何信息
    
    # 引用规则
    1. 每条关键事实后紧跟引用编号，例如：……[1]
    2. 引用必须能"指向支持该句的chunk"
    
    # 输出格式
    - 使用Markdown输出
    - 先给"结论"，再给"依据与说明"
    """;
```

---

## 4. 数据流设计

### 4.1 文档摄入流程

```
用户上传文档
    ↓
DocumentService.parseFile()
    ↓
文本提取（Apache Tika）
    ↓
DocumentService.chunkText()
    ↓
文本分块（递归分块策略）
    ↓
MetadataService.extractMetadata()
    ↓
元数据提取
    ↓
EmbeddingService.embedBatch()
    ↓
批量向量化（SiliconFlow API）
    ↓
MilvusService.insert()
    ↓
存储到Milvus
```

### 4.2 查询处理流程

```
用户提问
    ↓
RagController.query()
    ↓
RagPipeline.execute()
    ↓
┌─────────────────────────────────────────┐
│ 1. 检索阶段                              │
│    EmbeddingService.embed(query)         │
│    ↓                                     │
│    RetrievalService.hybridSearch()       │
│    ↓                                     │
│    RetrievalService.rerank()             │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ 2. 生成阶段                              │
│    PromptService.buildPrompt()           │
│    ↓                                     │
│    GenerationService.generate()          │
│    ↓                                     │
│    解析引用，构建RagResponse              │
└─────────────────────────────────────────┘
    ↓
返回RagResponse给用户
```

### 4.3 关键数据模型

**Chunk模型**：

```java
@Data
public class Chunk {
    private String id;           // chunk唯一ID
    private String content;      // 内容
    private String source;       // 来源文档
    private String sourceUrl;    // 原文链接
    private String updateTime;   // 更新时间
    private Map<String, String> metadata;  // 元数据
}
```

**SearchResult模型**：

```java
@Data
public class SearchResult {
    private String chunkId;      // chunk ID
    private String content;      // 内容
    private double score;        // 相似度分数
    private Map<String, String> metadata;  // 元数据
}
```

**RagResponse模型**：

```java
@Data
public class RagResponse {
    private String answer;                    // 模型回答
    private List<CitationInfo> citations;     // 引用信息
    
    @Data
    public static class CitationInfo {
        private Integer index;           // 引用编号
        private String source;       // 来源文档
        private String sourceUrl;    // 原文链接
        private String chunkContent; // 被引用的chunk内容
    }
}
```

---

## 5. 错误处理策略

### 5.1 异常分类

```java
public class RagException extends RuntimeException {
    private String errorCode;
    private String errorMessage;
    
    // 文档处理异常
    public static class DocumentParseException extends RagException { }
    
    // 向量化异常
    public static class EmbeddingException extends RagException { }
    
    // 检索异常
    public static class RetrievalException extends RagException { }
    
    // 生成异常
    public static class GenerationException extends RagException { }
}
```

### 5.2 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RagException.class)
    public ResponseEntity<ErrorResponse> handleRagException(RagException e) {
        ErrorResponse error = new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "系统内部错误");
        return ResponseEntity.internalServerError().body(error);
    }
}
```

### 5.3 重试机制

对于外部API调用（SiliconFlow、Milvus），实现指数退避重试：

```java
@Component
public class RetryTemplate {
    
    public <T> T executeWithRetry(Supplier<T> operation, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw e;
                }
                long delay = (long) Math.pow(2, attempt) * 1000; // 指数退避
                Thread.sleep(delay);
            }
        }
        throw new RuntimeException("重试次数已用完");
    }
}
```

---

## 6. 测试策略

### 6.1 单元测试

- 每个Service类都有对应的单元测试
- 使用Mockito模拟外部依赖
- 测试覆盖率目标：80%以上

### 6.2 集成测试

- 测试完整的RAG流程
- 使用Testcontainers运行Milvus
- 测试文档上传到问答的完整链路

### 6.3 端到端测试

- 使用真实的SiliconFlow API
- 测试真实的文档和查询
- 验证回答质量和引用准确性

---

## 7. 实现计划

### 7.1 第一阶段：核心RAG链路（2-3周）

**第1周：项目搭建和文档处理模块**

- 创建Spring Boot项目骨架
- 配置Maven依赖
- 实现文档解析服务（Apache Tika）
- 实现文本分块服务
- 实现元数据提取服务
- 编写单元测试

**第2周：向量化和检索模块**

- 实现SiliconFlow Embedding客户端
- 实现向量化服务
- 部署和配置Milvus
- 实现Milvus操作服务
- 实现混合检索服务
- 实现Reranker服务
- 编写集成测试

**第3周：生成模块和集成测试**

- 实现SiliconFlow Chat客户端
- 实现LLM生成服务
- 实现Prompt组装服务
- 实现RAG主流程编排
- 实现API接口
- 端到端测试
- 文档编写

### 7.2 第二阶段：高级功能（3-4周）

- 会话记忆管理
- 查询重写
- 意图识别
- 工具调用（Function Call）
- 流式响应优化

### 7.3 第三阶段：生产化（2-3周）

- 性能优化
- 监控和日志
- 安全加固
- 部署脚本
- 文档完善

---

## 8. 配置管理

### 8.1 application.yml

```yaml
spring:
  application:
    name: wok-rag-agent
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

server:
  port: 8080

# SiliconFlow配置
siliconflow:
  api-key: ${SILICONFLOW_API_KEY}
  base-url: https://api.siliconflow.cn/v1
  embedding-model: Qwen/Qwen3-Embedding-8B
  chat-model: Qwen/Qwen3-32B
  reranker-model: BAAI/bge-reranker-v2-m3

# Milvus配置
milvus:
  uri: http://localhost:19530
  collection-name: customer_service_chunks
  vector-dim: 4096

# RAG配置
rag:
  chunk-size: 500
  chunk-overlap: 50
  top-k: 8
  dense-recall-top-k: 20
  sparse-recall-top-k: 20
  rrf-k: 60
```

---

## 9. 依赖管理

### 9.1 pom.xml

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Apache Tika -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>3.2.3</version>
    </dependency>
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-parsers-standard-package</artifactId>
        <version>3.2.3</version>
    </dependency>

    <!-- Milvus Java SDK -->
    <dependency>
        <groupId>io.milvus</groupId>
        <artifactId>milvus-sdk-java</artifactId>
        <version>2.6.6</version>
    </dependency>

    <!-- OkHttp -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- Gson -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.13.1</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 10. 部署说明

### 10.1 环境要求

- JDK 17+
- Maven 3.8+
- Docker（用于Milvus）
- SiliconFlow API Key

### 10.2 启动步骤

1. 启动Milvus：
   ```bash
   docker-compose up -d
   ```

2. 配置环境变量：
   ```bash
   export SILICONFLOW_API_KEY=your-api-key
   ```

3. 启动应用：
   ```bash
   mvn spring-boot:run
   ```

4. 访问API：
   ```
   POST http://localhost:8080/api/rag/query
   ```

---

## 附录：参考资源

- [SiliconFlow官方文档](https://docs.siliconflow.cn/)
- [Milvus官方文档](https://milvus.io/docs)
- [Apache Tika官方文档](https://tika.apache.org/)
- [TinyRAG项目参考](https://github.com/nageoffer/tinyrag)
