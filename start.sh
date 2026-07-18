#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# 萤火番舍 AniGlow · 生产环境启动脚本
# 从 .env 加载配置后启动 Nuxt/Nitro 服务
# ═══════════════════════════════════════════════════════════════
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE="${SCRIPT_DIR}/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ 找不到 .env 文件: $ENV_FILE"
  echo "   请从 .env.docker 复制并填入实际值: cp .env.docker .env"
  exit 1
fi

echo "📦 从 .env 加载环境变量..."
set -a
source "$ENV_FILE"
set +a

echo "✅ Authing App ID: ${NUXT_PUBLIC_AUTHING_APP_ID:0:8}***"
export HOST="${HOST:-0.0.0.0}"
export PORT="${PORT:-3002}"

if [ -z "${NUXT_PUBLIC_SITE_URL:-}" ] || [ "${NUXT_PUBLIC_SITE_URL}" = "http://localhost:3000" ]; then
  export NUXT_PUBLIC_SITE_URL="http://www.mjj520.top"
fi

if [ -z "${NUXT_BACKEND_URL:-}" ] || [ "${NUXT_BACKEND_URL}" = "http://localhost:8081" ]; then
  export NUXT_BACKEND_URL="http://127.0.0.1:8081"
fi

if [ -z "${NUXT_PUBLIC_BACKEND_URL:-}" ]; then
  export NUXT_PUBLIC_BACKEND_URL="${NUXT_BACKEND_URL}"
fi

echo "🌐 监听地址: ${HOST}:${PORT}"
echo "🔗 站点地址: ${NUXT_PUBLIC_SITE_URL}"
echo "🚀 启动 Nitro 服务..."

exec node .output/server/index.mjs
