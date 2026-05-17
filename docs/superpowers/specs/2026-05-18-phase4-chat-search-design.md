# Phase 4: Chat History Search — Design Spec

Date: 2026-05-18

## Overview

Add full-text search for chat history in the sidebar, using SQLite LIKE query on messages table.

## Backend Design

### SessionController

New endpoint: `GET /api/rag/sessions/search?q={keyword}`

Returns sessions that contain messages matching the keyword:

```json
{
  "sessions": [
    {
      "sessionId": "xxx",
      "title": "关于假期的问题",
      "createdAt": "2026-05-17T10:00:00",
      "matchedPreview": "...公司年假制度是..."  // snippet with keyword highlighted context
    }
  ]
}
```

**SessionRepository changes:**
New method:
```sql
SELECT DISTINCT s.session_id, s.title, s.created_at,
       SUBSTR(m.content, MAX(1, INSTR(m.content, ?) - 20), 60) AS matched_preview
FROM sessions s
JOIN messages m ON s.session_id = m.session_id
WHERE m.content LIKE '%' || ? || '%'
ORDER BY s.last_active_at DESC
LIMIT 20
```

This returns unique sessions with a preview snippet (40 chars around the match), limited to 20 results.

## Frontend Design

### Sidebar.tsx

Add search input above the session list:
- Search icon + text input with placeholder "搜索聊天记录..."
- Debounced input (300ms) triggers `searchSessions(keyword)` API call
- When search keyword is non-empty:
  - Hide the normal session list
  - Show search results (matched sessions with preview snippet)
  - Clicking a result navigates to that session
- When search keyword is empty: show normal session list
- Clear button (X icon) to reset search

### sessionStore.ts

New state + action:
```ts
searchResults: SearchResult[]
isSearching: boolean
searchSessions: (keyword: string) => Promise<void>
clearSearch: () => void
```

### rag.ts API

New function:
```ts
export async function searchSessions(keyword: string): Promise<SearchResult[]>
```

Calls `GET /api/rag/sessions/search?q={encodeURIComponent(keyword)}`.

### SearchResult type

```ts
interface SearchResult {
  sessionId: string
  title: string
  createdAt: string
  matchedPreview: string
}
```

## Files to Modify

### Backend

| File | Changes |
|------|---------|
| `SessionController.java` | Add GET /sessions/search endpoint |
| `SessionRepository.java` | Add searchByKeyword method with JOIN query |

### Frontend

| File | Changes |
|------|---------|
| `api/rag.ts` | Add searchSessions API call |
| `stores/sessionStore.ts` | Add searchResults, searchSessions, clearSearch |
| `components/layout/Sidebar.tsx` | Add search input, render search results |
| `types/index.ts` | Add SearchResult interface |

## Testing

- Type keyword in sidebar search → matching sessions appear with preview
- Click search result → navigates to that session
- Clear search → normal session list restored
- Search with no matches → "无匹配结果" message
- Search is debounced (no request per keystroke)
