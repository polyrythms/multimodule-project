#!/bin/bash
# Скрипт настройки SSL сертификатов (запускается вручную)

set -e

cd /opt/multimodule-project

# Загружаем .env
if [ -f .env ]; then
    source .env
fi

DOMAIN=${DOMAIN:-localhost}
SSL_DIR="nginx/ssl"

mkdir -p "$SSL_DIR"

# Проверяем, нужно ли обновить сертификаты
if [ -f "$SSL_DIR/nginx.crt" ]; then
    EXPIRY=$(openssl x509 -enddate -noout -in "$SSL_DIR/nginx.crt" | cut -d= -f2)
    echo "📅 Current certificate expires: $EXPIRY"
fi

# Пытаемся получить Let's Encrypt сертификат
if command -v certbot &> /dev/null && [ "$DOMAIN" != "localhost" ]; then
    echo "🔐 Getting Let's Encrypt certificate for $DOMAIN..."
    docker compose -f docker-compose.prod.yml stop nginx 2>/dev/null || true
    sudo certbot certonly --standalone -d "$DOMAIN" --non-interactive --agree-tos --email admin@"$DOMAIN"
    sudo cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" "$SSL_DIR/nginx.crt"
    sudo cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" "$SSL_DIR/nginx.key"
    sudo chown -R $USER:$USER "$SSL_DIR"
else
    echo "🔐 Generating self-signed certificate for $DOMAIN..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$SSL_DIR/nginx.key" \
        -out "$SSL_DIR/nginx.crt" \
        -subj "/CN=$DOMAIN"
fi

chmod 600 "$SSL_DIR/nginx.key"
chmod 644 "$SSL_DIR/nginx.crt"

echo "✅ SSL certificates ready"
