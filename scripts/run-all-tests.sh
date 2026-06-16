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

# Фаза 1: юнит-тесты (быстро, без Docker)
echo ""
echo ">>> Phase 1: Unit tests (no Docker needed) <<<"
echo ""

echo "--- jerrgen ---"
cd "$ROOT/jerrgen"
./mvnw -q test
echo "  OK"

echo "--- ingestor ---"
cd "$ROOT/ingestor"
./mvnw -q test
echo "  OK"

echo "--- errapi ---"
cd "$ROOT/errapi"
./mvnw -q test
echo "  OK"

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

echo "--- ingestor (integration) ---"
cd "$ROOT/ingestor"
./mvnw -q verify
echo "  OK"

echo "--- errapi (integration) ---"
cd "$ROOT/errapi"
./mvnw -q verify
echo "  OK"

echo ""
echo "=========================================="
echo " All tests: PASSED"
echo "=========================================="
