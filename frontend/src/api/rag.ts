import { apiFetch } from './client'
import { useSettingsStore } from '@/stores/settingsStore'
import type { QueryRequest, RagResponse, HealthResponse, Session, SessionMessage, SearchResult } from '@/types'

export function queryRag(request: QueryRequest): Promise<RagResponse> {
  return apiFetch<RagResponse>('/api/rag/query', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

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
