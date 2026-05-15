import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSessionStore } from '@/stores/sessionStore'
import { useChatStore } from '@/stores/chatStore'
import { Button } from '@/components/ui/button'
import { History, MessageSquare, Trash2, Clock } from 'lucide-react'

export default function HistoryPage() {
  const navigate = useNavigate()
  const { sessions, loading, fetchSessions, removeSession } = useSessionStore()
  const { currentSessionId, clearMessages } = useChatStore()

  useEffect(() => {
    void fetchSessions()
  }, [fetchSessions])

  async function handleDelete(sessionId: string) {
    if (!confirm('确定删除此会话？')) return
    await removeSession(sessionId)
    if (currentSessionId === sessionId) {
      clearMessages()
    }
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-4xl mx-auto p-6">
        <h1 className="text-xl font-semibold text-foreground flex items-center gap-2 mb-6">
          <History size={22} />
          会话历史
        </h1>

        {loading ? (
          <div className="text-center py-12 text-muted-foreground">加载中...</div>
        ) : sessions.length === 0 ? (
          <div className="text-center py-12">
            <MessageSquare size={48} className="mx-auto mb-3 text-muted-foreground opacity-30" />
            <p className="text-muted-foreground">暂无历史会话</p>
          </div>
        ) : (
          <div className="space-y-2">
            {sessions.map((session) => (
              <div
                key={session.sessionId}
                onClick={() => navigate(`/chat/${session.sessionId}`)}
                className="flex items-center justify-between p-4 rounded-lg border border-border hover:bg-accent/50 cursor-pointer transition-colors group"
              >
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm font-medium text-foreground truncate">{session.title}</h3>
                  <p className="text-xs text-muted-foreground truncate mt-1">{session.lastMessage}</p>
                </div>
                <div className="flex items-center gap-3 ml-4">
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Clock size={12} />
                    {new Date(session.lastTime).toLocaleDateString()}
                  </div>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={(e) => {
                      e.stopPropagation()
                      void handleDelete(session.sessionId)
                    }}
                    className="opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-destructive"
                  >
                    <Trash2 size={16} />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
