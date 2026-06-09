#!/usr/bin/env bash
# 重载 Phase2 核心服务（auth / job / evolution）— reseed 后执行
set -euo pipefail
JAVA="${JAVA:-/Users/apple/tools/jdk8-payload-extract/Contents/Home/bin/java}"
BASE="$(cd "$(dirname "$0")/.." && pwd)"

start_svc() {
  local name=$1 port=$2
  local jar="$BASE/recruitos-$name/target/recruitos-$name-1.0.0-SNAPSHOT.jar"
  local log="/tmp/recruitos-$name.log"
  if [ ! -f "$jar" ]; then
    echo "缺少 $jar，请先 mvn -pl recruitos-$name -am package -DskipTests"
    exit 1
  fi
  /usr/bin/screen -S "ros-$name" -X quit >/dev/null 2>&1 || true
  sleep 1
  : > "$log"
  /usr/bin/screen -dmS "ros-$name" bash -c "cd \"$BASE/recruitos-$name\" && exec \"$JAVA\" -Xms128m -Xmx384m -jar \"$jar\" >> \"$log\" 2>&1"
  echo -n "等待 $name :$port "
  for _ in $(seq 1 60); do
    if lsof -ti tcp:$port >/dev/null 2>&1; then echo "✓"; return 0; fi
    echo -n "."
    sleep 1
  done
  echo "✗"; tail -20 "$log"; return 1
}

start_svc auth 8081
start_svc job 8084
start_svc evolution 8090
echo "核心服务已就绪。前端: cd recruitos-frontend && npm run dev → http://localhost:5001"
