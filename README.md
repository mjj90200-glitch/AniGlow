# AniGlow

一个温柔的动漫社区前端项目，采用 Nuxt 3 + Tailwind CSS 构建。

## 视觉风格

- **核心调性**：小清新、温柔、动漫风
- **色彩规范**：
  - 背景：柔和奶油白 (#FDFBF7)
  - 主色：半透明天蓝 (#A0D8EF)
  - 点缀：樱花粉 (#FFC0CB)、暖阳橙 (#FFB37E)
- **UI 设计**：全局使用 `rounded-3xl` 大圆角，柔和的弥散阴影

## 技术栈

- **框架**: Nuxt 3 (SSR 开启)
- **样式**: Tailwind CSS
- **图标**: Lucide Vue Next
- **状态管理**: Pinia
- **工具库**: @vueuse/core

## 启动项目

```bash
# 安装依赖
npm install

# 开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 项目结构

```
├── assets/css/       # 全局样式
├── components/       # Vue 组件
├── layouts/          # 布局文件
├── pages/            # 页面路由
├── stores/           # Pinia 状态管理
├── nuxt.config.ts    # Nuxt 配置
└── tailwind.config.js # Tailwind 配置
```
