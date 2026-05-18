import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import type { ChunkInfo } from '@/types'
import { getDocumentChunks, updateChunk, addChunk, deleteChunk, getDocuments } from '@/api/document'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { toast } from 'sonner'
import { useConfirm } from '@/components/ui/confirm-dialog'
import { ArrowLeft, Pencil, Plus, Loader2, Trash2, Layers } from 'lucide-react'

export default function ChunkPage() {
  const { docId } = useParams<{ docId: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const { confirm } = useConfirm()

  const [docName, setDocName] = useState((location.state as { docName?: string })?.docName || '')
  const [chunks, setChunks] = useState<ChunkInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  // Dialog state: null = closed, { mode: 'add' } or { mode: 'edit', chunk: ChunkInfo }
  const [dialog, setDialog] = useState<
    | null
    | { mode: 'add' }
    | { mode: 'edit'; chunk: ChunkInfo }
  >(null)
  const [dialogText, setDialogText] = useState('')

  // Floating buttons: visible when header is out of view
  const headerRef = useRef<HTMLDivElement>(null)
  const [headerVisible, setHeaderVisible] = useState(true)

  useEffect(() => {
    if (!headerRef.current) return
    const observer = new IntersectionObserver(
      ([entry]) => setHeaderVisible(entry.isIntersecting),
      { threshold: 0 }
    )
    observer.observe(headerRef.current)
    return () => observer.disconnect()
  }, [loading])

  useEffect(() => {
    if (!docId) return
    setLoading(true)

    const stateName = (location.state as { docName?: string })?.docName
    if (stateName) {
      setDocName(stateName)
      getDocumentChunks(docId)
        .then(setChunks)
        .catch(() => {
          toast.error('加载切片失败')
          navigate('/knowledge', { replace: true })
        })
        .finally(() => setLoading(false))
    } else {
      Promise.all([getDocuments(), getDocumentChunks(docId)])
        .then(([docs, chunkData]) => {
          const doc = docs.find((d) => d.id === docId)
          setDocName(doc?.name || docId)
          setChunks(chunkData)
        })
        .catch(() => {
          toast.error('文档不存在')
          navigate('/knowledge', { replace: true })
        })
        .finally(() => setLoading(false))
    }
  }, [docId])

  function openEditDialog(chunk: ChunkInfo) {
    setDialog({ mode: 'edit', chunk })
    setDialogText(chunk.chunkText)
  }

  function openAddDialog() {
    setDialog({ mode: 'add' })
    setDialogText('')
  }

  function closeDialog() {
    setDialog(null)
    setDialogText('')
  }

  async function handleDialogSave() {
    if (!dialogText.trim()) return
    setSaving(true)
    try {
      if (dialog?.mode === 'edit') {
        await updateChunk(dialog.chunk.milvusId, dialogText)
        setChunks((prev) =>
          prev.map((c) => (c.milvusId === dialog.chunk.milvusId ? { ...c, chunkText: dialogText } : c))
        )
        toast.success('切片已更新，向量已重新生成')
      } else {
        await addChunk(docId!, dialogText)
        const updated = await getDocumentChunks(docId!)
        setChunks(updated)
        toast.success('切片已添加，向量已生成')
      }
      closeDialog()
    } catch {
      toast.error(dialog?.mode === 'edit' ? '更新失败' : '添加失败')
    } finally {
      setSaving(false)
    }
  }

  async function handleDeleteChunk(milvusId: string) {
    const confirmed = await confirm({
      title: '删除切片',
      description: '确定删除此切片？删除后向量数据也将被清除。',
      variant: 'destructive',
      confirmText: '删除',
    })
    if (!confirmed) return
    try {
      await deleteChunk(milvusId, docId!)
      setChunks((prev) => prev.filter((c) => c.milvusId !== milvusId))
      toast.success('切片已删除')
    } catch {
      toast.error('删除失败')
    }
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-4xl mx-auto p-6">
        {/* Floating sticky bar - always in DOM, visible only when header is scrolled away */}
        <div className={`sticky top-0 z-40 -mx-6 -mt-6 px-6 py-3 flex items-center justify-between ${headerVisible ? 'pointer-events-none' : ''}`}>
          <Button
            variant="outline"
            size="icon"
            className={`rounded-full w-10 h-10 shadow-lg bg-background transition-opacity ${headerVisible ? 'opacity-0 pointer-events-none' : 'opacity-100'}`}
            onClick={() => navigate('/knowledge')}
          >
            <ArrowLeft size={18} />
          </Button>
          <Button
            variant="outline"
            size="icon"
            className={`rounded-full w-10 h-10 shadow-lg bg-background transition-opacity ${headerVisible ? 'opacity-0 pointer-events-none' : 'opacity-100'}`}
            onClick={openAddDialog}
          >
            <Plus size={18} />
          </Button>
        </div>

        {/* Header */}
        <div ref={headerRef} className="flex items-center gap-3 mb-6">
          <Button variant="ghost" size="icon" onClick={() => navigate('/knowledge')}>
            <ArrowLeft size={18} />
          </Button>
          <div className="flex items-center gap-2">
            <Layers size={22} className="text-muted-foreground" />
            <h1 className="text-xl font-semibold text-foreground">
              {docName || '切片管理'}
            </h1>
          </div>
          {!loading && (
            <Badge variant="secondary" className="ml-2">
              {chunks.length} 个切片
            </Badge>
          )}
          <div className="ml-auto">
            <Button variant="outline" size="sm" onClick={openAddDialog}>
              <Plus size={16} className="mr-1" />
              添加切片
            </Button>
          </div>
        </div>

        {/* Chunk list */}
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <Loader2 className="animate-spin mr-2" size={20} />
            <span className="text-muted-foreground">加载中...</span>
          </div>
        ) : chunks.length === 0 ? (
          <div className="text-center py-16">
            <Layers size={48} className="mx-auto mb-3 text-muted-foreground opacity-30" />
            <p className="text-muted-foreground">暂无切片</p>
          </div>
        ) : (
          <div className="space-y-3">
            {chunks.map((chunk) => (
              <div key={chunk.milvusId} className="border rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <Badge variant="outline">#{chunk.chunkIndex + 1}</Badge>
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => openEditDialog(chunk)}
                    >
                      <Pencil size={14} className="mr-1" />
                      编辑
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => void handleDeleteChunk(chunk.milvusId)}
                      className="text-muted-foreground hover:text-destructive"
                    >
                      <Trash2 size={14} />
                    </Button>
                  </div>
                </div>
                <p className="text-sm text-muted-foreground whitespace-pre-wrap">{chunk.chunkText}</p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Edit / Add Dialog */}
      <Dialog open={dialog !== null} onOpenChange={(o) => !o && closeDialog()}>
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>{dialog?.mode === 'edit' ? '编辑切片' : '添加切片'}</DialogTitle>
          </DialogHeader>
          <Textarea
            value={dialogText}
            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setDialogText(e.target.value)}
            placeholder="输入切片内容..."
            rows={12}
            autoFocus
          />
          <DialogFooter>
            <Button variant="outline" size="sm" onClick={closeDialog}>
              取消
            </Button>
            <Button size="sm" onClick={() => void handleDialogSave()} disabled={saving}>
              {saving ? <Loader2 className="animate-spin mr-1" size={14} /> : null}
              {dialog?.mode === 'edit' ? '保存' : '添加'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
