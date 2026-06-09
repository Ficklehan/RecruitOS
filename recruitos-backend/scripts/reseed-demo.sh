#!/usr/bin/env bash
# 重载演示数据：结构迁移 + 管道链路 + 业务关系
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
# 必须使用 utf8mb4 客户端字符集，否则中文会乱码写入
MYSQL=("$SCRIPTS/mysql.sh")

run_sql() {
  "${MYSQL[@]}" < "$1"
}

echo ">> migration v2 (schema align)"
run_sql "$ROOT/sql/migration-v2-schema-align.sql" 2>/dev/null || true

echo ">> migration v3 (sourcing campaign)"
run_sql "$ROOT/sql/migration-v3-sourcing-campaign.sql" 2>/dev/null || true

echo ">> migration v4 (updated_at columns)"
run_sql "$ROOT/sql/migration-v4-updated-at.sql" 2>/dev/null || true

echo ">> migration v5 (referral align)"
run_sql "$ROOT/sql/migration-v5-referral-align.sql" 2>/dev/null || true

echo ">> migration v6 (recruitment channel)"
run_sql "$ROOT/sql/migration-v6-recruitment-channel.sql" 2>/dev/null || true

echo ">> migration v7 (boss/liepin channels)"
run_sql "$ROOT/sql/migration-v7-channel-boss-liepin-only.sql" 2>/dev/null || true

echo ">> migration v10 (phase2 evolution + ops pack)"
run_sql "$ROOT/sql/migration-v10-phase2-evolution-ops.sql" 2>/dev/null || true

echo ">> migration v11 (safety_log align)"
run_sql "$ROOT/sql/migration-v11-safety-log-align.sql" 2>/dev/null || true

echo ">> test data (jobs, candidates, demands, ...)"
run_sql "$ROOT/sql/test-data.sql"

echo ">> pipeline seed"
run_sql "$ROOT/sql/test-data-pipeline-seed.sql"

echo ">> linkage seed (resume, campaign, timeline, approvals)"
run_sql "$ROOT/sql/test-data-linkage-seed.sql"

echo ">> candidate rich profiles (full resume JSON)"
run_sql "$ROOT/sql/test-data-candidate-rich.sql"

echo ">> phase2 seed (ops pack, evolution proposal, staging)"
run_sql "$ROOT/sql/test-data-phase2-seed.sql"

echo ">> done. 请重启 tenant(8082) candidate(8085) offer(8087) onboard(8088) agent(8091) evolution(8090) 服务"
echo ">> 验收路径见 RecruitOS/DEMO-VERIFICATION.md"
