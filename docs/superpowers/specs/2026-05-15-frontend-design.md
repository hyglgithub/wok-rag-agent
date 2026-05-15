# Wok RAG Agent 前端设计文档

## 概述

为 wok-rag-agent RAG 知识问答系统构建 React 前端应用，提供类 ChatGPT 的对话界面及管理功能。

**技术栈**：React 19 + TypeScript + Vite + Zustand + React Router + Tailwind CSS + @headlessui/react
**后端 API**：`http://localhost:8080`，REST + SSE
**部署**：Vercel

## 整体布局

侧边栏 + 主内容区布局（方案 A）：

- **左侧边栏**（固定，可折叠）：上半部分为会话列表 + 新建对话按钮，下半部分为导航菜单
- **右侧主内容区**：根据路由切换显示不同页面
- 移动端自动收起侧边栏

```
┌─────────────────────────────────────────────────┐
│  ┌──────────┐  ┌────────────────────────────┐   │
│  │ 侧边栏    │  │        主内容区              │   │
│  │           │  │                            │   │
│  │ [Logo]    │  │   根据路由切换显示：          │   │
│  │           │  │   - 聊天对话页              │   │
│  │ 新建对话   │  │   - 知识库管理页            │   │
│  │           │  │   - 会话历史页              │   │
│  │ 会话列表   │  │   - 系统状态页              │   │
│  │ - 会话1    │  │   - 设置页                 │   │
│  │ - 会话2    │  │                            │   │
│  │ - ...     │  │                            │   │
│  │           │  │                            │   │
│  │ ──────── │  │                            │   │
│  │ 知识库    │  │                            │   │
│  │ 会话历史  │  │                            │   │
│  │ 系统状态  │  │                            │   │
│  │ 设置     │  │                            │   │
│  └──────────┘  └────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

## 页面结构

| 路由 | 页面 | 功能 |
|------|------|------|
| `/chat` (首页) | 聊天对话 | 核心对话界面，流式输出，引用展示 |
| `/chat/:sessionId` | 聊天对话 | 继续指定会话 |
| `/knowledge` | 知识库管理 | 文档列表、上传、删除 |
| `/history` | 会话历史 | 查看所有历史会话 |
| `/status` | 状态监控 | 后端健康状态检查 |
| `/settings` | 设置 | API 地址、主题、语言配置 |

## 详细设计

### 1. 聊天对话页

**核心交互**：
- 流式输出：使用 `fetch` + `ReadableStream` 调用 `POST /api/rag/stream`，逐 token 显示打字机效果
- SSE 事件处理：`token` 事件拼接文本，`done` 事件解析完整响应和引用，`error` 事件显示错误
- 引用来源：回答中 `[N]` 标记可点击，底部展示引用卡片列表（序号、来源文件名、chunk 内容，可折叠）
- 会话记忆：每个对话携带 `sessionId`，后端维护上下文
- 发送方式：Enter 发送，Shift+Enter 换行
- 加载状态：等待首个 token 时显示 loading 动画
- 错误处理：红色消息气泡显示错误信息

**消息数据结构**：
```typescript
interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations: Citation[]
  timestamp: number
  error?: string
}

interface Citation {
  index: number
  source: string
  sourceUrl: string
  chunkContent: string
}
```

### 2. 知识库管理页

- 文档列表：展示已导入文档（文件名、来源、导入时间）
- 上传文档：拖拽或点击上传，显示上传进度
- 删除文档：确认后删除
- 搜索：按文件名或来源过滤

**文档数据结构**：
```typescript
interface DocumentInfo {
  id: string
  name: string
  source: string
  uploadTime: string
  chunkCount: number
}
```

### 3. 会话历史页

- 展示所有会话（标题、最后消息预览、时间）
- 点击跳转到聊天页继续对话
- 支持删除会话

**会话数据结构**：
```typescript
interface Session {
  sessionId: string
  title: string
  lastMessage: string
  lastTime: string
  messageCount: number
}
```

### 4. 系统状态页

调用 `/actuator/health` 获取后端健康状态，以卡片形式展示：
- 应用状态
- Milvus 连接状态
- SiliconFlow API 状态

绿色=正常，红色=异常。

### 5. 设置页

纯前端配置，存储在 `localStorage`：
- API 基础地址（默认 `http://localhost:8080`）
- 主题切换（浅色/深色）
- 语言（中文/英文）

