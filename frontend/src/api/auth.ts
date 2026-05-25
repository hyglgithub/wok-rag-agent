import { apiFetch } from './client'

export async function login(token: string): Promise<void> {
  await apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ token }),
  })
}

export async function checkAuth(token: string): Promise<boolean> {
  try {
    const res = await apiFetch<{ authenticated: boolean }>('/api/auth/status', {
      headers: { 'X-API-Key': token },
    })
    return res.authenticated
  } catch {
    return false
  }
}
