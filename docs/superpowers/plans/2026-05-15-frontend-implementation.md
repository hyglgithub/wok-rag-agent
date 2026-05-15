# Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a ChatGPT-style Vue 3 frontend for the wok-rag-agent RAG system with streaming chat, knowledge management, session history, status monitoring, and settings.

**Architecture:** SPA with sidebar navigation layout. Chat page uses SSE streaming via `fetch` + `ReadableStream`. State managed by Pinia. Styling via Tailwind CSS v4 + Headless UI for accessible interactions.

**Tech Stack:** Vue 3, TypeScript, Vite, Pinia, Vue Router, Tailwind CSS v4, @headlessui/vue, lucide-vue-next

**Backend dependency:** Some pages (Knowledge, History) require backend APIs not yet implemented. These pages will show empty states gracefully when APIs are unavailable. The Chat and Status pages work with existing endpoints.

**Spec:** `docs/superpowers/specs/2026-05-15-frontend-design.md`

---

## File Structure

```
frontend/src/
├── api/
│   ├── client.ts              # fetch wrapper with base URL config
│   ├── rag.ts                 # /api/rag/* endpoints
│   └── document.ts            # /api/documents/* endpoints
├── components/
│   ├── layout/
│   │   ├── AppLayout.vue      # sidebar + main content area
│   │   └── Sidebar.vue        # navigation + session list
│   ├── chat/
│   │   ├── MessageBubble.vue  # single message (user or assistant)
│   │   ├── ChatInput.vue      # textarea + send button
│   │   └── CitationCard.vue   # expandable citation display
│   └── common/
│       ├── StatusCard.vue     # health status indicator card
│       └── UploadDialog.vue   # file upload modal
├── views/
│   ├── ChatView.vue           # main chat page
│   ├── KnowledgeView.vue      # document management
│   ├── HistoryView.vue        # session list
│   ├── StatusView.vue         # health monitoring
│   └── SettingsView.vue       # app settings
├── stores/
│   ├── chat.ts                # messages, streaming state, current session
│   ├── session.ts             # session list management
│   └── settings.ts            # API URL, theme, language
├── types/
│   └── index.ts               # all TypeScript interfaces
├── router/
│   └── index.ts               # route definitions
├── assets/
│   └── main.css               # Tailwind imports + global styles
├── App.vue
└── main.ts
```

---

### Task 1: Install Dependencies and Configure Tailwind CSS

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.ts`
- Create: `frontend/src/assets/main.css`
- Modify: `frontend/src/main.ts`
- Modify: `frontend/index.html`

- [ ] **Step 1: Install npm dependencies**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend
npm install @headlessui/vue lucide-vue-next
npm install -D tailwindcss @tailwindcss/vite
```

- [ ] **Step 2: Configure Vite with Tailwind plugin**

Replace `frontend/vite.config.ts`:

```typescript
import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
})
```

- [ ] **Step 3: Create global CSS with Tailwind and theme variables**

Create `frontend/src/assets/main.css`:

```css
@import "tailwindcss";

@theme {
  --color-primary: #10a37f;
  --color-primary-hover: #0e8c6d;
  --color-surface: #ffffff;
  --color-surface-secondary: #f7f7f8;
  --color-border: #e5e5e5;
  --color-text: #1a1a1a;
  --color-text-secondary: #6b6b6b;
  --color-user-bubble: #f0f0f0;
  --color-error: #ef4444;
}

@custom-variant dark (&:where(.dark, .dark *));

.dark {
  --color-surface: #1e1e1e;
  --color-surface-secondary: #2a2a2a;
  --color-border: #3a3a3a;
  --color-text: #e5e5e5;
  --color-text-secondary: #9a9a9a;
  --color-user-bubble: #2f2f2f;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: var(--color-surface);
  color: var(--color-text);
}

/* Scrollbar styling */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}
```

- [ ] **Step 4: Update main.ts to import global styles**

Replace `frontend/src/main.ts`:

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
```

- [ ] **Step 5: Update index.html title**

Replace `frontend/index.html`:

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <link rel="icon" href="/favicon.ico">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Wok RAG Agent</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 6: Remove unused files and verify dev server starts**

```bash
rm -f E:/idea_workspace/wok-rag-agent/frontend/src/stores/counter.ts
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Expected: Dev server starts on `http://localhost:5173` without errors.

- [ ] **Step 7: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/
git commit -m "feat: setup Tailwind CSS v4, Headless UI, and project foundation"
```

---

### Task 2: Define TypeScript Types

**Files:**
- Create: `frontend/src/types/index.ts`

- [ ] **Step 1: Create type definitions**

Create `frontend/src/types/index.ts`:

```typescript
// API request/response types matching backend DTOs

export interface QueryRequest {
  question: string
  sessionId?: string
}

export interface RagResponse {
  answer: string
  sessionId: string | null
  citations: Citation[]
}

export interface Citation {
  index: number
  source: string
  sourceUrl: string
  chunkContent: string
}

export interface HealthResponse {
  status: string
  service: string
}

export interface ErrorResponse {
  errorCode: string
  errorMessage: string
}

// SSE streaming event types
export type SSEEvent =
  | { event: 'token'; data: string }
  | { event: 'done'; data: RagResponse }
  | { event: 'error'; data: { message: string } }

// Frontend-only types

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations: Citation[]
  timestamp: number
  error?: string
}

export interface Session {
  sessionId: string
  title: string
  lastMessage: string
  lastTime: string
  messageCount: number
}

export interface DocumentInfo {
  id: string
  name: string
  source: string
  uploadTime: string
  chunkCount: number
}

// Backend session message format (for GET /api/rag/sessions/{id}/messages)
export interface SessionMessage {
  role: 'user' | 'assistant'
  content: string
  citations?: Citation[]
  timestamp: string
}

export interface Settings {
  apiUrl: string
  theme: 'light' | 'dark'
  language: 'zh' | 'en'
}
```

- [ ] **Step 2: Verify TypeScript compiles**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx vue-tsc --build --noEmit
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/types/
git commit -m "feat: add TypeScript type definitions for API and frontend"
```

---

### Task 3: Create API Client Layer

**Files:**
- Create: `frontend/src/api/client.ts`
- Create: `frontend/src/api/rag.ts`
- Create: `frontend/src/api/document.ts`

- [ ] **Step 1: Create base fetch client**

Create `frontend/src/api/client.ts`:

```typescript
import { useSettingsStore } from '@/stores/settings'

function getBaseUrl(): string {
  const settings = useSettingsStore()
  return settings.apiUrl
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${getBaseUrl()}${path}`
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      errorCode: 'UNKNOWN_ERROR',
      errorMessage: `HTTP ${response.status}`,
    }))
    throw error
  }

  return response.json()
}

