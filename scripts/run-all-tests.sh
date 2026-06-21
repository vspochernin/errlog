#!/bin/bash
set -e

# Установка JDK 21 (обязательно для проекта)
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "")
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JDK 21 not found. Install it and try again."
    exit 1
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
echo "=========================================="
echo " Running all errlog tests"
echo " Root: $ROOT"
echo " JDK:  $JAVA_HOME"
echo "=========================================="

# Запуск Maven: тихий режим при успехе, но при падении — повтор с полным выводом,
# чтобы было видно какой тест упал и почему.
run_maven() {
    local module="$1"
    local goal="$2"
    local label="$3"

    echo "--- $label ---"
    cd "$ROOT/$module"
    if ./mvnw -q "$goal"; then
        echo "  OK"
    else
        echo "  FAILED — повтор с подробным выводом:"
        ./mvnw "$goal" || true
        echo ""
        echo "=========================================="
        echo " FAILED: $label"
        echo "=========================================="
        exit 1
    fi
}

# Фаза 1: юнит-тесты (быстро, без Docker)
echo ""
echo ">>> Phase 1: Unit tests (no Docker needed) <<<"
echo ""

run_maven jerrgen test "jerrgen"
run_maven ingestor test "ingestor"
run_maven errapi test "errapi"

# Фаза 2: интеграционные тесты (требуется Docker)
echo ""
echo ">>> Phase 2: Integration tests (Docker required) <<<"
echo ""

if ! docker info >/dev/null 2>&1; then
    echo "WARNING: Docker is not running. Skipping integration tests."
    echo ""
    echo "=========================================="
    echo " Unit tests: PASSED"
    echo " Integration tests: SKIPPED (Docker not available)"
    echo "=========================================="
    exit 0
fi

run_maven ingestor verify "ingestor (integration)"
run_maven errapi verify "errapi (integration)"

echo ""
echo "=========================================="
echo " All tests: PASSED"
echo "=========================================="
