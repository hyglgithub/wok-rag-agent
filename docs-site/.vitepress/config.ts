import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'Wok RAG Agent',
  description: '基于 Java 的 Agentic RAG 智能问答平台',
  base: '/wok-rag-agent/',
  ignoreDeadLinks: [
    /localhost/,
  ],
  head: [
    ['link', { rel: 'icon', href: '/wok-rag-agent/logo.png' }]
  ],
  themeConfig: {
    logo: '/logo.png',
    nav: [
      { text: '指南', link: '/guide/quick-start' },
      { text: 'API', link: '/api/endpoints' },
      { text: 'GitHub', link: 'https://github.com/hyglgithub/wok-rag-agent' }
    ],
    sidebar: {
      '/guide/': [
        {
          text: '指南',
          items: [
            { text: '快速开始', link: '/guide/quick-start' },
            { text: '部署指南', link: '/guide/deployment' },
            { text: '配置说明', link: '/guide/configuration' },
            { text: '架构说明', link: '/guide/architecture' }
          ]
        }
      ],
      '/api/': [
        {
          text: 'API 参考',
          items: [
            { text: '接口文档', link: '/api/endpoints' }
          ]
        }
      ]
    },
    outline: {
      level: [2, 3],
      label: '页面导航'
    },
    search: {
      provider: 'local',
      options: {
        translations: {
          button: { buttonText: '搜索文档' },
          modal: {
            noResultsText: '没有找到相关结果',
            footer: { selectText: '选择', navigateText: '切换' }
          }
        }
      }
    },
    editLink: {
      pattern: 'https://github.com/hyglgithub/wok-rag-agent/edit/main/docs-site/:path',
      text: '在 GitHub 上编辑此页面'
    },
    lastUpdated: {
      text: '最后更新'
    }
  }
})
