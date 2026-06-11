#!/usr/bin/env bash
# 快速重建并重启近期改动的微服务（进化/Agent/沟通/入职/Offer）
set -euo pipefail

BASE="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$BASE/.env"
JAVA="${JAVA:-$(command -v java)}"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

export MIMO_API_KEY="${MIMO_API_KEY:-}"
export MIMO_MODEL="${MIMO_MODEL:-mimo-v2.5}"
export MIMO_BASE_URL="${MIMO_BASE_URL:-https://token-plan-cn.xiaomimimo.com/v1}"

echo ">> mvn install (evolution, agent, communication, onboard, offer, common)..."
(cd "$BASE" && mvn install -DskipTests -q -pl \
  recruitos-evolution,recruitos-agent,recruitos-communication,recruitos-onboard,recruitos-offer,recruitos-interview,recruitos-llm,recruitos-job,recruitos-auth,recruitos-common -am)

start_svc() {
  local name=$1 port=$2
  local jar="$BASE/recruitos-$name/target/recruitos-$name-1.0.0-SNAPSHOT.jar"
  local log="/tmp/recruitos-$name.log"
  /usr/bin/screen -S "ros-$name" -X quit >/dev/null 2>&1 || true
  if lsof -ti "tcp:$port" >/dev/null 2>&1; then
    lsof -ti "tcp:$port" | xargs kill -9 2>/dev/null || true
    sleep 0.5
  fi
  sleep 0.3
  : > "$log"
  /usr/bin/screen -dmS "ros-$name" bash -c "cd \"$BASE/recruitos-$name\" && exec env MIMO_API_KEY=\"$MIMO_API_KEY\" MIMO_MODEL=\"$MIMO_MODEL\" MIMO_BASE_URL=\"$MIMO_BASE_URL\" \"$JAVA\" -Xms128m -Xmx512m -jar \"$jar\" >> \"$log\" 2>&1"
  echo -n "  $name :$port "
  for _ in $(seq 1 60); do
    if curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
      echo "✓"
      return 0
    fi
    echo -n "."
    sleep 1
  done
  echo "✗"
  tail -10 "$log" || true
  return 1
}

echo ">> restart hot services..."
FAILED=0
for spec in "auth:8081" "job:8084" "communication:8089" "offer:8087" "onboard:8088" "interview:8086" "evolution:8090" "agent:8091" "llm:8095"; do
  name="${spec%%:*}"
  port="${spec##*:}"
  start_svc "$name" "$port" || FAILED=1
done
if [ "$FAILED" -ne 0 ]; then
  echo "部分服务启动失败，请查看 /tmp/recruitos-*.log"
  exit 1
fi
echo "done."
