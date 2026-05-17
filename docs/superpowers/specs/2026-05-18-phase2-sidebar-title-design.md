# Phase 2: Custom Sidebar Title — Design Spec

Date: 2026-05-18

## Overview

Allow users to customize the sidebar header text ("Wok RAG Agent") via the Settings page.

## Design

### settingsStore.ts

Add `sidebarTitle` field:
```ts
sidebarTitle: string  // default: 'Wok RAG Agent'
updateSidebarTitle: (title: string) => void
```

Persisted to localStorage alongside other settings.

### SettingsPage.tsx

Add a new section below existing settings:
- Label: "侧边栏标题"
- Text input bound to `settings.sidebarTitle`
- Calls `updateSidebarTitle` on change

### Sidebar.tsx

Replace hardcoded `"Wok RAG Agent"` with `settings.sidebarTitle` from `useSettingsStore`.

## Files to Modify

| File | Changes |
|------|---------|
| `frontend/src/stores/settingsStore.ts` | Add sidebarTitle field + updateSidebarTitle action |
| `frontend/src/pages/SettingsPage.tsx` | Add sidebar title input |
| `frontend/src/components/layout/Sidebar.tsx` | Read sidebarTitle from settingsStore |
