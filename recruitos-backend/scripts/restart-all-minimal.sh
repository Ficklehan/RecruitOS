#!/usr/bin/env bash
# 用 screen 串行启动全部 RecruitOS 微服务（脱离 IDE 终端，避免会话结束连带杀 JVM）
set -uo pipefail
JAVA="${JAVA_HOME:-/Users/apple/tools/jdk8-payload-extract/Contents/Home}/bin/java"
JAVA_MEM=(-Xms128m -Xmx384m)
AGENT_JAVA_MEM=(-Xms256m -Xmx768m)
BASE="$(cd "$(dirname "$0")/.." && pwd)"
WAIT_SECS="${WAIT_SECS:-90}"
SCREEN_BIN="${SCREEN_BIN:-/usr/bin/screen}"

if [ ! -x "$SCREEN_BIN" ]; then
  echo "ERROR: screen not found ($SCREEN_BIN). Install screen or set SCREEN_BIN." >&2
  exit 1
fi

ALL_SERVICES=(auth tenant demand job candidate interview offer onboard communication evolution agent referral headhunter analytics)

echo ">> 停止已有 RecruitOS screen 会话与 JVM..."
for name in "${ALL_SERVICES[@]}"; do
  "$SCREEN_BIN" -S "ros-$name" -X quit >/dev/null 2>&1 || true
done
pkill -f 'recruitos-.*SNAPSHOT.jar' 2>/dev/null || true
sleep 3

wait_port() {
  local port=$1
  local i
  for ((i=1; i<=WAIT_SECS; i++)); do
    if lsof -ti "tcp:$port" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

start() {
  local name=$1 port=$2
  local jar="$BASE/recruitos-$name/target/recruitos-$name-1.0.0-SNAPSHOT.jar"
  local log="/tmp/recruitos-$name.log"
  local mem=("${JAVA_MEM[@]}")
  if [ "$name" = "agent" ]; then
    mem=("${AGENT_JAVA_MEM[@]}")
  fi
  if [ ! -f "$jar" ]; then
    echo "  recruitos-$name MISSING jar: $jar" >&2
    return 1
  fi
  "$SCREEN_BIN" -dmS "ros-$name" bash -c \
    "cd \"$BASE/recruitos-$name\" && exec \"$JAVA\" ${mem[*]} -jar \"$jar\" >> \"$log\" 2>&1"
  echo -n "  recruitos-$name (:$port) "
  if wait_port "$port"; then
    echo "UP"
  else
    echo "DOWN (see $log)"
    return 1
  fi
}

echo ">> 串行启动服务（screen 会话 ros-<name>）..."
start auth 8081 || true
start tenant 8082 || true
start demand 8083 || true
start job 8084 || true
start candidate 8085 || true
start interview 8086 || true
start offer 8087 || true
start onboard 8088 || true
start communication 8089 || true
start evolution 8090 || true
start agent 8091 || true
start referral 8092 || true
start headhunter 8093 || true
start analytics 8094 || true

echo ">> 最终状态:"
for port in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094; do
  printf "  :%-5s " "$port"
  lsof -ti "tcp:$port" >/dev/null 2>&1 && echo UP || echo DOWN
done
echo ">> screen 会话: screen -ls | grep ros-"
