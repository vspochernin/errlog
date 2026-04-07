#!/bin/bash

echo "Stop demo"
docker compose -f docker/docker-compose.demo.yml down --remove-orphans

echo "Stop core"
docker compose -f docker/docker-compose.core.yml down --remove-orphans

echo "Start core"
docker compose -f docker/docker-compose.core.yml up -d --build

echo "Start demo"
docker compose -f docker/docker-compose.demo.yml up -d --build
