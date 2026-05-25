# Wok RAG Agent

一个基于 Java 的 Agentic RAG 智能问答平台，支持文档上传、混合检索、多轮对话和工具调用。

> 用 Java 做 RAG，而不是 Python——因为大多数要落地 AI 应用的公司，技术栈是 Java。

> 自研 RAG 而非使用 Spring AI 或 LangChain4j——因为框架 API 稳定性不足、小版本易出破坏性变更且 RAG 与工具调用适配存在问题，开箱即用能力有限，而自研可实现完全可控、无升级负担与深度定制。



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
| 存储 | SQLite (会话), 本地文件系统 (文件) |
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
