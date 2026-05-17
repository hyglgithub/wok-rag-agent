# All Phases Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 5 frontend UX bugs, add custom sidebar title, enhance knowledge base with file storage/chunk editing/dedup, and add chat history search.

**Architecture:** Pure frontend changes for Phase 1-2. Phase 3 adds backend file storage service, SHA-256 dedup, and Milvus chunk CRUD. Phase 4 adds SQLite search endpoint.

**Tech Stack:** React 19, Zustand, shadcn/ui, Spring Boot 3.2.5, SQLite, Milvus 2.6.6, OkHttp, Apache Tika

---

## Phase 1: Bug Fixes & UX Polish

### Task 1: ConfirmDialog Component

**Files:**
- Create: `frontend/src/components/ui/confirm-dialog.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Create ConfirmDialog with useConfirm hook**

Create `frontend/src/components/ui/confirm-dialog.tsx`:

```tsx
import { useState, useCallback, createContext, useContext, useRef } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

interface ConfirmOptions {
  title: string
  description: string
  confirmText?: string
  cancelText?: string
  variant?: 'default' | 'destructive'
}

interface ConfirmContextValue {
  confirm: (opts: ConfirmOptions) => Promise<boolean>
}

const ConfirmContext = createContext<ConfirmContextValue | null>(null)

export function useConfirm(): ConfirmContextValue {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm must be used within ConfirmProvider')
  return ctx
}

export function ConfirmProvider({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = useState(false)
  const [options, setOptions] = useState<ConfirmOptions>({
    title: '',
    description: '',
  })
  const resolveRef = useRef<((value: boolean) => void) | null>(null)

  const confirm = useCallback((opts: ConfirmOptions): Promise<boolean> => {
    return new Promise<boolean>((resolve) => {
      setOptions(opts)
      setOpen(true)
      resolveRef.current = resolve
    })
  }, [])

  function handleConfirm() {
    resolveRef.current?.(true)
    setOpen(false)
  }

  function handleCancel() {
    resolveRef.current?.(false)
    setOpen(false)
  }

  function handleOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resolveRef.current?.(false)
      setOpen(false)
    }
  }

  return (
    <ConfirmContext.Provider value={{ confirm }}>
      {children}
      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{options.title}</DialogTitle>
            <DialogDescription>{options.description}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={handleCancel}>
              {options.cancelText || '取消'}
            </Button>
            <Button
              variant={options.variant === 'destructive' ? 'destructive' : 'default'}
              onClick={handleConfirm}
            >
              {options.confirmText || '确定'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </ConfirmContext.Provider>
  )
}
```

- [ ] **Step 2: Wrap app with ConfirmProvider**

Modify `frontend/src/App.tsx` — wrap the Router with `<ConfirmProvider>`:

```tsx
import { ConfirmProvider } from '@/components/ui/confirm-dialog'

// In the default export function, wrap the outermost element:
// <ConfirmProvider>
//   <Router>...</Router>
// </ConfirmProvider>
```

- [ ] **Step 3: Replace confirm() in HistoryPage**

Modify `frontend/src/pages/HistoryPage.tsx`:

Replace:
```tsx
import { useEffect } from 'react'
```
With:
```tsx
import { useEffect } from 'react'
import { useConfirm } from '@/components/ui/confirm-dialog'
import { toast } from 'sonner'
```

Add inside the component:
```tsx
const { confirm } = useConfirm()
```

Replace `handleDelete`:
```tsx
async function handleDelete(sessionId: string) {
  const confirmed = await confirm({
    title: '删除会话',
    description: '确定删除此会话？此操作不可撤销。',
    variant: 'destructive',
    confirmText: '删除',
  })
  if (!confirmed) return
  await removeSession(sessionId)
  if (currentSessionId === sessionId) {
    clearMessages()
  }
}
```

- [ ] **Step 4: Replace confirm()/alert() in KnowledgePage**

Modify `frontend/src/pages/KnowledgePage.tsx`:

Add imports:
```tsx
import { useConfirm } from '@/components/ui/confirm-dialog'
import { toast } from 'sonner'
```

Add inside component:
```tsx
const { confirm } = useConfirm()
```

Replace `handleUpload` catch block:
```tsx
} catch (err) {
  toast.error('上传失败: ' + (err instanceof Error ? err.message : '未知错误'))
}
```

Replace `handleDelete`:
```tsx
async function handleDelete(doc: DocumentInfo) {
  const confirmed = await confirm({
    title: '删除文档',
    description: `确定删除 "${doc.name}"？此操作不可撤销。`,
    variant: 'destructive',
    confirmText: '删除',
  })
  if (!confirmed) return
  try {
    await deleteDocument(doc.id)
    setDocuments((prev) => prev.filter((d) => d.id !== doc.id))
  } catch {
    toast.error('删除失败')
  }
}
```

- [ ] **Step 5: Commit**

```bash
cd frontend && git add src/components/ui/confirm-dialog.tsx src/App.tsx src/pages/HistoryPage.tsx src/pages/KnowledgePage.tsx
git commit -m "feat: add ConfirmDialog component, replace native alert/confirm"
```

---

### Task 2: Stop Generation Button

**Files:**
- Modify: `frontend/src/stores/chatStore.ts`
- Modify: `frontend/src/api/rag.ts`
- Modify: `frontend/src/components/chat/ChatInput.tsx`
- Modify: `frontend/src/pages/ChatPage.tsx`

- [ ] **Step 1: Add AbortSignal support to streamRag**

Modify `frontend/src/api/rag.ts` — add optional `signal` parameter:

```tsx
export async function streamRag(request: QueryRequest, signal?: AbortSignal): Promise<Response> {
  const url = `${useSettingsStore.getState().settings.apiUrl}/api/rag/stream`
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
    signal,
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({
      message: `HTTP ${response.status}`,
    }))
    throw error
  }
  return response
}
```

- [ ] **Step 2: Add abortController and stopGeneration to chatStore**

Modify `frontend/src/stores/chatStore.ts`:

Add to interface:
```tsx
interface ChatState {
  messages: Message[]
  currentSessionId: string | null
  isStreaming: boolean
  streamingContent: string
  abortController: AbortController | null
  sendMessage: (question: string) => Promise<void>
  stopGeneration: () => void
  clearMessages: () => void
  loadSession: (sessionId: string, messages: Message[]) => void
}
```

Add to initial state:
```tsx
abortController: null,
```

Add `stopGeneration` action:
```tsx
stopGeneration: () => {
  const state = get()
  state.abortController?.abort()
  set((s) => {
    const msgs = [...s.messages]
    const lastMsg = msgs[msgs.length - 1]
    if (lastMsg && lastMsg.role === 'assistant' && lastMsg.isStreaming) {
      msgs[msgs.length - 1] = { ...lastMsg, isStreaming: false }
    }
    return { messages: msgs, isStreaming: false, streamingContent: '', abortController: null }
  })
},
```

Modify `sendMessage` — create AbortController and pass signal:
```tsx
sendMessage: async (question: string) => {
  const state = get()
  if (state.isStreaming || !question.trim()) return

  const abortController = new AbortController()

  // ... (existing userMessage, assistantMessage, sessionId setup) ...

  set({
    messages: [...state.messages, userMessage, assistantMessage],
    currentSessionId: sessionId,
    isStreaming: true,
    streamingContent: '',
    abortController,
  })

  try {
    const response = await streamRag({
      question: question.trim(),
      sessionId,
    }, abortController.signal)

    // ... (existing SSE parsing logic unchanged) ...
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      // User stopped generation — preserve partial content
      set((s) => {
        const msgs = [...s.messages]
        const lastMsg = msgs[msgs.length - 1]
        if (lastMsg && lastMsg.role === 'assistant') {
          msgs[msgs.length - 1] = { ...lastMsg, isStreaming: false }
        }
        return { messages: msgs }
      })
    } else {
      // ... (existing error handling) ...
    }
  } finally {
    set({ isStreaming: false, streamingContent: '', abortController: null })
  }
},
```

- [ ] **Step 3: Update ChatInput to show stop button**

Modify `frontend/src/components/chat/ChatInput.tsx`:

```tsx
import { Send, Square } from 'lucide-react'

