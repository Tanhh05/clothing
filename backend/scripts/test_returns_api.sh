#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
DB_SERVICE="${DB_SERVICE:-postgres}"
DB_USER="${DB_USER:-clothing}"
DB_NAME="${DB_NAME:-clothing}"

suffix="$(date +%s)"
username="apitest_returns_${suffix}"
email="${username}@example.com"
password="Test@123456"
phone="090$(printf "%07d" "$((suffix % 10000000))")"

echo "[1/5] Register test user: ${username}"
register_response="$(curl -sS -X POST "${BASE_URL}/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${username}\",\"email\":\"${email}\",\"password\":\"${password}\",\"fullName\":\"API Returns Test\",\"phone\":\"${phone}\"}")"

access_token="$(printf "%s" "${register_response}" | python3 -c 'import json,sys;print(json.load(sys.stdin)["accessToken"])')"
user_id="$(printf "%s" "${register_response}" | python3 -c 'import json,sys;print(json.load(sys.stdin)["userId"])')"

echo "[2/5] Create DELIVERED order for user_id=${user_id}"
order_id="$(docker compose exec -T "${DB_SERVICE}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c \
  "INSERT INTO orders (user_id,total_price,status,payment_method,address) VALUES (${user_id},199000,'DELIVERED','COD','API returns smoke test') RETURNING id;")"
order_id="$(printf "%s" "${order_id}" | sed -n '1p' | tr -d '[:space:]')"
variant_id="$(docker compose exec -T "${DB_SERVICE}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c \
  "SELECT id FROM product_variants ORDER BY id ASC LIMIT 1;")"
variant_id="$(printf "%s" "${variant_id}" | sed -n '1p' | tr -d '[:space:]')"
order_item_id="$(docker compose exec -T "${DB_SERVICE}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c \
  "INSERT INTO order_items (order_id, variant_id, quantity, price) VALUES (${order_id}, ${variant_id}, 1, 199000) RETURNING id;")"
order_item_id="$(printf "%s" "${order_item_id}" | sed -n '1p' | tr -d '[:space:]')"
docker compose exec -T "${DB_SERVICE}" psql -U "${DB_USER}" -d "${DB_NAME}" -t -A -c \
  "INSERT INTO order_status_history (order_id, status, changed_at) VALUES (${order_id}, 'DELIVERED', NOW());" >/dev/null

echo "[3/5] GET /returns/my (expect 200)"
get_status="$(curl -sS -o /tmp/returns_my_smoke.json -w '%{http_code}' \
  "${BASE_URL}/returns/my" \
  -H "Authorization: Bearer ${access_token}")"
if [[ "${get_status}" != "200" ]]; then
  echo "FAIL: GET /returns/my returned ${get_status}"
  cat /tmp/returns_my_smoke.json
  exit 1
fi

echo "[4/5] POST /returns (expect 201)"
post_status="$(curl -sS -o /tmp/returns_create_smoke.json -w '%{http_code}' \
  -X POST "${BASE_URL}/returns" \
  -H "Authorization: Bearer ${access_token}" \
  -H 'Content-Type: application/json' \
  -d "{\"orderId\":${order_id},\"returnType\":\"EXCHANGE\",\"reasonCode\":\"WRONG_SIZE\",\"reasonDetail\":\"Muốn đổi kích cỡ vì mặc bị rộng\",\"evidenceUrls\":\"https://img.example.com/1.jpg\",\"items\":[{\"orderItemId\":${order_item_id},\"quantity\":1}]}")"
if [[ "${post_status}" != "201" ]]; then
  echo "FAIL: POST /returns returned ${post_status}"
  cat /tmp/returns_create_smoke.json
  exit 1
fi

echo "[5/5] POST duplicate /returns (expect 409 conflict)"
duplicate_status="$(curl -sS -o /tmp/returns_duplicate_smoke.json -w '%{http_code}' \
  -X POST "${BASE_URL}/returns" \
  -H "Authorization: Bearer ${access_token}" \
  -H 'Content-Type: application/json' \
  -d "{\"orderId\":${order_id},\"returnType\":\"REFUND\",\"reasonCode\":\"OTHER\",\"reasonDetail\":\"Tạo lại yêu cầu đổi trả để test conflict\",\"items\":[{\"orderItemId\":${order_item_id},\"quantity\":1}]}")"
if [[ "${duplicate_status}" != "409" ]]; then
  echo "FAIL: duplicate POST /returns returned ${duplicate_status}"
  cat /tmp/returns_duplicate_smoke.json
  exit 1
fi

echo "PASS: returns API smoke test completed (order_id=${order_id})"
