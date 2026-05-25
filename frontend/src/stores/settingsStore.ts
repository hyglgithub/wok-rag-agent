import { create } from 'zustand'
import type { Settings } from '@/types'

const STORAGE_KEY = 'wok-rag-settings'

function loadSettings(): Settings {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    try {
      return JSON.parse(stored) as Settings
    } catch {
      // ignore
    }
  }
  return {
    apiUrl: 'http://localhost:8080',
    theme: 'light',
    sidebarTitle: 'Wok RAG Agent',
  }
}

function persist(settings: Settings) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
}

interface SettingsState {
  settings: Settings
  updateTheme: (theme: 'light' | 'dark') => void
  updateApiUrl: (url: string) => void
  updateSidebarTitle: (title: string) => void
}

export const useSettingsStore = create<SettingsState>()((set) => {
  const initial = loadSettings()

  // Apply theme on init
  if (initial.theme === 'dark') {
    document.documentElement.classList.add('dark')
  }

  return {
    settings: initial,
    updateTheme: (theme) =>
      set((state) => {
        if (theme === 'dark') {
          document.documentElement.classList.add('dark')
        } else {
          document.documentElement.classList.remove('dark')
        }
        const next = { ...state.settings, theme }
        persist(next)
        return { settings: next }
      }),
    updateApiUrl: (url) =>
      set((state) => {
        const next = { ...state.settings, apiUrl: url }
        persist(next)
        return { settings: next }
      }),
    updateSidebarTitle: (title) =>
      set((state) => {
        const next = { ...state.settings, sidebarTitle: title }
        persist(next)
        return { settings: next }
      }),
  }
})
