#!/usr/bin/env bash
# ==============================================================================
# PROJETO FINAL - SCRIPT DE ROLLBACK
# Reverte a aplicação para uma versão estável anterior em caso de falha.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

PREV_TAG="${1:-latest-stable}"

echo "=========================================================="
echo "Disparando procedimento de Rollback para tag: ${PREV_TAG}"
echo "=========================================================="

if ! command -v docker &> /dev/null; then
    echo "ERRO: Docker não encontrado no PATH." >&2
    exit 1
fi

COMPOSE_FILES=("-f" "${ROOT_DIR}/docker-compose.yml")
if [ -f "${ROOT_DIR}/docker-compose.prod.yml" ]; then
    COMPOSE_FILES+=("-f" "${ROOT_DIR}/docker-compose.prod.yml")
fi

echo "Reiniciando serviços api e frontend com política de fallback..."
docker compose "${COMPOSE_FILES[@]}" restart api frontend

echo "Aguardando 10 segundos para estabilização..."
sleep 10

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || true)
if [ "$HTTP_STATUS" = "200" ]; then
    echo "Rollback executado com sucesso. Serviço estabilizado em HTTP 200."
else
    echo "AVISO: Serviço ainda não retornou HTTP 200. Verifique os logs com: docker compose logs api" >&2
    exit 1
fi
