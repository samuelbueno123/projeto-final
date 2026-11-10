#!/usr/bin/env bash
# ==============================================================================
# PROJETO FINAL - BACKUP DO BANCO DE DADOS (POSTGRESQL)
# Gera dumps compactados com carimbo de data/hora, checksums SHA256 e rotação.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

BACKUP_DIR="${ROOT_DIR}/backups"
RETENTION_DAYS=14
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/db_backup_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

DB_CONTAINER="${POSTGRES_CONTAINER:-projeto-final-postgres-1}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_NAME="${POSTGRES_DB:-gestao_condominial_db}"

echo "=========================================================="
echo "Iniciando Backup do Banco de Dados: ${DB_NAME}"
echo "Destino: ${BACKUP_FILE}"
echo "=========================================================="

# Verifica se o container ou o binário pg_dump está acessível
if command -v docker &> /dev/null && docker ps --format '{{.Names}}' | grep -q "postgres"; then
    CONTAINER_NAME=$(docker ps --format '{{.Names}}' | grep "postgres" | head -n 1)
    echo "Executando pg_dump via container Docker: ${CONTAINER_NAME}..."
    docker exec -t "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists | gzip > "${BACKUP_FILE}"
elif command -v pg_dump &> /dev/null; then
    echo "Executando pg_dump local..."
    pg_dump -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists | gzip > "${BACKUP_FILE}"
else
    echo "ERRO: Nem o Docker nem o cliente 'pg_dump' foram encontrados no sistema." >&2
    exit 1
fi

# Gera arquivo de checksum SHA256 para garantia de integridade criptográfica
if command -v sha256sum &> /dev/null; then
    sha256sum "${BACKUP_FILE}" > "${BACKUP_FILE}.sha256"
elif command -v shasum &> /dev/null; then
    shasum -a 256 "${BACKUP_FILE}" > "${BACKUP_FILE}.sha256"
fi

FILE_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
echo "Backup gerado com sucesso! Tamanho: ${FILE_SIZE}"

# Rotação de backups antigos (política de retenção de RETENTION_DAYS dias)
echo "Aplicando política de retenção (removendo backups com mais de ${RETENTION_DAYS} dias)..."
find "${BACKUP_DIR}" -name "db_backup_*.sql.gz*" -type f -mtime +"${RETENTION_DAYS}" -delete || true

echo "Backup finalizado com êxito."
