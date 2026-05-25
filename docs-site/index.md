---
layout: home
hero:
  name: Wok RAG Agent
  text: Agentic RAG 智能问答平台
  tagline: 基于 Java 的单体 RAG 解决方案，支持混合检索、多轮对话、工具调用
  actions:
    - theme: brand
      text: 快速开始
      link: /guide/quick-start
    - theme: alt
      text: GitHub
      link: https://github.com/hyglgithub/wok-rag-agent

features:
  - icon: 🔍
    title: 混合检索
    details: BM25 关键词检索 + 向量语义检索，RRF 融合 + Reranker 精排，精准召回
  - icon: 💬
    title: 多轮对话记忆
    details: 基于 Token 阈值的历史摘要 + 最近 N 轮保留策略，长对话不丢上下文
  - icon: 🎯
    title: 意图识别路由
    details: 规则 + LLM 混合分类，知识检索、工具调用、闲聊、澄清四路智能路由