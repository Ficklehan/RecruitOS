#!/usr/bin/env bash
# 加载 .env 并启动 RecruitOS 全部后端微服务（含 LLM 8095）
set -euo pipefail

JAVA="${JAVA:-$(command -v java)}"
BASE="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$BASE/.env"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  echo "已加载 $ENV_FILE"
fi

export MIMO_API_KEY="${MIMO_API_KEY:-}"
export MIMO_MODEL="${MIMO_MODEL:-mimo-v2.5}"
export MIMO_BASE_URL="${MIMO_BASE_URL:-https://token-plan-cn.xiaomimimo.com/v1}"
export MIMO_AUTH_MODE="${MIMO_AUTH_MODE:-api-key}"

start_svc() {
  local name=$1 port=$2
  local jar="$BASE/recruitos-$name/target/recruitos-$name-1.0.0-SNAPSHOT.jar"
  local log="/tmp/recruitos-$name.log"
  if [ ! -f "$jar" ]; then
    echo "缺少 $jar"
    echo "请先: cd \"$BASE\" && mvn install -DskipTests"
    exit 1
  fi
  /usr/bin/screen -S "ros-$name" -X quit >/dev/null 2>&1 || true
  sleep 0.5
  : > "$log"
  /usr/bin/screen -dmS "ros-$name" bash -c "cd \"$BASE/recruitos-$name\" && exec env MIMO_API_KEY=\"$MIMO_API_KEY\" MIMO_MODEL=\"$MIMO_MODEL\" MIMO_BASE_URL=\"$MIMO_BASE_URL\" \"$JAVA\" -Xms128m -Xmx512m -jar \"$jar\" >> \"$log\" 2>&1"
  echo -n "  $name :$port "
  for _ in $(seq 1 90); do
    if curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
      echo "✓"
      return 0
    fi
    if lsof -ti "tcp:$port" >/dev/null 2>&1; then
      echo "✓ (port up, health pending)"
      return 0
    fi
    echo -n "."
    sleep 1
  done
  echo "✗"
  tail -15 "$log" || true
  return 1
}

echo "构建后端..."
(cd "$BASE" && mvn install -DskipTests -q)

echo "启动微服务..."
start_svc auth 8081
start_svc tenant 8082
start_svc demand 8083
start_svc job 8084
start_svc candidate 8085
start_svc interview 8086
start_svc offer 8087
start_svc onboard 8088
start_svc communication 8089
start_svc evolution 8090
start_svc agent 8091
start_svc referral 8092
start_svc headhunter 8093
start_svc analytics 8094
start_svc llm 8095
start_svc brain 8100

echo ""
echo "全部服务已启动。"
echo "  前端: cd recruitos-frontend && npm run dev  → http://localhost:5001"
echo "  LLM:  http://localhost:8095/actuator/health  model=$MIMO_MODEL"
echo "  Brain: http://localhost:8100/actuator/health"
echo "  停止: screen -ls | grep ros-  然后 screen -S ros-<name> -X quit"
