#!/bin/bash
set -e

PROJECT_DIR="/opt/multimodule-project"

echo "Setting up server structure..."

# Создаем структуру папок
mkdir -p $PROJECT_DIR/{logs,data,configs/kafka,backup,scripts}

# Копируем docker-compose файлы (это нужно сделать вручную первый раз)
# scp docker-compose.yml docker-compose.prod.yml user@server:/opt/multimodule-project/

# Создаем сеть если не существует
docker network create app-network || true

# Устанавливаем права
chmod 755 $PROJECT_DIR/scripts
chmod 644 $PROJECT_DIR/*.yml

echo "Server structure created at $PROJECT_DIR"