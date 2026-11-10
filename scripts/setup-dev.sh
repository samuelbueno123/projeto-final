#!/usr/bin/env bash
# ==============================================================================
# PROJETO FINAL - SETUP DO AMBIENTE DE DESENVOLVIMENTO
# Inicializa chaves RSA, variáveis de ambiente e containers locais.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "=== [1/5] Verificando pré-requisitos locais ==="
for cmd in openssl; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "ERRO: Ferramenta obrigatória '$cmd' não encontrada no PATH." >&2
        exit 1
    fi
done

echo "=== [2/5] Configurando arquivo .env ==="
if [ ! -f "${ROOT_DIR}/.env" ]; then
    if [ -f "${ROOT_DIR}/.env.example" ]; then
        cp "${ROOT_DIR}/.env.example" "${ROOT_DIR}/.env"
        echo "Arquivo .env criado a partir de .env.example."
    fi
else
    echo "Arquivo .env já existente na raiz."
fi

if [ ! -f "${ROOT_DIR}/backend/.env" ]; then
    if [ -f "${ROOT_DIR}/backend/.env.example" ]; then
        cp "${ROOT_DIR}/backend/.env.example" "${ROOT_DIR}/backend/.env"
        echo "Arquivo backend/.env criado a partir de backend/.env.example."
    fi
fi

echo "=== [3/5] Gerando pares de chaves RSA para assinatura JWT (se ausentes) ==="
SECRETS_DIR="${ROOT_DIR}/backend/.secrets"
mkdir -p "${SECRETS_DIR}"

if [ ! -f "${SECRETS_DIR}/jwt-private.pem" ] || [ ! -f "${SECRETS_DIR}/jwt-public.pem" ]; then
    echo "Gerando novo par de chaves RSA 2048-bit em ${SECRETS_DIR}..."
    openssl genpkey -algorithm RSA -out "${SECRETS_DIR}/jwt-private.pem" -pkeyopt rsa_keygen_bits:2048
    openssl rsa -pubout -in "${SECRETS_DIR}/jwt-private.pem" -out "${SECRETS_DIR}/jwt-public.pem"
    chmod 600 "${SECRETS_DIR}/jwt-private.pem"
    chmod 644 "${SECRETS_DIR}/jwt-public.pem"
    echo "Chaves RSA criadas com sucesso."
else
    echo "Chaves RSA já presentes em ${SECRETS_DIR}."
fi

echo "=== [4/5] Criando diretórios de mídia e logs ==="
mkdir -p "${ROOT_DIR}/backend/media/pictures"
mkdir -p "${ROOT_DIR}/backend/logs"
mkdir -p "${ROOT_DIR}/backups"

echo "=== [5/5] Instruções de Execução ==="
if command -v docker &> /dev/null && docker compose version &> /dev/null; then
    echo "Docker detectado. Para iniciar todo o ecossistema com 1 comando, execute:"
    echo "  docker compose up --build -d"
    echo ""
    echo "Para acompanhar os logs:"
    echo "  docker compose logs -f api"
else
    echo "Ambiente local configurado com sucesso!"
    echo "Para rodar a API localmente via Maven:"
    echo "  cd backend && ./mvnw spring-boot:run"
    echo "Para rodar o Frontend via Flutter:"
    echo "  cd ../projeto-front && flutter run -d chrome"
fi

echo "=========================================================="
echo "Serviços disponíveis:"
echo " - API REST:        http://localhost:8080"
echo " - Swagger / OpenAPI: http://localhost:8080/swagger-ui.html"
echo " - Healthcheck:     http://localhost:8080/actuator/health"
echo " - Frontend Web:    http://localhost:5500"
echo " - PostgreSQL:      localhost:5433 (database: gestao_condominial_db)"
echo "=========================================================="
