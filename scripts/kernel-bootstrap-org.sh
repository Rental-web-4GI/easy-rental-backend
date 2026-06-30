#!/usr/bin/env bash
# Bootstrap org de test Easy Rental sur kernel (création optionnelle + approve + services).
# Prérequis: ACCESS_TOKEN admin (platform-admin après MFA).
# Usage:
#   export ACCESS_TOKEN=... && ./scripts/kernel-bootstrap-org.sh
#   ./scripts/kernel-bootstrap-org.sh   # lit .kernel-access-token si présent

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CREDS="${SCRIPT_DIR}/../kernel-core.credentials.env"
TOKEN_FILE="${SCRIPT_DIR}/../.kernel-access-token"
if [[ -f "$CREDS" ]]; then
  # shellcheck disable=SC1090
  source "$CREDS"
fi

: "${KERNEL_BASE_URL:?}"
: "${KERNEL_CLIENT_ID:?}"
: "${KERNEL_API_KEY:?}"
: "${KERNEL_TENANT_ID:?}"

if [[ -z "${ACCESS_TOKEN:-}" && -f "$TOKEN_FILE" ]]; then
  ACCESS_TOKEN=$(cat "$TOKEN_FILE")
fi
: "${ACCESS_TOKEN:?ACCESS_TOKEN required (login MFA ou .kernel-access-token)}"

ORG_CODE="${ORG_CODE:-ORG-EASY-RENTAL-TEST}"
ORG_NAME="${ORG_NAME:-Easy Rental Test Org}"
CREATE_ORG="${CREATE_ORG:-auto}"

auth_headers() {
  curl -s "$@" \
    -H "X-Client-Id: ${KERNEL_CLIENT_ID}" \
    -H "X-Api-Key: ${KERNEL_API_KEY}" \
    -H "X-Tenant-Id: ${KERNEL_TENANT_ID}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}"
}

echo "--- Mes organisations ---"
MY_ORGS=$(auth_headers "${KERNEL_BASE_URL}/api/organizations/my")
echo "$MY_ORGS" | jq '.'

if [[ -z "${ORGANIZATION_ID:-}" ]]; then
  ORGANIZATION_ID=$(echo "$MY_ORGS" | jq -r '
    if .data then
      (.data.organizations // .data // .) |
      if type == "array" then (.[0].id // empty) else (.id // empty) end
    elif type == "array" then (.[0].id // empty) else (.id // empty) end
  ' 2>/dev/null || true)
fi

if [[ -z "$ORGANIZATION_ID" || "$ORGANIZATION_ID" == "null" ]]; then
  if [[ "$CREATE_ORG" == "false" ]]; then
    echo "Aucune org. Définir ORGANIZATION_ID ou CREATE_ORG=auto."
    exit 1
  fi
  echo "--- Création organisation (${ORG_CODE}) ---"
  ACTOR_ID="${KERNEL_ACTOR_ID:-}"
  if [[ -z "$ACTOR_ID" ]]; then
    ME=$(auth_headers "${KERNEL_BASE_URL}/api/users/me")
    ACTOR_ID=$(echo "$ME" | jq -r '.data.actorId // .actorId // empty')
  fi
  if [[ -z "$ACTOR_ID" || "$ACTOR_ID" == "null" ]]; then
    echo "actorId introuvable. export KERNEL_ACTOR_ID=<uuid>"
    exit 1
  fi
  CREATE=$(auth_headers -X POST "${KERNEL_BASE_URL}/api/organizations" \
    -H "Content-Type: application/json" \
    -d "{\"businessActorId\":\"${ACTOR_ID}\",\"code\":\"${ORG_CODE}\",\"legalName\":\"${ORG_NAME}\",\"displayName\":\"${ORG_NAME}\",\"organizationType\":\"PRIVATE_COMPANY\"}")
  echo "$CREATE" | jq '.'
  ORGANIZATION_ID=$(echo "$CREATE" | jq -r '.data.id // .id // empty')
  if [[ -z "$ORGANIZATION_ID" || "$ORGANIZATION_ID" == "null" ]]; then
    echo "Échec création org."
    exit 1
  fi
  echo "ORGANIZATION_ID=${ORGANIZATION_ID}"
fi

echo "--- Souscription services pour ${ORGANIZATION_ID} ---"
for SVC in ORGANIZATION HRM SETTINGS RESOURCE COMMERCIAL; do
  auth_headers -X POST "${KERNEL_BASE_URL}/api/organizations/${ORGANIZATION_ID}/services" \
    -H "X-Organization-Id: ${ORGANIZATION_ID}" \
    -H "Content-Type: application/json" \
    -d "{\"serviceCode\":\"${SVC}\",\"requestQuotaLimit\":10000,\"requestQuotaWindowSeconds\":3600}" \
    | jq -c '{svc:"'"$SVC"'", ok:.success, err:.errorCode, msg:.message}'
done

echo "--- Approve org ---"
auth_headers -X POST "${KERNEL_BASE_URL}/api/organizations/${ORGANIZATION_ID}/approve" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Easy Rental P0 bootstrap"}' | jq '.'

echo ""
echo "export ORGANIZATION_ID='${ORGANIZATION_ID}'"
echo "${ORGANIZATION_ID}" > "${SCRIPT_DIR}/../.kernel-bootstrap-org-id"
