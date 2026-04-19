#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f ".env.local" ]]; then
  echo "Missing backend/.env.local"
  exit 1
fi

docker compose --env-file .env.local up -d --force-recreate be
