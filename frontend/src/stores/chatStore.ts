import { create } from 'zustand'
import type { Message, RagResponse } from '@/types'
import { streamRag } from '@/api/rag'

interface ChatState {
  messages: Message[]
  currentSessionId: string | null
  isStreaming: boolean
  streamingContent: string
  sendMessage: (question: string) => Promise<void>
  clearMessages: () => void
  loadSession: (sessionId: string, messages: Message[]) => void
}

function generateId(): string {
  return crypto.randomUUID()
}

function generateSessionId(): string {
  return `session-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export const useChatStore = create<ChatState>()((set, get) => ({
  messages: [],
  currentSessionId: null,
  isStreaming: false,
  streamingContent: '',

  clearMessages: () =>
    set({ messages: [], currentSessionId: null }),

  loadSession: (sessionId, messages) =>
    set({ currentSessionId: sessionId, messages }),

  sendMessage: async (question: string) => {
    const state = get()
    if (state.isStreaming || !question.trim()) return

    const userMessage: Message = {
      id: generateId(),
      role: 'user',
      content: question.trim(),
      citations: [],
      timestamp: Date.now(),
    }

    const assistantMessage: Message = {
      id: generateId(),
      role: 'assistant',
      content: '',
      citations: [],
      timestamp: Date.now(),
    }

    const sessionId = state.currentSessionId || generateSessionId()

    set({
      messages: [...state.messages, userMessage, assistantMessage],
      currentSessionId: sessionId,
      isStreaming: true,
      streamingContent: '',
    })

    try {
      const stream = streamRag({
        question: question.trim(),
        sessionId,
      })

      const reader = stream.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let eventType = 'token'
      let dataLines: string[] = []

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            eventType = line.slice(7).trim()
          } else if (line.startsWith('data: ')) {
            dataLines.push(line.slice(6))
          } else if (line === '') {
            // Empty line = event boundary. Process accumulated data.
            if (dataLines.length > 0) {
              const data = dataLines.join('\n')
              dataLines = []

              set((state) => {
                const msgs = [...state.messages]
                const lastMsg = msgs[msgs.length - 1]
                if (!lastMsg || lastMsg.role !== 'assistant') return state

                if (eventType === 'token') {
                  lastMsg.content += data
                  return { messages: msgs, streamingContent: lastMsg.content }
                } else if (eventType === 'done') {
                  try {
                    const response = JSON.parse(data) as RagResponse
                    lastMsg.content = response.answer
                    lastMsg.citations = response.citations || []
                    return {
                      messages: msgs,
                      currentSessionId: response.sessionId || state.currentSessionId,
                    }
                  } catch {
                    return state
                  }
                } else if (eventType === 'error') {
                  try {
                    const errData = JSON.parse(data) as { message: string }
                    lastMsg.error = errData.message || 'Stream error'
                  } catch {
                    lastMsg.error = 'Stream error'
                  }
                  return { messages: msgs }
                }

                return state
              })
            }
            eventType = 'token'
          }
        }
      }
    } catch (err) {
      set((state) => {
        const msgs = [...state.messages]
        const lastMsg = msgs[msgs.length - 1]
        if (lastMsg && lastMsg.role === 'assistant') {
          lastMsg.error = err instanceof Error ? err.message : 'Unknown error'
        }
        return { messages: msgs }
      })
    } finally {
      set({ isStreaming: false, streamingContent: '' })
    }
  },
}))