interface Props {
  disabled: boolean
  isStreaming: boolean
  onSend: (question: string) => void
  onStop: () => void
}

export default function ChatInput({ disabled, isStreaming, onSend, onStop }: Props) {
  // ... (existing state and handlers) ...

  return (
    <div className="border-t border-border bg-background p-4">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-end gap-2 rounded-xl border border-border bg-muted p-2">
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
            className="flex-1 bg-transparent resize-none px-2 py-1.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none"
            style={{ maxHeight: '200px' }}
          />
          {isStreaming ? (
            <Button onClick={onStop} size="icon" variant="destructive">
              <Square size={18} />
            </Button>
          ) : (
            <Button
              onClick={handleSend}
              disabled={disabled || !input.trim()}
              size="icon"
            >
              <Send size={18} />
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Pass stop handler from ChatPage**

Modify `frontend/src/pages/ChatPage.tsx`:

Add `stopGeneration` from store:
```tsx
const { messages, isStreaming, streamingContent, sendMessage, loadSession, stopGeneration } = useChatStore()
```

Update ChatInput usage:
```tsx
<ChatInput
  disabled={isStreaming}
  isStreaming={isStreaming}
  onSend={handleSend}
  onStop={stopGeneration}
/>
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/chatStore.ts frontend/src/api/rag.ts frontend/src/components/chat/ChatInput.tsx frontend/src/pages/ChatPage.tsx
git commit -m "feat: add stop generation button with AbortController"
```

---

### Task 3: Citations Sorting

**Files:**
- Modify: `frontend/src/components/chat/MessageBubble.tsx`

- [ ] **Step 1: Sort citations by index**

Modify `frontend/src/components/chat/MessageBubble.tsx`:

Add `useMemo` to imports:
```tsx
import { useMemo } from 'react'
```

Add sorted citations computation inside the component (after `const isUser = ...`):
```tsx
const sortedCitations = useMemo(
  () => [...message.citations].sort((a, b) => a.index - b.index),
  [message.citations]
)
```

Replace `message.citations.map(...)` with `sortedCitations.map(...)`:
```tsx
{sortedCitations.map((citation) => (
  <CitationCard key={citation.index} citation={citation} />
))}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/chat/MessageBubble.tsx
git commit -m "fix: sort citations by index before rendering"
```

---

### Task 4: Sidebar Session Loading

**Files:**
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Fetch sessions on mount in AppLayout**

Modify `frontend/src/components/layout/AppLayout.tsx`:

```tsx
import { useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { useSessionStore } from '@/stores/sessionStore'

export default function AppLayout() {
  const { fetchSessions } = useSessionStore()

  useEffect(() => {
    void fetchSessions()
  }, [fetchSessions])

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar />
      <main className="flex-1 overflow-hidden">
        <Outlet />
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/layout/AppLayout.tsx
git commit -m "fix: fetch sessions on app mount so sidebar shows history"
```

---

### Task 5: Sidebar Delete Confirmation

**Files:**
- Modify: `frontend/src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Add delete confirmation to Sidebar**

Modify `frontend/src/components/layout/Sidebar.tsx`:

Add import:
```tsx
import { useConfirm } from '@/components/ui/confirm-dialog'
```

Add inside component:
```tsx
const { confirm } = useConfirm()
```

Add handler function:
```tsx
async function handleDeleteSession(sessionId: string) {
  const confirmed = await confirm({
    title: '删除会话',
    description: '确定删除此会话？此操作不可撤销。',
    variant: 'destructive',
    confirmText: '删除',
  })
  if (confirmed) {
    await removeSession(sessionId)
  }
}
```

Replace the delete button onClick:
```tsx
onClick={(e) => {
  e.stopPropagation()
  void handleDeleteSession(session.sessionId)
}}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/layout/Sidebar.tsx
git commit -m "feat: add delete confirmation dialog to sidebar"
```

---

## Phase 2: Custom Sidebar Title

### Task 6: Custom Sidebar Title

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/stores/settingsStore.ts`
- Modify: `frontend/src/pages/SettingsPage.tsx`
- Modify: `frontend/src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Add sidebarTitle to Settings type**

Modify `frontend/src/types/index.ts` — add `sidebarTitle` to `Settings`:

```tsx
export interface Settings {
  apiUrl: string
  theme: 'light' | 'dark'
  language: 'zh' | 'en'
  sidebarTitle: string
}
```

- [ ] **Step 2: Add sidebarTitle to settingsStore**

Modify `frontend/src/stores/settingsStore.ts`:

Update default settings in `loadSettings()`:
```tsx
return {
  apiUrl: 'http://localhost:8080',
  theme: 'light',
  language: 'zh',
  sidebarTitle: 'Wok RAG Agent',
}
```

Add `updateSidebarTitle` to interface and implementation:
```tsx
interface SettingsState {
  settings: Settings
  updateTheme: (theme: 'light' | 'dark') => void
  updateApiUrl: (url: string) => void
  updateLanguage: (lang: 'zh' | 'en') => void
  updateSidebarTitle: (title: string) => void
}
```

```tsx
updateSidebarTitle: (title) =>
  set((state) => {
    const next = { ...state.settings, sidebarTitle: title }
    persist(next)
    return { settings: next }
  }),
```

- [ ] **Step 3: Add sidebar title input to SettingsPage**

Modify `frontend/src/pages/SettingsPage.tsx`:

Destructure `updateSidebarTitle`:
```tsx
const { settings, updateTheme, updateApiUrl, updateLanguage, updateSidebarTitle } = useSettingsStore()
```

Add new section after the Language section:
```tsx
{/* Sidebar Title */}
<div>
  <label className="block text-sm font-medium text-foreground mb-2">侧边栏标题</label>
  <Input
    value={settings.sidebarTitle}
    onChange={(e) => updateSidebarTitle(e.target.value)}
    placeholder="Wok RAG Agent"
  />
  <p className="text-xs text-muted-foreground mt-1">自定义侧边栏顶部显示的标题</p>
</div>
```

- [ ] **Step 4: Use sidebarTitle in Sidebar**

Modify `frontend/src/components/layout/Sidebar.tsx`:

Add import:
```tsx
import { useSettingsStore } from '@/stores/settingsStore'
```

Add inside component:
```tsx
const { settings } = useSettingsStore()
```

Replace hardcoded title:
```tsx
<span className="text-sm font-semibold text-foreground truncate">{settings.sidebarTitle || 'Wok RAG Agent'}</span>
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/types/index.ts frontend/src/stores/settingsStore.ts frontend/src/pages/SettingsPage.tsx frontend/src/components/layout/Sidebar.tsx
git commit -m "feat: add custom sidebar title setting"
```

---

## Phase 3: Knowledge Base Enhancements

### Task 7: Duplicate File Detection (Backend)

**Files:**
- Modify: `src/main/resources/schema.sql`
- Modify: `src/main/java/com/wokrag/agent/repository/DocumentRepository.java`
- Modify: `src/main/java/com/wokrag/agent/controller/DocumentController.java`

- [ ] **Step 1: Add file_hash and file_path columns to schema**

Modify `src/main/resources/schema.sql` — update the documents table:

```sql
CREATE TABLE IF NOT EXISTS documents (
    doc_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    source TEXT,
    upload_time TEXT NOT NULL DEFAULT (datetime('now')),
    chunk_count INTEGER NOT NULL DEFAULT 0,
    file_hash TEXT NOT NULL DEFAULT '',
    file_path TEXT NOT NULL DEFAULT ''
);
```

- [ ] **Step 2: Add findByHash and update save method**

Modify `src/main/java/com/wokrag/agent/repository/DocumentRepository.java`:

```java
package com.wokrag.agent.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DocumentRepository {

    private final JdbcTemplate jdbc;

    public void save(String docId, String name, String source, int chunkCount, String fileHash, String filePath) {
        jdbc.update(
                "INSERT INTO documents (doc_id, name, source, chunk_count, file_hash, file_path) VALUES (?, ?, ?, ?, ?, ?)",
                docId, name, source, chunkCount, fileHash, filePath
        );
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT doc_id, name, source, upload_time, chunk_count, file_hash, file_path FROM documents ORDER BY upload_time DESC"
        );
    }

    public void delete(String docId) {
        jdbc.update("DELETE FROM documents WHERE doc_id = ?", docId);
    }

    public boolean exists(String docId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE doc_id = ?",
                Integer.class, docId
        );
        return count != null && count > 0;
    }

    public Map<String, Object> findByHash(String fileHash) {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT doc_id, name, source, upload_time, chunk_count FROM documents WHERE file_hash = ? LIMIT 1",
                fileHash
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public String findFilePath(String docId) {
        return jdbc.queryForObject(
                "SELECT file_path FROM documents WHERE doc_id = ?",
                String.class, docId
        );
    }

    public void incrementChunkCount(String docId) {
        jdbc.update(
                "UPDATE documents SET chunk_count = chunk_count + 1 WHERE doc_id = ?",
                docId
        );
    }

    public String findNameByDocId(String docId) {
        return jdbc.queryForObject(
                "SELECT name FROM documents WHERE doc_id = ?",
                String.class, docId
        );
    }
}
```

- [ ] **Step 3: Add hash check to DocumentController upload**

Modify `src/main/java/com/wokrag/agent/controller/DocumentController.java`:

Add imports:
```java
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpStatus;
```

Add hash check at the beginning of `uploadDocument()`, before "Step 1: Parse file":

```java
// Step 0: Compute file hash and check for duplicates
String fileHash;
try {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = md.digest(file.getBytes());
    fileHash = Hex.encodeHexString(hashBytes);
} catch (Exception e) {
    return ResponseEntity.internalServerError().body(Map.of(
            "error", "HASH_ERROR",
            "message", "Failed to compute file hash"
    ));
}

Map<String, Object> existing = documentRepository.findByHash(fileHash);
if (existing != null) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", "DUPLICATE_FILE",
            "message", "文件已存在: " + existing.get("name")
    ));
}
```

Update the `documentRepository.save()` call to include hash and empty file path (file storage added in next task):
```java
documentRepository.save(docId, fileName, source, chunks.size(), fileHash, "");
```

- [ ] **Step 4: Handle 409 in frontend upload**

Modify `frontend/src/api/document.ts`:

```tsx
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
    if (response.status === 409) {
      const err = await response.json().catch(() => ({ message: '文件已存在' }))
      throw new Error(err.message || '文件已存在')
    }
    const error = await response.json().catch(() => ({
      errorCode: 'UPLOAD_ERROR',
      errorMessage: `HTTP ${response.status}`,
    }))
    throw error
  }

  return response.json()
}
```

- [ ] **Step 5: Add commons-codec dependency**

Check if `pom.xml` already has `commons-codec`. If not, add:
```xml
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
</dependency>
```

(Spring Boot parent manages the version.)

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/schema.sql src/main/java/com/wokrag/agent/repository/DocumentRepository.java src/main/java/com/wokrag/agent/controller/DocumentController.java frontend/src/api/document.ts
git commit -m "feat: add SHA-256 duplicate file detection on upload"
```

