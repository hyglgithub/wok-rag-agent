# 部署指南

## Docker 部署（推荐）

### 一键部署

```bash
# 1. 克隆项目
git clone https://github.com/hyglgithub/wok-rag-agent.git
cd wok-rag-agent

# 2. 设置环境变量
export SILICONFLOW_API_KEY=your-siliconflow-api-key

# 3. 启动所有服务
docker-compose up -d

# 4. 获取登录 Token（prod profile 启用认证后需要）
docker exec wok-rag-agent cat /app/data/auth-token.txt
```

启动完成后访问 http://localhost:8080 即可使用。首次访问时在登录页输入上一步获取的 Token。

::: tip
首次启动需要下载 Docker 镜像，可能需要几分钟时间。Milvus 健康检查需要约 90 秒启动。
:::

### 服务组件

`docker-compose up -d` 会启动以下服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| wok-rag-agent | 8080 | 主应用（前端 + 后端） |
| Milvus | 19530 | 向量数据库 |
| Attu | 8000 | Milvus 管理界面 |
| RustFS | 9000/9001 | Milvus 底层对象存储（应用不直接使用） |
| etcd | 2379 | Milvus 元数据存储 |

### 生产环境配置

Docker Compose 默认使用 prod profile，生产环境下：
- API 认证默认开启 — 后端启动时自动生成随机 Token 并打印到日志，首次访问时在登录页输入即可
- CORS 限制为配置的域名（可通过 `CORS_ALLOWED_ORIGINS` 环境变量覆盖）
- 日志格式为 JSON 结构化输出

::: tip
Token 每次重启都会更新。如需持久化，挂载 `data/` 目录：`-v ./data:/app/data`
:::

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

## 开发环境部署

### 环境要求

- Java 17
- Maven
- Node.js 20+
- Docker & Docker Compose（仅用于启动 Milvus 基础设施）
- [SiliconFlow API Key](https://cloud.siliconflow.cn/)

### 后端

```bash
# 启动 Milvus 基础设施
docker-compose up -d standalone etcd rustfs

# 设置环境变量
export SILICONFLOW_API_KEY=your-key

# 运行应用（默认 dev profile，认证关闭）
mvn spring-boot:run
```

后端运行在 http://localhost:8080。

如需模拟生产环境（启用认证、JSON 日志、CORS 限制）：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器运行在 http://localhost:5173，API 请求会自动代理到 http://localhost:8080。

### 认证说明

开发环境（dev profile）默认关闭认证，前端会自动检测后端认证状态，无需 Token，直接访问即可使用。

使用 prod profile 启动时认证开启，后端每次启动会自动生成随机 Token 并写入 `data/auth-token.txt`，同时打印到控制台日志。首次访问时在登录页输入该 Token 即可：

```bash
cat data/auth-token.txt
```

## 获取 SiliconFlow API Key

1. 访问 [SiliconFlow 控制台](https://cloud.siliconflow.cn/)
2. 注册并登录
3. 在 API Keys 页面创建新的密钥
4. 复制密钥并设置为环境变量

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

## 常用命令

### 停止与重启

```bash
# 停止项目（保留容器，下次可直接启动）
docker-compose stop

# 重新启动
docker-compose start

# 停止并删除容器（不删镜像，下次 up 很快）
docker-compose down

# 重新创建
docker-compose up -d
```

### 完全删除项目（谨慎）

```bash
# 删除容器 + 网络 + 数据卷（数据也会删除）
docker-compose down -v
```

### 镜像管理

```bash
# 查看镜像
docker images

# 删除指定镜像
docker rmi milvus-stack-app:latest

# 或通过镜像 ID 删除
docker rmi 镜像ID

# 强制删除
docker rmi -f 镜像ID
```

### 清理无用资源

```bash
# 一键清理所有无用 Docker 资源（停止的容器、无用镜像、build cache、network）
docker system prune -a
```

### 查看状态与日志

```bash
# 查看运行中的容器
docker ps

# 查看所有容器（含已停止）
docker ps -a

# 查看应用日志（实时跟踪）
docker logs -f wok-rag-agent
```

## 常见问题

### 镜像拉取失败

国内网络环境下拉取 Docker Hub 镜像可能超时，配置镜像源后重启 Docker 即可：

```json
{
    "registry-mirrors": [
        "https://docker.registry.cyou/",
        "https://docker-cf.registry.cyou/",
        "https://dockercf.jsdelivr.fyi/",
        "https://docker.jsdelivr.fyi/",
        "https://dockertest.jsdelivr.fyi/",
        "https://mirror.aliyuncs.com/",
        "https://dockerproxy.com/",
        "https://mirror.baidubce.com/",
        "https://docker.m.daocloud.io/",
        "https://docker.nju.edu.cn/",
        "https://docker.mirrors.sjtug.sjtu.edu.cn/",
        "https://docker.mirrors.ustc.edu.cn/",
        "https://mirror.iscas.ac.cn/",
        "https://docker.rainbond.cc/"
    ]
}
```

配置文件位置：
- Linux: `/etc/docker/daemon.json`
- Windows: Docker Desktop → Settings → Docker Engine
- macOS: Docker Desktop → Settings → Docker Engine

### 前端 TypeScript 编译错误

如果 `npm run build` 报错 `error TS6133: declared but never read`，是因为 `frontend/tsconfig.app.json` 中开启了 `noUnusedLocals` 和 `noUnusedParameters`。

快速解决：删除未使用的变量/参数，或者在 `frontend/tsconfig.app.json` 中关闭检查：

```json
{
  "compilerOptions": {
    "noUnusedLocals": false,
    "noUnusedParameters": false
  }
}
```
