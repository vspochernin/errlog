#!/bin/bash

echo "Stop demo"
docker compose -f docker-compose.demo.yml down -v --remove-orphans

echo "Start demo"
docker compose -f docker-compose.demo.yml up -d --build
