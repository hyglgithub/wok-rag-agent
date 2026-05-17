# Phase 3: Knowledge Base Enhancements — Design Spec

Date: 2026-05-18

## Overview

Three enhancements to the knowledge base module:
1. Duplicate file detection (SHA-256 hash)
2. Original file storage, download, and PDF inline preview
3. Chunk viewing, editing, and adding (with auto re-embed)

---

## 1. Duplicate File Detection

### Problem

Uploading the same file twice creates duplicate documents and chunks in Milvus, polluting search results.

### Backend Design

**DocumentRepository changes:**
- Add `file_hash` column to `documents` table: `file_hash TEXT NOT NULL DEFAULT ''`
- Add `findByHash(hash: string): DocumentInfo | null`
- Migration: `ALTER TABLE documents ADD COLUMN file_hash TEXT NOT NULL DEFAULT ''`

**DocumentController.uploadDocument():**
- Before processing, compute SHA-256 hash of the uploaded file bytes
- Call `documentRepository.findByHash(hash)`
- If found, return `409 Conflict` with message: `"文件已存在: {existingDoc.name}"`
- Otherwise, proceed with upload and save hash to SQLite

**Hash computation:**
```java
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] hashBytes = md.digest(file.getBytes());
String fileHash = Hex.encodeHexString(hashBytes);
```

### Frontend Design

**document.ts API:**
- Handle 409 response from upload, extract error message

**UploadDialog.tsx:**
- On 409 error, show toast: `"文件已存在: {name}"` instead of generic upload failure

---

## 2. File Storage, Download & Preview

### Problem

Original files are discarded after text extraction. Users cannot download or preview source documents.

### Backend Design

**FileStorageService (new):**
- Path: `src/main/java/com/wokrag/agent/service/document/FileStorageService.java`
- Storage root: configurable via `file.storage.path` (default: `data/documents/`)
- `store(docId: String, fileName: String, content: byte[]): String` — saves file as `{storageRoot}/{docId}/{fileName}`, returns the relative path
- `load(docId: String): Resource` — returns file as Spring `Resource` for streaming
- `delete(docId: String)` — removes the directory

**DocumentRepository changes:**
- Add `file_path` column: `file_path TEXT NOT NULL DEFAULT ''`
- Stores relative path from storage root

**DocumentController changes:**
- In `uploadDocument()`: after parsing, call `fileStorageService.store(docId, fileName, file.getBytes())`
- New endpoint: `GET /api/documents/{docId}/download` — returns file with `Content-Disposition: attachment`
- New endpoint: `GET /api/documents/{docId}/preview` — returns file with `Content-Disposition: inline` (for PDF viewer)
- In `deleteDocument()`: also call `fileStorageService.delete(docId)`

**application.yml:**
```yaml
file:
  storage:
    path: data/documents/
```

### Frontend Design

**document.ts API:**
- New: `downloadDocument(docId: string)` — opens `{API_URL}/api/documents/{docId}/download` in new tab
- New: `previewDocumentUrl(docId: string)` — returns URL string for iframe/embed

**KnowledgePage.tsx:**
- Add "下载" button per row in the documents table
- Add "预览" button (only for PDF files, check `name.endsWith('.pdf')`)

**PDF Preview:**
- New component: `PdfPreviewDialog.tsx` — renders an `<embed>` or `<iframe>` with the preview URL in a full-width Dialog
- Or simply open preview URL in a new browser tab (simpler)

**CitationCard.tsx:**
- "查看来源" link currently goes to `sourceUrl`. Change to: if `sourceUrl` is empty, construct download URL from `docId` (need to add `docId` to Citation type from backend)

---

## 3. Chunk Viewing, Editing & Adding

### Problem

Users cannot see how documents were split, edit chunk content, or add custom chunks.

### Backend Design

**New endpoints on DocumentController:**

`GET /api/documents/{docId}/chunks` — returns all chunks for a document:
```json
{
  "chunks": [
    {
      "milvusId": 12345,
      "chunkText": "...",
      "chunkIndex": 0,
      "source": "doc.pdf"
    }
  ]
}
```

Implementation: query Milvus by `doc_id` filter, return `id`, `chunk_text`, `source` fields.

`PUT /api/documents/chunks/{milvusId}` — update a chunk's text:
1. Receive new text
2. Re-embed the text via `embeddingService.embedBatch([newText])`
3. Update Milvus record: `chunk_text` and `text_dense` fields by primary key

`POST /api/documents/{docId}/chunks` — add a new chunk:
1. Receive text
2. Embed it
3. Insert into Milvus with the existing `doc_id` and `source`
4. Update SQLite: increment `chunk_count`

**MilvusClientWrapper changes:**
- New: `updateByPrimaryKey(long id, String chunkText, float[] vector)` — updates chunk_text and text_dense
- New: `queryByDocId(String docId)` — returns all rows matching doc_id

### Frontend Design

**document.ts API:**
- New: `getDocumentChunks(docId: string)` — GET
- New: `updateChunk(milvusId: number, text: string)` — PUT
- New: `addChunk(docId: string, text: string)` — POST

**KnowledgePage.tsx:**
- Add "查看切片" button per document row
- On click, opens `ChunkManagerDialog`

**ChunkManagerDialog.tsx (new component):**
- Fetches chunks via `getDocumentChunks(docId)`
- Renders a scrollable list of chunks, each showing:
  - Chunk index badge
  - Text content (truncated preview)
  - "编辑" button → opens inline textarea or sub-dialog
  - "保存" button → calls `updateChunk(milvusId, newText)`, triggers auto re-embed
- Bottom: "添加切片" button → textarea + save, calls `addChunk(docId, text)`
- After edit/add: show toast success, refresh chunk list

**Re-embed flow:**
- After `updateChunk` or `addChunk` returns success, show toast "切片已更新，向量已重新生成"
- No explicit user action needed — backend handles re-embedding

---

## Files to Modify

### Backend

| File | Changes |
|------|---------|
| `DocumentRepository.java` | Add file_hash, file_path columns + findByHash |
| `DocumentController.java` | Hash check, file storage, download/preview endpoints, chunk endpoints |
| `FileStorageService.java` | NEW — local filesystem CRUD |
| `MilvusClientWrapper.java` | Add updateByPrimaryKey, queryByDocId |
| `schema.sql` | Add file_hash, file_path columns to documents table |
| `application.yml` | Add file.storage.path config |

### Frontend

| File | Changes |
|------|---------|
| `api/document.ts` | Add download, preview, chunk CRUD APIs |
| `pages/KnowledgePage.tsx` | Download/preview buttons, "查看切片" button |
| `components/common/ChunkManagerDialog.tsx` | NEW — chunk list, edit, add |
| `components/common/PdfPreviewDialog.tsx` | NEW — PDF embed preview (or use new tab) |
| `components/chat/CitationCard.tsx` | Update "查看来源" to use download URL fallback |

## Testing

- Upload same file twice → second upload returns 409 with toast
- Upload file → download returns original file
- Upload PDF → preview opens inline viewer
- Click "查看切片" → dialog shows all chunks
- Edit chunk text → re-embed happens automatically
- Add new chunk → appears in list, searchable
