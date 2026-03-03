#!/bin/bash

echo "Stop demo"
docker compose -f docker-compose.demo.yml down --remove-orphans

echo "Stop core"
docker compose -f docker-compose.core.yml down --remove-orphans

echo "Stop connected stand"
docker compose -f docker-compose.yml down --remove-orphans
