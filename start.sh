#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# AniGlow · 一键启动（Docker Compose 全栈）
# 用法:  ./start.sh          → 启动并等待就绪（约 30-60 秒）
#        ./start.sh quick    → 只启动不等待，立即返回
# ═══════════════════════════════════════════════════════════════
cd "$(dirname "$0")"

# 加载环境变量（.env.local 为机器特定密钥，.env 为通用配置）
[ -f .env.local ] && { set -a; source .env.local; set +a; }
[ -f .env ] && { set -a; source .env; set +a; }

echo "🚀 启动 AniGlow 全栈..."
docker compose up -d

if [ "$1" = "quick" ]; then
  echo "⚡ 已后台启动: http://localhost:3000 （服务就绪约需 30-60 秒）"
  exit 0
fi

# ── 等待后端就绪（最多 120 秒）──
echo -n "⏳ 等待服务就绪 "
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w "%{http_code}" --noproxy '*' http://localhost:8081/api/actuator/health 2>/dev/null)
  if [ "$code" = "200" ] || [ "$code" = "503" ]; then
    echo "✅"
    echo ""
    echo "🎉 AniGlow 已启动"
    echo "   前端   http://localhost:3000"
    echo "   后端   http://localhost:8081/api"
    echo "   健康   http://localhost:8081/api/actuator/health"
    exit 0
  fi
  echo -n "."
  sleep 2
done

echo ""
echo "⚠️ 120 秒内未就绪，请检查: docker compose logs backend"
exit 1
