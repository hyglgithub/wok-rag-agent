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

export function createSSEStream(
  path: string,
  body: unknown
): ReadableStream<Uint8Array> {
  const url = `${getBaseUrl()}${path}`

  return new ReadableStream({
    async start(controller) {
      try {
        const response = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
        })

        if (!response.ok) {
          const error = await response.json().catch(() => ({
            message: `HTTP ${response.status}`,
          }))
          const encoder = new TextEncoder()
          controller.enqueue(encoder.encode(`event: error\ndata: ${JSON.stringify(error)}\n\n`))
          controller.close()
          return
        }

        const reader = response.body?.getReader()
        if (!reader) {
          controller.close()
          return
        }

        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          controller.enqueue(value)
        }
        controller.close()
      } catch (err) {
        const encoder = new TextEncoder()
        const error = { message: err instanceof Error ? err.message : 'Network error' }
        controller.enqueue(encoder.encode(`event: error\ndata: ${JSON.stringify(error)}\n\n`))
        controller.close()
      }
    },
  })
}
