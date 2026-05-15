import { useState, useEffect, useCallback } from 'react'
import { getHealth, getActuatorHealth } from '@/api/rag'
import StatusCard from '@/components/common/StatusCard'
import { Button } from '@/components/ui/button'
import { Activity, RefreshCw } from 'lucide-react'

interface HealthStatus {
  title: string
  status: 'ok' | 'error' | 'loading'
  detail: string
}

export default function StatusPage() {
  const [statuses, setStatuses] = useState<HealthStatus[]>([
    { title: '应用状态', status: 'loading', detail: '' },
    { title: 'Milvus 连接', status: 'loading', detail: '' },
    { title: 'SiliconFlow API', status: 'loading', detail: '' },
  ])
  const [lastCheck, setLastCheck] = useState('')

  const checkHealth = useCallback(async () => {
    setStatuses((prev) => prev.map((s) => ({ ...s, status: 'loading' as const })))

    // Basic health check
    try {
      const health = await getHealth()
      setStatuses((prev) => {
        const next = [...prev]
        next[0] = {
          title: '应用状态',
          status: health.status === 'OK' ? 'ok' : 'error',
          detail: `服务: ${health.service}`,
        }
        return next
      })
    } catch {
      setStatuses((prev) => {
        const next = [...prev]
        next[0] = { title: '应用状态', status: 'error', detail: '无法连接到后端服务' }
        return next
      })
    }

    // Actuator health
    try {
      const actuator = await getActuatorHealth()
      const components = (actuator.components || {}) as Record<string, Record<string, string>>

      const milvus = components.milvus
      setStatuses((prev) => {
        const next = [...prev]
        next[1] = {
          title: 'Milvus 连接',
          status: milvus?.status === 'UP' ? 'ok' : 'error',
          detail: milvus?.status === 'UP' ? '向量数据库连接正常' : '连接异常',
        }
        return next
      })

      const siliconflow = components.siliconFlow
      setStatuses((prev) => {
        const next = [...prev]
        next[2] = {
          title: 'SiliconFlow API',
          status: siliconflow?.status === 'UP' ? 'ok' : 'error',
          detail: siliconflow?.status === 'UP' ? 'API 服务可用' : 'API 不可用',
        }
        return next
      })
    } catch {
      setStatuses((prev) => {
        const next = [...prev]
        next[1] = { title: 'Milvus 连接', status: 'error', detail: '无法获取状态' }
        next[2] = { title: 'SiliconFlow API', status: 'error', detail: '无法获取状态' }
        return next
      })
    }

    setLastCheck(new Date().toLocaleTimeString())
  }, [])

  useEffect(() => {
    void checkHealth()
  }, [checkHealth])

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-2xl mx-auto p-6">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-foreground flex items-center gap-2">
            <Activity size={22} />
            系统状态
          </h1>
          <Button variant="outline" size="sm" onClick={() => void checkHealth()}>
            <RefreshCw size={14} className="mr-2" />
            刷新
          </Button>
        </div>

        <div className="space-y-3">
          {statuses.map((s, i) => (
            <StatusCard key={i} title={s.title} status={s.status} detail={s.detail} />
          ))}
        </div>

        {lastCheck && (
          <p className="text-xs text-muted-foreground mt-4 text-right">最后检查: {lastCheck}</p>
        )}
      </div>
    </div>
  )
}