export function apiFetchStream(
  path: string,
  body: unknown
): ReadableStream<Uint8Array> {
  const url = `${getBaseUrl()}${path}`
  const encoder = new TextEncoder()

  return new ReadableStream({
    async start(controller) {
      try {
        const response = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
        })

        if (!response.ok) {
          const error = await response.json().catch(() => ({
            message: `HTTP ${response.status}`,
          }))
          controller.enqueue(encoder.encode(`event: error\ndata: ${JSON.stringify(error)}\n\n`))
          controller.close()
          return
        }

        const reader = response.body?.getReader()
        if (!reader) {
          controller.close()
          return
        }

        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          controller.enqueue(value)
        }
        controller.close()
      } catch (err) {
        const error = { message: err instanceof Error ? err.message : 'Network error' }
        controller.enqueue(encoder.encode(`event: error\ndata: ${JSON.stringify(error)}\n\n`))
        controller.close()
      }
    },
  })
}
```

- [ ] **Step 2: Create RAG API functions**

Create `frontend/src/api/rag.ts`:

```typescript
import { apiFetch, apiFetchStream } from './client'
import type { QueryRequest, RagResponse, HealthResponse, Session, SessionMessage } from '@/types'

export function queryRag(request: QueryRequest): Promise<RagResponse> {
  return apiFetch<RagResponse>('/api/rag/query', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function streamRag(request: QueryRequest): ReadableStream<Uint8Array> {
  return apiFetchStream('/api/rag/stream', request)
}

export function getHealth(): Promise<HealthResponse> {
  return apiFetch<HealthResponse>('/api/rag/health')
}

export function getActuatorHealth(): Promise<Record<string, unknown>> {
  return apiFetch<Record<string, unknown>>('/actuator/health')
}

// --- Backend-dependent APIs (may not exist yet) ---

export async function getSessions(): Promise<Session[]> {
  try {
    const res = await apiFetch<{ sessions: Session[] }>('/api/rag/sessions')
    return res.sessions
  } catch {
    return []
  }
}

export async function getSessionMessages(sessionId: string): Promise<SessionMessage[]> {
  try {
    const res = await apiFetch<{ messages: SessionMessage[] }>(`/api/rag/sessions/${sessionId}/messages`)
    return res.messages
  } catch {
    return []
  }
}

export async function deleteSession(sessionId: string): Promise<void> {
  await apiFetch(`/api/rag/sessions/${sessionId}`, { method: 'DELETE' })
}
```

- [ ] **Step 3: Create Document API functions**

Create `frontend/src/api/document.ts`:

```typescript
import { apiFetch } from './client'
import type { DocumentInfo } from '@/types'

export async function getDocuments(): Promise<DocumentInfo[]> {
  try {
    const res = await apiFetch<{ documents: DocumentInfo[] }>('/api/documents')
    return res.documents
  } catch {
    return []
  }
}

export async function uploadDocument(file: File, source?: string): Promise<DocumentInfo> {
  const formData = new FormData()
  formData.append('file', file)
  if (source) formData.append('source', source)

  const settings = (await import('@/stores/settings')).useSettingsStore()
  const response = await fetch(`${settings.apiUrl}/api/documents/upload`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      errorCode: 'UPLOAD_ERROR',
      errorMessage: `HTTP ${response.status}`,
    }))
    throw error
  }

  return response.json()
}

export async function deleteDocument(docId: string): Promise<void> {
  await apiFetch(`/api/documents/${docId}`, { method: 'DELETE' })
}
```

- [ ] **Step 4: Verify TypeScript compiles**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx vue-tsc --build --noEmit
```

Expected: No errors.

- [ ] **Step 5: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/api/
git commit -m "feat: add API client layer with fetch wrapper and SSE streaming"
```

---

### Task 4: Create Pinia Stores

**Files:**
- Create: `frontend/src/stores/settings.ts`
- Create: `frontend/src/stores/chat.ts`
- Create: `frontend/src/stores/session.ts`

- [ ] **Step 1: Create settings store**

Create `frontend/src/stores/settings.ts`:

```typescript
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { Settings } from '@/types'

const STORAGE_KEY = 'wok-rag-settings'

function loadSettings(): Settings {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    try {
      return JSON.parse(stored)
    } catch {
      // ignore
    }
  }
  return {
    apiUrl: 'http://localhost:8080',
    theme: 'light',
    language: 'zh',
  }
}

export const useSettingsStore = defineStore('settings', () => {
  const settings = ref<Settings>(loadSettings())

  function updateTheme(theme: 'light' | 'dark') {
    settings.value.theme = theme
    if (theme === 'dark') {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  function updateApiUrl(url: string) {
    settings.value.apiUrl = url
  }

  function updateLanguage(lang: 'zh' | 'en') {
    settings.value.language = lang
  }

  // Persist to localStorage on change
  watch(settings, (val) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(val))
  }, { deep: true })

  // Apply theme on init
  if (settings.value.theme === 'dark') {
    document.documentElement.classList.add('dark')
  }

  return { settings, updateTheme, updateApiUrl, updateLanguage }
})
```

- [ ] **Step 2: Create chat store**

Create `frontend/src/stores/chat.ts`:

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { v4 as uuidv4 } from 'crypto'  // will use crypto.randomUUID()
import type { Message, Citation, RagResponse } from '@/types'
import { streamRag } from '@/api/rag'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const currentSessionId = ref<string | null>(null)
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const error = ref<string | null>(null)

  const hasMessages = computed(() => messages.value.length > 0)

  function generateId(): string {
    return crypto.randomUUID()
  }

  function generateSessionId(): string {
    return `session-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  }

  function clearMessages() {
    messages.value = []
    currentSessionId.value = null
    error.value = null
  }

  function loadSession(sessionId: string, sessionMessages: Message[]) {
    currentSessionId.value = sessionId
    messages.value = sessionMessages
    error.value = null
  }

  async function sendMessage(question: string) {
    if (isStreaming.value || !question.trim()) return

    error.value = null

    // Add user message
    const userMessage: Message = {
      id: generateId(),
      role: 'user',
      content: question.trim(),
      citations: [],
      timestamp: Date.now(),
    }
    messages.value.push(userMessage)

    // Ensure session ID exists
    if (!currentSessionId.value) {
      currentSessionId.value = generateSessionId()
    }

    // Create placeholder assistant message
    const assistantMessage: Message = {
      id: generateId(),
      role: 'assistant',
      content: '',
      citations: [],
      timestamp: Date.now(),
    }
    messages.value.push(assistantMessage)

    isStreaming.value = true
    streamingContent.value = ''

    try {
      const stream = streamRag({
        question: question.trim(),
        sessionId: currentSessionId.value,
      })

      const reader = stream.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            const eventType = line.slice(7).trim()
            // Next line should be data
            continue
          }
          if (line.startsWith('data: ')) {
            const data = line.slice(6)

            // Determine event type from preceding event line
            // We need to track the current event type
            // Re-parse: look at the raw buffer more carefully
            handleSSELine(data, assistantMessage)
          }
        }
      }

      // Process any remaining buffer
      if (buffer.startsWith('data: ')) {
        handleSSELine(buffer.slice(6), assistantMessage)
      }
    } catch (err) {
      assistantMessage.error = err instanceof Error ? err.message : 'Unknown error'
    } finally {
      isStreaming.value = false
      streamingContent.value = ''
    }
  }

  // Track current event type for proper parsing
  let currentEventType = ''

  function handleSSEData(eventType: string, data: string, msg: Message) {
    if (eventType === 'token') {
      msg.content += data
      streamingContent.value = msg.content
    } else if (eventType === 'done') {
      try {
        const response: RagResponse = JSON.parse(data)
        msg.content = response.answer
        msg.citations = response.citations || []
        if (response.sessionId) {
          currentSessionId.value = response.sessionId
        }
      } catch {
        // Keep accumulated tokens as content
      }
    } else if (eventType === 'error') {
      try {
        const errData = JSON.parse(data)
        msg.error = errData.message || 'Stream error'
      } catch {
        msg.error = 'Stream error'
      }
    }
  }

  // Proper SSE parser that tracks event type
  async function sendMessageProper(question: string) {
    if (isStreaming.value || !question.trim()) return

    error.value = null

    const userMessage: Message = {
      id: generateId(),
      role: 'user',
      content: question.trim(),
      citations: [],
      timestamp: Date.now(),
    }
    messages.value.push(userMessage)

    if (!currentSessionId.value) {
      currentSessionId.value = generateSessionId()
    }

    const assistantMessage: Message = {
      id: generateId(),
      role: 'assistant',
      content: '',
      citations: [],
      timestamp: Date.now(),
    }
    messages.value.push(assistantMessage)

    isStreaming.value = true
    streamingContent.value = ''

    try {
      const stream = streamRag({
        question: question.trim(),
        sessionId: currentSessionId.value,
      })

      const reader = stream.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let eventType = 'token'

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            eventType = line.slice(7).trim()
          } else if (line.startsWith('data: ')) {
            const data = line.slice(6)
            handleSSEData(eventType, data, assistantMessage)
            eventType = 'token' // reset
          }
        }
      }
    } catch (err) {
      assistantMessage.error = err instanceof Error ? err.message : 'Unknown error'
    } finally {
      isStreaming.value = false
      streamingContent.value = ''
    }
  }

  return {
    messages,
    currentSessionId,
    isStreaming,
    streamingContent,
    error,
    hasMessages,
    clearMessages,
    loadSession,
    sendMessage: sendMessageProper,
  }
})
```

- [ ] **Step 3: Create session store**

Create `frontend/src/stores/session.ts`:

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Session } from '@/types'
import { getSessions, deleteSession as apiDeleteSession } from '@/api/rag'

export const useSessionStore = defineStore('session', () => {
  const sessions = ref<Session[]>([])
  const loading = ref(false)

  async function fetchSessions() {
    loading.value = true
    try {
      sessions.value = await getSessions()
    } finally {
      loading.value = false
    }
  }

  async function removeSession(sessionId: string) {
    try {
      await apiDeleteSession(sessionId)
      sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    } catch {
      // Silently fail if backend doesn't support this yet
    }
  }

  // Add a session entry locally (when backend doesn't have list API yet)
  function addLocalSession(sessionId: string, title: string) {
    if (!sessions.value.find(s => s.sessionId === sessionId)) {
      sessions.value.unshift({
        sessionId,
        title,
        lastMessage: '',
        lastTime: new Date().toISOString(),
        messageCount: 0,
      })
    }
  }

  return { sessions, loading, fetchSessions, removeSession, addLocalSession }
})
```

