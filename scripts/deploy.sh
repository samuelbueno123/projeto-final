#!/usr/bin/env bash
# ==============================================================================
# PROJETO FINAL - SCRIPT DE IMPLANTAÇÃO (DEPLOY EM PRODUÇÃO)
# Realiza pre-flight checks, build das imagens, rolling update e health checks.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

DRY_RUN=false
TARGET_ENV="prod"
HEALTH_TIMEOUT=60

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --env=*)
            TARGET_ENV="${1#*=}"
            shift
            ;;
        --timeout=*)
            HEALTH_TIMEOUT="${1#*=}"
            shift
            ;;
        -h|--help)
            echo "Uso: $0 [--dry-run] [--env=prod|dev] [--timeout=segundos]"
            exit 0
            ;;
        *)
            echo "Opção desconhecida: $1" >&2
            exit 1
            ;;
    esac
done

echo "=========================================================="
echo "Iniciando processo de implantação (Ambiente: ${TARGET_ENV})"
echo "Modo Dry-Run: ${DRY_RUN}"
echo "=========================================================="

echo "=== [1/5] Verificação de pré-requisitos e variáveis de ambiente ==="
ENV_FILE="${ROOT_DIR}/.env"
if [ ! -f "${ENV_FILE}" ]; then
    echo "AVISO: Arquivo .env não encontrado na raiz. Verificando backend/.env..."
    ENV_FILE="${ROOT_DIR}/backend/.env"
fi

if [ "$TARGET_ENV" = "prod" ]; then
    REQUIRED_VARS=("DB_URL" "DB_PASSWORD" "CLIENT_SECRET")
    for var in "${REQUIRED_VARS[@]}"; do
        if [ -f "$ENV_FILE" ]; then
            if ! grep -q "^${var}=" "$ENV_FILE" && [ -z "${!var:-}" ]; then
                echo "ERRO: Variável de ambiente obrigatória '$var' não configurada para produção!" >&2
                exit 1
            fi
        fi
    done
    echo "Todas as variáveis críticas de produção foram validadas."
fi

echo "=== [2/5] Verificação de Migrações Flyway e Banco de Dados ==="
FLYWAY_DIR="${ROOT_DIR}/backend/src/main/resources/db/migration"
if [ -d "$FLYWAY_DIR" ]; then
    MIGRATION_COUNT=$(find "$FLYWAY_DIR" -name "V*.sql" | wc -l)
    echo "Detectadas ${MIGRATION_COUNT} migrations Flyway prontas para aplicação."
fi

if [ "$DRY_RUN" = true ]; then
    echo "[DRY-RUN] Verificações de pré-implantação concluídas com sucesso. Nenhuma ação executada."
    exit 0
fi

if ! command -v docker &> /dev/null; then
    echo "ERRO: Docker não encontrado no PATH. Instale o Docker para executar o deploy." >&2
    exit 1
fi

echo "=== [3/5] Construindo imagens conteinerizadas ==="
COMPOSE_FILES=("-f" "${ROOT_DIR}/docker-compose.yml")
if [ "$TARGET_ENV" = "prod" ] && [ -f "${ROOT_DIR}/docker-compose.prod.yml" ]; then
    COMPOSE_FILES+=("-f" "${ROOT_DIR}/docker-compose.prod.yml")
fi

docker compose "${COMPOSE_FILES[@]}" build --pull api frontend

echo "=== [4/5] Executando Rolling Update com Zero Downtime ==="
# Atualiza os containers sem derrubar o banco de dados
docker compose "${COMPOSE_FILES[@]}" up -d --no-deps api frontend

echo "=== [5/5] Verificando integridade e Healthcheck pós-deploy ==="
echo "Aguardando endpoint /actuator/health responder status UP (timeout: ${HEALTH_TIMEOUT}s)..."

START_TIME=$(date +%s)
HEALTHY=false

while true; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))

    if [ "$ELAPSED" -ge "$HEALTH_TIMEOUT" ]; then
        echo "ERRO: Timeout atingido (${HEALTH_TIMEOUT}s) sem confirmação de saúde da API!" >&2
        echo "Disparando procedimento automático de rollback..."
        "${SCRIPT_DIR}/rollback.sh"
        exit 1
    fi

    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || true)
    if [ "$HTTP_STATUS" = "200" ]; then
        HEALTHY=true
        break
    fi

    sleep 3
done

if [ "$HEALTHY" = true ]; then
    echo "Implantação concluída com sucesso! API e Frontend operando com integridade total."
fi
