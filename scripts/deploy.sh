#!/bin/bash
# Скрипт деплоя для CI/CD

set -e

cd /opt/multimodule-project

echo "========================================="
echo "🚀 Starting deployment at $(date)"
echo "========================================="

# 1. Сохраняем .env если он существует
echo "💾 Backing up .env..."
if [ -f .env ]; then
    cp .env /tmp/.env.backup
fi

# 2. Обновляем код из git
echo "🔄 Updating code from git..."
git fetch origin
git reset --hard origin/main

# 3. Восстанавливаем .env
if [ -f /tmp/.env.backup ]; then
    echo "📁 Restoring .env..."
    cp /tmp/.env.backup .env
    rm /tmp/.env.backup
fi

# 4. Генерируем конфиги
echo "🔧 Generating configurations..."
./scripts/generate-config.sh

# 5. Проверяем SSL сертификаты
if [ ! -f nginx/ssl/nginx.crt ]; then
    echo "🔐 Generating SSL certificates..."
    mkdir -p nginx/ssl
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout nginx/ssl/nginx.key \
        -out nginx/ssl/nginx.crt \
        -subj "/CN=${DOMAIN:-localhost}"
fi

# 6. Запускаем сервисы
echo "🐳 Starting Docker services..."
docker compose -f docker-compose.yml up -d
docker compose -f docker-compose.apps.yml up -d
docker compose -f docker-compose.prod.yml up -d nginx

# 7. Healthcheck
echo "🏥 Running healthcheck..."
sleep 10
if curl -k -f "https://${DOMAIN:-localhost}/health" > /dev/null 2>&1; then
    echo "✅ Healthcheck passed"
else
    echo "⚠️ Healthcheck failed, but services may still work"
fi

echo "========================================="
echo "✅ Deployment completed at $(date)"
echo "========================================="