- [ ] **Step 4: Verify TypeScript compiles**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx vue-tsc --build --noEmit
```

Expected: No errors. (The `handleSSEData` function reference inside `sendMessage` may need adjustment — the final version should use `sendMessageProper`.)

- [ ] **Step 5: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/stores/
git commit -m "feat: add Pinia stores for settings, chat, and session management"
```

---

### Task 5: Build Layout Components (Sidebar + AppLayout)

**Files:**
- Create: `frontend/src/components/layout/AppLayout.vue`
- Create: `frontend/src/components/layout/Sidebar.vue`
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: Create Sidebar component**

Create `frontend/src/components/layout/Sidebar.vue`:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useSessionStore } from '@/stores/session'
import {
  MessageSquare,
  Database,
  History,
  Activity,
  Settings,
  Plus,
  PanelLeftClose,
  PanelLeftOpen,
  Trash2,
} from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const sessionStore = useSessionStore()

const collapsed = ref(false)

const navItems = [
  { path: '/knowledge', label: '知识库', icon: Database },
  { path: '/history', label: '会话历史', icon: History },
  { path: '/status', label: '系统状态', icon: Activity },
  { path: '/settings', label: '设置', icon: Settings },
]

function newChat() {
  chatStore.clearMessages()
  router.push('/chat')
}

function openSession(sessionId: string) {
  router.push(`/chat/${sessionId}`)
}

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<template>
  <aside
    class="flex flex-col h-screen border-r border-border bg-surface-secondary transition-all duration-300"
    :class="collapsed ? 'w-16' : 'w-64'"
  >
    <!-- Header -->
    <div class="flex items-center justify-between p-3 border-b border-border">
      <span v-if="!collapsed" class="text-sm font-semibold text-text truncate">
        Wok RAG Agent
      </span>
      <button
        @click="collapsed = !collapsed"
        class="p-1.5 rounded-lg hover:bg-border/50 text-text-secondary"
      >
        <PanelLeftClose v-if="!collapsed" :size="18" />
        <PanelLeftOpen v-else :size="18" />
      </button>
    </div>

    <!-- New Chat Button -->
    <div class="p-2">
      <button
        @click="newChat"
        class="flex items-center gap-2 w-full p-2.5 rounded-lg border border-border
               hover:bg-border/50 text-text text-sm transition-colors"
        :class="collapsed ? 'justify-center' : ''"
      >
        <Plus :size="18" />
        <span v-if="!collapsed">新建对话</span>
      </button>
    </div>

    <!-- Session List -->
    <div v-if="!collapsed" class="flex-1 overflow-y-auto px-2 space-y-0.5">
      <div v-if="sessionStore.sessions.length === 0" class="p-3 text-text-secondary text-xs text-center">
        暂无会话
      </div>
      <button
        v-for="session in sessionStore.sessions"
        :key="session.sessionId"
        @click="openSession(session.sessionId)"
        class="flex items-center justify-between w-full p-2 rounded-lg text-sm text-left
               hover:bg-border/50 transition-colors group"
        :class="chatStore.currentSessionId === session.sessionId ? 'bg-border/50' : ''"
      >
        <div class="flex items-center gap-2 min-w-0">
          <MessageSquare :size="16" class="shrink-0 text-text-secondary" />
          <span class="truncate text-text">{{ session.title }}</span>
        </div>
        <button
          @click.stop="sessionStore.removeSession(session.sessionId)"
          class="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-error/10 text-text-secondary hover:text-error"
        >
          <Trash2 :size="14" />
        </button>
      </button>
    </div>

    <!-- Navigation -->
    <nav class="border-t border-border p-2 space-y-0.5">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="flex items-center gap-2 p-2 rounded-lg text-sm transition-colors"
        :class="[
          isActive(item.path) ? 'bg-border/50 text-text' : 'text-text-secondary hover:bg-border/30',
          collapsed ? 'justify-center' : '',
        ]"
      >
        <component :is="item.icon" :size="18" />
        <span v-if="!collapsed">{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>
```

- [ ] **Step 2: Create AppLayout component**

Create `frontend/src/components/layout/AppLayout.vue`:

```vue
<script setup lang="ts">
import Sidebar from './Sidebar.vue'
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-surface">
    <Sidebar />
    <main class="flex-1 overflow-hidden">
      <router-view />
    </main>
  </div>
</template>
```

- [ ] **Step 3: Update App.vue to use layout**

Replace `frontend/src/App.vue`:

```vue
<script setup lang="ts">
import AppLayout from './components/layout/AppLayout.vue'
</script>

<template>
  <AppLayout />
</template>
```

- [ ] **Step 4: Set up router with all routes**

Replace `frontend/src/router/index.ts`:

```typescript
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/chat',
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
    },
    {
      path: '/chat/:sessionId',
      name: 'chat-session',
      component: () => import('@/views/ChatView.vue'),
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('@/views/KnowledgeView.vue'),
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('@/views/HistoryView.vue'),
    },
    {
      path: '/status',
      name: 'status',
      component: () => import('@/views/StatusView.vue'),
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/SettingsView.vue'),
    },
  ],
})

