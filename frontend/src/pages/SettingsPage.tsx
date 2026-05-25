import { useNavigate } from 'react-router-dom'
import { useSettingsStore } from '@/stores/settingsStore'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Settings, Sun, Moon, Globe, LogOut } from 'lucide-react'

export default function SettingsPage() {
  const { settings, updateTheme, updateApiUrl, updateLanguage, updateSidebarTitle } = useSettingsStore()
  const clearToken = useAuthStore((s) => s.clearToken)
  const navigate = useNavigate()

  function handleLogout() {
    clearToken()
    navigate('/', { replace: true })
  }

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-2xl mx-auto p-6">
        <h1 className="text-xl font-semibold text-foreground flex items-center gap-2 mb-6">
          <Settings size={22} />
          设置
        </h1>

        <div className="space-y-6">
          {/* API URL */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-2">API 基础地址</label>
            <Input
              value={settings.apiUrl}
              onChange={(e) => updateApiUrl(e.target.value)}
            />
            <p className="text-xs text-muted-foreground mt-1">后端 API 地址，默认 http://localhost:8080</p>
          </div>

          {/* Theme */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-2">主题</label>
            <div className="flex gap-2">
              <Button
                variant={settings.theme === 'light' ? 'default' : 'outline'}
                onClick={() => updateTheme('light')}
              >
                <Sun size={16} className="mr-2" />
                浅色
              </Button>
              <Button
                variant={settings.theme === 'dark' ? 'default' : 'outline'}
                onClick={() => updateTheme('dark')}
              >
                <Moon size={16} className="mr-2" />
                深色
              </Button>
            </div>
          </div>

          {/* Language */}
          {/* <div>
            <label className="block text-sm font-medium text-foreground mb-2">
              <Globe size={14} className="inline mr-1" />
              语言
            </label>
            <div className="flex gap-2">
              <Button
                variant={settings.language === 'zh' ? 'default' : 'outline'}
                onClick={() => updateLanguage('zh')}
              >
                中文
              </Button>
              <Button
                variant={settings.language === 'en' ? 'default' : 'outline'}
                onClick={() => updateLanguage('en')}
              >
                English
              </Button>
            </div>
          </div> */}

          {/* Sidebar Title */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-2">侧边栏标题</label>
            <Input
              value={settings.sidebarTitle}
              onChange={(e) => updateSidebarTitle(e.target.value)}
              placeholder="Wok RAG Agent"
            />
            <p className="text-xs text-muted-foreground mt-1">自定义侧边栏顶部显示的标题</p>
          </div>

          {/* Logout */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-2">账号</label>
            <Button variant="destructive" onClick={handleLogout}>
              <LogOut size={16} className="mr-2" />
              退出登录
            </Button>
            <p className="text-xs text-muted-foreground mt-1">清除本地 Token，返回登录页</p>
          </div>
        </div>
      </div>
    </div>
  )
}
