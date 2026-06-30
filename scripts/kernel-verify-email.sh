#!/usr/bin/env bash
# Confirme la vérification email kernel (lien /auth/verify-email cassé en prod).
# Usage:
#   ./scripts/kernel-verify-email.sh '<jwt_from_email_url>'
#   ./scripts/kernel-verify-email.sh --url 'https://kernel-core.yowyob.com/auth/verify-email?token=...'

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CREDS="${SCRIPT_DIR}/../kernel-core.credentials.env"
if [[ -f "$CREDS" ]]; then
  # shellcheck disable=SC1090
  source "$CREDS"
fi

: "${KERNEL_BASE_URL:?}"
: "${KERNEL_CLIENT_ID:?}"
: "${KERNEL_API_KEY:?}"
: "${KERNEL_TENANT_ID:?}"

TOKEN="${1:-}"
if [[ "${1:-}" == "--url" ]]; then
  URL="${2:-}"
  TOKEN=$(python3 -c "from urllib.parse import urlparse, parse_qs; u=urlparse('''$URL'''); print(parse_qs(u.query).get('token',[''])[0])")
fi

if [[ -z "$TOKEN" || "$TOKEN" == "--url" ]]; then
  echo "Usage: $0 '<verification_jwt>'"
  echo "   ou: $0 --url 'https://kernel-core.yowyob.com/auth/verify-email?token=...'"
  exit 1
fi

RESP=$(curl -s -w "\nHTTP:%{http_code}" -X POST "${KERNEL_BASE_URL}/api/auth/email-verification/confirm" \
  -H "X-Client-Id: ${KERNEL_CLIENT_ID}" \
  -H "X-Api-Key: ${KERNEL_API_KEY}" \
  -H "X-Tenant-Id: ${KERNEL_TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"verificationToken\":\"${TOKEN}\"}")

HTTP=$(echo "$RESP" | tail -1 | cut -d: -f2)
BODY=$(echo "$RESP" | sed '$d')
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
echo "HTTP: $HTTP"

if [[ "$HTTP" == "200" ]] || echo "$BODY" | jq -e '.success == true' >/dev/null 2>&1; then
  echo "Email vérifié. Connectez-vous sur http://localhost:3003/organisation (mode Connexion)."
  exit 0
fi
exit 1
