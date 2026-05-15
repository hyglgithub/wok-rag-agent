# Frontend Implementation Plan (React)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a ChatGPT-style React frontend for the wok-rag-agent RAG system with streaming chat, knowledge management, session history, status monitoring, and settings.

**Architecture:** SPA with sidebar navigation layout. Chat page uses SSE streaming via `fetch` + `ReadableStream`. State managed by Zustand. Styling via Tailwind CSS v4 + shadcn/ui (Radix UI) for accessible, customizable components.

**Tech Stack:** React 19, TypeScript, Vite, Zustand, React Router v7, Tailwind CSS v4, shadcn/ui, lucide-react

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
│   ├── ui/                    # shadcn/ui components (auto-generated)
│   │   ├── button.tsx
│   │   ├── dialog.tsx
│   │   ├── input.tsx
│   │   ├── table.tsx
│   │   ├── badge.tsx
│   │   ├── card.tsx
│   │   ├── scroll-area.tsx
│   │   └── tooltip.tsx
│   ├── layout/
│   │   ├── AppLayout.tsx      # sidebar + main content area
│   │   └── Sidebar.tsx        # navigation + session list
│   ├── chat/
│   │   ├── MessageBubble.tsx  # single message (user or assistant)
│   │   ├── ChatInput.tsx      # textarea + send button
│   │   └── CitationCard.tsx   # expandable citation display
│   └── common/
│       ├── StatusCard.tsx     # health status indicator card
│       └── UploadDialog.tsx   # file upload modal
├── pages/
│   ├── ChatPage.tsx           # main chat page
│   ├── KnowledgePage.tsx      # document management
│   ├── HistoryPage.tsx        # session list
│   ├── StatusPage.tsx         # health monitoring
│   └── SettingsPage.tsx       # app settings
├── stores/
│   ├── chatStore.ts           # messages, streaming state, current session
│   ├── sessionStore.ts        # session list management
│   └── settingsStore.ts       # API URL, theme, language
├── types/
│   └── index.ts               # all TypeScript interfaces
├── App.tsx                    # router setup
├── main.tsx                   # entry point
└── index.css                  # Tailwind imports + global styles
```

---

### Task 1: Install Dependencies and Configure Tailwind CSS + shadcn/ui

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.ts`
- Modify: `frontend/tsconfig.app.json`
- Modify: `frontend/src/index.css`
- Modify: `frontend/index.html`
- Modify: `frontend/src/main.tsx`
- Create: `frontend/src/lib/utils.ts` (shadcn/ui utility)
- Create: `frontend/components.json` (shadcn/ui config)
- Auto-generated: `frontend/src/components/ui/*.tsx` (shadcn/ui components)
- Delete: `frontend/src/App.css`, `frontend/src/assets/hero.png`, `frontend/src/assets/react.svg`, `frontend/src/assets/vite.svg`

