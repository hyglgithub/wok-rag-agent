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

```bash
# 启动服务（自动使用 prod profile）
docker-compose up -d
```

生产环境下：
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
