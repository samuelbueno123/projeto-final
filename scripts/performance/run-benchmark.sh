#!/usr/bin/env bash
# ==============================================================================
# PROJETO FINAL - EXECUTOR DE BENCHMARK E TESTES DE CARGA
# Executa testes automatizados com Locust e compila métricas de percentis e RPS.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_URL="${1:-http://localhost:8080}"
USERS="${2:-30}"
SPAWN_RATE="${3:-5}"
DURATION="${4:-20s}"

REPORT_DIR="${SCRIPT_DIR}/reports"
mkdir -p "${REPORT_DIR}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_PREFIX="${REPORT_DIR}/benchmark_${TIMESTAMP}"

LOCUST_BIN="/tmp/bench-venv/bin/locust"
if [ ! -f "$LOCUST_BIN" ]; then
    echo "Instalando Locust no ambiente isolado..."
    uv venv /tmp/bench-venv
    uv pip install --python /tmp/bench-venv locust
fi

echo "=========================================================="
echo "Iniciando Teste de Desempenho / Carga"
echo "Alvo: ${TARGET_URL}"
echo "Usuários Concorrentes: ${USERS}"
echo "Duração: ${DURATION}"
echo "Relatórios: ${REPORT_PREFIX}_*.csv"
echo "=========================================================="

"$LOCUST_BIN" -f "${SCRIPT_DIR}/locustfile.py" \
    --host="${TARGET_URL}" \
    --headless \
    --users="${USERS}" \
    --spawn-rate="${SPAWN_RATE}" \
    --run-time="${DURATION}" \
    --csv="${REPORT_PREFIX}" \
    --only-summary

echo "=========================================================="
echo "Resumo dos Resultados Gerados:"
if [ -f "${REPORT_PREFIX}_stats.csv" ]; then
    cat "${REPORT_PREFIX}_stats.csv"
fi
echo "=========================================================="
