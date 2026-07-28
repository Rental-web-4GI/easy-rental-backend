#!/usr/bin/env bash
# Smoke test R2 — vérifie que les nouveaux endpoints sont câblés (répondent, pas 404).
# Sans token : on attend 401/403 (endpoint existe + sécurisé), jamais 404 (route absente).
#
# Usage:
#   ./scripts/smoke-r2.sh                       # reachability (sans auth)
#   BASE=http://localhost:8081 ./scripts/smoke-r2.sh
#
# Pour un smoke fonctionnel complet (avec token + location PAID), voir docs/API-R2.md.

set -u
BASE="${BASE:-http://localhost:8081}"
RID="00000000-0000-0000-0000-000000000000"   # UUID bidon — on teste le routage, pas la donnée
AID="00000000-0000-0000-0000-000000000000"
pass=0; fail=0

check() {
  local method="$1" path="$2" expected_not="404"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE$path" \
           -H "Content-Type: application/json" -d '{}' 2>/dev/null)
  if [ "$code" = "$expected_not" ]; then
    echo "  ✗ $method $path -> $code (route ABSENTE)"; fail=$((fail+1))
  else
    echo "  ✓ $method $path -> $code (route présente)"; pass=$((pass+1))
  fi
}

echo "== Smoke R2 sur $BASE =="
echo "-- Cycle location --"
check POST "/api/rentals/$RID/check-in"
check POST "/api/rentals/$RID/signal-end"
check POST "/api/rentals/$RID/check-out"
check PUT  "/api/rentals/$RID/settle-return"
echo "-- Inspections --"
check POST "/api/inspections/rentals/$RID"
check GET  "/api/inspections/rentals/$RID/all"
check GET  "/api/inspections/rentals/$RID/comparison"
echo "-- Tracking --"
check POST "/api/rentals/$RID/positions"
check GET  "/api/rentals/$RID/tracking"
echo "-- Ratings --"
check POST "/api/ratings"
check GET  "/api/ratings/agencies/$AID/stats"

echo ""
echo "== $pass routes présentes, $fail absentes =="
[ "$fail" -eq 0 ] && echo "SMOKE OK" || { echo "SMOKE FAILED"; exit 1; }
