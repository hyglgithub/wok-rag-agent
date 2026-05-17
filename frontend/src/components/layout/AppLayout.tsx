import { useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { useSessionStore } from '@/stores/sessionStore'

export default function AppLayout() {
  const { fetchSessions } = useSessionStore()

  useEffect(() => {
    void fetchSessions()
  }, [fetchSessions])

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar />
      <main className="flex-1 overflow-hidden">
        <Outlet />
      </main>
    </div>
  )
}
