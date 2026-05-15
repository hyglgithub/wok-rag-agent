import { useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { useChatStore } from '@/stores/chatStore'
import { useSessionStore } from '@/stores/sessionStore'
import MessageBubble from '@/components/chat/MessageBubble'
import ChatInput from '@/components/chat/ChatInput'
import { MessageSquare, Loader2 } from 'lucide-react'

export default function ChatPage() {
  const { sessionId } = useParams()
  const { messages, isStreaming, streamingContent, sendMessage } = useChatStore()
  const { addLocalSession } = useSessionStore()
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Load session from route param
  useEffect(() => {
    if (sessionId) {
      useChatStore.setState({ currentSessionId: sessionId })
    }
  }, [sessionId])

  // Scroll to bottom on new messages or streaming
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length, streamingContent])

  async function handleSend(question: string) {
    await sendMessage(question)
    const currentId = useChatStore.getState().currentSessionId
    if (currentId) {
      addLocalSession(currentId, question.slice(0, 30) + (question.length > 30 ? '...' : ''))
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* Messages area */}
      <div className="flex-1 overflow-y-auto">
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-muted-foreground">
            <MessageSquare size={48} className="mb-4 opacity-30" />
            <p className="text-lg font-medium">有什么可以帮你的？</p>
            <p className="text-sm mt-1">基于知识库的智能问答助手</p>
          </div>
        ) : (
          <div className="max-w-3xl mx-auto py-4">
            {messages.map((msg) => (
              <MessageBubble key={msg.id} message={msg} />
            ))}

            {/* Streaming indicator */}
            {isStreaming && !streamingContent && (
              <div className="flex items-center gap-2 px-4 py-2 text-muted-foreground text-sm">
                <Loader2 size={16} className="animate-spin" />
                <span>思考中...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Input area */}
      <ChatInput disabled={isStreaming} onSend={handleSend} />
    </div>
  )
}
