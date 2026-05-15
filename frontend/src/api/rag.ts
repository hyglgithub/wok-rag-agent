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
