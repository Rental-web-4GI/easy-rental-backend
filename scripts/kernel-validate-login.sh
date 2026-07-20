#!/usr/bin/env bash
# Valide l'accès production kernel-core (login + MFA optionnel).
# Usage: cd easy-rental-backend && source kernel-core.credentials.env && ./scripts/kernel-validate-login.sh [MFA_CODE]
#        ./scripts/kernel-validate-login.sh [MFA_CODE]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CREDS="${SCRIPT_DIR}/../kernel-core.credentials.env"
if [[ -f "$CREDS" ]]; then
  # shellcheck disable=SC1090
  source "$CREDS"
fi

: "${KERNEL_BASE_URL:?KERNEL_BASE_URL required}"
: "${KERNEL_CLIENT_ID:?KERNEL_CLIENT_ID required}"
: "${KERNEL_API_KEY:?KERNEL_API_KEY required}"
: "${KERNEL_TENANT_ID:?KERNEL_TENANT_ID required}"
: "${KERNEL_ADMIN_USERNAME:?KERNEL_ADMIN_USERNAME required}"
: "${KERNEL_ADMIN_PASSWORD:?KERNEL_ADMIN_PASSWORD required}"

MFA_SESSION="${SCRIPT_DIR}/../.kernel-mfa-session"
CODE="${1:-}"

# Étape 2 : confirmer MFA avec le token sauvegardé (sans relancer un login)
if [[ -n "$CODE" && -f "$MFA_SESSION" ]]; then
  MFA_TOKEN=$(cat "$MFA_SESSION")
  echo "Confirmation MFA (session existante)..."
  CONFIRM=$(curl -s -w "\n%{http_code}" -X POST "${KERNEL_BASE_URL}/api/auth/login/mfa/confirm" \
    -H "X-Client-Id: ${KERNEL_CLIENT_ID}" \
    -H "X-Api-Key: ${KERNEL_API_KEY}" \
    -H "X-Tenant-Id: ${KERNEL_TENANT_ID}" \
    -H "Content-Type: application/json" \
    -d "{\"mfaToken\":\"${MFA_TOKEN}\",\"code\":\"${CODE}\"}")
  HTTP_CONFIRM=$(echo "$CONFIRM" | tail -n1)
  BODY_CONFIRM=$(echo "$CONFIRM" | sed '$d')
  echo "MFA confirm HTTP: $HTTP_CONFIRM"
  echo "$BODY_CONFIRM" | jq '.' 2>/dev/null || echo "$BODY_CONFIRM"
  ACCESS_TOKEN=$(echo "$BODY_CONFIRM" | jq -r '.data.accessToken // empty')
  REFRESH_TOKEN=$(echo "$BODY_CONFIRM" | jq -r '.data.refreshToken // .data.sessionToken // empty')
  if [[ -n "$ACCESS_TOKEN" && "$ACCESS_TOKEN" != "null" ]]; then
    rm -f "$MFA_SESSION"
    echo "$ACCESS_TOKEN" > "${SCRIPT_DIR}/../.kernel-access-token"
    if [[ -n "$REFRESH_TOKEN" && "$REFRESH_TOKEN" != "null" ]]; then
      echo "$REFRESH_TOKEN" > "${SCRIPT_DIR}/../.kernel-refresh-token"
      echo "Refresh token sauvegardé dans .kernel-refresh-token"
    fi
    echo "--- JWT payload (claims) ---"
    echo "$ACCESS_TOKEN" | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | jq '.'
    echo "--- GET /api/users/me ---"
    curl -s "${KERNEL_BASE_URL}/api/users/me" \
      -H "X-Client-Id: ${KERNEL_CLIENT_ID}" \
      -H "X-Api-Key: ${KERNEL_API_KEY}" \
      -H "X-Tenant-Id: ${KERNEL_TENANT_ID}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq '.'
    echo ""
    echo "export ACCESS_TOKEN='${ACCESS_TOKEN}'"
    exit 0
  fi
  echo "Code invalide ou expiré. Relancer sans argument pour un nouveau code email."
  exit 1
fi

LOGIN_JSON=$(curl -s -w "\n%{http_code}" -X POST "${KERNEL_BASE_URL}/api/auth/login" \
  -H "X-Client-Id: ${KERNEL_CLIENT_ID}" \
  -H "X-Api-Key: ${KERNEL_API_KEY}" \
  -H "X-Tenant-Id: ${KERNEL_TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"principal\":\"${KERNEL_ADMIN_USERNAME}\",\"password\":\"${KERNEL_ADMIN_PASSWORD}\"}")

HTTP_CODE=$(echo "$LOGIN_JSON" | tail -n1)
BODY=$(echo "$LOGIN_JSON" | sed '$d')

echo "Login HTTP: $HTTP_CODE"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [[ "$HTTP_CODE" == "202" ]]; then
  MFA_TOKEN=$(echo "$BODY" | jq -r '.data.mfaToken')
  echo "$MFA_TOKEN" > "$MFA_SESSION"
  if [[ -z "$CODE" ]]; then
    echo "MFA requis. Consultez votre email, puis relancer: $0 <code>"
    exit 0
  fi
  echo "Aucune session MFA en cache. Relancer d'abord sans argument, puis avec le code."
  exit 1
else
  ACCESS_TOKEN=$(echo "$BODY" | jq -r '.data.accessToken')
fi

if [[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]]; then
  echo "Pas de accessToken."
  exit 1
fi

echo "--- JWT payload (claims) ---"
echo "$ACCESS_TOKEN" | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | jq '.'

echo "--- GET /api/users/me ---"
curl -s "${KERNEL_BASE_URL}/api/users/me" \
  -H "X-Client-Id: ${KERNEL_CLIENT_ID}" \
  -H "X-Api-Key: ${KERNEL_API_KEY}" \
  -H "X-Tenant-Id: ${KERNEL_TENANT_ID}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq '.'
echo ""
echo "export ACCESS_TOKEN='${ACCESS_TOKEN}'"
