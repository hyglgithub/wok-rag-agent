export interface SSEEvent {
  event: string
  data: string
}

export async function* parseSSEStream(
  stream: ReadableStream<Uint8Array>
): AsyncGenerator<SSEEvent> {
  const reader = stream.getReader()
  const decoder = new TextDecoder('utf-8')

  let lineBuffer = ''
  let currentEvent = ''
  let currentDataLines: string[] = []

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      lineBuffer += decoder.decode(value, { stream: true })

      const lines = lineBuffer.split('\n')
      // Last element may be a partial line — keep it in buffer
      lineBuffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          // Handle both "data:payload" and "data: payload"
          const payload = line[5] === ' ' ? line.slice(6) : line.slice(5)
          currentDataLines.push(payload)
        } else if (line === '') {
          // Blank line = end of event
          if (currentEvent && currentDataLines.length > 0) {
            yield { event: currentEvent, data: currentDataLines.join('\n') }
          }
          currentEvent = ''
          currentDataLines = []
        }
        // Ignore other prefixes (id:, retry:, :comments)
      }
    }

    // Flush remaining bytes from TextDecoder
    lineBuffer += decoder.decode()

    // Process any remaining lines after stream ends
    if (lineBuffer) {
      const lines = lineBuffer.split('\n')
      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const payload = line[5] === ' ' ? line.slice(6) : line.slice(5)
          currentDataLines.push(payload)
        } else if (line === '') {
          if (currentEvent && currentDataLines.length > 0) {
            yield { event: currentEvent, data: currentDataLines.join('\n') }
          }
          currentEvent = ''
          currentDataLines = []
        }
      }
    }

    // Yield any final pending event (stream may not end with \n\n)
    if (currentEvent && currentDataLines.length > 0) {
      yield { event: currentEvent, data: currentDataLines.join('\n') }
    }
  } finally {
    reader.releaseLock()
  }
}
