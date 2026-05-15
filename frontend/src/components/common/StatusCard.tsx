import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'

interface Props {
  title: string
  status: 'ok' | 'error' | 'loading'
  detail?: string
}

export default function StatusCard({ title, status, detail }: Props) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium">{title}</CardTitle>
          {status === 'ok' && <CheckCircle size={18} className="text-green-500" />}
          {status === 'error' && <XCircle size={18} className="text-destructive" />}
          {status === 'loading' && <Loader2 size={18} className="animate-spin text-muted-foreground" />}
        </div>
      </CardHeader>
      {detail && (
        <CardContent>
          <p className="text-xs text-muted-foreground">{detail}</p>
        </CardContent>
      )}
    </Card>
  )
}
