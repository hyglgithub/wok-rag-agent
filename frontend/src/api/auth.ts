import { apiFetch } from './client'

export async function login(token: string): Promise<void> {
  await apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ token }),
  })
}

export interface AuthStatus {
  authenticated: boolean
  authEnabled: boolean
}

export async function checkAuth(token: string | null): Promise<AuthStatus> {
  try {
    const headers: Record<string, string> = {}
    if (token) {
      headers['X-API-Key'] = token
    }
    const res = await apiFetch<{ authenticated: boolean; authEnabled: boolean }>('/api/auth/status', {
      headers,
    })
    return { authenticated: res.authenticated, authEnabled: res.authEnabled }
  } catch {
    return { authenticated: false, authEnabled: true }
  }
}
