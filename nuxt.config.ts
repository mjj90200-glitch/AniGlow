import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  ssr: true,
  devtools: { enabled: process.env.NODE_ENV !== 'production' },

  modules: [
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
    '@vueuse/nuxt',
  ],

  css: ['~/assets/css/main.css', '~/assets/css/pages/agent.css'],

  app: {
    head: {
      title: '萤火番舍 · AniGlow — 发现你的下一部心动动漫',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1, viewport-fit=cover' },
        { name: 'theme-color', content: '#00E676' },
        { name: 'mobile-web-app-capable', content: 'yes' },
        { name: 'apple-mobile-web-app-capable', content: 'yes' },
        { name: 'apple-mobile-web-app-title', content: '萤火番舍' },
        { name: 'apple-mobile-web-app-status-bar-style', content: 'default' },
        { name: 'format-detection', content: 'telephone=no' },
        {
          name: 'description',
          content: '萤火番舍 AniGlow — 琉璃般通透的动漫社区。探索番剧、投票排行、AIGC 驻站，发现触动心灵的精彩故事。'
        },
        { property: 'og:title', content: '萤火番舍 · AniGlow' },
        { property: 'og:description', content: '琉璃般通透的动漫社区。探索番剧、投票排行、AIGC 驻站。' },
        { property: 'og:type', content: 'website' },
      ],
      link: [
        { rel: 'manifest', href: '/manifest.webmanifest' },
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        { rel: 'apple-touch-icon', href: '/pwa/app-icon.svg' },
        { rel: 'mask-icon', href: '/pwa/app-icon.svg', color: '#00E676' },
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
      ],
      htmlAttrs: {
        lang: 'zh-CN',
      },
    },
  },


  devServer: {
    port: 3001,
    host: '0.0.0.0',
  },

  // 使用 Nuxt 标准环境变量映射，私密值不写入构建产物。
  runtimeConfig: {
    backendUrl: 'http://localhost:8081',
    authingAppSecret: '',
    authBridgeSecret: '',
    trustedProxies: '127.0.0.1/32,::1/128',

    public: {
      apiBase: '/api',
      authingAppId: '',
      authingHost: 'https://core.authing.cn',
      authingIssuer: '',
      siteUrl: 'http://localhost:3001',
    },
  },

  compatibilityDate: '2025-04-01',
})
