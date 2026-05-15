import type { Message } from '@/types'
import CitationCard from './CitationCard'
import { User, Bot } from 'lucide-react'

interface Props {
  message: Message
}

export default function MessageBubble({ message }: Props) {
  const isUser = message.role === 'user'

  return (
    <div className={`flex gap-3 py-4 px-4 ${isUser ? 'justify-end' : 'justify-start'}`}>
      {/* Avatar (assistant only) */}
      {!isUser && (
        <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center shrink-0">
          <Bot size={16} className="text-primary-foreground" />
        </div>
      )}

      {/* Message content */}
      <div
        className={`max-w-[70%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${
          isUser ? 'bg-primary text-primary-foreground' : 'bg-muted text-foreground'
        }`}
      >
        {message.error ? (
          <div className="text-destructive">{message.error}</div>
        ) : (
          <div className="whitespace-pre-wrap">{message.content}</div>
        )}

        {/* Citations */}
        {message.citations.length > 0 && (
          <div className="mt-3 pt-3 border-t border-border space-y-2">
            {message.citations.map((citation) => (
              <CitationCard key={citation.index} citation={citation} />
            ))}
          </div>
        )}
      </div>

      {/* Avatar (user only) */}
      {isUser && (
        <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center shrink-0">
          <User size={16} className="text-muted-foreground" />
        </div>
      )}
    </div>
  )
}