- [ ] **Step 1: Install npm dependencies**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend
npm install react-router-dom zustand lucide-react
npm install -D tailwindcss @tailwindcss/vite
```

- [ ] **Step 2: Configure Vite with Tailwind plugin and API proxy**

Replace `frontend/vite.config.ts`:

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
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

- [ ] **Step 3: Add path alias to tsconfig.app.json**

Replace `frontend/tsconfig.app.json`:

```json
{
  "compilerOptions": {
    "tsBuildInfoFile": "./node_modules/.tmp/tsconfig.app.tsbuildinfo",
    "target": "es2023",
    "lib": ["ES2023", "DOM"],
    "module": "esnext",
    "types": ["vite/client"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "verbatimModuleSyntax": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "react-jsx",
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "erasableSyntaxOnly": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src"]
}
```

- [ ] **Step 4: Initialize shadcn/ui**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend
npx shadcn@latest init -d -y
```

This will:
- Create `components.json` (shadcn config)
- Create `src/lib/utils.ts` (with `cn()` helper using clsx + tailwind-merge)
- Modify `src/index.css` (add shadcn CSS variables)
- Install `clsx`, `tailwind-merge`, `class-variance-authority`

After init, verify `src/lib/utils.ts` exists:

```typescript
import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

- [ ] **Step 5: Add shadcn/ui components**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend
npx shadcn@latest add button dialog input table badge card scroll-area tooltip sonner -y
```

This creates component files in `src/components/ui/`. Verify they exist:

```bash
ls E:/idea_workspace/wok-rag-agent/frontend/src/components/ui/
```

Expected: `button.tsx`, `dialog.tsx`, `input.tsx`, `table.tsx`, `badge.tsx`, `card.tsx`, `scroll-area.tsx`, `tooltip.tsx`, `sonner.tsx`

- [ ] **Step 6: Update index.css with custom theme tokens**

The shadcn init already creates a base `index.css`. Add custom tokens after the shadcn variables. Append to `frontend/src/index.css` (after the shadcn-generated content):

```css
@custom-variant dark (&:where(.dark, .dark *));

/* Custom tokens for chat UI */
@theme {
  --color-primary: #10a37f;
  --color-primary-hover: #0e8c6d;
  --color-user-bubble: #f0f0f0;
}

.dark {
  --color-user-bubble: #2f2f2f;
}

::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: hsl(var(--border));
  border-radius: 3px;
}
```

- [ ] **Step 7: Update index.html**

Replace `frontend/index.html`:

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Wok RAG Agent</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 8: Remove unused boilerplate files**

```bash
rm -f E:/idea_workspace/wok-rag-agent/frontend/src/App.css
rm -f E:/idea_workspace/wok-rag-agent/frontend/src/assets/hero.png
rm -f E:/idea_workspace/wok-rag-agent/frontend/src/assets/react.svg
rm -f E:/idea_workspace/wok-rag-agent/frontend/src/assets/vite.svg
```

- [ ] **Step 9: Update main.tsx to be clean**

Replace `frontend/src/main.tsx`:

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

- [ ] **Step 10: Verify dev server starts**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Expected: Dev server starts on `http://localhost:5173` without errors.

- [ ] **Step 11: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/
git commit -m "feat: setup Tailwind CSS v4, shadcn/ui, Zustand, and React Router"
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
cd E:/idea_workspace/wok-rag-agent/frontend && npx tsc -b
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
import { useSettingsStore } from '@/stores/settingsStore'

function getBaseUrl(): string {
  return useSettingsStore.getState().settings.apiUrl
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

export function createSSEStream(
  path: string,
  body: unknown
): ReadableStream<Uint8Array> {
  const url = `${getBaseUrl()}${path}`

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
          const encoder = new TextEncoder()
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
        const encoder = new TextEncoder()
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
import { apiFetch, createSSEStream } from './client'
import type { QueryRequest, RagResponse, HealthResponse, Session, SessionMessage } from '@/types'

export function queryRag(request: QueryRequest): Promise<RagResponse> {
  return apiFetch<RagResponse>('/api/rag/query', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function streamRag(request: QueryRequest): ReadableStream<Uint8Array> {
  return createSSEStream('/api/rag/stream', request)
}

export function getHealth(): Promise<HealthResponse> {
  return apiFetch<HealthResponse>('/api/rag/health')
}

export function getActuatorHealth(): Promise<Record<string, unknown>> {
  return apiFetch<Record<string, unknown>>('/actuator/health')
}

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
import { useSettingsStore } from '@/stores/settingsStore'

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

  const baseUrl = useSettingsStore.getState().settings.apiUrl
  const response = await fetch(`${baseUrl}/api/documents/upload`, {
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
cd E:/idea_workspace/wok-rag-agent/frontend && npx tsc -b
```

Expected: No errors.

- [ ] **Step 5: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/api/
git commit -m "feat: add API client layer with fetch wrapper and SSE streaming"
```

---

### Task 4: Create Zustand Stores

**Files:**
- Create: `frontend/src/stores/settingsStore.ts`
- Create: `frontend/src/stores/chatStore.ts`
- Create: `frontend/src/stores/sessionStore.ts`

- [ ] **Step 1: Create settings store**

Create `frontend/src/stores/settingsStore.ts`:

```typescript
import { create } from 'zustand'
import type { Settings } from '@/types'

const STORAGE_KEY = 'wok-rag-settings'

function loadSettings(): Settings {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    try {
      return JSON.parse(stored) as Settings
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

function persist(settings: Settings) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
}

interface SettingsState {
  settings: Settings
  updateTheme: (theme: 'light' | 'dark') => void
  updateApiUrl: (url: string) => void
  updateLanguage: (lang: 'zh' | 'en') => void
}

export const useSettingsStore = create<SettingsState>()((set) => {
  const initial = loadSettings()

  // Apply theme on init
  if (initial.theme === 'dark') {
    document.documentElement.classList.add('dark')
  }

  return {
    settings: initial,
    updateTheme: (theme) =>
      set((state) => {
        if (theme === 'dark') {
          document.documentElement.classList.add('dark')
        } else {
          document.documentElement.classList.remove('dark')
        }
        const next = { ...state.settings, theme }
        persist(next)
        return { settings: next }
      }),
    updateApiUrl: (url) =>
      set((state) => {
        const next = { ...state.settings, apiUrl: url }
        persist(next)
        return { settings: next }
      }),
    updateLanguage: (lang) =>
      set((state) => {
        const next = { ...state.settings, language: lang }
        persist(next)
        return { settings: next }
      }),
  }
})
```

- [ ] **Step 2: Create chat store**

Create `frontend/src/stores/chatStore.ts`:

```typescript
import { create } from 'zustand'
import type { Message, RagResponse } from '@/types'
import { streamRag } from '@/api/rag'

interface ChatState {
  messages: Message[]
  currentSessionId: string | null
  isStreaming: boolean
  streamingContent: string
  sendMessage: (question: string) => Promise<void>
  clearMessages: () => void
  loadSession: (sessionId: string, messages: Message[]) => void
}

function generateId(): string {
  return crypto.randomUUID()
}

function generateSessionId(): string {
  return `session-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export const useChatStore = create<ChatState>()((set, get) => ({
  messages: [],
  currentSessionId: null,
  isStreaming: false,
  streamingContent: '',

  clearMessages: () =>
    set({ messages: [], currentSessionId: null }),

  loadSession: (sessionId, messages) =>
    set({ currentSessionId: sessionId, messages }),

  sendMessage: async (question: string) => {
    const state = get()
    if (state.isStreaming || !question.trim()) return

    const userMessage: Message = {
      id: generateId(),
      role: 'user',
      content: question.trim(),
      citations: [],
      timestamp: Date.now(),
    }

    const assistantMessage: Message = {
      id: generateId(),
      role: 'assistant',
      content: '',
      citations: [],
      timestamp: Date.now(),
    }

    const sessionId = state.currentSessionId || generateSessionId()

    set({
      messages: [...state.messages, userMessage, assistantMessage],
      currentSessionId: sessionId,
      isStreaming: true,
      streamingContent: '',
    })

    try {
      const stream = streamRag({
        question: question.trim(),
        sessionId,
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

            set((state) => {
              const msgs = [...state.messages]
              const lastMsg = msgs[msgs.length - 1]
              if (!lastMsg || lastMsg.role !== 'assistant') return state

              if (eventType === 'token') {
                lastMsg.content += data
                return { messages: msgs, streamingContent: lastMsg.content }
              } else if (eventType === 'done') {
                try {
                  const response = JSON.parse(data) as RagResponse
                  lastMsg.content = response.answer
                  lastMsg.citations = response.citations || []
                  return {
                    messages: msgs,
                    currentSessionId: response.sessionId || state.currentSessionId,
                  }
                } catch {
                  return state
                }
              } else if (eventType === 'error') {
                try {
                  const errData = JSON.parse(data) as { message: string }
                  lastMsg.error = errData.message || 'Stream error'
                } catch {
                  lastMsg.error = 'Stream error'
                }
                return { messages: msgs }
              }

              return state
            })

            eventType = 'token'
          }
        }
      }
    } catch (err) {
      set((state) => {
        const msgs = [...state.messages]
        const lastMsg = msgs[msgs.length - 1]
        if (lastMsg && lastMsg.role === 'assistant') {
          lastMsg.error = err instanceof Error ? err.message : 'Unknown error'
        }
        return { messages: msgs }
      })
    } finally {
      set({ isStreaming: false, streamingContent: '' })
    }
  },
}))
```

- [ ] **Step 3: Create session store**

Create `frontend/src/stores/sessionStore.ts`:

```typescript
import { create } from 'zustand'
import type { Session } from '@/types'
import { getSessions, deleteSession as apiDeleteSession } from '@/api/rag'

interface SessionState {
  sessions: Session[]
  loading: boolean
  fetchSessions: () => Promise<void>
  removeSession: (sessionId: string) => Promise<void>
  addLocalSession: (sessionId: string, title: string) => void
}

export const useSessionStore = create<SessionState>()((set) => ({
  sessions: [],
  loading: false,

  fetchSessions: async () => {
    set({ loading: true })
    try {
      const sessions = await getSessions()
      set({ sessions })
    } finally {
      set({ loading: false })
    }
  },

  removeSession: async (sessionId: string) => {
    try {
      await apiDeleteSession(sessionId)
      set((state) => ({
        sessions: state.sessions.filter((s) => s.sessionId !== sessionId),
      }))
    } catch {
      // Silently fail if backend doesn't support this yet
    }
  },

  addLocalSession: (sessionId: string, title: string) => {
    set((state) => {
      if (state.sessions.find((s) => s.sessionId === sessionId)) return state
      return {
        sessions: [
          {
            sessionId,
            title,
            lastMessage: '',
            lastTime: new Date().toISOString(),
            messageCount: 0,
          },
          ...state.sessions,
        ],
      }
    })
  },
}))
```

- [ ] **Step 4: Verify TypeScript compiles**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx tsc -b
```

Expected: No errors.

- [ ] **Step 5: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/stores/
git commit -m "feat: add Zustand stores for settings, chat, and session management"
```

---

### Task 5: Build Layout and Router

**Files:**
- Create: `frontend/src/components/layout/Sidebar.tsx`
- Create: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Create Sidebar component**

Create `frontend/src/components/layout/Sidebar.tsx`:

```tsx
import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useChatStore } from '@/stores/chatStore'
import { useSessionStore } from '@/stores/sessionStore'
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
  Menu,
} from 'lucide-react'

export default function Sidebar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { currentSessionId, clearMessages } = useChatStore()
  const { sessions, removeSession } = useSessionStore()
  const [collapsed, setCollapsed] = useState(false)
  const [isMobile, setIsMobile] = useState(false)

  useEffect(() => {
    const check = () => {
      const mobile = window.innerWidth < 768
      setIsMobile(mobile)
      if (mobile) setCollapsed(true)
    }
    check()
    window.addEventListener('resize', check)
    return () => window.removeEventListener('resize', check)
  }, [])

  const navItems = [
    { path: '/knowledge', label: '知识库', icon: Database },
    { path: '/history', label: '会话历史', icon: History },
    { path: '/status', label: '系统状态', icon: Activity },
    { path: '/settings', label: '设置', icon: Settings },
  ]

  function newChat() {
    clearMessages()
    navigate('/chat')
  }

  function isActive(path: string): boolean {
    return location.pathname === path || location.pathname.startsWith(path + '/')
  }

  return (
    <>
      {/* Mobile overlay */}
      {isMobile && !collapsed && (
        <div
          className="fixed inset-0 bg-black/30 z-40 md:hidden"
          onClick={() => setCollapsed(true)}
        />
      )}

      {/* Mobile hamburger */}
      {isMobile && collapsed && (
        <button
          onClick={() => setCollapsed(false)}
          className="fixed top-3 left-3 z-30 p-2 rounded-lg bg-surface border border-border text-text-secondary hover:bg-surface-secondary"
        >
          <Menu size={18} />
        </button>
      )}

      <aside
        className={`flex flex-col h-screen border-r border-border bg-surface-secondary transition-all duration-300 z-50 ${
          collapsed ? 'w-16' : 'w-64'
        } ${isMobile && collapsed ? '-translate-x-full' : ''}`}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-3 border-b border-border">
          {!collapsed && (
            <span className="text-sm font-semibold text-text truncate">Wok RAG Agent</span>
          )}
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="p-1.5 rounded-lg hover:bg-border/50 text-text-secondary"
          >
            {collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
          </button>
        </div>

        {/* New Chat Button */}
        <div className="p-2">
          <button
            onClick={newChat}
            className={`flex items-center gap-2 w-full p-2.5 rounded-lg border border-border hover:bg-border/50 text-text text-sm transition-colors ${
              collapsed ? 'justify-center' : ''
            }`}
          >
            <Plus size={18} />
            {!collapsed && <span>新建对话</span>}
          </button>
        </div>

        {/* Session List */}
        {!collapsed && (
          <div className="flex-1 overflow-y-auto px-2 space-y-0.5">
            {sessions.length === 0 ? (
              <div className="p-3 text-text-secondary text-xs text-center">暂无会话</div>
            ) : (
              sessions.map((session) => (
                <button
                  key={session.sessionId}
                  onClick={() => navigate(`/chat/${session.sessionId}`)}
                  className={`flex items-center justify-between w-full p-2 rounded-lg text-sm text-left hover:bg-border/50 transition-colors group ${
                    currentSessionId === session.sessionId ? 'bg-border/50' : ''
                  }`}
                >
                  <div className="flex items-center gap-2 min-w-0">
                    <MessageSquare size={16} className="shrink-0 text-text-secondary" />
                    <span className="truncate text-text">{session.title}</span>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      void removeSession(session.sessionId)
                    }}
                    className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-error/10 text-text-secondary hover:text-error"
                  >
                    <Trash2 size={14} />
                  </button>
                </button>
              ))
            )}
          </div>
        )}

        {/* Navigation */}
        <nav className="border-t border-border p-2 space-y-0.5">
          {navItems.map((item) => (
            <button
              key={item.path}
              onClick={() => {
                navigate(item.path)
                if (isMobile) setCollapsed(true)
              }}
              className={`flex items-center gap-2 w-full p-2 rounded-lg text-sm transition-colors ${
                collapsed ? 'justify-center' : ''
              } ${
                isActive(item.path)
                  ? 'bg-border/50 text-text'
                  : 'text-text-secondary hover:bg-border/30'
              }`}
            >
              <item.icon size={18} />
              {!collapsed && <span>{item.label}</span>}
            </button>
          ))}
        </nav>
      </aside>
    </>
  )
}
```

- [ ] **Step 2: Create AppLayout component**

Create `frontend/src/components/layout/AppLayout.tsx`:

```tsx
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'

export default function AppLayout() {
  return (
    <div className="flex h-screen overflow-hidden bg-surface">
      <Sidebar />
      <main className="flex-1 overflow-hidden">
        <Outlet />
      </main>
    </div>
  )
}
```

- [ ] **Step 3: Set up router in App.tsx**

Replace `frontend/src/App.tsx`:

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AppLayout from '@/components/layout/AppLayout'
import ChatPage from '@/pages/ChatPage'
import KnowledgePage from '@/pages/KnowledgePage'
import HistoryPage from '@/pages/HistoryPage'
import StatusPage from '@/pages/StatusPage'
import SettingsPage from '@/pages/SettingsPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/chat" replace />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/chat/:sessionId" element={<ChatPage />} />
          <Route path="/knowledge" element={<KnowledgePage />} />
          <Route path="/history" element={<HistoryPage />} />
          <Route path="/status" element={<StatusPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
```

- [ ] **Step 4: Create placeholder page components**

Create `frontend/src/pages/ChatPage.tsx`:

```tsx
export default function ChatPage() {
  return (
    <div className="flex items-center justify-center h-full text-text-secondary">
      Chat Page - Coming soon
    </div>
  )
}
```

Create `frontend/src/pages/KnowledgePage.tsx`:

```tsx
export default function KnowledgePage() {
  return (
    <div className="flex items-center justify-center h-full text-text-secondary">
      Knowledge Page - Coming soon
    </div>
  )
}
```

Create `frontend/src/pages/HistoryPage.tsx`:

```tsx
export default function HistoryPage() {
  return (
    <div className="flex items-center justify-center h-full text-text-secondary">
      History Page - Coming soon
    </div>
  )
}
```

Create `frontend/src/pages/StatusPage.tsx`:

```tsx
export default function StatusPage() {
  return (
    <div className="flex items-center justify-center h-full text-text-secondary">
      Status Page - Coming soon
    </div>
  )
}
```

Create `frontend/src/pages/SettingsPage.tsx`:

```tsx
export default function SettingsPage() {
  return (
    <div className="flex items-center justify-center h-full text-text-secondary">
      Settings Page - Coming soon
    </div>
  )
}
```

- [ ] **Step 5: Verify dev server and routing**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Open `http://localhost:5173`. Verify:
- Sidebar appears with navigation
- Clicking nav items changes the main content area
- "New Chat" button navigates to `/chat`
- Sidebar collapses/expands
- Mobile view (< 768px) auto-collapses sidebar

- [ ] **Step 6: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/
git commit -m "feat: add sidebar layout, router, and placeholder pages"
```

---

### Task 6: Build Chat Page — Messages and Input

**Files:**
- Create: `frontend/src/components/chat/CitationCard.tsx`
- Create: `frontend/src/components/chat/MessageBubble.tsx`
- Create: `frontend/src/components/chat/ChatInput.tsx`
- Modify: `frontend/src/pages/ChatPage.tsx`

- [ ] **Step 1: Create CitationCard component**

Create `frontend/src/components/chat/CitationCard.tsx`:

```tsx
import { useState } from 'react'
import type { Citation } from '@/types'
import { ChevronDown, ChevronUp, ExternalLink } from 'lucide-react'

interface Props {
  citation: Citation
}

export default function CitationCard({ citation }: Props) {
  const [expanded, setExpanded] = useState(false)

  return (
    <div className="rounded-lg bg-surface border border-border text-xs">
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center justify-between w-full p-2 hover:bg-border/30 transition-colors"
      >
        <div className="flex items-center gap-2">
          <span className="w-5 h-5 rounded bg-primary/10 text-primary flex items-center justify-center text-xs font-medium">
            {citation.index}
          </span>
          <span className="text-text-secondary truncate">{citation.source}</span>
        </div>
        {expanded ? <ChevronUp size={14} className="text-text-secondary" /> : <ChevronDown size={14} className="text-text-secondary" />}
      </button>

      {expanded && (
        <div className="px-2 pb-2 border-t border-border">
          <p className="mt-2 text-text-secondary leading-relaxed">{citation.chunkContent}</p>
          {citation.sourceUrl && (
            <a
              href={citation.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 mt-2 text-primary hover:underline"
            >
              <ExternalLink size={12} />
              查看来源
            </a>
          )}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Create MessageBubble component**

Create `frontend/src/components/chat/MessageBubble.tsx`:

```tsx
import type { Message } from '@/types'
import CitationCard from './CitationCard'
import { User, Bot } from 'lucide-react'

interface Props {
  message: Message
}

export default function MessageBubble({ message }: Props) {
  const isUser = message.role === 'user'

  return (
    <div className={`flex gap-3 py-4 px-4 ${isUser ? 'justify-end' : 'justify-start'}`}>
      {/* Avatar (assistant only) */}
      {!isUser && (
        <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center shrink-0">
          <Bot size={16} className="text-white" />
        </div>
      )}

      {/* Message content */}
      <div
        className={`max-w-[70%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${
          isUser ? 'bg-user-bubble text-text' : 'bg-surface-secondary text-text'
        }`}
      >
        {message.error ? (
          <div className="text-error">{message.error}</div>
        ) : (
          <div className="whitespace-pre-wrap">{message.content}</div>
        )}

        {/* Citations */}
        {message.citations.length > 0 && (
          <div className="mt-3 pt-3 border-t border-border space-y-2">
            {message.citations.map((citation) => (
              <CitationCard key={citation.index} citation={citation} />
            ))}
          </div>
        )}
      </div>

      {/* Avatar (user only) */}
      {isUser && (
        <div className="w-8 h-8 rounded-full bg-user-bubble flex items-center justify-center shrink-0">
          <User size={16} className="text-text-secondary" />
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 3: Create ChatInput component**

Create `frontend/src/components/chat/ChatInput.tsx`:

```tsx
import { useState, useRef, useEffect, useCallback } from 'react'
import { Send } from 'lucide-react'

interface Props {
  disabled: boolean
  onSend: (question: string) => void
}

export default function ChatInput({ disabled, onSend }: Props) {
  const [input, setInput] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const adjustHeight = useCallback(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }, [])

  useEffect(() => {
    textareaRef.current?.focus()
  }, [])

  function handleSend() {
    if (input.trim()) {
      onSend(input)
      setInput('')
      requestAnimationFrame(() => adjustHeight())
    }
  }

  function handleKeydown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="border-t border-border bg-surface p-4">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-end gap-2 rounded-xl border border-border bg-surface-secondary p-2">
          <textarea
            ref={textareaRef}
            value={input}
            onChange={(e) => {
              setInput(e.target.value)
              adjustHeight()
            }}
            onKeyDown={handleKeydown}
            placeholder="输入你的问题... (Enter 发送, Shift+Enter 换行)"
            rows={1}
            className="flex-1 bg-transparent resize-none px-2 py-1.5 text-sm text-text placeholder:text-text-secondary focus:outline-none"
            style={{ maxHeight: '200px' }}
          />
          <button
            onClick={handleSend}
            disabled={disabled || !input.trim()}
            className="p-2 rounded-lg bg-primary text-white hover:bg-primary-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <Send size={18} />
          </button>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Implement ChatPage**

Replace `frontend/src/pages/ChatPage.tsx`:

```tsx
import { useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { useChatStore } from '@/stores/chatStore'
import { useSessionStore } from '@/stores/sessionStore'
import MessageBubble from '@/components/chat/MessageBubble'
import ChatInput from '@/components/chat/ChatInput'
import { MessageSquare, Loader2 } from 'lucide-react'

export default function ChatPage() {
  const { sessionId } = useParams()
  const { messages, isStreaming, streamingContent, sendMessage, loadSession } = useChatStore()
  const { addLocalSession } = useSessionStore()
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Load session from route param
  useEffect(() => {
    if (sessionId) {
      // TODO: load session messages from backend when API is available
      useChatStore.setState({ currentSessionId: sessionId })
    }
  }, [sessionId])

  // Scroll to bottom on new messages or streaming
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length, streamingContent])

  async function handleSend(question: string) {
    await sendMessage(question)
    const currentId = useChatStore.getState().currentSessionId
    if (currentId) {
      addLocalSession(currentId, question.slice(0, 30) + (question.length > 30 ? '...' : ''))
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* Messages area */}
      <div className="flex-1 overflow-y-auto">
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-text-secondary">
            <MessageSquare size={48} className="mb-4 opacity-30" />
            <p className="text-lg font-medium">有什么可以帮你的？</p>
            <p className="text-sm mt-1">基于知识库的智能问答助手</p>
          </div>
        ) : (
          <div className="max-w-3xl mx-auto py-4">
            {messages.map((msg) => (
              <MessageBubble key={msg.id} message={msg} />
            ))}

            {/* Streaming indicator */}
            {isStreaming && !streamingContent && (
              <div className="flex items-center gap-2 px-4 py-2 text-text-secondary text-sm">
                <Loader2 size={16} className="animate-spin" />
                <span>思考中...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Input area */}
      <ChatInput disabled={isStreaming} onSend={handleSend} />
    </div>
  )
}
```

- [ ] **Step 5: Verify TypeScript compiles**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx tsc -b
```

Expected: No errors.

- [ ] **Step 6: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/
git commit -m "feat: add chat page with message bubbles, input, and citation cards"
```

---

### Task 7: Build Knowledge Management Page

**Files:**
- Create: `frontend/src/components/common/UploadDialog.tsx`
- Modify: `frontend/src/pages/KnowledgePage.tsx`

- [ ] **Step 1: Create UploadDialog component**

Create `frontend/src/components/common/UploadDialog.tsx`:

```tsx
import { useState, useRef } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Upload, FileText } from 'lucide-react'

interface Props {
  open: boolean
  onClose: () => void
  onUpload: (file: File, source: string) => void
}

export default function UploadDialog({ open, onClose, onUpload }: Props) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [source, setSource] = useState('')
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files[0]
    if (file) setSelectedFile(file)
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) setSelectedFile(file)
  }

  function handleUpload() {
    if (selectedFile) {
      onUpload(selectedFile, source)
      setSelectedFile(null)
      setSource('')
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>上传文档</DialogTitle>
        </DialogHeader>

        {/* Drop zone */}
        <div
          onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
            dragOver ? 'border-primary bg-primary/5' : 'border-border'
          }`}
        >
          {selectedFile ? (
            <FileText size={32} className="mx-auto mb-2 text-primary" />
          ) : (
            <Upload size={32} className="mx-auto mb-2 text-muted-foreground" />
          )}
          <p className="text-sm">
            {selectedFile ? selectedFile.name : '拖拽文件到此处或点击选择'}
          </p>
          <p className="text-xs text-muted-foreground mt-1">支持 PDF、TXT、DOCX 等格式</p>
          <input
            ref={fileInputRef}
            type="file"
            className="hidden"
            onChange={handleFileSelect}
            accept=".pdf,.txt,.docx,.doc,.md"
          />
        </div>

        {/* Source input */}
        <div className="mt-2">
          <label className="block text-sm text-muted-foreground mb-1">来源描述（可选）</label>
          <Input
            value={source}
            onChange={(e) => setSource(e.target.value)}
            placeholder="例如：官网、客服中心"
          />
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>取消</Button>
          <Button onClick={handleUpload} disabled={!selectedFile}>上传</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
```

- [ ] **Step 2: Implement KnowledgePage**

Replace `frontend/src/pages/KnowledgePage.tsx`:

```tsx
import { useState, useEffect, useMemo } from 'react'
import type { DocumentInfo } from '@/types'
import { getDocuments, uploadDocument, deleteDocument } from '@/api/document'
import UploadDialog from '@/components/common/UploadDialog'
import { Database, Trash2, Upload, Search, FileText } from 'lucide-react'

export default function KnowledgePage() {
  const [documents, setDocuments] = useState<DocumentInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [showUpload, setShowUpload] = useState(false)

  const filteredDocs = useMemo(() => {
    if (!searchQuery) return documents
    const q = searchQuery.toLowerCase()
    return documents.filter(
      (d) => d.name.toLowerCase().includes(q) || d.source.toLowerCase().includes(q)
    )
  }, [documents, searchQuery])

  useEffect(() => {
    setLoading(true)
    getDocuments()
      .then(setDocuments)
      .finally(() => setLoading(false))
  }, [])

  async function handleUpload(file: File, source: string) {
    try {
      const result = await uploadDocument(file, source)
      setDocuments((prev) => [result, ...prev])
      setShowUpload(false)
    } catch (err) {
      alert('上传失败: ' + (err instanceof Error ? err.message : '未知错误'))
    }
  }

  async function handleDelete(doc: DocumentInfo) {
    if (!confirm(`确定删除 "${doc.name}"？`)) return
    try {
      await deleteDocument(doc.id)
      setDocuments((prev) => prev.filter((d) => d.id !== doc.id))
    } catch {
      alert('删除失败')
    }
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-4xl mx-auto p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-semibold text-text flex items-center gap-2">
              <Database size={22} />
              知识库管理
            </h1>
            <p className="text-sm text-text-secondary mt-1">管理已导入的文档</p>
          </div>
          <button
            onClick={() => setShowUpload(true)}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-white text-sm hover:bg-primary-hover transition-colors"
          >
            <Upload size={16} />
            上传文档
          </button>
        </div>

        {/* Search */}
        <div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary" />
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            type="text"
            placeholder="搜索文档..."
            className="w-full rounded-lg border border-border bg-surface-secondary pl-9 pr-3 py-2 text-sm text-text placeholder:text-text-secondary focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>

        {/* Document list */}
        {loading ? (
          <div className="text-center py-12 text-text-secondary">加载中...</div>
        ) : filteredDocs.length === 0 ? (
          <div className="text-center py-12">
            <FileText size={48} className="mx-auto mb-3 text-text-secondary opacity-30" />
            <p className="text-text-secondary">{searchQuery ? '没有匹配的文档' : '暂无文档，点击上方按钮上传'}</p>
          </div>
        ) : (
          <div className="border border-border rounded-lg overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-surface-secondary text-text-secondary">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">文档名称</th>
                  <th className="text-left px-4 py-3 font-medium">来源</th>
                  <th className="text-left px-4 py-3 font-medium">上传时间</th>
                  <th className="text-right px-4 py-3 font-medium">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filteredDocs.map((doc) => (
                  <tr key={doc.id} className="hover:bg-surface-secondary/50">
                    <td className="px-4 py-3 text-text">{doc.name}</td>
                    <td className="px-4 py-3 text-text-secondary">{doc.source || '-'}</td>
                    <td className="px-4 py-3 text-text-secondary">
                      {new Date(doc.uploadTime).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => void handleDelete(doc)}
                        className="p-1.5 rounded hover:bg-error/10 text-text-secondary hover:text-error transition-colors"
                      >
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <UploadDialog
        open={showUpload}
        onClose={() => setShowUpload(false)}
        onUpload={(file, source) => void handleUpload(file, source)}
      />
    </div>
  )
}
```

- [ ] **Step 3: Verify TypeScript compiles**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx tsc -b
```

Expected: No errors.

- [ ] **Step 4: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/
git commit -m "feat: add knowledge management page with upload dialog"
```

---

### Task 8: Build History, Status, and Settings Pages

**Files:**
- Create: `frontend/src/components/common/StatusCard.tsx`
- Modify: `frontend/src/pages/HistoryPage.tsx`
- Modify: `frontend/src/pages/StatusPage.tsx`
- Modify: `frontend/src/pages/SettingsPage.tsx`

- [ ] **Step 1: Create StatusCard component**

Create `frontend/src/components/common/StatusCard.tsx`:

```tsx
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'

interface Props {
  title: string
  status: 'ok' | 'error' | 'loading'
  detail?: string
}

export default function StatusCard({ title, status, detail }: Props) {
  return (
    <div className="rounded-lg border border-border bg-surface-secondary p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-medium text-text">{title}</h3>
        {status === 'ok' && <CheckCircle size={18} className="text-green-500" />}
        {status === 'error' && <XCircle size={18} className="text-error" />}
        {status === 'loading' && <Loader2 size={18} className="animate-spin text-text-secondary" />}
      </div>
      {detail && <p className="text-xs text-text-secondary mt-1">{detail}</p>}
    </div>
  )
}
```

- [ ] **Step 2: Implement HistoryPage**

Replace `frontend/src/pages/HistoryPage.tsx`:

```tsx
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSessionStore } from '@/stores/sessionStore'
import { useChatStore } from '@/stores/chatStore'
import { History, MessageSquare, Trash2, Clock } from 'lucide-react'

export default function HistoryPage() {
  const navigate = useNavigate()
  const { sessions, loading, fetchSessions, removeSession } = useSessionStore()
  const { currentSessionId, clearMessages } = useChatStore()

  useEffect(() => {
    void fetchSessions()
  }, [fetchSessions])

  async function handleDelete(sessionId: string) {
    if (!confirm('确定删除此会话？')) return
    await removeSession(sessionId)
    if (currentSessionId === sessionId) {
      clearMessages()
    }
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-4xl mx-auto p-6">
        <h1 className="text-xl font-semibold text-text flex items-center gap-2 mb-6">
          <History size={22} />
          会话历史
        </h1>

        {loading ? (
          <div className="text-center py-12 text-text-secondary">加载中...</div>
        ) : sessions.length === 0 ? (
          <div className="text-center py-12">
            <MessageSquare size={48} className="mx-auto mb-3 text-text-secondary opacity-30" />
            <p className="text-text-secondary">暂无历史会话</p>
          </div>
        ) : (
          <div className="space-y-2">
            {sessions.map((session) => (
              <div
                key={session.sessionId}
                onClick={() => navigate(`/chat/${session.sessionId}`)}
                className="flex items-center justify-between p-4 rounded-lg border border-border hover:bg-surface-secondary/50 cursor-pointer transition-colors group"
              >
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm font-medium text-text truncate">{session.title}</h3>
                  <p className="text-xs text-text-secondary truncate mt-1">{session.lastMessage}</p>
                </div>
                <div className="flex items-center gap-3 ml-4">
                  <div className="flex items-center gap-1 text-xs text-text-secondary">
                    <Clock size={12} />
                    {new Date(session.lastTime).toLocaleDateString()}
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      void handleDelete(session.sessionId)
                    }}
                    className="p-1.5 rounded opacity-0 group-hover:opacity-100 hover:bg-error/10 text-text-secondary hover:text-error transition-all"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Implement StatusPage**

Replace `frontend/src/pages/StatusPage.tsx`:

```tsx
import { useState, useEffect, useCallback } from 'react'
import { getHealth, getActuatorHealth } from '@/api/rag'
import StatusCard from '@/components/common/StatusCard'
import { Activity, RefreshCw } from 'lucide-react'

interface HealthStatus {
  title: string
  status: 'ok' | 'error' | 'loading'
  detail: string
}

export default function StatusPage() {
  const [statuses, setStatuses] = useState<HealthStatus[]>([
    { title: '应用状态', status: 'loading', detail: '' },
    { title: 'Milvus 连接', status: 'loading', detail: '' },
    { title: 'SiliconFlow API', status: 'loading', detail: '' },
  ])
  const [lastCheck, setLastCheck] = useState('')

  const checkHealth = useCallback(async () => {
    setStatuses((prev) => prev.map((s) => ({ ...s, status: 'loading' as const })))

    // Basic health check
    try {
      const health = await getHealth()
      setStatuses((prev) => {
        const next = [...prev]
        next[0] = {
          title: '应用状态',
          status: health.status === 'OK' ? 'ok' : 'error',
          detail: `服务: ${health.service}`,
        }
        return next
      })
    } catch {
      setStatuses((prev) => {
        const next = [...prev]
        next[0] = { title: '应用状态', status: 'error', detail: '无法连接到后端服务' }
        return next
      })
    }

    // Actuator health
    try {
      const actuator = await getActuatorHealth()
      const components = (actuator.components || {}) as Record<string, Record<string, string>>

      const milvus = components.milvus
      setStatuses((prev) => {
        const next = [...prev]
        next[1] = {
          title: 'Milvus 连接',
          status: milvus?.status === 'UP' ? 'ok' : 'error',
          detail: milvus?.status === 'UP' ? '向量数据库连接正常' : '连接异常',
        }
        return next
      })

      const siliconflow = components.siliconFlow
      setStatuses((prev) => {
        const next = [...prev]
        next[2] = {
          title: 'SiliconFlow API',
          status: siliconflow?.status === 'UP' ? 'ok' : 'error',
          detail: siliconflow?.status === 'UP' ? 'API 服务可用' : 'API 不可用',
        }
        return next
      })
    } catch {
      setStatuses((prev) => {
        const next = [...prev]
        next[1] = { title: 'Milvus 连接', status: 'error', detail: '无法获取状态' }
        next[2] = { title: 'SiliconFlow API', status: 'error', detail: '无法获取状态' }
        return next
      })
    }

    setLastCheck(new Date().toLocaleTimeString())
  }, [])

  useEffect(() => {
    void checkHealth()
  }, [checkHealth])

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-2xl mx-auto p-6">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-text flex items-center gap-2">
            <Activity size={22} />
            系统状态
          </h1>
          <button
            onClick={() => void checkHealth()}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm text-text-secondary hover:bg-border/50 transition-colors"
          >
            <RefreshCw size={14} />
            刷新
          </button>
        </div>

        <div className="space-y-3">
          {statuses.map((s, i) => (
            <StatusCard key={i} title={s.title} status={s.status} detail={s.detail} />
          ))}
        </div>

        {lastCheck && (
          <p className="text-xs text-text-secondary mt-4 text-right">最后检查: {lastCheck}</p>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Implement SettingsPage**

Replace `frontend/src/pages/SettingsPage.tsx`:

```tsx
import { useSettingsStore } from '@/stores/settingsStore'
import { Settings, Sun, Moon, Globe } from 'lucide-react'

export default function SettingsPage() {
  const { settings, updateTheme, updateApiUrl, updateLanguage } = useSettingsStore()

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-2xl mx-auto p-6">
        <h1 className="text-xl font-semibold text-text flex items-center gap-2 mb-6">
          <Settings size={22} />
          设置
        </h1>

        <div className="space-y-6">
          {/* API URL */}
          <div>
            <label className="block text-sm font-medium text-text mb-2">API 基础地址</label>
            <input
              value={settings.apiUrl}
              onChange={(e) => updateApiUrl(e.target.value)}
              type="text"
              className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-text focus:outline-none focus:ring-2 focus:ring-primary/30"
            />
            <p className="text-xs text-text-secondary mt-1">后端 API 地址，默认 http://localhost:8080</p>
          </div>

          {/* Theme */}
          <div>
            <label className="block text-sm font-medium text-text mb-2">主题</label>
            <div className="flex gap-2">
              <button
                onClick={() => updateTheme('light')}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm border transition-colors ${
                  settings.theme === 'light'
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-border text-text-secondary hover:bg-border/30'
                }`}
              >
                <Sun size={16} />
                浅色
              </button>
              <button
                onClick={() => updateTheme('dark')}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm border transition-colors ${
                  settings.theme === 'dark'
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-border text-text-secondary hover:bg-border/30'
                }`}
              >
                <Moon size={16} />
                深色
              </button>
            </div>
          </div>

          {/* Language */}
          <div>
            <label className="block text-sm font-medium text-text mb-2">
              <Globe size={14} className="inline mr-1" />
              语言
            </label>
            <div className="flex gap-2">
              <button
                onClick={() => updateLanguage('zh')}
                className={`px-4 py-2 rounded-lg text-sm border transition-colors ${
                  settings.language === 'zh'
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-border text-text-secondary hover:bg-border/30'
                }`}
              >
                中文
              </button>
              <button
                onClick={() => updateLanguage('en')}
                className={`px-4 py-2 rounded-lg text-sm border transition-colors ${
                  settings.language === 'en'
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-border text-text-secondary hover:bg-border/30'
                }`}
              >
                English
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 5: Verify TypeScript compiles**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx tsc -b
```

Expected: No errors.

- [ ] **Step 6: Commit**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/src/
git commit -m "feat: add history, status, and settings pages"
```

---

### Task 9: Final Verification and Build

**Files:** None created/modified.

- [ ] **Step 1: Run full TypeScript check**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npx tsc -b
```

Expected: No errors.

- [ ] **Step 2: Run production build**

```bash
cd E:/idea_workspace/wok-rag-agent/frontend && npm run build
```

Expected: Build succeeds, output in `frontend/dist/`.

- [ ] **Step 3: Test with backend (optional)**

```bash
# Terminal 1: Start backend
cd E:/idea_workspace/wok-rag-agent && mvn spring-boot:run

# Terminal 2: Start frontend
cd E:/idea_workspace/wok-rag-agent/frontend && npm run dev
```

Verify in browser:
- Chat page: type a question, see streaming response with citations
- Status page: shows health indicators
- Settings page: theme toggle works, persists on refresh
- Knowledge page: shows empty state (backend API not implemented yet)

- [ ] **Step 4: Commit any final fixes**

```bash
cd E:/idea_workspace/wok-rag-agent
git add frontend/
git commit -m "chore: final frontend verification and cleanup"
```
