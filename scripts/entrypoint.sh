#!/bin/sh
# Общий entrypoint для всех сервисов

set -e  # exit on error

# Определяем имя сервиса из переменной окружения или из пути
if [ -n "$SERVICE_NAME" ]; then
    SERVICE="$SERVICE_NAME"
else
    # Пробуем определить по пути, где находится JAR
    SERVICE=$(basename "$(dirname "$(find /app -name "*.jar" -type f 2>/dev/null | head -1)")" 2>/dev/null || echo "unknown")
fi

echo "🚀 Инициализация контейнера ${SERVICE}..."
echo "Пользователь: $(whoami)"
echo "UID: $(id -u)"

# Создаем папку для логов (сейчас мы root)
mkdir -p /app/logs
chmod 755 /app/logs

# Меняем владельца папки на appuser
chown -R appuser:appgroup /app/logs

echo "✅ Папка /app/logs готова:"
ls -la /app/logs

# Запускаем приложение
echo "🚀 Запуск приложения от пользователя appuser..."
exec su-exec appuser:appgroup java \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -jar /app/app.jar