## API 对接

### 现有后端接口

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/api/rag/query` | 同步查询 |
| POST | `/api/rag/stream` | SSE 流式查询 |
| GET | `/api/rag/health` | 简单健康检查 |
| GET | `/actuator/health` | 详细健康状态 |

### 需要后端补充的接口

| 方法 | 路径 | 用途 | 优先级 |
|------|------|------|--------|
| GET | `/api/rag/sessions` | 获取会话列表 | 高 |
| GET | `/api/rag/sessions/{id}/messages` | 获取会话消息历史 | 高 |
| DELETE | `/api/rag/sessions/{id}` | 删除会话 | 中 |
| GET | `/api/documents` | 获取已导入文档列表 | 高 |
| POST | `/api/documents/upload` | 上传文档到知识库 | 高 |
| DELETE | `/api/documents/{id}` | 删除文档 | 中 |

**接口详细规格**：

#### GET /api/rag/sessions

响应：
```json
{
  "sessions": [
    {
      "sessionId": "session-abc123",
      "title": "退货政策咨询",
      "lastMessage": "根据我们的退货政策...",
      "lastTime": "2026-05-15T10:30:00",
      "messageCount": 4
    }
  ]
}
```

#### GET /api/rag/sessions/{id}/messages

响应：
```json
{
  "messages": [
    {
      "role": "user",
      "content": "退货政策是什么？",
      "timestamp": "2026-05-15T10:28:00"
    },
    {
      "role": "assistant",
      "content": "根据我们的退货政策[1]...",
      "citations": [...],
      "timestamp": "2026-05-15T10:28:05"
    }
  ]
}
```

#### GET /api/documents

响应：
```json
{
  "documents": [
    {
      "id": "doc-001",
      "name": "退货政策.pdf",
      "source": "官网",
      "uploadTime": "2026-05-10T08:00:00",
      "chunkCount": 15
    }
  ]
}
```

#### POST /api/documents/upload

请求：`multipart/form-data`，字段 `file`（文件）、`source`（来源描述，可选）
响应：
```json
{
  "id": "doc-002",
  "name": "物流说明.pdf",
  "chunkCount": 8
}
```

#### DELETE /api/rag/sessions/{id} 和 DELETE /api/documents/{id}

响应：HTTP 204 No Content

## 前端项目结构

```
frontend/src/
├── api/                    # API 请求封装
│   ├── client.ts           # fetch 基础配置
│   ├── rag.ts              # RAG 相关接口
│   └── document.ts         # 文档管理接口
├── components/             # 通用组件
│   ├── chat/
│   │   ├── MessageBubble.tsx
│   │   ├── ChatInput.tsx
│   │   └── CitationCard.tsx
│   ├── layout/
│   │   ├── AppLayout.tsx
│   │   └── Sidebar.tsx
│   └── common/
│       ├── StatusCard.tsx
│       └── UploadDialog.tsx
├── pages/                  # 页面组件
│   ├── ChatPage.tsx
│   ├── KnowledgePage.tsx
│   ├── HistoryPage.tsx
│   ├── StatusPage.tsx
│   └── SettingsPage.tsx
├── stores/                 # Zustand 状态管理
│   ├── chatStore.ts        # 聊天状态（消息、流式输出）
│   ├── sessionStore.ts     # 会话管理
│   └── settingsStore.ts    # 设置
├── types/
│   └── index.ts            # TypeScript 类型定义
├── App.tsx
├── main.tsx
└── index.css               # Tailwind 入口
```

## UI 风格与技术选型

**框架**：React 19 + TypeScript + Vite
**路由**：React Router v7
**状态管理**：Zustand — 轻量、无 Provider、API 简洁
**样式方案**：Tailwind CSS + @headlessui/react
- Tailwind CSS 负责所有样式定制，完全控制视觉风格
- Headless UI 提供无障碍交互、键盘导航等现成逻辑
- 不使用 Ant Design 等重组件库，避免千篇一律的后台管理风格
- 参考 ChatGPT 的简洁对话界面

**部署**：Vercel — 配置 rewrite 将 `/api/*` 代理到后端

**主题**：浅色为主，支持深色模式切换（Tailwind `dark:` 变体 + CSS 变量）
**布局**：响应式，移动端自动收起侧边栏
**消息气泡**：用户消息靠右，AI 回答靠左
**引用卡片**：灰色背景，可折叠展开
