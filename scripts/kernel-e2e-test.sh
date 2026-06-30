#!/usr/bin/env bash
# Tests E2E intégration kernel P0 (login → bootstrap → backend local → façade).
# Usage:
#   ./scripts/kernel-e2e-test.sh              # MFA : étape 1 envoie email
#   MFA_CODE=123456 ./scripts/kernel-e2e-test.sh
#   ACCESS_TOKEN=... ./scripts/kernel-e2e-test.sh   # skip login si token valide

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${SCRIPT_DIR}/.."
RENTAL_URL="${RENTAL_URL:-http://localhost:8081}"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export JAVA_HOME

cd "$BACKEND_DIR"

# shellcheck disable=SC1090
[[ -f kernel-core.credentials.env ]] && source kernel-core.credentials.env
export KERNEL_INTEGRATION_ENABLED=true

pass() { echo "✅ $*"; }
fail() { echo "❌ $*"; exit 1; }
wait_api() {
  local i
  for i in $(seq 1 90); do
    if curl -s -o /dev/null -w "%{http_code}" "${RENTAL_URL}/v3/api-docs" | grep -q 200; then
      return 0
    fi
    sleep 2
  done
  return 1
}

echo "========== 1. Login kernel (MFA) =========="
if [[ -n "${ACCESS_TOKEN:-}" ]]; then
  pass "ACCESS_TOKEN fourni — skip login"
elif [[ -f .kernel-access-token ]]; then
  ACCESS_TOKEN=$(cat .kernel-access-token)
  HTTP_ME=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "X-Client-Id: ${KERNEL_CLIENT_ID}" \
    -H "X-Api-Key: ${KERNEL_API_KEY}" \
    -H "X-Tenant-Id: ${KERNEL_TENANT_ID}" \
    "${KERNEL_BASE_URL}/api/users/me")
  if [[ "$HTTP_ME" != "200" ]]; then
    echo "Token expiré (HTTP ${HTTP_ME}). Nouveau login MFA requis."
    unset ACCESS_TOKEN
  else
    pass "Token .kernel-access-token encore valide"
  fi
fi

if [[ -z "${ACCESS_TOKEN:-}" ]]; then
  if [[ -n "${MFA_CODE:-}" ]]; then
    "${SCRIPT_DIR}/kernel-validate-login.sh" "$MFA_CODE"
  else
    "${SCRIPT_DIR}/kernel-validate-login.sh"
    echo ""
    echo "Un code MFA a été envoyé par email."
    echo "Relancer: MFA_CODE=<code> ./scripts/kernel-e2e-test.sh"
    exit 0
  fi
  ACCESS_TOKEN=$(cat .kernel-access-token)
fi
export ACCESS_TOKEN

echo ""
echo "========== 2. Bootstrap org kernel =========="
"${SCRIPT_DIR}/kernel-bootstrap-org.sh" | tee /tmp/kernel-bootstrap.log
ORG_LINE=$(grep '^export ORGANIZATION_ID=' /tmp/kernel-bootstrap.log | tail -1 || true)
if [[ -n "$ORG_LINE" ]]; then
  # shellcheck disable=SC1090
  eval "$ORG_LINE"
fi
[[ -n "${ORGANIZATION_ID:-}" ]] && pass "Org kernel: ${ORGANIZATION_ID}" || echo "⚠️  Vérifier ORGANIZATION_ID"

echo ""
echo "========== 3. Backend Easy Rental (Maven local) =========="
docker compose stop backend 2>/dev/null || true
if command -v lsof >/dev/null && lsof -i :8081 -t >/dev/null 2>&1; then
  lsof -i :8081 -t | xargs -r kill -9 || true
  sleep 2
fi

LOG_FILE="${BACKEND_DIR}/.kernel-e2e-spring.log"
nohup ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=local >"$LOG_FILE" 2>&1 &
SPRING_PID=$!
echo "Spring Boot PID=${SPRING_PID} (log: ${LOG_FILE})"

wait_api && pass "API ${RENTAL_URL} up" || fail "Backend non démarré — voir ${LOG_FILE}"

echo ""
echo "========== 4. Façade auth (proxy kernel) =========="
LOGIN=$(curl -s -w "\nHTTP:%{http_code}" -X POST "${RENTAL_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${KERNEL_ADMIN_USERNAME}\",\"password\":\"${KERNEL_ADMIN_PASSWORD}\"}")
HTTP_LOGIN=$(echo "$LOGIN" | tail -1 | cut -d: -f2)
BODY_LOGIN=$(echo "$LOGIN" | sed '$d')
echo "$BODY_LOGIN" | jq '.' 2>/dev/null || echo "$BODY_LOGIN"

RENTAL_TOKEN=""
if [[ "$HTTP_LOGIN" == "202" ]] || echo "$BODY_LOGIN" | jq -e '.mfaRequired == true' >/dev/null 2>&1; then
  pass "Façade login → MFA (proxy kernel OK)"
  MFA_TOKEN=$(echo "$BODY_LOGIN" | jq -r '.mfaToken // empty')
  if [[ -n "${MFA_CODE:-}" && -n "$MFA_TOKEN" ]]; then
    FACADE_MFA=$(curl -s -X POST "${RENTAL_URL}/auth/login/mfa/confirm" \
      -H "Content-Type: application/json" \
      -d "{\"mfaToken\":\"${MFA_TOKEN}\",\"code\":\"${MFA_CODE}\"}")
    RENTAL_TOKEN=$(echo "$FACADE_MFA" | jq -r '.token // empty')
    [[ -n "$RENTAL_TOKEN" && "$RENTAL_TOKEN" != "null" ]] && pass "Façade MFA → token" || fail "Façade MFA échouée"
  else
    echo "Test permissions avec token kernel admin (façade MFA non confirmée)"
    RENTAL_TOKEN="${ACCESS_TOKEN}"
  fi
else
  RENTAL_TOKEN=$(echo "$BODY_LOGIN" | jq -r '.token // empty')
  [[ -n "$RENTAL_TOKEN" && "$RENTAL_TOKEN" != "null" ]] && pass "Façade login → token direct" || fail "Login façade HTTP ${HTTP_LOGIN}"
fi

echo ""
echo "========== 5. Permissions JWT (façade) =========="
PERMS=$(curl -s -w "\nHTTP:%{http_code}" \
  -H "Authorization: Bearer ${RENTAL_TOKEN}" \
  "${RENTAL_URL}/api/users/me/permissions")
HTTP_PERMS=$(echo "$PERMS" | tail -1 | cut -d: -f2)
PERMS_BODY=$(echo "$PERMS" | sed '$d')
echo "$PERMS_BODY" | jq '.' 2>/dev/null | head -20 || echo "$PERMS_BODY" | head -5
[[ "$HTTP_PERMS" == "200" ]] && pass "GET /api/users/me/permissions HTTP 200" || fail "Permissions HTTP ${HTTP_PERMS}"

echo ""
echo "========== Résumé =========="
pass "Tests E2E kernel P0 terminés"
echo "ORGANIZATION_ID=${ORGANIZATION_ID:-non défini}"
echo "Spring Boot tourne (PID ${SPRING_PID}). Arrêt: kill ${SPRING_PID}"
echo "Swagger: ${RENTAL_URL}/swagger-ui.html"
