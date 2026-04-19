#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-120}"
RETRY_INTERVAL_SECONDS="${RETRY_INTERVAL_SECONDS:-5}"

if [[ -z "${BASE_URL}" ]]; then
  echo "BASE_URL is required. Example: BASE_URL=https://staging.example.com/api"
  exit 1
fi

public_endpoint="${BASE_URL%/}/store-settings"
products_endpoint="${BASE_URL%/}/products?size=1"
protected_endpoint="${BASE_URL%/}/admin/vouchers"
auth_endpoint="${BASE_URL%/}/auth/login"

deadline=$((SECONDS + TIMEOUT_SECONDS))

wait_for_public_ready() {
  while (( SECONDS < deadline )); do
    status="$(curl -sS -o /tmp/smoke_public.json -w '%{http_code}' "${public_endpoint}" || true)"
    if [[ "${status}" == "200" ]]; then
      return 0
    fi
    sleep "${RETRY_INTERVAL_SECONDS}"
  done
  echo "Timeout waiting for public endpoint readiness: ${public_endpoint}"
  cat /tmp/smoke_public.json 2>/dev/null || true
  return 1
}

assert_status() {
  local endpoint="$1"
  local expected="$2"
  local out_file="$3"
  local status
  status="$(curl -sS -o "${out_file}" -w '%{http_code}' "${endpoint}" || true)"
  if [[ "${status}" != "${expected}" ]]; then
    echo "Unexpected status for ${endpoint}. Expected ${expected}, got ${status}"
    cat "${out_file}" 2>/dev/null || true
    exit 1
  fi
}

assert_status_one_of() {
  local endpoint="$1"
  local expected_a="$2"
  local expected_b="$3"
  local out_file="$4"
  local status
  status="$(curl -sS -o "${out_file}" -w '%{http_code}' "${endpoint}" || true)"
  if [[ "${status}" != "${expected_a}" && "${status}" != "${expected_b}" ]]; then
    echo "Unexpected status for ${endpoint}. Expected ${expected_a} or ${expected_b}, got ${status}"
    cat "${out_file}" 2>/dev/null || true
    exit 1
  fi
}

echo "[1/4] Waiting for API ready: ${public_endpoint}"
wait_for_public_ready

echo "[2/4] Public settings endpoint"
assert_status "${public_endpoint}" "200" "/tmp/smoke_settings.json"

echo "[3/4] Public products endpoint"
assert_status "${products_endpoint}" "200" "/tmp/smoke_products.json"

echo "[4/4] Protected endpoint must reject anonymous"
assert_status_one_of "${protected_endpoint}" "401" "403" "/tmp/smoke_protected.json"

auth_status="$(curl -sS -o /tmp/smoke_auth.json -w '%{http_code}' \
  -X POST "${auth_endpoint}" \
  -H 'Content-Type: application/json' \
  -d '{}' || true)"
if [[ "${auth_status}" != "400" && "${auth_status}" != "401" ]]; then
  echo "Unexpected status for auth validation endpoint. Expected 400 or 401, got ${auth_status}"
  cat /tmp/smoke_auth.json 2>/dev/null || true
  exit 1
fi

echo "Smoke test passed for ${BASE_URL}"
