#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-120}"
RETRY_INTERVAL_SECONDS="${RETRY_INTERVAL_SECONDS:-5}"

if [[ -z "${BASE_URL}" ]]; then
  echo "BASE_URL is required"
  exit 1
fi

deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))

check_endpoint() {
  local url="$1"
  local expected="${2:-200}"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url" || true)
  [[ "$status" == "$expected" ]]
}

echo "[smoke] Waiting for API at ${BASE_URL} (timeout=${TIMEOUT_SECONDS}s)"
while true; do
  if check_endpoint "${BASE_URL}/api/products?page=0&size=1" "200"; then
    echo "[smoke] Product endpoint is up"
    break
  fi

  if (( $(date +%s) >= deadline )); then
    echo "[smoke] Timeout waiting for ${BASE_URL}/api/products"
    exit 1
  fi
  sleep "${RETRY_INTERVAL_SECONDS}"
done

echo "[smoke] Checking category listing endpoint"
if ! check_endpoint "${BASE_URL}/api/categories?page=0&size=1" "200"; then
  echo "[smoke] /api/categories check failed"
  exit 1
fi

echo "[smoke] API smoke test passed"
