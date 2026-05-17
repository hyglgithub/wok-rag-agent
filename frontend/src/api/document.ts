import { apiFetch } from './client'
import type { DocumentInfo, ChunkInfo } from '@/types'
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

export async function deleteDocument(docId: string): Promise<void> {
  await apiFetch(`/api/documents/${docId}`, { method: 'DELETE' })
}

export function downloadDocument(docId: string) {
  const baseUrl = useSettingsStore.getState().settings.apiUrl
  window.open(`${baseUrl}/api/documents/${docId}/download`, '_blank')
}

export function getPreviewUrl(docId: string): string {
  const baseUrl = useSettingsStore.getState().settings.apiUrl
  return `${baseUrl}/api/documents/${docId}/preview`
}

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
