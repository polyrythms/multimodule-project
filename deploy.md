# Deploy Guide: Multimodule Project

## 📋 Содержание
- [Предварительные требования](#предварительные-требования)
- [Подготовка сервера](#подготовка-сервера)
- [Настройка GitHub Actions](#настройка-github-actions)
- [Переменные окружения](#переменные-окружения)
- [Процесс деплоя](#процесс-деплоя)
- [Мониторинг](#мониторинг)

---

## Предварительные требования

### Локальное окружение
- Java 21+
- Docker Desktop
- Git
- Maven 3.9+

### Сервер (VPS)
- Ubuntu 22.04/24.04 LTS
- Docker Engine 24.0+
- Docker Compose V2
- 4GB RAM минимум (рекомендуется 8GB)
- Открытые порты: 22 (SSH), 8081–8082 (приложения), 9000–9001 (MinIO), 9092 (Kafka)

---

## Подготовка сервера

### 1. Подключение к серверу

```bash
ssh user@your-server.com
```

### 2. Обновление системы

```bash
sudo apt update && sudo apt upgrade -y
```

### 3. Установка Docker и Docker Compose

```bash
# Установка Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Добавление пользователя в группу docker
sudo usermod -aG docker $USER

# Выход и повторный вход для применения прав
exit
ssh user@your-server.com

# Проверка установки
docker --version
docker compose version
```

### 4. Создание директории проекта

```bash
sudo mkdir -p /opt/multimodule-project
sudo chown -R $USER:$USER /opt/multimodule-project
cd /opt/multimodule-project
```

---

## Настройка GitHub Actions

### 1. Создание SSH ключа для деплоя

```bash
# На локальной машине
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github-actions

# Добавление публичного ключа на сервер
ssh-copy-id -i ~/.ssh/github-actions.pub user@your-server.com
```

### 2. Добавление секретов в GitHub

В репозитории: **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Описание |
|--------|----------|
| `SERVER_HOST` | IP-адрес или домен сервера |
| `SERVER_USER` | Имя пользователя для SSH |
| `SSH_PRIVATE_KEY` | Приватный SSH ключ (содержимое файла `~/.ssh/github-actions`) |

### 3. Файл workflow

В репозитории уже настроен `.github/workflows/deploy.yml`. При пуше в ветки `main` или `MM-refactor` автоматически запускается:

- Запуск тестов
- Сборка Docker образов
- Публикация в GHCR
- Деплой на сервер по SSH

---

## Переменные окружения

### Создание `.env` файла на сервере

Скопируйте `.env.example` из репозитория и заполните значения:

```bash
cp .env.example .env
nano .env
```

### Защита файла

```bash
chmod 600 /opt/multimodule-project/.env
```

---

## Процесс деплоя

### 1. Первоначальный деплой (на сервере)

```bash
cd /opt/multimodule-project

# Клонирование репозитория
git clone https://github.com/polyrythms/multimodule-project.git .

# Создание .env из примера
cp .env.example .env
nano .env  # заполнить значения

# Запуск инфраструктурных сервисов (MinIO, Kafka, Zookeeper)
docker compose -f docker-compose.yml up -d

# Проверка статуса
docker compose -f docker-compose.yml ps
```

### 2. Автоматический деплой через GitHub Actions

При пуше в ветку `main` или `MM-refactor`:

1. GitHub Actions запускает тесты
2. Собирает Docker образы `telegram-bot` и `audio-service`
3. Пушит образы в GHCR
4. Подключается к серверу по SSH
5. Скачивает новые образы
6. Перезапускает контейнеры приложений

### 3. Ручной деплой (при необходимости)

```bash
cd /opt/multimodule-project

# Обновление образов приложений
docker compose -f docker-compose.prod.yml pull

# Перезапуск приложений
docker compose -f docker-compose.prod.yml up -d --remove-orphans

# Проверка статуса всех контейнеров
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

---

## Мониторинг

### Базовые команды

```bash
# Просмотр всех контейнеров
docker ps -a

# Логи контейнера
docker logs telegram-bot --tail 100 -f
docker logs audio-service --tail 100 -f

# Потребление ресурсов
docker stats --no-stream

# Проверка health
curl http://localhost:8082/actuator/health
curl http://localhost:8081/actuator/health

# Проверка портов
netstat -tlnp | grep -E "8081|8082|9000|9092"
```

### Просмотр логов GitHub Actions

1. Перейти в репозиторий на GitHub
2. Нажать вкладку **Actions**
3. Выбрать workflow **Build and Deploy**
4. Нажать на конкретный запуск
5. Просмотреть логи каждой job


## Безопасность и сетевая архитектура

### Новая сетевая архитектура

После внедрения Nginx как единой точки входа:

**Внешние порты (открыты на хосте):**
- `80` (HTTP) - перенаправляет на HTTPS
- `443` (HTTPS) - единственная точка входа

**Внутренние сервисы (только внутри Docker сети):**
- telegram-bot:8080 (приложение), 8081 (метрики)
- audio-service:8080 (приложение), 8081 (метрики)
- minio:9000 (API), 9001 (console)
- kafka:29092
- prometheus:9090
- grafana:3000

### Доступ к сервисам

**Извне (через Nginx):**
- Web App: `https://polyrythms.ru/app/`
- Bot API: `https://polyrythms.ru/api/bot/`
- Metrics (ограничен по IP): `https://polyrythms.ru/metrics`

**Изнутри (между сервисами):**
- Используйте имена сервисов в Docker сети:
    - `http://telegram-bot:8080`
    - `http://audio-service:8080`
    - `http://minio:9000`
    - `kafka:29092`

### Проверка безопасности

```bash
# Проверить открытые порты (должны быть только 80 и 443)
sudo ss -tlnp | grep -E ":(80|443)"

# Проверить, что внутренние порты не доступны извне
curl -I http://your-server.com:8082  # Должен быть timeout/refused
curl -I https://your-server.com:9092  # Должен быть timeout/refused

# Проверить доступ через Nginx
curl -I https://your-server.com/health