---

### Task 8: File Storage Service (Backend)

**Files:**
- Create: `src/main/java/com/wokrag/agent/service/document/FileStorageService.java`
- Modify: `src/main/java/com/wokrag/agent/controller/DocumentController.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Create FileStorageService**

Create `src/main/java/com/wokrag/agent/service/document/FileStorageService.java`:

```java
package com.wokrag.agent.service.document;

import com.wokrag.agent.config.FileStorageConfig;
import com.wokrag.agent.exception.RagException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Service
public class FileStorageService {

    private final Path storageRoot;

    public FileStorageService(FileStorageConfig config) {
        this.storageRoot = Paths.get(config.getPath()).toAbsolutePath();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RagException("Failed to create storage directory: " + storageRoot, e);
        }
    }

    public String store(String docId, String fileName, byte[] content) {
        try {
            Path docDir = storageRoot.resolve(docId);
            Files.createDirectories(docDir);
            Path filePath = docDir.resolve(fileName);
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            log.info("Stored file: {}", filePath);
            return docId + "/" + fileName;
        } catch (IOException e) {
            throw new RagException("Failed to store file for docId: " + docId, e);
        }
    }

    public Resource load(String docId, String fileName) {
        Path filePath = storageRoot.resolve(docId).resolve(fileName);
        if (!Files.exists(filePath)) {
            throw new RagException.DocumentParseException("File not found: " + filePath);
        }
        return new FileSystemResource(filePath);
    }

    public void delete(String docId) {
        try {
            Path docDir = storageRoot.resolve(docId);
            if (Files.exists(docDir)) {
                Files.walk(docDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try { Files.delete(path); } catch (IOException ignored) {}
                        });
                log.info("Deleted file directory: {}", docDir);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file directory for docId: {}", docId, e);
        }
    }
}
```

- [ ] **Step 2: Create FileStorageConfig**

Create `src/main/java/com/wokrag/agent/config/FileStorageConfig.java`:

```java
package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageConfig {
    private String path = "data/documents/";
}
```

- [ ] **Step 3: Add file.storage.path to application.yml**

Modify `src/main/resources/application.yml` — add at the end:

```yaml
# File Storage
file:
  storage:
    path: data/documents/
