#!/usr/bin/env bash
# 重启核心微服务（tenant/candidate/offer/onboard），限制 JVM 内存避免 OOM(137)
set -euo pipefail
JAVA="${JAVA_HOME:-/Users/apple/tools/jdk8-payload-extract/Contents/Home}/bin/java"
JAVA_MEM=(-Xms128m -Xmx256m)
BASE="$(cd "$(dirname "$0")/.." && pwd)"

for port in 8082 8085 8087 8088; do
  pid=$(lsof -ti tcp:$port 2>/dev/null || true)
  [ -n "$pid" ] && kill -9 $pid || true
done
sleep 2

start() {
  local name=$1
  cd "$BASE/recruitos-$name"
  nohup "$JAVA" "${JAVA_MEM[@]}" -jar "target/recruitos-$name-1.0.0-SNAPSHOT.jar" >> "/tmp/recruitos-$name.log" 2>&1 &
  echo "started recruitos-$name pid=$!"
}

start tenant
start candidate
start offer
start onboard
echo "logs: /tmp/recruitos-{tenant,candidate,offer,onboard}.log"
