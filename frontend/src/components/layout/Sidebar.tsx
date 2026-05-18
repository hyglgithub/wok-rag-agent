import { useState, useEffect, useRef } from 'react'
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
  Search,
  X,
} from 'lucide-react'
import { useConfirm } from '@/components/ui/confirm-dialog'
import { useSettingsStore } from '@/stores/settingsStore'

export default function Sidebar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { currentSessionId, clearMessages } = useChatStore()
  const { sessions, removeSession, searchResults, isSearching, searchSessions, clearSearch } = useSessionStore()
  const { confirm } = useConfirm()
  const { settings } = useSettingsStore()
  const [collapsed, setCollapsed] = useState(false)
  const [isMobile, setIsMobile] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)

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

  useEffect(() => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current)
    if (!searchQuery.trim()) {
      clearSearch()
      return
    }
    searchTimerRef.current = setTimeout(() => {
      void searchSessions(searchQuery)
    }, 300)
    return () => {
      if (searchTimerRef.current) clearTimeout(searchTimerRef.current)
    }
  }, [searchQuery, searchSessions, clearSearch])

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
            {/* Search input */}
            <div className="relative mb-2">
              <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="搜索聊天记录..."
                className="w-full pl-8 pr-8 py-1.5 text-xs rounded-lg border border-border bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  <X size={12} />
                </button>
              )}
            </div>

            {/* Search results or normal list */}
            {searchQuery.trim() ? (
              isSearching ? (
                <div className="p-3 text-muted-foreground text-xs text-center">搜索中...</div>
              ) : searchResults.length === 0 ? (
                <div className="p-3 text-muted-foreground text-xs text-center">无匹配结果</div>
              ) : (
                searchResults.map((result) => (
                  <button
                    key={result.sessionId}
                    onClick={() => {
                      navigate(`/chat/${result.sessionId}`)
                      setSearchQuery('')
                    }}
                    className="flex flex-col w-full p-2 rounded-lg text-sm text-left hover:bg-accent transition-colors"
                  >
                    <span className="truncate text-foreground text-xs font-medium">{result.title}</span>
                    <span className="truncate text-muted-foreground text-xs mt-0.5">{result.matchedPreview}</span>
                  </button>
                ))
              )
            ) : (
              sessions.length === 0 ? (
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
              )
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
