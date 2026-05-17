import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useChatStore } from '@/stores/chatStore'
import { useSessionStore } from '@/stores/sessionStore'
import {
  MessageSquare,
  Database,
  History,
  Activity,
  Settings,
  Plus,
  PanelLeftClose,
  PanelLeftOpen,
  Trash2,
  Menu,
} from 'lucide-react'
import { useConfirm } from '@/components/ui/confirm-dialog'
import { useSettingsStore } from '@/stores/settingsStore'

export default function Sidebar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { currentSessionId, clearMessages } = useChatStore()
  const { sessions, removeSession } = useSessionStore()
  const { confirm } = useConfirm()
  const { settings } = useSettingsStore()
  const [collapsed, setCollapsed] = useState(false)
  const [isMobile, setIsMobile] = useState(false)

  useEffect(() => {
    const check = () => {
      const mobile = window.innerWidth < 768
      setIsMobile(mobile)
      if (mobile) setCollapsed(true)
    }
    check()
    window.addEventListener('resize', check)
    return () => window.removeEventListener('resize', check)
  }, [])

  const navItems = [
    { path: '/knowledge', label: '知识库', icon: Database },
    { path: '/history', label: '会话历史', icon: History },
    { path: '/status', label: '系统状态', icon: Activity },
    { path: '/settings', label: '设置', icon: Settings },
  ]

  function newChat() {
    clearMessages()
    navigate('/chat')
  }

  function isActive(path: string): boolean {
    return location.pathname === path || location.pathname.startsWith(path + '/')
  }

  async function handleDeleteSession(sessionId: string) {
    const confirmed = await confirm({
      title: '删除会话',
      description: '确定删除此会话？此操作不可撤销。',
      variant: 'destructive',
      confirmText: '删除',
    })
    if (confirmed) {
      await removeSession(sessionId)
    }
  }

  return (
    <>
      {/* Mobile overlay */}
      {isMobile && !collapsed && (
        <div
          className="fixed inset-0 bg-black/30 z-40 md:hidden"
          onClick={() => setCollapsed(true)}
        />
      )}

      {/* Mobile hamburger */}
      {isMobile && collapsed && (
        <button
          onClick={() => setCollapsed(false)}
          className="fixed top-3 left-3 z-30 p-2 rounded-lg bg-background border border-border text-muted-foreground hover:bg-accent"
        >
          <Menu size={18} />
        </button>
      )}

      <aside
        className={`flex flex-col h-screen border-r border-border bg-muted/50 transition-all duration-300 z-50 ${
          collapsed ? 'w-16' : 'w-64'
        } ${isMobile && collapsed ? '-translate-x-full' : ''}`}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-3 border-b border-border">
          {!collapsed && (
            <span className="text-sm font-semibold text-foreground truncate">{settings.sidebarTitle || 'Wok RAG Agent'}</span>
          )}
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="p-1.5 rounded-lg hover:bg-accent text-muted-foreground"
          >
            {collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
          </button>
        </div>

        {/* New Chat Button */}
        <div className="p-2">
          <button
            onClick={newChat}
            className={`flex items-center gap-2 w-full p-2.5 rounded-lg border border-border hover:bg-accent text-foreground text-sm transition-colors ${
              collapsed ? 'justify-center' : ''
            }`}
          >
            <Plus size={18} />
            {!collapsed && <span>新建对话</span>}
          </button>
        </div>

        {/* Session List */}
        {!collapsed && (
          <div className="flex-1 overflow-y-auto px-2 space-y-0.5">
            {sessions.length === 0 ? (
              <div className="p-3 text-muted-foreground text-xs text-center">暂无会话</div>
            ) : (
              sessions.map((session) => (
                <button
                  key={session.sessionId}
                  onClick={() => navigate(`/chat/${session.sessionId}`)}
                  className={`flex items-center justify-between w-full p-2 rounded-lg text-sm text-left hover:bg-accent transition-colors group ${
                    currentSessionId === session.sessionId ? 'bg-accent' : ''
                  }`}
                >
                  <div className="flex items-center gap-2 min-w-0">
                    <MessageSquare size={16} className="shrink-0 text-muted-foreground" />
                    <span className="truncate text-foreground">{session.title}</span>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      void handleDeleteSession(session.sessionId)
                    }}
                    className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-destructive/10 text-muted-foreground hover:text-destructive"
                  >
                    <Trash2 size={14} />
                  </button>
                </button>
              ))
            )}
          </div>
        )}

        {/* Navigation */}
        <nav className="border-t border-border p-2 space-y-0.5">
          {navItems.map((item) => (
            <button
              key={item.path}
              onClick={() => {
                navigate(item.path)
                if (isMobile) setCollapsed(true)
              }}
              className={`flex items-center gap-2 w-full p-2 rounded-lg text-sm transition-colors ${
                collapsed ? 'justify-center' : ''
              } ${
                isActive(item.path)
                  ? 'bg-accent text-foreground'
                  : 'text-muted-foreground hover:bg-accent/50'
              }`}
            >
              <item.icon size={18} />
              {!collapsed && <span>{item.label}</span>}
            </button>
          ))}
        </nav>
      </aside>
    </>
  )
}
