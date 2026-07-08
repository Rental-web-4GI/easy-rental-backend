#!/usr/bin/env bash
# Démarre easy-rental-backend en local avec intégration kernel (Java 21 requis).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/.."

if [[ -f kernel-core.credentials.env ]]; then
  set -a
  # shellcheck disable=SC1091
  source kernel-core.credentials.env
  set +a
fi

export KERNEL_INTEGRATION_ENABLED=true
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export PATH="${JAVA_HOME}/bin:${PATH}"

echo "Java: $(java -version 2>&1 | head -1)"
echo "Kernel: ${KERNEL_INTEGRATION_ENABLED} → ${KERNEL_BASE_URL:-non configuré}"
echo "Client auth: local PostgreSQL (easy-rental.client.skip-kernel-auth=true dans application-local.properties)"
echo "Port: 8081 — arrêtez le conteneur docker backend s'il tourne (docker compose stop backend)"

docker compose stop backend 2>/dev/null || docker-compose stop backend 2>/dev/null || true
(docker compose up -d postgres redis 2>/dev/null || docker-compose up -d postgres redis 2>/dev/null || true)

exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
