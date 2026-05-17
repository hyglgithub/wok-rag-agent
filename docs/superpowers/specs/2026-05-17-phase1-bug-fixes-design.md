# Phase 1: Bug Fixes & UX Polish — Design Spec

Date: 2026-05-17

## Overview

Fix 5 frontend UX issues: replace native alert/confirm with custom dialogs, add stop-generation button, fix citations sorting, fix sidebar session loading, and add sidebar delete confirmation.

## 1. ConfirmDialog Component

### Problem

Multiple pages use native `window.confirm()` and `window.alert()` for delete confirmations and error messages. These are visually inconsistent with the app's shadcn/ui design system and provide poor UX.

### Current usage sites

- `HistoryPage.tsx:17` — `confirm('确定删除此会话？')`
- `KnowledgePage.tsx:49` — `confirm('确定删除 "${doc.name}"？')`
- `KnowledgePage.tsx:44` — `alert('上传失败: ' + ...)`
- `KnowledgePage.tsx:54` — `alert('删除失败')`
- `Sidebar.tsx:123-126` — no confirmation at all

### Design

Create a `useConfirm` hook + `ConfirmDialog` component that returns a Promise<boolean>.

**File: `frontend/src/components/ui/confirm-dialog.tsx`**

```tsx
interface ConfirmOptions {
  title: string
  description: string
  confirmText?: string   // default: "确定"
  cancelText?: string    // default: "取消"
  variant?: 'default' | 'destructive'  // destructive = red confirm button
}

function useConfirm(): { confirm: (opts: ConfirmOptions) => Promise<boolean> }
```

Implementation:
- React Context + useState holds `{ isOpen, options, resolve }` state (lighter than Zustand for UI-only singleton state)
- `confirm()` sets the store state and returns a Promise that resolves `true` on confirm, `false` on cancel/close
- `ConfirmDialog` renders a shadcn `Dialog` with title, description, two buttons
- `destructive` variant uses `bg-destructive text-destructive-foreground` on the confirm button

**For `alert()` replacements**, use the existing `sonner` toast library (already in project):
```tsx
import { toast } from 'sonner'
toast.error('上传失败: ' + message)
```

### Migration

Replace all `confirm()` calls with `await confirm({ ... })` from `useConfirm`.
Replace all `alert()` calls with `toast.error()` or `toast.success()` from sonner.

## 2. Stop Generation Button

### Problem

During SSE streaming, the user has no way to cancel the request. The send button is disabled, and there's no stop button.

### Design

**`chatStore.ts` changes:**
- Add `abortController: AbortController | null` to store state
- `sendMessage()` creates a new `AbortController`, passes its signal to `streamRag()`
- New `stopGeneration()` action: calls `abortController.abort()`, sets `isStreaming: false`, preserves partial content
- On abort, the SSE stream reader is cancelled via the signal

**`ChatInput.tsx` changes:**
- Accept `isStreaming` and `onStop` props
- When `isStreaming`: button shows `Square` icon (red background), onClick calls `onStop`
- When not streaming: button shows `Send` icon (current behavior)

**`ChatPage.tsx` changes:**
- Pass `isStreaming` and `onStop={() => stopGeneration()}` to `ChatInput`

**Streaming cleanup:**
- In `sendMessage`, wrap the fetch + SSE parsing in a try/catch for `AbortError`
- On `AbortError`: set `isStreaming: false`, do NOT clear partial content (user sees what was received)

## 3. Citations Sorting

### Problem

Citations render in API response order, not by `index` field.

### Design

In `MessageBubble.tsx`, sort citations before rendering:

```tsx
const sortedCitations = useMemo(
  () => [...message.citations].sort((a, b) => a.index - b.index),
  [message.citations]
)
```

Replace `message.citations.map(...)` with `sortedCitations.map(...)`.

## 4. Sidebar Session Loading

### Problem

Sidebar shows empty session list on fresh page load. Sessions only appear after visiting `/history` or creating a new chat.

### Root Cause

`Sidebar.tsx` reads sessions from `useSessionStore` but never triggers `fetchSessions()`. The fetch only happens in `HistoryPage.tsx`'s `useEffect`.

### Design

Add `fetchSessions()` call in `AppLayout.tsx`'s `useEffect`:

```tsx
useEffect(() => {
  void fetchSessions()
}, [fetchSessions])
```

This ensures sessions are loaded regardless of which route the user lands on. `AppLayout` wraps all routes, so this runs once on app mount.

## 5. Sidebar Delete Confirmation

### Problem

Sidebar delete button (`Sidebar.tsx:123-126`) immediately deletes without any confirmation.

### Design

Use the `ConfirmDialog` from item #1:

```tsx
const { confirm } = useConfirm()

async function handleDelete(sessionId: string) {
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

## Files to Modify

| File | Changes |
|------|---------|
| `frontend/src/components/ui/confirm-dialog.tsx` | NEW — ConfirmDialog + useConfirm hook |
| `frontend/src/stores/chatStore.ts` | Add abortController, stopGeneration() |
| `frontend/src/components/chat/ChatInput.tsx` | Stop button variant when streaming |
| `frontend/src/pages/ChatPage.tsx` | Pass stop handler to ChatInput |
| `frontend/src/components/chat/MessageBubble.tsx` | Sort citations by index |
| `frontend/src/components/layout/AppLayout.tsx` | Fetch sessions on mount |
| `frontend/src/components/layout/Sidebar.tsx` | Add delete confirmation |
| `frontend/src/pages/HistoryPage.tsx` | Replace confirm() with useConfirm |
| `frontend/src/pages/KnowledgePage.tsx` | Replace confirm()/alert() with useConfirm/toast |
| `frontend/src/api/rag.ts` | Add AbortSignal support to streamRag |

## Testing

- Verify custom dialog appears for all delete actions (history, knowledge, sidebar)
- Verify stop button appears during streaming and cancels the request
- Verify partial content is preserved on stop
- Verify citations render in index order
- Verify sidebar shows sessions on fresh page load
