#!/bin/bash

echo "Stop demo"
docker compose -f docker-compose.demo.yml down --remove-orphans

echo "Start demo"
docker compose -f docker-compose.demo.yml up -d --build
