import { create } from 'zustand'
import type { Message, RagResponse } from '@/types'
import { streamRag } from '@/api/rag'
import { parseSSEStream } from '@/lib/sseParser'

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
      isStreaming: true,
    }

    const sessionId = state.currentSessionId || generateSessionId()

    set({
      messages: [...state.messages, userMessage, assistantMessage],
      currentSessionId: sessionId,
      isStreaming: true,
      streamingContent: '',
    })

    try {
      const response = await streamRag({
        question: question.trim(),
        sessionId,
      })

      const body = response.body
      if (!body) throw new Error('Response body is null')

      for await (const event of parseSSEStream(body)) {
        if (event.event === 'token') {
          set((state) => {
            const msgs = [...state.messages]
            const lastMsg = msgs[msgs.length - 1]
            if (!lastMsg || lastMsg.role !== 'assistant') return state
            const updated = { ...lastMsg, content: lastMsg.content + event.data }
            msgs[msgs.length - 1] = updated
            return { messages: msgs, streamingContent: updated.content }
          })
        }

        if (event.event === 'done') {
          const result = JSON.parse(event.data) as RagResponse
          set((state) => {
            const msgs = [...state.messages]
            const lastMsg = msgs[msgs.length - 1]
            if (!lastMsg || lastMsg.role !== 'assistant') return state
            msgs[msgs.length - 1] = {
              ...lastMsg,
              content: result.answer,
              citations: result.citations || [],
              isStreaming: false,
            }
            return {
              messages: msgs,
              currentSessionId: result.sessionId || state.currentSessionId,
            }
          })
        }

        if (event.event === 'error') {
          let errorMsg = 'Stream error'
          try {
            const errData = JSON.parse(event.data)
            errorMsg = errData.message || errorMsg
          } catch { /* use default */ }
          set((state) => {
            const msgs = [...state.messages]
            const lastMsg = msgs[msgs.length - 1]
            if (lastMsg && lastMsg.role === 'assistant') {
              msgs[msgs.length - 1] = { ...lastMsg, error: errorMsg, isStreaming: false }
            }
            return { messages: msgs }
          })
        }
      }
    } catch (err) {
      set((state) => {
        const msgs = [...state.messages]
        const lastMsg = msgs[msgs.length - 1]
        if (lastMsg && lastMsg.role === 'assistant') {
          msgs[msgs.length - 1] = {
            ...lastMsg,
            error: err instanceof Error ? err.message : 'Unknown error',
            isStreaming: false,
          }
        }
        return { messages: msgs }
      })
    } finally {
      set({ isStreaming: false, streamingContent: '' })
    }
  },
}))
