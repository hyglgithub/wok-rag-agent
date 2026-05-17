import React, { useMemo } from 'react'
import type { Message } from '@/types'
import CitationCard from './CitationCard'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { User, Bot } from 'lucide-react'

interface Props {
  message: Message
}

function MessageBubble({ message }: Props) {
  const isUser = message.role === 'user'
  const sortedCitations = useMemo(
    () => [...message.citations].sort((a, b) => a.index - b.index),
    [message.citations]
  )

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
        ) : isUser ? (
          <div className="whitespace-pre-wrap">{message.content}</div>
        ) : (
          <div className="text-sm leading-relaxed">
            <Markdown
              remarkPlugins={[remarkGfm]}
              components={{
                p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
                strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
                em: ({ children }) => <em className="italic">{children}</em>,
                ol: ({ children }) => <ol className="list-decimal list-inside mb-2 space-y-1">{children}</ol>,
                ul: ({ children }) => <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul>,
                li: ({ children }) => <li>{children}</li>,
                code: ({ className, children, ...props }) => {
                  const isInline = !className
                  return isInline ? (
                    <code className="bg-muted px-1 py-0.5 rounded text-xs" {...props}>{children}</code>
                  ) : (
                    <code className={className} {...props}>{children}</code>
                  )
                },
                pre: ({ children }) => <pre className="bg-muted p-3 rounded-md overflow-x-auto mb-2 text-xs">{children}</pre>,
                h1: ({ children }) => <h1 className="text-lg font-bold mb-2">{children}</h1>,
                h2: ({ children }) => <h2 className="text-base font-bold mb-2">{children}</h2>,
                h3: ({ children }) => <h3 className="text-sm font-bold mb-1">{children}</h3>,
                blockquote: ({ children }) => <blockquote className="border-l-2 border-muted-foreground pl-3 italic mb-2">{children}</blockquote>,
                a: ({ href, children }) => <a href={href} className="text-primary underline" target="_blank" rel="noopener noreferrer">{children}</a>,
                table: ({ children }) => <table className="border-collapse mb-2 text-sm w-full">{children}</table>,
                th: ({ children }) => <th className="border border-border px-2 py-1 bg-muted font-semibold text-left">{children}</th>,
                td: ({ children }) => <td className="border border-border px-2 py-1">{children}</td>,
                hr: () => <hr className="border-border my-2" />,
              }}
            >
              {message.content}
            </Markdown>
            {message.isStreaming && (
              <span className="inline-block w-0.5 h-4 bg-foreground animate-blink align-middle ml-0.5" />
            )}
          </div>
        )}

        {/* Citations */}
        {message.citations.length > 0 && (
          <div className="mt-3 pt-3 border-t border-border space-y-2">
            {sortedCitations.map((citation) => (
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

export default React.memo(MessageBubble)
