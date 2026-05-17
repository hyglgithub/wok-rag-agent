import { useState, useRef, useEffect, useCallback } from 'react'
import { Button } from '@/components/ui/button'
import { Send, Square } from 'lucide-react'

interface Props {
  disabled: boolean
  isStreaming: boolean
  onSend: (question: string) => void
  onStop: () => void
}

export default function ChatInput({ disabled, isStreaming, onSend, onStop }: Props) {
  const [input, setInput] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const adjustHeight = useCallback(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }, [])

  useEffect(() => {
    textareaRef.current?.focus()
  }, [])

  function handleSend() {
    if (input.trim()) {
      onSend(input)
      setInput('')
      requestAnimationFrame(() => adjustHeight())
    }
  }

  function handleKeydown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="border-t border-border bg-background p-4">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-end gap-2 rounded-xl border border-border bg-muted p-2">
          <textarea
            ref={textareaRef}
            value={input}
            onChange={(e) => {
              setInput(e.target.value)
              adjustHeight()
            }}
            onKeyDown={handleKeydown}
            placeholder="输入你的问题... (Enter 发送, Shift+Enter 换行)"
            rows={1}
            className="flex-1 bg-transparent resize-none px-2 py-1.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none"
            style={{ maxHeight: '200px' }}
          />
          {isStreaming ? (
            <Button onClick={onStop} size="icon" variant="destructive">
              <Square size={18} />
            </Button>
          ) : (
            <Button
              onClick={handleSend}
              disabled={disabled || !input.trim()}
              size="icon"
            >
              <Send size={18} />
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
