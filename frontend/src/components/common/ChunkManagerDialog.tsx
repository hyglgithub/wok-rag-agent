import { useState, useEffect } from 'react'
import type { ChunkInfo } from '@/types'
import { getDocumentChunks, updateChunk, addChunk } from '@/api/document'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import { toast } from 'sonner'
import { Pencil, Plus, Loader2 } from 'lucide-react'

interface Props {
  open: boolean
  onClose: () => void
  docId: string
  docName: string
}

export default function ChunkManagerDialog({ open, onClose, docId, docName }: Props) {
  const [chunks, setChunks] = useState<ChunkInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editText, setEditText] = useState('')
  const [saving, setSaving] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [newText, setNewText] = useState('')

  useEffect(() => {
    if (open) {
      setLoading(true)
      getDocumentChunks(docId)
        .then(setChunks)
        .finally(() => setLoading(false))
    }
  }, [open, docId])

  async function handleSaveEdit(milvusId: number) {
    setSaving(true)
    try {
      await updateChunk(milvusId, editText)
      setChunks((prev) =>
        prev.map((c) => (c.milvusId === milvusId ? { ...c, chunkText: editText } : c))
      )
      setEditingId(null)
      toast.success('切片已更新，向量已重新生成')
    } catch {
      toast.error('更新失败')
    } finally {
      setSaving(false)
    }
  }

  async function handleAddChunk() {
    if (!newText.trim()) return
    setSaving(true)
    try {
      await addChunk(docId, newText)
      const updated = await getDocumentChunks(docId)
      setChunks(updated)
      setNewText('')
      setShowAdd(false)
      toast.success('切片已添加，向量已生成')
    } catch {
      toast.error('添加失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>切片管理 — {docName}</DialogTitle>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto space-y-3 pr-1">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="animate-spin mr-2" size={20} />
              <span className="text-muted-foreground">加载中...</span>
            </div>
          ) : chunks.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">暂无切片</p>
          ) : (
            chunks.map((chunk) => (
              <div key={chunk.milvusId} className="border rounded-lg p-3">
                <div className="flex items-center justify-between mb-2">
                  <Badge variant="outline">#{chunk.chunkIndex + 1}</Badge>
                  {editingId !== chunk.milvusId && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setEditingId(chunk.milvusId)
                        setEditText(chunk.chunkText)
                      }}
                    >
                      <Pencil size={14} className="mr-1" />
                      编辑
                    </Button>
                  )}
                </div>

                {editingId === chunk.milvusId ? (
                  <div className="space-y-2">
                    <Textarea
                      value={editText}
                      onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setEditText(e.target.value)}
                      rows={4}
                    />
                    <div className="flex gap-2 justify-end">
                      <Button variant="outline" size="sm" onClick={() => setEditingId(null)}>
                        取消
                      </Button>
                      <Button size="sm" onClick={() => void handleSaveEdit(chunk.milvusId)} disabled={saving}>
                        {saving ? <Loader2 className="animate-spin mr-1" size={14} /> : null}
                        保存
                      </Button>
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground whitespace-pre-wrap">{chunk.chunkText}</p>
                )}
              </div>
            ))
          )}
        </div>

        <div className="border-t pt-3 mt-2">
          {showAdd ? (
            <div className="space-y-2">
              <Textarea
                value={newText}
                onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setNewText(e.target.value)}
                placeholder="输入新的切片内容..."
                rows={3}
              />
              <div className="flex gap-2 justify-end">
                <Button variant="outline" size="sm" onClick={() => setShowAdd(false)}>
                  取消
                </Button>
                <Button size="sm" onClick={() => void handleAddChunk()} disabled={saving}>
                  {saving ? <Loader2 className="animate-spin mr-1" size={14} /> : null}
                  添加
                </Button>
              </div>
            </div>
          ) : (
            <Button variant="outline" onClick={() => setShowAdd(true)} className="w-full">
              <Plus size={16} className="mr-2" />
              添加切片
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
