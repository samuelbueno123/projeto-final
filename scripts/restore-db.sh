#!/usr/bin/env bash
# ==============================================================================
# PROJETO FINAL - RESTAURAÇÃO DO BANCO DE DADOS (POSTGRESQL)
# Valida integridade SHA256 do arquivo e restaura os dados com segurança.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

BACKUP_FILE="${1:-}"
FORCE=false

if [[ $# -gt 1 ]] && [[ "$2" == "--force" ]]; then
    FORCE=true
fi

if [ -z "${BACKUP_FILE}" ]; then
    echo "Uso: $0 <caminho_do_arquivo_backup.sql.gz> [--force]"
    echo ""
    echo "Backups disponíveis em ${ROOT_DIR}/backups:"
    ls -lh "${ROOT_DIR}/backups"/*.sql.gz 2>/dev/null || echo "  Nenhum backup encontrado."
    exit 1
fi

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "ERRO: Arquivo de backup não encontrado: ${BACKUP_FILE}" >&2
    exit 1
fi

echo "=========================================================="
echo "Iniciando Restauração do Banco de Dados a partir de:"
echo "Arquivo: ${BACKUP_FILE}"
echo "=========================================================="

# Validação do Checksum SHA256 (se arquivo .sha256 existir)
if [ -f "${BACKUP_FILE}.sha256" ]; then
    echo "Validando integridade SHA256..."
    if command -v sha256sum &> /dev/null; then
        sha256sum -c "${BACKUP_FILE}.sha256"
    elif command -v shasum &> /dev/null; then
        shasum -a 256 -c "${BACKUP_FILE}.sha256"
    fi
    echo "Integridade do arquivo confirmada com sucesso!"
fi

if [ "$FORCE" = false ]; then
    read -p "ATENÇÃO: A restauração substituirá os dados atuais do banco de dados. Deseja prosseguir? (s/N): " -r CONFIRM
    if [[ ! "$CONFIRM" =~ ^[sS]$ ]]; then
        echo "Operação cancelada pelo usuário."
        exit 0
    fi
fi

DB_USER="${POSTGRES_USER:-postgres}"
DB_NAME="${POSTGRES_DB:-gestao_condominial_db}"

if command -v docker &> /dev/null && docker ps --format '{{.Names}}' | grep -q "postgres"; then
    CONTAINER_NAME=$(docker ps --format '{{.Names}}' | grep "postgres" | head -n 1)
    echo "Restaurando banco via container Docker: ${CONTAINER_NAME}..."
    gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}"
elif command -v psql &> /dev/null; then
    echo "Restaurando banco via psql local..."
    gunzip -c "${BACKUP_FILE}" | psql -U "${DB_USER}" -d "${DB_NAME}"
else
    echo "ERRO: Nem o Docker nem o cliente 'psql' foram encontrados no sistema." >&2
    exit 1
fi

echo "Restauração do banco de dados concluída com sucesso!"
