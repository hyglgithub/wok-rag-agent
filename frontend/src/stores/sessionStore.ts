import { create } from 'zustand'
import type { Session } from '@/types'
import { getSessions, deleteSession as apiDeleteSession } from '@/api/rag'

interface SessionState {
  sessions: Session[]
  loading: boolean
  fetchSessions: () => Promise<void>
  removeSession: (sessionId: string) => Promise<void>
  addLocalSession: (sessionId: string, title: string) => void
}

export const useSessionStore = create<SessionState>()((set) => ({
  sessions: [],
  loading: false,

  fetchSessions: async () => {
    set({ loading: true })
    try {
      const sessions = await getSessions()
      set({ sessions })
    } finally {
      set({ loading: false })
    }
  },

  removeSession: async (sessionId: string) => {
    try {
      await apiDeleteSession(sessionId)
      set((state) => ({
        sessions: state.sessions.filter((s) => s.sessionId !== sessionId),
      }))
    } catch {
      // Silently fail if backend doesn't support this yet
    }
  },

  addLocalSession: (sessionId: string, title: string) => {
    set((state) => {
      if (state.sessions.find((s) => s.sessionId === sessionId)) return state
      return {
        sessions: [
          {
            sessionId,
            title,
            lastMessage: '',
            lastTime: new Date().toISOString(),
            messageCount: 0,
          },
          ...state.sessions,
        ],
      }
    })
  },
}))