export default router
```

- [ ] **Step 5: Create placeholder view components**

Create `frontend/src/views/ChatView.vue`:

```vue
<template>
  <div class="flex items-center justify-center h-full text-text-secondary">
    Chat View - Coming soon
  </div>
</template>
```

Create `frontend/src/views/KnowledgeView.vue`:

```vue
<template>
  <div class="flex items-center justify-center h-full text-text-secondary">
    Knowledge View - Coming soon
  </div>
</template>
```

Create `frontend/src/views/HistoryView.vue`:

```vue
<template>
  <div class="flex items-center justify-center h-full text-text-secondary">
    History View - Coming soon
  </div>
</template>
```

Create `frontend/src/views/StatusView.vue`:

```vue
<template>
  <div class="flex items-center justify-center h-full text-text-secondary">
    Status View - Coming soon
  </div>
</template>
```

Create `frontend/src/views/SettingsView.vue`:

```vue
<template>
  <div class="flex items-center justify-center h-full text-text-secondary">
    Settings View - Coming soon
  </div>
</template>
```

- [ ] **Step 6: Verify dev server shows sidebar and routes work**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Open `http://localhost:5173` in browser. Verify:
- Sidebar appears with navigation
- Clicking nav items changes the main content area
- "New Chat" button navigates to `/chat`
- Sidebar collapses/expands

- [ ] **Step 7: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/components/ frontend/src/views/ frontend/src/App.vue frontend/src/router/
git commit -m "feat: add sidebar layout, router, and placeholder views"
```

---

### Task 6: Build Chat Page — Messages and Input

**Files:**
- Create: `frontend/src/components/chat/MessageBubble.vue`
- Create: `frontend/src/components/chat/ChatInput.vue`
- Modify: `frontend/src/views/ChatView.vue`

- [ ] **Step 1: Create MessageBubble component**

Create `frontend/src/components/chat/MessageBubble.vue`:

```vue
<script setup lang="ts">
import type { Message } from '@/types'
import CitationCard from './CitationCard.vue'
import { User, Bot } from 'lucide-vue-next'

defineProps<{
  message: Message
}>()
</script>

<template>
  <div
    class="flex gap-3 py-4 px-4"
    :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
  >
    <!-- Avatar (assistant only) -->
    <div
      v-if="message.role === 'assistant'"
      class="w-8 h-8 rounded-full bg-primary flex items-center justify-center shrink-0"
    >
      <Bot :size="16" class="text-white" />
    </div>

    <!-- Message content -->
    <div
      class="max-w-[70%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
      :class="message.role === 'user'
        ? 'bg-user-bubble text-text'
        : 'bg-surface-secondary text-text'"
    >
      <!-- Error state -->
      <div v-if="message.error" class="text-error">
        {{ message.error }}
      </div>

      <!-- Normal content -->
      <div v-else class="whitespace-pre-wrap">{{ message.content }}</div>

      <!-- Citations -->
      <div v-if="message.citations.length > 0" class="mt-3 pt-3 border-t border-border space-y-2">
        <CitationCard
          v-for="citation in message.citations"
          :key="citation.index"
          :citation="citation"
        />
      </div>
    </div>

    <!-- Avatar (user only) -->
    <div
      v-if="message.role === 'user'"
      class="w-8 h-8 rounded-full bg-user-bubble flex items-center justify-center shrink-0"
    >
      <User :size="16" class="text-text-secondary" />
    </div>
  </div>
</template>
```

- [ ] **Step 2: Create CitationCard component**

Create `frontend/src/components/chat/CitationCard.vue`:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import type { Citation } from '@/types'
import { ChevronDown, ChevronUp, ExternalLink } from 'lucide-vue-next'

defineProps<{
  citation: Citation
}>()

const expanded = ref(false)
</script>

<template>
  <div class="rounded-lg bg-surface border border-border text-xs">
    <button
      @click="expanded = !expanded"
      class="flex items-center justify-between w-full p-2 hover:bg-border/30 transition-colors"
    >
      <div class="flex items-center gap-2">
        <span class="w-5 h-5 rounded bg-primary/10 text-primary flex items-center justify-center text-xs font-medium">
          {{ citation.index }}
        </span>
        <span class="text-text-secondary truncate">{{ citation.source }}</span>
      </div>
      <ChevronUp v-if="expanded" :size="14" class="text-text-secondary" />
      <ChevronDown v-else :size="14" class="text-text-secondary" />
    </button>

    <div v-if="expanded" class="px-2 pb-2 border-t border-border">
      <p class="mt-2 text-text-secondary leading-relaxed">{{ citation.chunkContent }}</p>
      <a
        v-if="citation.sourceUrl"
        :href="citation.sourceUrl"
        target="_blank"
        class="flex items-center gap-1 mt-2 text-primary hover:underline"
      >
        <ExternalLink :size="12" />
        查看来源
      </a>
    </div>
  </div>
</template>
```

- [ ] **Step 3: Create ChatInput component**

Create `frontend/src/components/chat/ChatInput.vue`:

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Send } from 'lucide-vue-next'

const emit = defineEmits<{
  send: [question: string]
}>()

defineProps<{
  disabled: boolean
}>()

const input = ref('')
const textareaRef = ref<HTMLTextAreaElement>()

