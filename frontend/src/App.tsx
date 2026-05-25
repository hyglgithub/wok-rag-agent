import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ConfirmProvider } from '@/components/ui/confirm-dialog'
import { useAuthStore } from '@/stores/authStore'
import { checkAuth } from '@/api/auth'
import AppLayout from '@/components/layout/AppLayout'
import ChatPage from '@/pages/ChatPage'
import KnowledgePage from '@/pages/KnowledgePage'
import HistoryPage from '@/pages/HistoryPage'
import StatusPage from '@/pages/StatusPage'
import SettingsPage from '@/pages/SettingsPage'
import ChunkPage from '@/pages/ChunkPage'
import LoginPage from '@/pages/LoginPage'

function AuthGuard({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, authEnabled, clearToken, setToken, loadToken, setAuthEnabled } = useAuthStore()
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    loadToken()
  }, [loadToken])

  useEffect(() => {
    const stored = useAuthStore.getState().token
    checkAuth(stored).then((status) => {
      setAuthEnabled(status.authEnabled)
      if (!status.authEnabled) {
        // Auth disabled on backend — skip login
        setChecking(false)
        return
      }
      if (status.authenticated) {
        setToken(stored!)
      } else {
        clearToken()
      }
      setChecking(false)
    })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  if (checking) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-muted-foreground">加载中...</p>
      </div>
    )
  }

  if (authEnabled && !isAuthenticated) {
    return <LoginPage />
  }

  return <>{children}</>
}

export default function App() {
  return (
    <ConfirmProvider>
    <BrowserRouter>
      <AuthGuard>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<Navigate to="/chat" replace />} />
            <Route path="/chat" element={<ChatPage />} />
            <Route path="/chat/:sessionId" element={<ChatPage />} />
            <Route path="/knowledge" element={<KnowledgePage />} />
            <Route path="/knowledge/:docId/chunks" element={<ChunkPage />} />
            <Route path="/history" element={<HistoryPage />} />
            <Route path="/status" element={<StatusPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
        </Routes>
      </AuthGuard>
    </BrowserRouter>
    </ConfirmProvider>
  )
}