```

- [ ] **Step 4: Add download and preview endpoints to DocumentController**

Modify `src/main/java/com/wokrag/agent/controller/DocumentController.java`:

Add `FileStorageService` to constructor injection:
```java
private final FileStorageService fileStorageService;
```

Add import:
```java
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
```

Store file during upload (after Step 5 "Save metadata"):
```java
// Step 6: Store original file
String filePath = fileStorageService.store(docId, fileName, file.getBytes());
```

Update the save call to use `filePath`:
```java
documentRepository.save(docId, fileName, source, chunks.size(), fileHash, filePath);
```

Add download endpoint:
```java
@GetMapping("/{docId}/download")
@Operation(summary = "Download original document file")
public ResponseEntity<Resource> downloadDocument(@PathVariable String docId) {
    String filePath = documentRepository.findFilePath(docId);
    if (filePath == null || filePath.isEmpty()) {
        return ResponseEntity.notFound().build();
    }
    // filePath is "docId/fileName"
    String[] parts = filePath.split("/", 2);
    Resource resource = fileStorageService.load(parts[0], parts[1]);

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + parts[1] + "\"")
            .body(resource);
}
```

Add preview endpoint:
```java
@GetMapping("/{docId}/preview")
@Operation(summary = "Preview document file (for PDF inline viewing)")
public ResponseEntity<Resource> previewDocument(@PathVariable String docId) {
    String filePath = documentRepository.findFilePath(docId);
    if (filePath == null || filePath.isEmpty()) {
        return ResponseEntity.notFound().build();
    }
    String[] parts = filePath.split("/", 2);
    Resource resource = fileStorageService.load(parts[0], parts[1]);

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + parts[1] + "\"")
            .body(resource);
}
```

Update `deleteDocument` to also delete files:
```java
@DeleteMapping("/{docId}")
@Operation(summary = "Delete a document and its chunks")
public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String docId) {
    log.info("Deleting document: {}", docId);
    milvusClient.deleteByDocId(docId);
    fileStorageService.delete(docId);
    documentRepository.delete(docId);
    log.info("Document deleted: {}", docId);
    return ResponseEntity.ok(Map.of("status", "ok"));
}
```

- [ ] **Step 5: Add frontend download/preview APIs**

Modify `frontend/src/api/document.ts`:

```tsx
export function downloadDocument(docId: string) {
  const baseUrl = useSettingsStore.getState().settings.apiUrl
  window.open(`${baseUrl}/api/documents/${docId}/download`, '_blank')
}