function handleSend() {
  if (input.value.trim()) {
    emit('send', input.value)
    input.value = ''
    adjustHeight()
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function adjustHeight() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

onMounted(() => {
  textareaRef.value?.focus()
})
</script>

<template>
  <div class="border-t border-border bg-surface p-4">
    <div class="max-w-3xl mx-auto">
      <div class="flex items-end gap-2 rounded-xl border border-border bg-surface-secondary p-2">
        <textarea
          ref="textareaRef"
          v-model="input"
          @keydown="handleKeydown"
          @input="adjustHeight"
          placeholder="输入你的问题... (Enter 发送, Shift+Enter 换行)"
          rows="1"
          class="flex-1 bg-transparent resize-none px-2 py-1.5 text-sm text-text
                 placeholder:text-text-secondary focus:outline-none"
          style="max-height: 200px"
        />
        <button
          @click="handleSend"
          :disabled="disabled || !input.trim()"
          class="p-2 rounded-lg bg-primary text-white hover:bg-primary-hover
                 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <Send :size="18" />
        </button>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 4: Implement ChatView**

Replace `frontend/src/views/ChatView.vue`:

```vue
<script setup lang="ts">
import { watch, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useSessionStore } from '@/stores/session'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import { MessageSquare } from 'lucide-vue-next'

const route = useRoute()
const chatStore = useChatStore()
const sessionStore = useSessionStore()

// Scroll to bottom when messages change
async function scrollToBottom() {
  await nextTick()
  const container = document.getElementById('chat-messages')
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

watch(() => chatStore.messages.length, scrollToBottom)
watch(() => chatStore.streamingContent, scrollToBottom)

// Load session from route param
onMounted(() => {
  const sessionId = route.params.sessionId as string | undefined
  if (sessionId && sessionId !== chatStore.currentSessionId) {
    // Load session messages from backend (if available)
    // For now, just set the session ID
    chatStore.currentSessionId = sessionId
  }
})

async function handleSend(question: string) {
  await chatStore.sendMessage(question)
  // Add to local session list if new
  if (chatStore.currentSessionId) {
    sessionStore.addLocalSession(
      chatStore.currentSessionId,
      question.slice(0, 30) + (question.length > 30 ? '...' : '')
    )
  }
}
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- Messages area -->
    <div id="chat-messages" class="flex-1 overflow-y-auto">
      <!-- Empty state -->
      <div v-if="!chatStore.hasMessages" class="flex flex-col items-center justify-center h-full text-text-secondary">
        <MessageSquare :size="48" class="mb-4 opacity-30" />
        <p class="text-lg font-medium">有什么可以帮你的？</p>
        <p class="text-sm mt-1">基于知识库的智能问答助手</p>
      </div>

      <!-- Message list -->
      <div v-else class="max-w-3xl mx-auto py-4">
        <MessageBubble
          v-for="msg in chatStore.messages"
          :key="msg.id"
          :message="msg"
        />
      </div>
    </div>

    <!-- Input area -->
    <ChatInput
      :disabled="chatStore.isStreaming"
      @send="handleSend"
    />
  </div>
</template>
```

- [ ] **Step 5: Verify chat page renders correctly**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Open browser, verify:
- Empty state shows centered icon and text
- Input area at bottom with send button
- Clicking send with text does nothing yet (backend may not be running)

- [ ] **Step 6: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/components/chat/ frontend/src/views/ChatView.vue
git commit -m "feat: add chat page with message bubbles, input, and citation cards"
```

---

### Task 7: Build Chat Page — SSE Streaming Integration

**Files:**
- Modify: `frontend/src/stores/chat.ts`
- Modify: `frontend/src/views/ChatView.vue` (minor)

- [ ] **Step 1: Fix the SSE parser in chat store**

The chat store created in Task 4 has a duplicate `sendMessage` function. Replace `frontend/src/stores/chat.ts` with the cleaned-up version:

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Message, RagResponse } from '@/types'
import { streamRag } from '@/api/rag'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const currentSessionId = ref<string | null>(null)
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const error = ref<string | null>(null)

  const hasMessages = computed(() => messages.value.length > 0)

  function generateId(): string {
    return crypto.randomUUID()
  }

  function generateSessionId(): string {
    return `session-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  }

  function clearMessages() {
    messages.value = []
    currentSessionId.value = null
    error.value = null
  }

  function loadSession(sessionId: string, sessionMessages: Message[]) {
    currentSessionId.value = sessionId
    messages.value = sessionMessages
    error.value = null
  }

  async function sendMessage(question: string) {
    if (isStreaming.value || !question.trim()) return

    error.value = null

    const userMessage: Message = {
      id: generateId(),
      role: 'user',
      content: question.trim(),
      citations: [],
      timestamp: Date.now(),
    }
    messages.value.push(userMessage)

    if (!currentSessionId.value) {
      currentSessionId.value = generateSessionId()
    }

    const assistantMessage: Message = {
      id: generateId(),
      role: 'assistant',
      content: '',
      citations: [],
      timestamp: Date.now(),
    }
    messages.value.push(assistantMessage)

    isStreaming.value = true
    streamingContent.value = ''

    try {
      const stream = streamRag({
        question: question.trim(),
        sessionId: currentSessionId.value!,
      })

      const reader = stream.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let eventType = 'token'

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            eventType = line.slice(7).trim()
          } else if (line.startsWith('data: ')) {
            const data = line.slice(6)

            if (eventType === 'token') {
              assistantMessage.content += data
              streamingContent.value = assistantMessage.content
            } else if (eventType === 'done') {
              try {
                const response: RagResponse = JSON.parse(data)
                assistantMessage.content = response.answer
                assistantMessage.citations = response.citations || []
                if (response.sessionId) {
                  currentSessionId.value = response.sessionId
                }
              } catch {
                // Keep accumulated tokens
              }
            } else if (eventType === 'error') {
              try {
                const errData = JSON.parse(data)
                assistantMessage.error = errData.message || 'Stream error'
              } catch {
                assistantMessage.error = 'Stream error'
              }
            }

            eventType = 'token' // reset for next pair
          }
        }
      }
    } catch (err) {
      assistantMessage.error = err instanceof Error ? err.message : 'Unknown error'
    } finally {
      isStreaming.value = false
      streamingContent.value = ''
    }
  }

  return {
    messages,
    currentSessionId,
    isStreaming,
    streamingContent,
    error,
    hasMessages,
    clearMessages,
    loadSession,
    sendMessage,
  }
})
```

- [ ] **Step 2: Add streaming indicator to ChatView**

Add a loading indicator below the last message when streaming. Update `frontend/src/views/ChatView.vue` to add after the message list:

```vue
<script setup lang="ts">
import { watch, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useSessionStore } from '@/stores/session'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import { MessageSquare, Loader2 } from 'lucide-vue-next'

const route = useRoute()
const chatStore = useChatStore()
const sessionStore = useSessionStore()

