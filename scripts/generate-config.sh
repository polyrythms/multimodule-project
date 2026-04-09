#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Загружаем переменные из .env
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# Устанавливаем DOMAIN по умолчанию, если не задан
DOMAIN=${DOMAIN:-_}

echo "🔧 Generating nginx.conf with DOMAIN=${DOMAIN}..."

# Генерируем nginx.conf
export DOMAIN
envsubst '${DOMAIN}' < nginx/nginx.conf.template > nginx/nginx.conf

echo "✅ nginx.conf generated"

# Проверяем конфиг
if command -v docker &> /dev/null; then
    echo "🔍 Validating nginx configuration..."
    docker run --rm -v $(pwd)/nginx/nginx.conf:/etc/nginx/nginx.conf:ro nginx:alpine nginx -t
fi

echo "✅ Configuration generation completed"
