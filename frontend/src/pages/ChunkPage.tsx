import { useState, useEffect, useRef, useCallback } from 'react'
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
import { ArrowLeft, ArrowUp, Pencil, Plus, Loader2, Trash2, Layers } from 'lucide-react'

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

  // Scroll to top
  const scrollContainerRef = useRef<HTMLDivElement>(null)
  const [showScrollTop, setShowScrollTop] = useState(false)

  useEffect(() => {
    const container = scrollContainerRef.current
    if (!container) return
    function handleScroll() {
      setShowScrollTop(container!.scrollTop > 200)
    }
    container.addEventListener('scroll', handleScroll)
    return () => container.removeEventListener('scroll', handleScroll)
  }, [])

  // Chunk nav state
  const [activeChunkId, setActiveChunkId] = useState<string | null>(null)
  const chunkRefs = useRef<Map<string, HTMLDivElement>>(new Map())
  const navContainerRef = useRef<HTMLDivElement>(null)
  const isClicking = useRef(false)

  const setChunkRef = useCallback((el: HTMLDivElement | null, milvusId: string) => {
    if (el) {
      chunkRefs.current.set(milvusId, el)
    } else {
      chunkRefs.current.delete(milvusId)
    }
  }, [])

  // IntersectionObserver for chunk visibility tracking
  useEffect(() => {
    if (loading || chunks.length === 0) return

    const observer = new IntersectionObserver(
      (entries) => {
        if (isClicking.current) return
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const id = entry.target.getAttribute('data-chunk-id')
            if (id) setActiveChunkId(id)
          }
        }
      },
      { rootMargin: '-20% 0px -70% 0px', threshold: 0 }
    )

    const refs = chunkRefs.current
    refs.forEach((el) => observer.observe(el))
    return () => observer.disconnect()
  }, [loading, chunks])

  function smartScrollNav() {
    const container = navContainerRef.current
    if (!container) return
    const activeIndex = chunks.findIndex(c => c.milvusId === activeChunkId)
    if (activeIndex < 0) return
    const items = container.querySelectorAll('.chunk-nav-item')
    if (activeIndex === 0) {
      container.scrollTo({ top: 0, behavior: 'smooth' })
    } else if (activeIndex === chunks.length - 1) {
      container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' })
    } else {
      items[activeIndex]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }

  useEffect(() => {
    if (activeChunkId) smartScrollNav()
  }, [activeChunkId])

  function scrollToChunk(milvusId: string) {
    isClicking.current = true
    setActiveChunkId(milvusId)
    const el = chunkRefs.current.get(milvusId)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
    setTimeout(() => { isClicking.current = false }, 800)
  }

  useEffect(() => {
    if (!docId) return
    setLoading(true)

    const stateName = (location.state as { docName?: string })?.docName
    if (stateName) {
      setDocName(stateName)
      getDocumentChunks(docId)
        .then((data) => { setChunks(data); setActiveChunkId(data[0]?.milvusId || null) })
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
          setActiveChunkId(chunkData[0]?.milvusId || null)
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
    <div ref={scrollContainerRef} className="h-full overflow-y-auto">
      <div className="max-w-4xl mx-auto p-6">
        {/* Header */}
        <div className="flex items-center gap-3 mb-6">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
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
              <div
                key={chunk.milvusId}
                ref={(el) => setChunkRef(el, chunk.milvusId)}
                data-chunk-id={chunk.milvusId}
                className="border rounded-lg p-4"
              >
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

      {/* Chunk Navigation Sidebar */}
      {!loading && chunks.length > 0 && (
        <>
          <style>{`
            .chunk-nav {
              position: fixed;
              right: 0;
              top: 50%;
              transform: translateY(-50%);
              width: 30px;
              max-height: 70vh;
              background: transparent;
              transition: all 0.3s ease;
              overflow-y: auto;
              overflow-x: hidden;
              z-index: 99;
              scrollbar-width: none;
              padding: 0 2px;
              box-sizing: content-box;
            }
            .chunk-nav::-webkit-scrollbar { display: none; }
            .chunk-nav:hover {
              width: 240px;
              background: #ffffff;
              box-shadow: -2px 0 10px rgba(0,0,0,0.08);
              padding: 0;
              box-sizing: border-box;
            }
            .chunk-nav-item {
              position: relative;
              display: flex;
              align-items: center;
              justify-content: flex-end;
              height: 38px;
              padding: 0 2px;
              cursor: pointer;
              width: 100%;
              overflow: visible;
            }
            .chunk-nav-index {
              width: 26px;
              height: 22px;
              line-height: 22px;
              text-align: center;
              border-radius: 4px;
              background: #e5e7eb;
              color: #666;
              font-size: 12px;
              font-weight: 500;
              flex-shrink: 0;
              margin-left: 0;
              margin-right: 0;
              transition: all 0.2s ease;
              position: relative;
              z-index: 10;
            }
            .chunk-nav-item.active .chunk-nav-index {
              background: #0070E0;
              color: #fff;
              transform: scale(1.08);
            }
            .chunk-nav-text {
              position: absolute;
              right: 42px;
              width: 165px;
              font-size: 13px;
              color: #666;
              opacity: 0;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
              text-align: right;
              transition: opacity 0.3s ease;
              pointer-events: none;
            }
            .chunk-nav-item.active .chunk-nav-text {
              color: #0070E0;
              font-weight: 500;
            }
            .chunk-nav:hover .chunk-nav-text { opacity: 1; }
            .chunk-nav:hover .chunk-nav-item { padding: 0 8px; }
            .chunk-nav:hover .chunk-nav-index { margin-left: 8px; }
          `}</style>
          <div
            ref={navContainerRef}
            className="chunk-nav"
            onWheel={(e) => {
              e.stopPropagation()
              e.currentTarget.scrollTop += e.deltaY
            }}
          >
            {chunks.map((chunk) => (
              <div
                key={chunk.milvusId}
                className={`chunk-nav-item ${activeChunkId === chunk.milvusId ? 'active' : ''}`}
                onClick={() => scrollToChunk(chunk.milvusId)}
              >
                <span className="chunk-nav-text">
                  {chunk.chunkText.slice(0, 20)}
                  {chunk.chunkText.length > 20 ? '...' : ''}
                </span>
                <div className="chunk-nav-index">#{chunk.chunkIndex + 1}</div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Scroll to top button */}
      {showScrollTop && (
        <Button
          variant="outline"
          size="icon"
          className="fixed bottom-6 right-6 rounded-full w-10 h-10 shadow-lg z-50"
          onClick={() => scrollContainerRef.current?.scrollTo({ top: 0, behavior: 'smooth' })}
        >
          <ArrowUp size={18} />
        </Button>
      )}
    </div>
  )
}
