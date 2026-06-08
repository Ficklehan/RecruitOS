#!/usr/bin/env bash
# 为 recruitos-agent Playwright RPA 安装 Chromium 浏览器
set -euo pipefail
BASE="$(cd "$(dirname "$0")/.." && pwd)"
JAVA="${JAVA_HOME:-/usr/bin}/bin/java"
MVN="${MVN:-mvn}"

echo ">> 编译 recruitos-agent..."
cd "$BASE"
"$MVN" -q -pl recruitos-agent -am package -DskipTests

JAR=$(ls "$BASE/recruitos-agent/target"/playwright-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  echo ">> 通过 Maven 依赖解析 Playwright CLI..."
  cd "$BASE/recruitos-agent"
  "$MVN" -q exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install chromium"
else
  echo ">> 使用 Playwright JAR 安装 chromium..."
  "$JAVA" -cp "$JAR" com.microsoft.playwright.CLI install chromium
fi

echo ">> Playwright Chromium 安装完成"
