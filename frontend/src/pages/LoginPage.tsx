import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { login } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

export default function LoginPage() {
  const [token, setToken] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const setAuthToken = useAuthStore((s) => s.setToken)
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      await login(token)
      setAuthToken(token)
      navigate('/', { replace: true })
    } catch {
      setError('Token 无效，请重新输入')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <form onSubmit={handleSubmit} className="w-full max-w-sm p-6 space-y-4">
        <div className="flex flex-col items-center gap-2">
          <img src="/favicon.png" alt="Logo" className="w-12 h-12" />
          <h1 className="text-xl font-semibold text-center text-foreground">
            Wok RAG Agent
          </h1>
        </div>
        <p className="text-sm text-muted-foreground text-center">
          请输入 API Token 登录
        </p>
        <Input
          type="text"
          placeholder="API Token"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          autoFocus
        />
        {error && (
          <p className="text-sm text-destructive text-center">{error}</p>
        )}
        <Button type="submit" className="w-full" disabled={loading || !token.trim()}>
          {loading ? '登录中...' : '登录'}
        </Button>
      </form>
    </div>
  )
}
