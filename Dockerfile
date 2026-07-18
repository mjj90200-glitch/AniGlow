# ═══════════════════════════════════════════════════════════════
# 萤火番舍 AniGlow · 前端 Dockerfile（Nuxt 3 + pnpm）
# 使用多阶段构建，最终镜像仅包含运行时所需文件
# ═══════════════════════════════════════════════════════════════

# ─── Stage 1: 构建阶段 ───────────────────────────────────────
FROM node:22-alpine AS builder

RUN corepack enable && corepack prepare pnpm@10.33.3 --activate

WORKDIR /app

# pnpm 依赖缓存层
COPY package.json pnpm-lock.yaml .npmrc* ./

RUN pnpm install --frozen-lockfile

# 复制所有源码和配置文件
COPY . .

# 构建时注入的环境变量（Authing 公开配置）
ARG NUXT_PUBLIC_AUTHING_APP_ID
ARG NUXT_PUBLIC_AUTHING_HOST
ARG NUXT_PUBLIC_SITE_URL

ENV NUXT_PUBLIC_AUTHING_APP_ID=${NUXT_PUBLIC_AUTHING_APP_ID}
ENV NUXT_PUBLIC_AUTHING_HOST=${NUXT_PUBLIC_AUTHING_HOST:-https://core.authing.cn}
ENV NUXT_PUBLIC_SITE_URL=${NUXT_PUBLIC_SITE_URL:-http://localhost:3000}

# 构建 Nuxt 生产版本（包含 Nitro 服务端路由）
RUN pnpm build

# ─── Stage 2: 运行阶段 ───────────────────────────────────────
FROM node:22-alpine AS runner

WORKDIR /app

# 仅复制构建产物
COPY --from=builder /app/.output /app/.output

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3000

# Authing 运行时配置（docker-compose / docker run 时通过 -e 传入覆盖）
ENV NUXT_PUBLIC_AUTHING_APP_ID=
ENV NUXT_PUBLIC_AUTHING_HOST=https://core.authing.cn
ENV NUXT_PUBLIC_SITE_URL=http://localhost:3000

# 后端 URL（容器内通过服务名访问）
ENV NUXT_BACKEND_URL=http://backend:8081

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:3000/ || exit 1

CMD ["node", ".output/server/index.mjs"]
