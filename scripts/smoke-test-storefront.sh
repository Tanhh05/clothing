#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:3000}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-120}"
RETRY_INTERVAL_SECONDS="${RETRY_INTERVAL_SECONDS:-5}"

echo "Smoke testing storefront at ${BASE_URL}"

check_endpoint() {
  local path="$1"
  local expected="$2"
  local status body
  body="$(mktemp)"
  status="$(curl -sS -o "$body" -w "%{http_code}" "${BASE_URL}${path}" || true)"
  if [[ "$status" != "200" ]]; then
    rm -f "$body"
    return 1
  fi
  if ! grep -qi "$expected" "$body"; then
    rm -f "$body"
    return 1
  fi
  rm -f "$body"
  return 0
}

wait_until_ok() {
  local started_at now elapsed
  started_at="$(date +%s)"
  while true; do
    if check_endpoint "/" "haru"; then
      echo "Storefront home is healthy."
      return 0
    fi
    now="$(date +%s)"
    elapsed=$((now - started_at))
    if (( elapsed >= TIMEOUT_SECONDS )); then
      echo "Timed out waiting for storefront home."
      return 1
    fi
    sleep "$RETRY_INTERVAL_SECONDS"
  done
}

wait_until_ok
check_endpoint "/shopping-cart" "haru"
check_endpoint "/orders" "haru"
echo "Storefront smoke test passed."