async function scrollToBottom() {
  await nextTick()
  const container = document.getElementById('chat-messages')
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

watch(() => chatStore.messages.length, scrollToBottom)
watch(() => chatStore.streamingContent, scrollToBottom)

onMounted(() => {
  const sessionId = route.params.sessionId as string | undefined
  if (sessionId && sessionId !== chatStore.currentSessionId) {
    chatStore.currentSessionId = sessionId
  }
})

async function handleSend(question: string) {
  await chatStore.sendMessage(question)
  if (chatStore.currentSessionId) {
    sessionStore.addLocalSession(
      chatStore.currentSessionId,
      question.slice(0, 30) + (question.length > 30 ? '...' : '')
    )
  }
}
</script>

<template>
  <div class="flex flex-col h-full">
    <div id="chat-messages" class="flex-1 overflow-y-auto">
      <div v-if="!chatStore.hasMessages" class="flex flex-col items-center justify-center h-full text-text-secondary">
        <MessageSquare :size="48" class="mb-4 opacity-30" />
        <p class="text-lg font-medium">有什么可以帮你的？</p>
        <p class="text-sm mt-1">基于知识库的智能问答助手</p>
      </div>

      <div v-else class="max-w-3xl mx-auto py-4">
        <MessageBubble
          v-for="msg in chatStore.messages"
          :key="msg.id"
          :message="msg"
        />

        <!-- Streaming indicator -->
        <div v-if="chatStore.isStreaming && !chatStore.streamingContent" class="flex items-center gap-2 px-4 py-2 text-text-secondary text-sm">
          <Loader2 :size="16" class="animate-spin" />
          <span>思考中...</span>
        </div>
      </div>
    </div>

    <ChatInput
      :disabled="chatStore.isStreaming"
      @send="handleSend"
    />
  </div>
</template>
```

- [ ] **Step 3: Test streaming with backend**

Start backend and frontend:

```bash
# Terminal 1: Start backend
cd E:/idea_workspace/wok-rag-agent && mvn spring-boot:run

# Terminal 2: Start frontend
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Set `SILICONFLOW_API_KEY` env var, then in browser:
1. Type a question and press Enter
2. Verify tokens appear one by one (typewriter effect)
3. Verify citations appear after response completes
4. Verify session ID is maintained for follow-up questions

- [ ] **Step 4: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/stores/chat.ts frontend/src/views/ChatView.vue
git commit -m "feat: integrate SSE streaming with proper event parsing"
```

---

### Task 8: Build Knowledge Management Page

**Files:**
- Create: `frontend/src/components/common/UploadDialog.vue`
- Modify: `frontend/src/views/KnowledgeView.vue`

- [ ] **Step 1: Create UploadDialog component**

Create `frontend/src/components/common/UploadDialog.vue`:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Dialog, DialogPanel, DialogTitle, TransitionRoot, TransitionChild } from '@headlessui/vue'
import { Upload, X, File } from 'lucide-vue-next'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
  upload: [file: File, source: string]
}>()

const selectedFile = ref<File | null>(null)
const source = ref('')
const uploading = ref(false)
const dragOver = ref(false)

function handleDrop(e: DragEvent) {
  dragOver.value = false
  const file = e.dataTransfer?.files[0]
  if (file) selectedFile.value = file
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) selectedFile.value = file
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    emit('upload', selectedFile.value, source.value)
    selectedFile.value = null
    source.value = ''
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <TransitionRoot :show="open" as="template">
    <Dialog @close="emit('close')">
      <TransitionChild
        enter="duration-200 ease-out"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="duration-150 ease-in"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-black/30 z-50" />
      </TransitionChild>

      <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <TransitionChild
          enter="duration-200 ease-out"
          enter-from="opacity-0 scale-95"
          enter-to="opacity-100 scale-100"
          leave="duration-150 ease-in"
          leave-from="opacity-100 scale-100"
          leave-to="opacity-0 scale-95"
        >
          <DialogPanel class="w-full max-w-md rounded-xl bg-surface border border-border p-6 shadow-xl">
            <div class="flex items-center justify-between mb-4">
              <DialogTitle class="text-lg font-semibold text-text">上传文档</DialogTitle>
              <button @click="emit('close')" class="p-1 rounded hover:bg-border/50 text-text-secondary">
                <X :size="18" />
              </button>
            </div>

            <!-- Drop zone -->
            <div
              @dragover.prevent="dragOver = true"
              @dragleave="dragOver = false"
              @drop.prevent="handleDrop"
              class="border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors"
              :class="dragOver ? 'border-primary bg-primary/5' : 'border-border'"
              @click="($refs.fileInput as HTMLInputElement).click()"
            >
              <File v-if="selectedFile" :size="32" class="mx-auto mb-2 text-primary" />
              <Upload v-else :size="32" class="mx-auto mb-2 text-text-secondary" />
              <p class="text-sm text-text">
                {{ selectedFile ? selectedFile.name : '拖拽文件到此处或点击选择' }}
              </p>
              <p class="text-xs text-text-secondary mt-1">支持 PDF、TXT、DOCX 等格式</p>
              <input
                ref="fileInput"
                type="file"
                class="hidden"
                @change="handleFileSelect"
                accept=".pdf,.txt,.docx,.doc,.md"
              />
            </div>

            <!-- Source input -->
            <div class="mt-4">
              <label class="block text-sm text-text-secondary mb-1">来源描述（可选）</label>
              <input
                v-model="source"
                type="text"
                placeholder="例如：官网、客服中心"
                class="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-text
                       placeholder:text-text-secondary focus:outline-none focus:ring-2 focus:ring-primary/30"
              />
            </div>

            <!-- Actions -->
            <div class="flex justify-end gap-2 mt-6">
              <button
                @click="emit('close')"
                class="px-4 py-2 rounded-lg text-sm text-text-secondary hover:bg-border/50 transition-colors"
              >
                取消
              </button>
              <button
                @click="handleUpload"
                :disabled="!selectedFile || uploading"
                class="px-4 py-2 rounded-lg text-sm text-white bg-primary hover:bg-primary-hover
                       disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {{ uploading ? '上传中...' : '上传' }}
              </button>
            </div>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>
```

- [ ] **Step 2: Implement KnowledgeView**

Replace `frontend/src/views/KnowledgeView.vue`:

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { DocumentInfo } from '@/types'
import { getDocuments, uploadDocument, deleteDocument } from '@/api/document'
import UploadDialog from '@/components/common/UploadDialog.vue'
import { Database, Trash2, Upload, Search, FileText } from 'lucide-vue-next'

const documents = ref<DocumentInfo[]>([])
const loading = ref(false)
const searchQuery = ref('')
const showUpload = ref(false)

const filteredDocs = computed(() => {
  if (!searchQuery.value) return documents.value
  const q = searchQuery.value.toLowerCase()
  return documents.value.filter(
    d => d.name.toLowerCase().includes(q) || d.source.toLowerCase().includes(q)
  )
})

async function fetchDocuments() {
  loading.value = true
  try {
    documents.value = await getDocuments()
  } finally {
    loading.value = false
  }
}

async function handleUpload(file: File, source: string) {
  try {
    const result = await uploadDocument(file, source)
    documents.value.unshift(result)
    showUpload.value = false
  } catch (err) {
    alert('上传失败: ' + (err instanceof Error ? err.message : '未知错误'))
  }
}

async function handleDelete(doc: DocumentInfo) {
  if (!confirm(`确定删除 "${doc.name}"？`)) return
  try {
    await deleteDocument(doc.id)
    documents.value = documents.value.filter(d => d.id !== doc.id)
  } catch {
    alert('删除失败')
  }
}

onMounted(fetchDocuments)
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="max-w-4xl mx-auto p-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-xl font-semibold text-text flex items-center gap-2">
            <Database :size="22" />
            知识库管理
          </h1>
          <p class="text-sm text-text-secondary mt-1">管理已导入的文档</p>
        </div>
        <button
          @click="showUpload = true"
          class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-white text-sm
                 hover:bg-primary-hover transition-colors"
        >
          <Upload :size="16" />
          上传文档
        </button>
      </div>

      <!-- Search -->
      <div class="relative mb-4">
        <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索文档..."
          class="w-full rounded-lg border border-border bg-surface-secondary pl-9 pr-3 py-2 text-sm text-text
                 placeholder:text-text-secondary focus:outline-none focus:ring-2 focus:ring-primary/30"
        />
      </div>

      <!-- Document list -->
      <div v-if="loading" class="text-center py-12 text-text-secondary">加载中...</div>

      <div v-else-if="filteredDocs.length === 0" class="text-center py-12">
        <FileText :size="48" class="mx-auto mb-3 text-text-secondary opacity-30" />
        <p class="text-text-secondary">{{ searchQuery ? '没有匹配的文档' : '暂无文档，点击上方按钮上传' }}</p>
      </div>

      <div v-else class="border border-border rounded-lg overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-surface-secondary text-text-secondary">
            <tr>
              <th class="text-left px-4 py-3 font-medium">文档名称</th>
              <th class="text-left px-4 py-3 font-medium">来源</th>
              <th class="text-left px-4 py-3 font-medium">上传时间</th>
              <th class="text-right px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border">
            <tr v-for="doc in filteredDocs" :key="doc.id" class="hover:bg-surface-secondary/50">
              <td class="px-4 py-3 text-text">{{ doc.name }}</td>
              <td class="px-4 py-3 text-text-secondary">{{ doc.source || '-' }}</td>
              <td class="px-4 py-3 text-text-secondary">{{ new Date(doc.uploadTime).toLocaleDateString() }}</td>
              <td class="px-4 py-3 text-right">
                <button
                  @click="handleDelete(doc)"
                  class="p-1.5 rounded hover:bg-error/10 text-text-secondary hover:text-error transition-colors"
                >
                  <Trash2 :size="16" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Upload dialog -->
    <UploadDialog
      :open="showUpload"
      @close="showUpload = false"
      @upload="handleUpload"
    />
  </div>
</template>
```

- [ ] **Step 3: Verify knowledge page renders**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Navigate to `/knowledge`. Verify:
- Header with title and upload button
- Search input
- Empty state message (backend API may not be available yet)
- Upload dialog opens on button click

- [ ] **Step 4: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/views/KnowledgeView.vue frontend/src/components/common/UploadDialog.vue
git commit -m "feat: add knowledge management page with upload dialog"
```

---

### Task 9: Build History, Status, and Settings Pages

**Files:**
- Modify: `frontend/src/views/HistoryView.vue`
- Modify: `frontend/src/views/StatusView.vue`
- Modify: `frontend/src/views/SettingsView.vue`
- Create: `frontend/src/components/common/StatusCard.vue`

- [ ] **Step 1: Create StatusCard component**

Create `frontend/src/components/common/StatusCard.vue`:

```vue
<script setup lang="ts">
import { CheckCircle, XCircle, Loader2 } from 'lucide-vue-next'

defineProps<{
  title: string
  status: 'ok' | 'error' | 'loading'
  detail?: string
}>()
</script>

<template>
  <div class="rounded-lg border border-border bg-surface-secondary p-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-medium text-text">{{ title }}</h3>
      <div class="flex items-center gap-2">
        <CheckCircle v-if="status === 'ok'" :size="18" class="text-green-500" />
        <XCircle v-else-if="status === 'error'" :size="18" class="text-error" />
        <Loader2 v-else :size="18" class="animate-spin text-text-secondary" />
      </div>
    </div>
    <p v-if="detail" class="text-xs text-text-secondary mt-1">{{ detail }}</p>
  </div>
</template>
```

- [ ] **Step 2: Implement HistoryView**

Replace `frontend/src/views/HistoryView.vue`:

```vue
<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useChatStore } from '@/stores/chat'
import { History, MessageSquare, Trash2, Clock } from 'lucide-vue-next'

const router = useRouter()
const sessionStore = useSessionStore()
const chatStore = useChatStore()

onMounted(() => {
  sessionStore.fetchSessions()
})

function openSession(sessionId: string) {
  router.push(`/chat/${sessionId}`)
}

async function deleteSession(sessionId: string) {
  if (!confirm('确定删除此会话？')) return
  await sessionStore.removeSession(sessionId)
  if (chatStore.currentSessionId === sessionId) {
    chatStore.clearMessages()
  }
}
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="max-w-4xl mx-auto p-6">
      <h1 class="text-xl font-semibold text-text flex items-center gap-2 mb-6">
        <History :size="22" />
        会话历史
      </h1>

      <div v-if="sessionStore.loading" class="text-center py-12 text-text-secondary">加载中...</div>

      <div v-else-if="sessionStore.sessions.length === 0" class="text-center py-12">
        <MessageSquare :size="48" class="mx-auto mb-3 text-text-secondary opacity-30" />
        <p class="text-text-secondary">暂无历史会话</p>
      </div>

      <div v-else class="space-y-2">
        <div
          v-for="session in sessionStore.sessions"
          :key="session.sessionId"
          class="flex items-center justify-between p-4 rounded-lg border border-border
                 hover:bg-surface-secondary/50 cursor-pointer transition-colors group"
          @click="openSession(session.sessionId)"
        >
          <div class="min-w-0 flex-1">
            <h3 class="text-sm font-medium text-text truncate">{{ session.title }}</h3>
            <p class="text-xs text-text-secondary truncate mt-1">{{ session.lastMessage }}</p>
          </div>
          <div class="flex items-center gap-3 ml-4">
            <div class="flex items-center gap-1 text-xs text-text-secondary">
              <Clock :size="12" />
              {{ new Date(session.lastTime).toLocaleDateString() }}
            </div>
            <button
              @click.stop="deleteSession(session.sessionId)"
              class="p-1.5 rounded opacity-0 group-hover:opacity-100
                     hover:bg-error/10 text-text-secondary hover:text-error transition-all"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 3: Implement StatusView**

Replace `frontend/src/views/StatusView.vue`:

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHealth, getActuatorHealth } from '@/api/rag'
import StatusCard from '@/components/common/StatusCard.vue'
import { Activity, RefreshCw } from 'lucide-vue-next'

interface HealthStatus {
  title: string
  status: 'ok' | 'error' | 'loading'
  detail: string
}

const statuses = ref<HealthStatus[]>([
  { title: '应用状态', status: 'loading', detail: '' },
  { title: 'Milvus 连接', status: 'loading', detail: '' },
  { title: 'SiliconFlow API', status: 'loading', detail: '' },
])

const lastCheck = ref('')

async function checkHealth() {
  statuses.value.forEach(s => { s.status = 'loading' })

  // Basic health check
  try {
    const health = await getHealth()
    statuses.value[0] = {
      title: '应用状态',
      status: health.status === 'OK' ? 'ok' : 'error',
      detail: `服务: ${health.service}`,
    }
  } catch {
    statuses.value[0] = {
      title: '应用状态',
      status: 'error',
      detail: '无法连接到后端服务',
    }
  }

  // Actuator health (Milvus + SiliconFlow)
  try {
    const actuator = await getActuatorHealth() as Record<string, unknown>
    const components = (actuator.components || {}) as Record<string, Record<string, string>>

    // Milvus
    const milvus = components?.milvus
    statuses.value[1] = {
      title: 'Milvus 连接',
      status: milvus?.status === 'UP' ? 'ok' : 'error',
      detail: milvus?.status === 'UP' ? '向量数据库连接正常' : '连接异常',
    }

    // SiliconFlow
    const siliconflow = components?.siliconFlow
    statuses.value[2] = {
      title: 'SiliconFlow API',
      status: siliconflow?.status === 'UP' ? 'ok' : 'error',
      detail: siliconflow?.status === 'UP' ? 'API 服务可用' : 'API 不可用',
    }
  } catch {
    statuses.value[1] = { title: 'Milvus 连接', status: 'error', detail: '无法获取状态' }
    statuses.value[2] = { title: 'SiliconFlow API', status: 'error', detail: '无法获取状态' }
  }

  lastCheck.value = new Date().toLocaleTimeString()
}

onMounted(checkHealth)
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="max-w-2xl mx-auto p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-xl font-semibold text-text flex items-center gap-2">
          <Activity :size="22" />
          系统状态
        </h1>
        <button
          @click="checkHealth"
          class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm text-text-secondary
                 hover:bg-border/50 transition-colors"
        >
          <RefreshCw :size="14" />
          刷新
        </button>
      </div>

      <div class="space-y-3">
        <StatusCard
          v-for="(s, i) in statuses"
          :key="i"
          :title="s.title"
          :status="s.status"
          :detail="s.detail"
        />
      </div>

      <p v-if="lastCheck" class="text-xs text-text-secondary mt-4 text-right">
        最后检查: {{ lastCheck }}
      </p>
    </div>
  </div>
</template>
```

- [ ] **Step 4: Implement SettingsView**

Replace `frontend/src/views/SettingsView.vue`:

```vue
<script setup lang="ts">
import { useSettingsStore } from '@/stores/settings'
import { Settings, Sun, Moon, Globe } from 'lucide-vue-next'

const settingsStore = useSettingsStore()
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="max-w-2xl mx-auto p-6">
      <h1 class="text-xl font-semibold text-text flex items-center gap-2 mb-6">
        <Settings :size="22" />
        设置
      </h1>

      <div class="space-y-6">
        <!-- API URL -->
        <div>
          <label class="block text-sm font-medium text-text mb-2">API 基础地址</label>
          <input
            :value="settingsStore.settings.apiUrl"
            @input="settingsStore.updateApiUrl(($event.target as HTMLInputElement).value)"
            type="text"
            class="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-text
                   focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
          <p class="text-xs text-text-secondary mt-1">后端 API 地址，默认 http://localhost:8080</p>
        </div>

        <!-- Theme -->
        <div>
          <label class="block text-sm font-medium text-text mb-2">主题</label>
          <div class="flex gap-2">
            <button
              @click="settingsStore.updateTheme('light')"
              class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm border transition-colors"
              :class="settingsStore.settings.theme === 'light'
                ? 'border-primary bg-primary/10 text-primary'
                : 'border-border text-text-secondary hover:bg-border/30'"
            >
              <Sun :size="16" />
              浅色
            </button>
            <button
              @click="settingsStore.updateTheme('dark')"
              class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm border transition-colors"
              :class="settingsStore.settings.theme === 'dark'
                ? 'border-primary bg-primary/10 text-primary'
                : 'border-border text-text-secondary hover:bg-border/30'"
            >
              <Moon :size="16" />
              深色
            </button>
          </div>
        </div>

        <!-- Language -->
        <div>
          <label class="block text-sm font-medium text-text mb-2">
            <Globe :size="14" class="inline mr-1" />
            语言
          </label>
          <div class="flex gap-2">
            <button
              @click="settingsStore.updateLanguage('zh')"
              class="px-4 py-2 rounded-lg text-sm border transition-colors"
              :class="settingsStore.settings.language === 'zh'
                ? 'border-primary bg-primary/10 text-primary'
                : 'border-border text-text-secondary hover:bg-border/30'"
            >
              中文
            </button>
            <button
              @click="settingsStore.updateLanguage('en')"
              class="px-4 py-2 rounded-lg text-sm border transition-colors"
              :class="settingsStore.settings.language === 'en'
                ? 'border-primary bg-primary/10 text-primary'
                : 'border-border text-text-secondary hover:bg-border/30'"
            >
              English
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 5: Verify all pages render**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Navigate through all pages and verify:
- `/history` — empty state or session list
- `/status` — three status cards with loading/ok/error states
- `/settings` — API URL input, theme toggle, language toggle
- Theme toggle switches between light/dark mode
- Settings persist after page refresh

- [ ] **Step 6: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/views/StatusView.vue frontend/src/views/HistoryView.vue frontend/src/views/SettingsView.vue frontend/src/components/common/StatusCard.vue
git commit -m "feat: add history, status, and settings pages"
```

---

### Task 10: Responsive Design and Polish

**Files:**
- Modify: `frontend/src/components/layout/Sidebar.vue`
- Modify: `frontend/src/components/layout/AppLayout.vue`

- [ ] **Step 1: Add mobile responsive behavior to Sidebar**

Update `frontend/src/components/layout/Sidebar.vue` to auto-collapse on mobile and add overlay behavior:

Add to the `<script setup>`:

```typescript
import { ref, onMounted, onUnmounted } from 'vue'

const isMobile = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) collapsed.value = true
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
```

Add mobile overlay to the template — wrap the `<aside>` with:

```vue
<!-- Mobile overlay -->
<div
  v-if="isMobile && !collapsed"
  class="fixed inset-0 bg-black/30 z-40 md:hidden"
  @click="collapsed = true"
/>

<aside
  class="flex flex-col h-screen border-r border-border bg-surface-secondary transition-all duration-300 z-50"
  :class="[
    collapsed ? 'w-16' : 'w-64',
    isMobile && collapsed ? '-translate-x-full md:translate-x-0' : '',
  ]"
>
  <!-- ... existing content ... -->
</aside>
```

- [ ] **Step 2: Add mobile hamburger menu to AppLayout**

Update `frontend/src/components/layout/AppLayout.vue` to expose a toggle for mobile:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import { Menu } from 'lucide-vue-next'

const sidebarRef = ref<InstanceType<typeof Sidebar>>()
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-surface">
    <Sidebar ref="sidebarRef" />
    <main class="flex-1 overflow-hidden relative">
      <!-- Mobile menu button -->
      <button
        @click="sidebarRef?.toggle()"
        class="md:hidden absolute top-3 left-3 z-30 p-2 rounded-lg bg-surface border border-border
               text-text-secondary hover:bg-surface-secondary"
      >
        <Menu :size="18" />
      </button>
      <router-view />
    </main>
  </div>
</template>
```

Expose `toggle` from Sidebar by adding to its `<script setup>`:

```typescript
defineExpose({ toggle: () => { collapsed.value = !collapsed.value } })
```

- [ ] **Step 3: Verify responsive behavior**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

- Resize browser to mobile width (< 768px)
- Sidebar should auto-collapse
- Hamburger menu button should appear
- Clicking hamburger opens sidebar with overlay
- Clicking overlay closes sidebar

- [ ] **Step 4: Final type check and build verification**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx vue-tsc --build --noEmit && npm run build
```

Expected: No TypeScript errors, build succeeds.

- [ ] **Step 5: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/
git commit -m "feat: add responsive design with mobile sidebar support"
```
