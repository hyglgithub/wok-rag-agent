import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { Citation } from '@/types'
import { ChevronDown, ChevronUp, ExternalLink } from 'lucide-react'

interface Props {
  citation: Citation
}

export default function CitationCard({ citation }: Props) {
  const [expanded, setExpanded] = useState(false)

  return (
    <div className="rounded-lg bg-muted border border-border text-xs">
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center justify-between w-full p-2 hover:bg-accent transition-colors"
      >
        <div className="flex items-center gap-2">
          <span className="w-5 h-5 rounded bg-primary/10 text-primary flex items-center justify-center text-xs font-medium">
            {citation.index}
          </span>
          <span className="text-muted-foreground truncate">{citation.source}</span>
        </div>
        {expanded ? <ChevronUp size={14} className="text-muted-foreground" /> : <ChevronDown size={14} className="text-muted-foreground" />}
      </button>

      {expanded && (
        <div className="px-2 pb-2 border-t border-border">
          <p className="mt-2 text-muted-foreground leading-relaxed">{citation.chunkContent}</p>
          {citation.sourceUrl && (
            <Link
              to={citation.sourceUrl}
              className="flex items-center gap-1 mt-2 text-primary hover:underline"
            >
              <ExternalLink size={12} />
              查看来源
            </Link>
          )}
        </div>
      )}
    </div>
  )
}
