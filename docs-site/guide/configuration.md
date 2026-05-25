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

## 安全配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `security.api-key.enabled` | `false` | 是否启用 API 认证（生产环境自动开启） |

认证开启后，后端每次启动自动生成随机 Token 并打印到日志，同时写入 `data/auth-token.txt`。前端通过登录页输入 Token 进行认证。

## 其他配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `rewrite.enabled` | `true` | 是否启用问题重写 |
| `rewrite.model` | `Qwen/Qwen2.5-7B-Instruct` | 重写模型 |
| `tool.enabled` | `true` | 是否启用工具调用 |
| `tool.model` | `Qwen/Qwen2.5-7B-Instruct` | 工具调用模型 |
| `intent.enabled` | `true` | 是否启用意图识别 |
| `intent.model` | `Qwen/Qwen2.5-7B-Instruct` | 意图分类模型 |
| `intent.confidence-threshold` | `0.5` | 意图分类置信度阈值 |
| `cors.allowed-origins` | `*` | CORS 允许的来源 |
| `file.storage.path` | `data/documents/` | 文档存储路径 |
