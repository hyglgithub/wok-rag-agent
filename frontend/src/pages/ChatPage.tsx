import { useEffect, useRef, useState, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { useChatStore } from '@/stores/chatStore'
import { useSessionStore } from '@/stores/sessionStore'
import { getSessionMessages } from '@/api/rag'
import MessageBubble from '@/components/chat/MessageBubble'
import ChatInput from '@/components/chat/ChatInput'
import { MessageSquare, Loader2 } from 'lucide-react'
import type { Message } from '@/types'

export default function ChatPage() {
  const { sessionId } = useParams()
  const { messages, isStreaming, streamingContent, sendMessage, stopGeneration, loadSession } = useChatStore()
  const { addLocalSession } = useSessionStore()
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Message nav state
  const [activeMsgId, setActiveMsgId] = useState<string | null>(null)
  const msgRefs = useRef<Map<string, HTMLDivElement>>(new Map())
  const navContainerRef = useRef<HTMLDivElement>(null)
  const isClicking = useRef(false)

  const setMsgRef = useCallback((el: HTMLDivElement | null, id: string) => {
    if (el) {
      msgRefs.current.set(id, el)
    } else {
      msgRefs.current.delete(id)
    }
  }, [])

  // IntersectionObserver for message visibility tracking
  useEffect(() => {
    if (messages.length === 0) return

    const observer = new IntersectionObserver(
      (entries) => {
        if (isClicking.current) return
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const id = entry.target.getAttribute('data-msg-id')
            if (id) setActiveMsgId(id)
          }
        }
      },
      { rootMargin: '-20% 0px -70% 0px', threshold: 0 }
    )

    const refs = msgRefs.current
    refs.forEach((el) => observer.observe(el))
    return () => observer.disconnect()
  }, [messages])

  // Set initial active message
  useEffect(() => {
    if (messages.length > 0 && !activeMsgId) {
      setActiveMsgId(messages[0].id)
    }
  }, [messages, activeMsgId])

  // Smart scroll nav container
  function smartScrollNav() {
    const container = navContainerRef.current
    if (!container) return
    const activeIndex = messages.findIndex(m => m.id === activeMsgId)
    if (activeIndex < 0) return
    const items = container.querySelectorAll('.msg-nav-item')
    if (activeIndex === 0) {
      container.scrollTo({ top: 0, behavior: 'smooth' })
    } else if (activeIndex === messages.length - 1) {
      container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' })
    } else {
      items[activeIndex]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }

  useEffect(() => {
    if (activeMsgId) smartScrollNav()
  }, [activeMsgId])

  function scrollToMsg(id: string) {
    isClicking.current = true
    setActiveMsgId(id)
    const el = msgRefs.current.get(id)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
    setTimeout(() => { isClicking.current = false }, 800)
  }

  // Load session messages
  useEffect(() => {
    if (!sessionId) return
    let isActive = true
    useChatStore.setState({ currentSessionId: sessionId })
    loadSession(sessionId, [])
    getSessionMessages(sessionId).then((sessionMessages) => {
      if (!isActive) return
      const messages: Message[] = sessionMessages.map((m, i) => ({
        id: `hist-${sessionId}-${i}`,
        role: m.role as 'user' | 'assistant',
        content: m.content,
        citations: m.citations || [],
        timestamp: new Date(m.timestamp).getTime(),
      }))
      loadSession(sessionId, messages)
    })
    return () => {
      isActive = false
    }
  }, [sessionId, loadSession])

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
              <div key={msg.id} ref={(el) => setMsgRef(el, msg.id)} data-msg-id={msg.id}>
                <MessageBubble message={msg} />
              </div>
            ))}

            {/* Streaming indicator */}
            {isStreaming && !streamingContent && (
              <div className="flex items-center gap-2 px-4 py-2 text-muted-foreground text-sm">
                <Loader2 size={16} className="animate-spin" />
                <span>检索中...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Message Navigation Sidebar */}
      {messages.length > 0 && (
        <>
          <style>{`
            .msg-nav {
              position: fixed;
              right: 0;
              top: 50%;
              transform: translateY(-50%);
              width: 30px;
              max-height: 70vh;
              background: transparent;
              transition: all 0.3s ease;
              overflow-y: auto;
              overflow-x: hidden;
              z-index: 99;
              scrollbar-width: none;
            }
            .msg-nav::-webkit-scrollbar { display: none; }
            .msg-nav:hover {
              width: 240px;
              background: var(--background);
              box-shadow: -2px 0 10px rgba(0,0,0,0.15);
              border-left: 1px solid var(--border);
              border-radius: 8px 0 0 8px;
            }
            .msg-nav-item {
              position: relative;
              display: flex;
              align-items: center;
              justify-content: flex-end;
              height: 38px;
              padding: 0 10px;
              cursor: pointer;
              width: 100%;
            }
            .msg-nav-bar {
              width: 12px;
              height: 4px;
              border-radius: 2px;
              background: var(--muted-foreground);
              flex-shrink: 0;
              margin-left: 12px;
              transition: all 0.2s ease;
            }
            .msg-nav-item.active .msg-nav-bar {
              width: 15px;
              height: 5px;
              background: var(--primary);
            }
            .msg-nav-text {
              position: absolute;
              right: 30px;
              width: 180px;
              font-size: 13px;
              color: var(--muted-foreground);
              opacity: 0;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
              text-align: right;
              transition: opacity 0.3s ease;
              pointer-events: none;
            }
            .msg-nav-item.active .msg-nav-text {
              color: var(--primary);
              font-weight: 500;
            }
            .msg-nav:hover .msg-nav-text { opacity: 1; }
          `}</style>
          <div
            ref={navContainerRef}
            className="msg-nav"
            onWheel={(e) => {
              e.stopPropagation()
              e.currentTarget.scrollTop += e.deltaY
            }}
          >
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`msg-nav-item ${activeMsgId === msg.id ? 'active' : ''}`}
                onClick={() => scrollToMsg(msg.id)}
              >
                <span className="msg-nav-text">
                  {msg.content.slice(0, 25)}
                  {msg.content.length > 25 ? '...' : ''}
                </span>
                <div className="msg-nav-bar" />
              </div>
            ))}
          </div>
        </>
      )}

      {/* Input area */}
      <ChatInput disabled={isStreaming} isStreaming={isStreaming} onSend={handleSend} onStop={stopGeneration} />
    </div>
  )
}
