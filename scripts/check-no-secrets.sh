#!/usr/bin/env bash
set -euo pipefail

echo "Running secret guard scan on tracked files..."

# Keep this focused on high-signal patterns to reduce false positives.
PATTERN='(AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z\-_]{35}|ghp_[0-9A-Za-z]{36}|github_pat_[0-9A-Za-z_]{82,}|xox[baprs]-[0-9A-Za-z-]{10,}|-----BEGIN (RSA|EC|OPENSSH|DSA|PGP) PRIVATE KEY-----)'

if git grep -nEI "${PATTERN}" -- . \
  ':(exclude)*.md' \
  ':(exclude)*.mdx' \
  ':(exclude)*.lock' \
  ':(exclude)*package-lock.json' \
  ':(exclude)*pnpm-lock.yaml' \
  ':(exclude)*yarn.lock' \
  ':(exclude)api-server/.env.example' \
  ':(exclude)storefront/.env.example'; then
  echo "Potential leaked secrets found. Please remove or rotate them."
  exit 1
fi

echo "Secret guard passed."
