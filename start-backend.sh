#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# 萤火番舍 AniGlow · 后端启动脚本
# 带 JVM 内存控制和 GC 优化参数
# ═══════════════════════════════════════════════════════════════
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
fi

# 本地开发凭据（.env.local，不入库），在 .env 之后加载以优先生效
if [ -f "$SCRIPT_DIR/.env.local" ]; then
    set -a
    source "$SCRIPT_DIR/.env.local"
    set +a
fi

# ─── JDK 选择 ──────────────────────────────────────────────────
# 优先使用 ~/.jdks 下的 Temurin 21（编译版本），回退 JAVA_HOME / PATH
JDK21_CANDIDATES=(~/.jdks/temurin-21*/bin/java)
if [ -e "${JDK21_CANDIDATES[0]}" ]; then
    JAVA_BIN="${JDK21_CANDIDATES[0]}"
elif [ -n "${JAVA_HOME:-}" ] && [ -f "$JAVA_HOME/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
else
    JAVA_BIN="java"
fi

JAR_FILE="$SCRIPT_DIR/aniglow-backend/target/aniglow-backend-1.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "❌ 找不到 JAR 文件: $JAR_FILE"
    echo "   请先运行: cd aniglow-backend && mvn package -DskipTests"
    exit 1
fi

# ─── JVM 参数（8GB 机器优化） ──────────────────────────────────
JVM_OPTS=(
    # 堆内存：初始512MB，最大1.5GB（留够给 MySQL/Redis/Node/OS）
    -Xms512m
    -Xmx1536m

    # G1 垃圾回收器（适合服务端应用）
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:G1HeapRegionSize=4m

    # GC 日志（出现问题时排查用）
    -Xlog:gc*:file=/tmp/aniglow-gc.log:time,level:filecount=5,filesize=10m

    # OOM 时自动 dump 堆快照
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath=/tmp/aniglow-heapdump.hprof

    # 优化
    -XX:+UseStringDeduplication
    -XX:+OptimizeStringConcat
)

echo "☕ JDK: $JAVA_BIN"
echo "📦 JAR: $JAR_FILE"
echo "🔧 JVM: ${JVM_OPTS[*]}"
echo "🚀 启动 AniGlow 后端..."

exec "$JAVA_BIN" "${JVM_OPTS[@]}" -jar "$JAR_FILE"
