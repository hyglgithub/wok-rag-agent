import { useSettingsStore } from '@/stores/settingsStore'

function getBaseUrl(): string {
  return useSettingsStore.getState().settings.apiUrl
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${getBaseUrl()}${path}`
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      errorCode: 'UNKNOWN_ERROR',
      errorMessage: `HTTP ${response.status}`,
    }))
    throw error
  }

  return response.json()
}

