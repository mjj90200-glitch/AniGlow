#!/bin/bash

set -euo pipefail

RUNTIME_DIR="/Users/mac/Library/Application Support/AniGlow"
JAVA="/Users/mac/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin/java"
ENV_FILE="${RUNTIME_DIR}/config/runtime.env"

set -a
source "$ENV_FILE"
set +a

cd "${RUNTIME_DIR}/backend"
exec "$JAVA" -jar aniglow-backend.jar --spring.profiles.active=prod
