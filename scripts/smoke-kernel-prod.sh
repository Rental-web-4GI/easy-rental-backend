#!/usr/bin/env bash
# Smoke E2E kernel prod — run against staging/prod API (default :8081)
set -euo pipefail

BASE_URL="${SMOKE_BASE_URL:-http://localhost:8081}"
echo "Smoke test against ${BASE_URL}"

curl -sf "${BASE_URL}/actuator/health" | grep -q '"status":"UP"' && echo "OK health"

# Anonymous support config (no auth)
curl -sf "${BASE_URL}/api/support/config" >/dev/null && echo "OK support config"

echo ""
echo "Manual E2E checklist:"
echo "  1. Login organisation (email verify ON in prod)"
echo "  2. Create agency + staff (no email verify)"
echo "  3. Create vehicle (resource-core, no local fallback)"
echo "  4. Idle 40 min FE session + POST /auth/refresh"
echo "Done."
