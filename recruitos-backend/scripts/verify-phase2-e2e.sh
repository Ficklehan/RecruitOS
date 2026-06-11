#!/usr/bin/env bash
# Phase 2 端到端 API 验收（对齐 DEMO-VERIFICATION.md）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AGENT_DIR="$(cd "$ROOT/../TestAgent" 2>/dev/null || cd "$ROOT/../../TestAgent" && pwd)"
PASS=0

echo "=== RecruitOS Phase 2 E2E Verification ==="

# 可选：重载演示数据
if [[ "${RESEED:-0}" == "1" ]]; then
  echo ">> Reseeding demo data..."
  "$ROOT/scripts/reseed-demo.sh"
fi

# 检查关键端口
for port in 8081 8084 8086 8087 8088 8089 8090 8091 8095; do
  if ! lsof -ti "tcp:$port" >/dev/null 2>&1; then
    echo "WARN: port $port not listening — run scripts/start-all-services.sh"
    PASS=1
  fi
done

if [[ "$PASS" -ne 0 ]]; then
  echo ""
  echo "请先启动服务:"
  echo "  cd $ROOT && ./scripts/start-all-services.sh"
  exit 1
fi

if ! python3 -c "import httpx" 2>/dev/null; then
  pip install -q -r "$AGENT_DIR/requirements.txt"
fi

export PYTHONPATH="${AGENT_DIR}/src:${PYTHONPATH:-}"
cd "$AGENT_DIR"
python3 -m test_agent.cli phase2 --config config/agent.yaml
