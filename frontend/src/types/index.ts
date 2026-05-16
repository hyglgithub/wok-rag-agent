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

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations: Citation[]
  timestamp: number
  error?: string
  isStreaming?: boolean
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
