#!/bin/bash

echo "Stop core"
docker compose -f docker-compose.core.yml down -v --remove-orphans
echo "Start core"
docker compose -f docker-compose.core.yml up -d --build
