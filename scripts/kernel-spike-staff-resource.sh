#!/usr/bin/env bash
# Spike kernel P1 — documente payloads staff invite + resource-core.
# Usage: ACCESS_TOKEN=... ORGANIZATION_ID=... ./scripts/kernel-spike-staff-resource.sh

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1090
[[ -f "${SCRIPT_DIR}/../kernel-core.credentials.env" ]] && source "${SCRIPT_DIR}/../kernel-core.credentials.env"
: "${KERNEL_BASE_URL:?}"
: "${ACCESS_TOKEN:?}"
ORG_ID="${ORGANIZATION_ID:?}"

auth() {
  curl -s "$@" \
    -H "X-Client-Id: ${KERNEL_CLIENT_ID}" \
    -H "X-Api-Key: ${KERNEL_API_KEY}" \
    -H "X-Tenant-Id: ${KERNEL_TENANT_ID}" \
    -H "X-Organization-Id: ${ORG_ID}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}"
}

echo "=== POST /api/employees/invite (payload attendu: userId, roleId, agencyId) ==="
auth -X POST "${KERNEL_BASE_URL}/api/employees/invite?organizationId=${ORG_ID}" \
  -H "Content-Type: application/json" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","roleId":"00000000-0000-0000-0000-000000000002","agencyId":"00000000-0000-0000-0000-000000000003"}' | head -c 800
echo ""
echo "=== POST /api/resources (resource-core VEHICLE) ==="
auth -X POST "${KERNEL_BASE_URL}/api/resources" \
  -H "Content-Type: application/json" \
  -d '{"code":"SPIKE-01","name":"Spike Vehicle","resourceType":"VEHICLE","organizationId":"'"${ORG_ID}"'"}' | head -c 800
echo ""
