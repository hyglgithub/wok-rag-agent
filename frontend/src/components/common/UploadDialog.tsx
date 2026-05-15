import { useState, useRef } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Upload, FileText } from 'lucide-react'

interface Props {
  open: boolean
  onClose: () => void
  onUpload: (file: File, source: string) => void
}

export default function UploadDialog({ open, onClose, onUpload }: Props) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [source, setSource] = useState('')
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files[0]
    if (file) setSelectedFile(file)
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) setSelectedFile(file)
  }

  function handleUpload() {
    if (selectedFile) {
      onUpload(selectedFile, source)
      setSelectedFile(null)
      setSource('')
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>上传文档</DialogTitle>
        </DialogHeader>

        {/* Drop zone */}
        <div
          onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
            dragOver ? 'border-primary bg-primary/5' : 'border-border'
          }`}
        >
          {selectedFile ? (
            <FileText size={32} className="mx-auto mb-2 text-primary" />
          ) : (
            <Upload size={32} className="mx-auto mb-2 text-muted-foreground" />
          )}
          <p className="text-sm">
            {selectedFile ? selectedFile.name : '拖拽文件到此处或点击选择'}
          </p>
          <p className="text-xs text-muted-foreground mt-1">支持 PDF、TXT、DOCX 等格式</p>
          <input
            ref={fileInputRef}
            type="file"
            className="hidden"
            onChange={handleFileSelect}
            accept=".pdf,.txt,.docx,.doc,.md"
          />
        </div>

        {/* Source input */}
        <div className="mt-2">
          <label className="block text-sm text-muted-foreground mb-1">来源描述（可选）</label>
          <Input
            value={source}
            onChange={(e) => setSource(e.target.value)}
            placeholder="例如：官网、客服中心"
          />
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>取消</Button>
          <Button onClick={handleUpload} disabled={!selectedFile}>上传</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
