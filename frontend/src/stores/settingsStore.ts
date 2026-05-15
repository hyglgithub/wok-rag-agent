import { create } from 'zustand'
import type { Settings } from '@/types'

interface SettingsState {
  settings: Settings
}

export const useSettingsStore = create<SettingsState>()(() => ({
  settings: {
    apiUrl: 'http://localhost:8080',
    theme: 'light' as const,
    language: 'zh' as const,
  },
}))