export function getPreviewUrl(docId: string): string {
  const baseUrl = useSettingsStore.getState().settings.apiUrl
  return `${baseUrl}/api/documents/${docId}/preview`
}
```

- [ ] **Step 6: Add download/preview buttons to KnowledgePage**

Modify `frontend/src/pages/KnowledgePage.tsx`:

Add imports:
```tsx
import { Database, Trash2, Upload, Search, FileText, Download, Eye } from 'lucide-react'
import { downloadDocument, getPreviewUrl } from '@/api/document'
```

Add a `PdfPreviewDialog` state and component. Add buttons in the table row actions column. Add a simple PdfPreviewDialog component inline or as a separate file.

For simplicity, add download button and preview button (PDF only) next to delete:

```tsx
<TableCell className="text-right">
  <Button
    variant="ghost"
    size="icon"
    onClick={() => downloadDocument(doc.id)}
    title="下载"
  >
    <Download size={16} />
  </Button>
  {doc.name.toLowerCase().endsWith('.pdf') && (
    <Button
      variant="ghost"
      size="icon"
      onClick={() => window.open(getPreviewUrl(doc.id), '_blank')}
      title="预览"
    >
      <Eye size={16} />
    </Button>
  )}
  <Button
    variant="ghost"
    size="icon"
    onClick={() => void handleDelete(doc)}
    className="text-muted-foreground hover:text-destructive"
    title="删除"
  >
    <Trash2 size={16} />
  </Button>
