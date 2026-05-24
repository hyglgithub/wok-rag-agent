# 贡献指南

感谢你对 Wok RAG Agent 的关注！

## 如何贡献

1. Fork 本仓库
2. 创建你的特性分支：`git checkout -b feature/amazing-feature`
3. 提交你的改动：`git commit -m 'feat: add amazing feature'`
4. 推送到分支：`git push origin feature/amazing-feature`
5. 提交 Pull Request

## 开发环境

### 后端

- Java 17
- Maven
- Docker（用于 Milvus）

```bash
# 启动 Milvus 基础设施
docker-compose up -d standalone

# 运行应用
export SILICONFLOW_API_KEY=your-key
mvn spring-boot:run
```

### 前端

- Node.js 20+

```bash
cd frontend
npm install
npm run dev
```

## 代码规范

- 后端：使用 Lombok（`@Data`, `@RequiredArgsConstructor`, `@Slf4j`），JSON 序列化用 Gson
- 前端：遵循 ESLint 规则，使用 TypeScript
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范

## Pull Request 规范

- 确保 PR 只解决一个问题
- 提供清晰的 PR 描述，说明改动原因
- 确保代码通过 `mvn test` 和 `npm run lint`
- 更新相关文档（如有必要）
