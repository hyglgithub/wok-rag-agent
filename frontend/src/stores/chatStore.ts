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
      const response = await streamRag({
        question: question.trim(),
        sessionId,
      })

      const text = await response.text()

      // Find the done event and extract its JSON payload
      const doneIdx = text.indexOf('event:done')
      if (doneIdx !== -1) {
        const afterDone = text.slice(doneIdx)
        const lines = afterDone.split('\n')
        const dataLines: string[] = []

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            dataLines.push(line.slice(6))
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5))
          } else if (line === '' && dataLines.length > 0) {
            break
          }
        }

        if (dataLines.length > 0) {
          try {
            const result = JSON.parse(dataLines.join('\n')) as RagResponse
            set((state) => {
              const msgs = [...state.messages]
              const lastMsg = msgs[msgs.length - 1]
              if (!lastMsg || lastMsg.role !== 'assistant') return state
              lastMsg.content = result.answer
              lastMsg.citations = result.citations || []
              return {
                messages: msgs,
                currentSessionId: result.sessionId || state.currentSessionId,
              }
            })
          } catch {
            set((state) => {
              const msgs = [...state.messages]
              const lastMsg = msgs[msgs.length - 1]
              if (lastMsg && lastMsg.role === 'assistant') {
                lastMsg.error = 'Failed to parse response'
              }
              return { messages: msgs }
            })
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
