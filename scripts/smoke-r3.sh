#!/usr/bin/env bash
# Smoke test Release 3 — fidélité + chat + sécurité admin.
# Le litige (T11/T12) est reporté en R4 : non testé ici.
#
# Prérequis :
#   - backend up (localhost:8081)
#   - export TOKEN_ADMIN=<jwt-admin>
#   - export TOKEN_CLIENT=<jwt-client>   (pour fidélité + chat côté client)
#   - export CLIENT_ID=<uuid client>
#   - export AGENCY_ID=<uuid agence cible du chat>   (optionnel)
#   - export ORG_ID=<uuid org à suspendre/réactiver>  (optionnel)
set -euo pipefail

: "${TOKEN_ADMIN:?export TOKEN_ADMIN=<admin-jwt>}"
: "${BASE:=http://localhost:8081}"

hr() { printf '\n── %s ──\n' "$1"; }
A=(-H "Authorization: Bearer $TOKEN_ADMIN")

# ─────────────────────────── Sécurité admin ───────────────────────────
hr "1. Dashboard plateforme (champs R3 : suspended + dette)"
curl -sSf "${A[@]}" "$BASE/api/admin/stats/platform" \
  | jq '{suspended: .organizations.suspended, debt: .revenue.total_outstanding_debt}'

hr "2. Audit events avec IP (dernier 5)"
curl -sSf "${A[@]}" "$BASE/api/admin/audit-events?size=5" \
  | jq '.[] | {action, created_at, ip, user_agent}'

hr "3. Supervision chat admin (admin/all)"
curl -sSf "${A[@]}" "$BASE/api/conversations/admin/all?size=5" \
  | jq 'length as $n | "conversations visibles admin: \($n)"'

if [ -n "${ORG_ID:-}" ]; then
  hr "4. Suspension organisation $ORG_ID"
  curl -sSf "${A[@]}" -H "Content-Type: application/json" \
    -X POST "$BASE/api/admin/organizations/$ORG_ID/suspend" \
    -d '{"reason":"smoke-r3"}' | jq '{id, status, suspension_reason}'
  hr "5. Réactivation organisation $ORG_ID"
  curl -sSf "${A[@]}" -X POST "$BASE/api/admin/organizations/$ORG_ID/reactivate" \
    | jq '{id, status}'
else
  hr "4-5. Suspension/réactivation SKIP (export ORG_ID pour tester)"
fi

# ─────────────────────────── Fidélité (client) ───────────────────────────
if [ -n "${TOKEN_CLIENT:-}" ] && [ -n "${CLIENT_ID:-}" ]; then
  C=(-H "Authorization: Bearer $TOKEN_CLIENT")
  hr "6. Solde fidélité client"
  curl -sSf "${C[@]}" "$BASE/api/loyalty/balance/$CLIENT_ID" \
    | jq '{balance, annual_points, tier}'
  hr "7. Historique fidélité client (dernier 5)"
  curl -sSf "${C[@]}" "$BASE/api/loyalty/history/$CLIENT_ID" \
    | jq 'length as $n | "entrées ledger: \($n)"'

  # ─────────────────────── Chat (client) ───────────────────────
  if [ -n "${AGENCY_ID:-}" ]; then
    hr "8. Ouverture conversation client → agence"
    CONV=$(curl -sSf "${C[@]}" -H "Content-Type: application/json" \
      -X POST "$BASE/api/conversations/open" \
      -d "{\"target_type\":\"AGENCY\",\"target_id\":\"$AGENCY_ID\"}")
    echo "$CONV" | jq '{id, conversation_type: .conversationType}'
    CONV_ID=$(echo "$CONV" | jq -r '.id')
    hr "9. Envoi message + relecture"
    curl -sSf "${C[@]}" -H "Content-Type: application/json" \
      -X POST "$BASE/api/conversations/$CONV_ID/messages" \
      -d '{"body":"Bonjour, smoke-r3"}' | jq '{id, body}'
    curl -sSf "${C[@]}" "$BASE/api/conversations/$CONV_ID/messages?size=5" \
      | jq 'length as $n | "messages: \($n)"'
  else
    hr "8-9. Chat SKIP (export AGENCY_ID pour tester)"
  fi
else
  hr "6-9. Fidélité + chat client SKIP (export TOKEN_CLIENT et CLIENT_ID)"
fi

hr "OK — R3 endpoints répondent"
