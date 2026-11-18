#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
if [ ! -f .env ]; then
  printf '%s\n' 'Сначала скопируйте .env.example в .env и укажите подключение к PostgreSQL.'
  exit 1
fi
set -a
. ./.env
set +a
exec ./mvnw spring-boot:run
