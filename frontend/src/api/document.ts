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
