#!/usr/bin/env bash
# Smoke test Release 1 — vérifie les 3 endpoints admin ajoutés.
# Prérequis :
#   - backend up (localhost:8081)
#   - export TOKEN=<jwt-admin>
set -euo pipefail

: "${TOKEN:?export TOKEN=<admin-jwt> avant de lancer ce script}"
: "${BASE:=http://localhost:8081}"

hr() { printf '\n── %s ──\n' "$1"; }

hr "1. Platform stats"
curl -sSf -H "Authorization: Bearer $TOKEN" "$BASE/api/admin/stats/platform" | jq .

hr "2. Audit events (last 10)"
curl -sSf -H "Authorization: Bearer $TOKEN" "$BASE/api/admin/audit-events?size=10" \
  | jq '.[] | {action, createdAt, userId, ip}'

hr "3. Audit events filtered (LOGIN_FAILED)"
curl -sSf -H "Authorization: Bearer $TOKEN" "$BASE/api/admin/audit-events?action=LOGIN_FAILED&size=5" \
  | jq 'length as $n | "LOGIN_FAILED events: \($n)"'

hr "OK — R1 endpoints répondent"
