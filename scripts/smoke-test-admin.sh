#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:9528}"

echo "Smoke testing admin portal at ${BASE_URL}"

check_status_200() {
  local path="$1"
  local status
  status="$(curl -sS -o /dev/null -w "%{http_code}" "${BASE_URL}${path}" || true)"
  [[ "$status" == "200" ]]
}

if ! check_status_200 "/"; then
  echo "Admin root endpoint is not healthy."
  exit 1
fi

if ! check_status_200 "/#/login"; then
  echo "Admin login route is not healthy."
  exit 1
fi

echo "Admin portal smoke test passed."
