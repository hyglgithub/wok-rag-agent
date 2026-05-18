import { create } from 'zustand'
import type { Session } from '@/types'
import { getSessions, deleteSession as apiDeleteSession, searchSessions as apiSearchSessions } from '@/api/rag'
import type { SearchResult } from '@/types'

interface SessionState {
  sessions: Session[]
  loading: boolean
  searchResults: SearchResult[]
  isSearching: boolean
  searchKeyword: string
  fetchSessions: () => Promise<void>
  removeSession: (sessionId: string) => Promise<void>
  addLocalSession: (sessionId: string, title: string) => void
  searchSessions: (keyword: string) => Promise<void>
  clearSearch: () => void
}

export const useSessionStore = create<SessionState>()((set, get) => ({
  sessions: [],
  loading: false,
  searchResults: [],
  isSearching: false,
  searchKeyword: '',

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

  searchSessions: async (keyword: string) => {
    if (!keyword.trim()) {
      set({ searchResults: [], isSearching: false, searchKeyword: '' })
      return
    }
    set({ isSearching: true, searchKeyword: keyword })
    try {
      const results = await apiSearchSessions(keyword)
      if (get().searchKeyword !== keyword) return
      set({ searchResults: results })
    } finally {
      if (get().searchKeyword !== keyword) return
      set({ isSearching: false })
    }
  },

  clearSearch: () => set({ searchResults: [], isSearching: false, searchKeyword: '' }),
}))
