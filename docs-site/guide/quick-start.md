# 快速开始

## Docker 一键部署

```bash
git clone https://github.com/hyglgithub/wok-rag-agent.git
cd wok-rag-agent
export SILICONFLOW_API_KEY=your-siliconflow-api-key
docker-compose up -d
```

启动完成后访问 http://localhost:8080 即可使用。

::: tip
首次启动需要下载 Docker 镜像，可能需要几分钟时间。Milvus 健康检查需要约 90 秒启动。
:::

## 下一步

- [部署指南](/guide/deployment) - Docker 部署、开发环境、反向代理、常见问题
- [配置说明](/guide/configuration) - 了解所有可配置参数
- [架构说明](/guide/architecture) - 了解系统设计
- [API 文档](/api/endpoints) - 查看接口详情
