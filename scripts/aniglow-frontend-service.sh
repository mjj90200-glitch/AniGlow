#!/bin/bash

set -euo pipefail

RUNTIME_DIR="/Users/mac/Library/Application Support/AniGlow"
ENV_FILE="${RUNTIME_DIR}/config/frontend.env"

set -a
source "$ENV_FILE"
set +a

export HOST="0.0.0.0"
export PORT="3002"
export NUXT_PUBLIC_SITE_URL="http://www.mjj520.top"
export NUXT_BACKEND_URL="http://127.0.0.1:8081"
export NUXT_PUBLIC_BACKEND_URL="http://127.0.0.1:8081"

cd "${RUNTIME_DIR}/frontend"
exec /Users/mac/.local/bin/node server/index.mjs