</TableCell>
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/wokrag/agent/service/document/FileStorageService.java src/main/java/com/wokrag/agent/config/FileStorageConfig.java src/main/java/com/wokrag/agent/controller/DocumentController.java src/main/resources/application.yml frontend/src/api/document.ts frontend/src/pages/KnowledgePage.tsx
git commit -m "feat: add file storage, download, and PDF preview"
```

---

### Task 9: Chunk Viewing, Editing & Adding (Backend)

**Files:**
- Modify: `src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java`
- Modify: `src/main/java/com/wokrag/agent/controller/DocumentController.java`

- [ ] **Step 1: Add queryByDocId to MilvusClientWrapper**

Modify `src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java`:

Add imports:
```java
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.QueryResp;
```

Add method:
```java
public List<QueryResp.QueryResult> queryByDocId(String docId) {
    try {
        String expr = "doc_id == \"" + docId + "\"";
        QueryResp resp = client.query(QueryReq.builder()
                .collectionName(config.getCollectionName())
                .filter(expr)
                .outputFields(List.of("id", "chunk_text", "doc_id", "source"))
                .build());
        return resp.getQueryResults();
    } catch (Exception e) {
        throw new RagException.RetrievalException("Failed to query chunks for doc_id: " + docId, e);
    }
}
```

- [ ] **Step 2: Add updateByPrimaryKey to MilvusClientWrapper**

```java
public void updateByPrimaryKey(long id, String chunkText, float[] vector) {
    try {
        // Milvus v2 SDK uses upsert for updates
        // We need to query first to get existing fields, then upsert
        // Since we only update chunk_text and text_dense, we use upsert with the same id
        JsonObject row = new JsonObject();
        row.addProperty("id", id);
        row.addProperty("chunk_text", chunkText);
        row.add("text_dense", gson.toJsonTree(vector));

        client.upsert(UpsertReq.builder()
                .collectionName(config.getCollectionName())
                .data(List.of(row))
                .build());
        log.info("Updated Milvus chunk id={}", id);
    } catch (Exception e) {
        throw new RagException.RetrievalException("Failed to update chunk id=" + id, e);
    }
}
```

- [ ] **Step 3: Add chunk endpoints to DocumentController**

Add to `DocumentController.java`:

```java
@GetMapping("/{docId}/chunks")
@Operation(summary = "Get all chunks for a document")
public ResponseEntity<Map<String, Object>> getChunks(@PathVariable String docId) {
    var results = milvusClient.queryByDocId(docId);

    List<Map<String, Object>> chunks = new ArrayList<>();
    for (int i = 0; i < results.size(); i++) {
        var result = results.get(i);
        Map<String, Object> chunk = new LinkedHashMap<>();
        // Extract fields from the result entity
        var entity = result.getEntity();
        chunk.put("milvusId", entity.get("id"));
        chunk.put("chunkText", entity.get("chunk_text"));
        chunk.put("chunkIndex", i);
        chunk.put("source", entity.get("source"));
        chunks.add(chunk);
    }

    return ResponseEntity.ok(Map.of("chunks", chunks));
}
```

```java
@PutMapping("/chunks/{milvusId}")
@Operation(summary = "Update a chunk's text and re-embed")
public ResponseEntity<Map<String, String>> updateChunk(
        @PathVariable long milvusId,
        @RequestBody Map<String, String> body) {
    String newText = body.get("text");
    if (newText == null || newText.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
    }

    // Re-embed
    List<double[]> embeddings = embeddingService.embedBatch(List.of(newText));
    float[] vector = toFloatArray(embeddings.get(0));

    // Update in Milvus
    milvusClient.updateByPrimaryKey(milvusId, newText, vector);

    return ResponseEntity.ok(Map.of("status", "ok"));
}
```

```java
@PostMapping("/{docId}/chunks")
@Operation(summary = "Add a new chunk to a document")
public ResponseEntity<Map<String, Object>> addChunk(
        @PathVariable String docId,
        @RequestBody Map<String, String> body) {
    String text = body.get("text");
    if (text == null || text.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
    }

    // Get document source name
    String source = documentRepository.findNameByDocId(docId);

    // Embed
    List<double[]> embeddings = embeddingService.embedBatch(List.of(text));
    float[] vector = toFloatArray(embeddings.get(0));

    // Insert into Milvus
    Map<String, Object> row = new HashMap<>();
    row.put("chunk_text", text);
    row.put("text_dense", vector);
    row.put("doc_id", docId);
    row.put("source", source);
    row.put("source_url", "");
    milvusClient.insert(List.of(row));

    // Update chunk count in SQLite
    documentRepository.incrementChunkCount(docId);

    return ResponseEntity.ok(Map.of("status", "ok"));
}
```

Add helper method:
```java
private float[] toFloatArray(double[] doubles) {
    float[] floats = new float[doubles.length];
    for (int i = 0; i < doubles.length; i++) {
        floats[i] = (float) doubles[i];
    }
    return floats;
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java src/main/java/com/wokrag/agent/controller/DocumentController.java
git commit -m "feat: add chunk CRUD endpoints (list, update, add)"
```

---

### Task 10: Chunk Viewing, Editing & Adding (Frontend)

**Files:**
- Modify: `frontend/src/api/document.ts`
- Modify: `frontend/src/types/index.ts`
- Create: `frontend/src/components/common/ChunkManagerDialog.tsx`
- Modify: `frontend/src/pages/KnowledgePage.tsx`

- [ ] **Step 1: Add chunk types and API functions**

Modify `frontend/src/types/index.ts` — add:
```tsx
export interface ChunkInfo {
  milvusId: number
  chunkText: string
  chunkIndex: number
  source: string
}
```

Modify `frontend/src/api/document.ts` — add:
```tsx
import type { ChunkInfo } from '@/types'

export async function getDocumentChunks(docId: string): Promise<ChunkInfo[]> {
  const res = await apiFetch<{ chunks: ChunkInfo[] }>(`/api/documents/${docId}/chunks`)
  return res.chunks
}

export async function updateChunk(milvusId: number, text: string): Promise<void> {
  await apiFetch(`/api/documents/chunks/${milvusId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  })
}

export async function addChunk(docId: string, text: string): Promise<void> {
  await apiFetch(`/api/documents/${docId}/chunks`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  })
}
```

- [ ] **Step 2: Create ChunkManagerDialog**

Create `frontend/src/components/common/ChunkManagerDialog.tsx`:

```tsx
import { useState, useEffect } from 'react'
import type { ChunkInfo } from '@/types'
import { getDocumentChunks, updateChunk, addChunk } from '@/api/document'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import { toast } from 'sonner'
import { Pencil, Plus, Loader2 } from 'lucide-react'

interface Props {
  open: boolean
  onClose: () => void
  docId: string
  docName: string
}

export default function ChunkManagerDialog({ open, onClose, docId, docName }: Props) {
  const [chunks, setChunks] = useState<ChunkInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editText, setEditText] = useState('')
  const [saving, setSaving] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [newText, setNewText] = useState('')

  useEffect(() => {
    if (open) {
      setLoading(true)
      getDocumentChunks(docId)
        .then(setChunks)
        .finally(() => setLoading(false))
    }
  }, [open, docId])

  async function handleSaveEdit(milvusId: number) {
    setSaving(true)
    try {
      await updateChunk(milvusId, editText)
      setChunks((prev) =>
        prev.map((c) => (c.milvusId === milvusId ? { ...c, chunkText: editText } : c))
      )
      setEditingId(null)
      toast.success('切片已更新，向量已重新生成')
    } catch {
      toast.error('更新失败')
    } finally {
      setSaving(false)
    }
  }

  async function handleAddChunk() {
    if (!newText.trim()) return
    setSaving(true)
    try {
      await addChunk(docId, newText)
      // Refresh chunk list
      const updated = await getDocumentChunks(docId)
      setChunks(updated)
      setNewText('')
      setShowAdd(false)
      toast.success('切片已添加，向量已生成')
    } catch {
      toast.error('添加失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>切片管理 — {docName}</DialogTitle>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto space-y-3 pr-1">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="animate-spin mr-2" size={20} />
              <span className="text-muted-foreground">加载中...</span>
            </div>
          ) : chunks.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">暂无切片</p>
          ) : (
            chunks.map((chunk) => (
              <div key={chunk.milvusId} className="border rounded-lg p-3">
                <div className="flex items-center justify-between mb-2">
                  <Badge variant="outline">#{chunk.chunkIndex + 1}</Badge>
                  {editingId !== chunk.milvusId && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setEditingId(chunk.milvusId)
                        setEditText(chunk.chunkText)
                      }}
                    >
                      <Pencil size={14} className="mr-1" />
                      编辑
                    </Button>
                  )}
                </div>

                {editingId === chunk.milvusId ? (
                  <div className="space-y-2">
                    <Textarea
                      value={editText}
                      onChange={(e) => setEditText(e.target.value)}
                      rows={4}
                    />
                    <div className="flex gap-2 justify-end">
                      <Button variant="outline" size="sm" onClick={() => setEditingId(null)}>
                        取消
                      </Button>
                      <Button size="sm" onClick={() => void handleSaveEdit(chunk.milvusId)} disabled={saving}>
                        {saving ? <Loader2 className="animate-spin mr-1" size={14} /> : null}
                        保存
                      </Button>
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground whitespace-pre-wrap">{chunk.chunkText}</p>
                )}
              </div>
            ))
          )}
        </div>

        {/* Add new chunk */}
        <div className="border-t pt-3 mt-2">
          {showAdd ? (
            <div className="space-y-2">
              <Textarea
                value={newText}
                onChange={(e) => setNewText(e.target.value)}
                placeholder="输入新的切片内容..."
                rows={3}
              />
              <div className="flex gap-2 justify-end">
                <Button variant="outline" size="sm" onClick={() => setShowAdd(false)}>
                  取消
                </Button>
                <Button size="sm" onClick={() => void handleAddChunk()} disabled={saving}>
                  {saving ? <Loader2 className="animate-spin mr-1" size={14} /> : null}
                  添加
                </Button>
              </div>
            </div>
          ) : (
            <Button variant="outline" onClick={() => setShowAdd(true)} className="w-full">
              <Plus size={16} className="mr-2" />
              添加切片
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
```

- [ ] **Step 3: Add "查看切片" button to KnowledgePage**

Modify `frontend/src/pages/KnowledgePage.tsx`:

Add imports:
```tsx
import { Database, Trash2, Upload, Search, FileText, Download, Eye, Layers } from 'lucide-react'
import ChunkManagerDialog from '@/components/common/ChunkManagerDialog'
```

Add state:
```tsx
const [chunkDialog, setChunkDialog] = useState<{ open: boolean; docId: string; docName: string }>({
  open: false, docId: '', docName: '',
})
```

Add button in table actions (before download button):
```tsx
<Button
  variant="ghost"
  size="icon"
  onClick={() => setChunkDialog({ open: true, docId: doc.id, docName: doc.name })}
  title="查看切片"
>
  <Layers size={16} />
</Button>
```

Add dialog at the end of the component (before closing `</div>`):
```tsx
<ChunkManagerDialog
  open={chunkDialog.open}
  onClose={() => setChunkDialog({ ...chunkDialog, open: false })}
  docId={chunkDialog.docId}
  docName={chunkDialog.docName}
/>
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/document.ts frontend/src/types/index.ts frontend/src/components/common/ChunkManagerDialog.tsx frontend/src/pages/KnowledgePage.tsx
git commit -m "feat: add chunk management dialog (view, edit, add)"
```

---

## Phase 4: Chat History Search

### Task 11: Chat History Search (Backend)

**Files:**
- Modify: `src/main/java/com/wokrag/agent/repository/SessionRepository.java`
- Modify: `src/main/java/com/wokrag/agent/controller/SessionController.java`

- [ ] **Step 1: Add searchByKeyword to SessionRepository**

Modify `src/main/java/com/wokrag/agent/repository/SessionRepository.java`:

```java
public List<Map<String, Object>> searchByKeyword(String keyword) {
    return jdbc.queryForList(
            "SELECT DISTINCT s.session_id, s.title, s.created_at, " +
            "SUBSTR(m.content, MAX(1, INSTR(m.content, ?) - 20), 60) AS matched_preview " +
            "FROM sessions s " +
            "JOIN messages m ON s.session_id = m.session_id " +
            "WHERE m.content LIKE '%' || ? || '%' " +
            "ORDER BY s.last_active_at DESC " +
            "LIMIT 20",
            keyword, keyword
    );
}
```

- [ ] **Step 2: Add search endpoint to SessionController**

Modify `src/main/java/com/wokrag/agent/controller/SessionController.java`:

```java
@GetMapping("/search")
@Operation(summary = "Search sessions by message content")
public ResponseEntity<Map<String, Object>> searchSessions(@RequestParam("q") String keyword) {
    if (keyword == null || keyword.isBlank()) {
        return ResponseEntity.ok(Map.of("sessions", List.of()));
    }

    List<Map<String, Object>> rows = sessionRepository.searchByKeyword(keyword.trim());

    List<Map<String, Object>> sessions = new ArrayList<>();
    for (Map<String, Object> row : rows) {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("sessionId", row.get("session_id"));
        session.put("title", row.get("title"));
        session.put("createdAt", row.get("created_at"));
        session.put("matchedPreview", row.get("matched_preview") != null ? row.get("matched_preview") : "");
        sessions.add(session);
    }

    return ResponseEntity.ok(Map.of("sessions", sessions));
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wokrag/agent/repository/SessionRepository.java src/main/java/com/wokrag/agent/controller/SessionController.java
git commit -m "feat: add chat history search endpoint"
```

---

### Task 12: Chat History Search (Frontend)

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/api/rag.ts`
- Modify: `frontend/src/stores/sessionStore.ts`
- Modify: `frontend/src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Add SearchResult type**

Modify `frontend/src/types/index.ts`:
```tsx
export interface SearchResult {
  sessionId: string
  title: string
  createdAt: string
  matchedPreview: string
}
```

- [ ] **Step 2: Add searchSessions API**

Modify `frontend/src/api/rag.ts`:
```tsx
import type { SearchResult } from '@/types'

export async function searchSessions(keyword: string): Promise<SearchResult[]> {
  try {
    const res = await apiFetch<{ sessions: SearchResult[] }>(
      `/api/rag/sessions/search?q=${encodeURIComponent(keyword)}`
    )
    return res.sessions
  } catch {
    return []
  }
}
```

- [ ] **Step 3: Add search state to sessionStore**

Modify `frontend/src/stores/sessionStore.ts`:

Add to imports:
```tsx
import { getSessions, deleteSession as apiDeleteSession, searchSessions as apiSearchSessions } from '@/api/rag'
import type { SearchResult } from '@/types'
```

Add to interface:
```tsx
interface SessionState {
  sessions: Session[]
  loading: boolean
  searchResults: SearchResult[]
  isSearching: boolean
  fetchSessions: () => Promise<void>
  removeSession: (sessionId: string) => Promise<void>
  addLocalSession: (sessionId: string, title: string) => void
  searchSessions: (keyword: string) => Promise<void>
  clearSearch: () => void
}
```

Add to store:
```tsx
searchResults: [],
isSearching: false,

searchSessions: async (keyword: string) => {
  if (!keyword.trim()) {
    set({ searchResults: [], isSearching: false })
    return
  }
  set({ isSearching: true })
  try {
    const results = await apiSearchSessions(keyword)
    set({ searchResults: results })
  } finally {
    set({ isSearching: false })
  }
},

clearSearch: () => set({ searchResults: [], isSearching: false }),
```

- [ ] **Step 4: Add search UI to Sidebar**

Modify `frontend/src/components/layout/Sidebar.tsx`:

Add imports:
```tsx
import { useState, useEffect, useRef, useCallback } from 'react'
import { Search, X } from 'lucide-react'  // add Search and X to existing lucide imports
import { useSessionStore } from '@/stores/sessionStore'  // already imported
```

Add search state and debounce:
```tsx
const { sessions, removeSession, searchResults, isSearching, searchSessions, clearSearch } = useSessionStore()
const [searchQuery, setSearchQuery] = useState('')
const searchTimerRef = useRef<ReturnType<typeof setTimeout>>()
```

Add debounced search effect:
```tsx
useEffect(() => {
  if (searchTimerRef.current) clearTimeout(searchTimerRef.current)
  if (!searchQuery.trim()) {
    clearSearch()
    return
  }
  searchTimerRef.current = setTimeout(() => {
    void searchSessions(searchQuery)
  }, 300)
  return () => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current)
  }
}, [searchQuery, searchSessions, clearSearch])
```

Replace the session list section with search-aware rendering. The full session list block (lines 104-135) becomes:

```tsx
{/* Session List */}
{!collapsed && (
  <div className="flex-1 overflow-y-auto px-2 space-y-0.5">
    {/* Search input */}
    <div className="relative mb-2">
      <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
      <input
        type="text"
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
        placeholder="搜索聊天记录..."
        className="w-full pl-8 pr-8 py-1.5 text-xs rounded-lg border border-border bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
      />
      {searchQuery && (
        <button
          onClick={() => setSearchQuery('')}
          className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
        >
          <X size={12} />
        </button>
      )}
    </div>

    {/* Search results or normal list */}
    {searchQuery.trim() ? (
      isSearching ? (
        <div className="p-3 text-muted-foreground text-xs text-center">搜索中...</div>
      ) : searchResults.length === 0 ? (
        <div className="p-3 text-muted-foreground text-xs text-center">无匹配结果</div>
      ) : (
        searchResults.map((result) => (
          <button
            key={result.sessionId}
            onClick={() => {
              navigate(`/chat/${result.sessionId}`)
              setSearchQuery('')
            }}
            className="flex flex-col w-full p-2 rounded-lg text-sm text-left hover:bg-accent transition-colors"
          >
            <span className="truncate text-foreground text-xs font-medium">{result.title}</span>
            <span className="truncate text-muted-foreground text-xs mt-0.5">{result.matchedPreview}</span>
          </button>
        ))
      )
    ) : (
      sessions.length === 0 ? (
        <div className="p-3 text-muted-foreground text-xs text-center">暂无会话</div>
      ) : (
        sessions.map((session) => (
          <button
            key={session.sessionId}
            onClick={() => navigate(`/chat/${session.sessionId}`)}
            className={`flex items-center justify-between w-full p-2 rounded-lg text-sm text-left hover:bg-accent transition-colors group ${
              currentSessionId === session.sessionId ? 'bg-accent' : ''
            }`}
          >
            <div className="flex items-center gap-2 min-w-0">
              <MessageSquare size={16} className="shrink-0 text-muted-foreground" />
              <span className="truncate text-foreground">{session.title}</span>
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation()
                void handleDeleteSession(session.sessionId)
              }}
              className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-destructive/10 text-muted-foreground hover:text-destructive"
            >
              <Trash2 size={14} />
            </button>
          </button>
        ))
      )
    )}
  </div>
)}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/types/index.ts frontend/src/api/rag.ts frontend/src/stores/sessionStore.ts frontend/src/components/layout/Sidebar.tsx
git commit -m "feat: add chat history search in sidebar"
```
