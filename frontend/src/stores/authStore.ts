import { create } from 'zustand'

const TOKEN_KEY = 'wok-rag-token'

interface AuthState {
  token: string | null
  isAuthenticated: boolean
  setToken: (token: string) => void
  clearToken: () => void
  loadToken: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  token: null,
  isAuthenticated: false,

  setToken: (token) => {
    if (!token) {
      localStorage.removeItem(TOKEN_KEY)
      set({ token: null, isAuthenticated: false })
      return
    }
    localStorage.setItem(TOKEN_KEY, token)
    set({ token, isAuthenticated: true })
  },

  clearToken: () => {
    localStorage.removeItem(TOKEN_KEY)
    set({ token: null, isAuthenticated: false })
  },

  loadToken: () => {
    const stored = localStorage.getItem(TOKEN_KEY)
    if (stored) {
      set({ token: stored, isAuthenticated: true })
    }
  },
}))
