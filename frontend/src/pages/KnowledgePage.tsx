import { useState, useEffect, useMemo } from 'react'
import type { DocumentInfo } from '@/types'
import { getDocuments, uploadDocument, deleteDocument } from '@/api/document'
import UploadDialog from '@/components/common/UploadDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Database, Trash2, Upload, Search, FileText } from 'lucide-react'

export default function KnowledgePage() {
  const [documents, setDocuments] = useState<DocumentInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [showUpload, setShowUpload] = useState(false)

  const filteredDocs = useMemo(() => {
    if (!searchQuery) return documents
    const q = searchQuery.toLowerCase()
    return documents.filter(
      (d) => d.name.toLowerCase().includes(q) || d.source.toLowerCase().includes(q)
    )
  }, [documents, searchQuery])

  useEffect(() => {
    setLoading(true)
    getDocuments()
      .then(setDocuments)
      .finally(() => setLoading(false))
  }, [])

  async function handleUpload(file: File, source: string) {
    try {
      const result = await uploadDocument(file, source)
      setDocuments((prev) => [result, ...prev])
      setShowUpload(false)
    } catch (err) {
      alert('上传失败: ' + (err instanceof Error ? err.message : '未知错误'))
    }
  }

  async function handleDelete(doc: DocumentInfo) {
    if (!confirm(`确定删除 "${doc.name}"？`)) return
    try {
      await deleteDocument(doc.id)
      setDocuments((prev) => prev.filter((d) => d.id !== doc.id))
    } catch {
      alert('删除失败')
    }
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-4xl mx-auto p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-semibold text-foreground flex items-center gap-2">
              <Database size={22} />
              知识库管理
            </h1>
            <p className="text-sm text-muted-foreground mt-1">管理已导入的文档</p>
          </div>
          <Button onClick={() => setShowUpload(true)}>
            <Upload size={16} className="mr-2" />
            上传文档
          </Button>
        </div>

        {/* Search */}
        <div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索文档..."
            className="pl-9"
          />
        </div>

        {/* Document list */}
        {loading ? (
          <div className="text-center py-12 text-muted-foreground">加载中...</div>
        ) : filteredDocs.length === 0 ? (
          <div className="text-center py-12">
            <FileText size={48} className="mx-auto mb-3 text-muted-foreground opacity-30" />
            <p className="text-muted-foreground">{searchQuery ? '没有匹配的文档' : '暂无文档，点击上方按钮上传'}</p>
          </div>
        ) : (
          <div className="border rounded-lg overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>文档名称</TableHead>
                  <TableHead>来源</TableHead>
                  <TableHead>上传时间</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredDocs.map((doc) => (
                  <TableRow key={doc.id}>
                    <TableCell className="font-medium">{doc.name}</TableCell>
                    <TableCell className="text-muted-foreground">{doc.source || '-'}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {new Date(doc.uploadTime).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => void handleDelete(doc)}
                        className="text-muted-foreground hover:text-destructive"
                      >
                        <Trash2 size={16} />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      <UploadDialog
        open={showUpload}
        onClose={() => setShowUpload(false)}
        onUpload={(file, source) => void handleUpload(file, source)}
      />
    </div>
  )
}
