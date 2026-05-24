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
