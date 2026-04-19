#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if ! command -v git >/dev/null 2>&1; then
  echo "git is required"
  exit 1
fi

echo "Running secret guard..."

# Detect accidental secrets committed in tracked files.
# Allow placeholders from *.env.example.
matches="$(
  git grep -nE '^(JWT_SECRET|R2_ACCESS_KEY|R2_SECRET_KEY|GHN_TOKEN|MOMO_ACCESS_KEY|MOMO_SECRET_KEY)=' -- \
    ':!**/.env.example' \
    ':!**/.env' || true
)"

if [[ -n "${matches}" ]]; then
  echo "Secret-like keys detected in tracked files:"
  echo "${matches}"
  echo "Move secret values to untracked .env and keep only placeholders in tracked files."
  exit 1
fi

hardcoded_tokens="$(
  git grep -nE '(AKIA[0-9A-Z]{16}|-----BEGIN (RSA|EC|OPENSSH) PRIVATE KEY-----|xox[baprs]-[A-Za-z0-9-]{10,})' -- . || true
)"

if [[ -n "${hardcoded_tokens}" ]]; then
  echo "Potential hardcoded token/private key detected:"
  echo "${hardcoded_tokens}"
  exit 1
fi

echo "Secret guard passed."
