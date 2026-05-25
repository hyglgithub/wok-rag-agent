import { useSettingsStore } from '@/stores/settingsStore'
import { useAuthStore } from '@/stores/authStore'

function getBaseUrl(): string {
  return useSettingsStore.getState().settings.apiUrl
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${getBaseUrl()}${path}`
  const token = useAuthStore.getState().token

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }
  if (token) {
    headers['X-API-Key'] = token
  }

  const response = await fetch(url, {
    ...options,
    headers,
  })

  if (response.status === 401) {
    useAuthStore.getState().clearToken()
    throw { errorCode: 'UNAUTHORIZED', errorMessage: 'Authentication required' }
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      errorCode: 'UNKNOWN_ERROR',
      errorMessage: `HTTP ${response.status}`,
    }))
    throw error
  }

  return response.json()
}

