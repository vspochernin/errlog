#!/bin/bash

echo "Stop core"
docker compose -f docker/docker-compose.core.yml down --remove-orphans

echo "Start core"
docker compose -f docker/docker-compose.core.yml up -d --build
