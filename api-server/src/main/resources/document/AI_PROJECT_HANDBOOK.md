# Clothing Project - AI Handover Handbook

Tài liệu này dùng để bàn giao cho AI/Dev khác: đọc 1 file là hiểu cấu trúc dự án, cách chạy, cách test, và các điểm cần lưu ý khi tiếp tục phát triển.

## 1) Tổng quan kỹ thuật

- Monorepo gồm:
  - `backend`: Spring Boot 3 (Java 17), JPA/Hibernate, PostgreSQL, Redis, RabbitMQ, Elasticsearch
  - `frontend`: Vue 3 + Vite + Pinia + Vue Router + Element Plus
- Timezone dự án đang dùng: `Asia/Ho_Chi_Minh`
- Backend chạy qua Docker Compose là luồng chuẩn.

## 2) Cấu trúc thư mục chính

```text
clothing/
├─ api-server/
│  ├─ src/main/java/com/clothing/
│  │  ├─ controller/
│  │  ├─ service/ + service/impl/
│  │  ├─ repository/
│  │  ├─ entity/
│  │  ├─ dto/request + dto/response
│  │  └─ config/ security/
│  ├─ src/main/resources/application.yml
│  ├─ docker-compose.yml
│  ├─ Dockerfile
│  └─ .env
├─ frontend/
│  ├─ src/
│  │  ├─ modules/ (auth, product, order, banner, voucher, warehouse, returns, notification, settings...)
│  │  ├─ components/common/ (AppHeader, AppFooter...)
│  │  ├─ layouts/ (ClientLayout, AdminLayout, AuthLayout)
│  │  ├─ router/
│  │  ├─ store/
│  │  └─ plugins/services (axios, api)
│  └─ package.json
└─ document/
   ├─ clothing.sql
   ├─ clothing_seed.sql
   └─ AI_PROJECT_HANDBOOK.md
```

## 3) Cách chạy dự án

### 3.1 Backend (chuẩn)

```bash
cd backend
docker compose up -d
```

Build lại backend sau khi sửa code:

```bash
cd backend
docker compose build be
docker compose up -d --force-recreate be
```

Xem log backend:

```bash
cd backend
docker compose logs -f be
```

### 3.2 Frontend

```bash
cd frontend
npm install
npm run dev
```

Build check:

```bash
cd frontend
npm run build
```

## 4) Ports mặc định

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api`
- Postgres: `5432`
- Redis: `6379`
- RabbitMQ: `5672`
- Elasticsearch: `9200`

## 5) Auth + token (frontend)

- Axios interceptor: `frontend/src/plugins/axios.js`
- Token lưu ở:
  - key cũ: `auth`
  - key mới: `clothing_auth`
- Có tách token theo ngữ cảnh:
  - `clientToken` cho client route
  - `adminToken` cho admin route

## 6) Route frontend chính

- Client:
  - `/`
  - `/products`
  - `/products/:slug`
  - `/cart`
  - `/orders` (requires auth)
  - `/wishlist` (requires auth)
- Admin:
  - `/admin/login`
  - `/admin` dashboard
  - `/admin/products`
  - `/admin/categories`
  - `/admin/banners`
  - `/admin/customers`
  - `/admin/orders`
  - `/admin/vouchers`
  - `/admin/warehouse-inbound`
  - `/admin/returns`
  - `/admin/notifications`
  - `/admin/settings`

## 7) API quan trọng đã có

### 7.1 Settings

- Public:
  - `GET /api/store-settings` (client đọc cấu hình cửa hàng)
- Admin:
  - `GET /api/admin/store-settings`
  - `PUT /api/admin/store-settings`

### 7.2 Notifications

- Admin:
  - `GET /api/admin/notifications`
  - `POST /api/admin/notifications` (NOW/SCHEDULED)
- Client:
  - `GET /api/user/notifications`
  - `GET /api/user/notifications/unread-count`
  - `PATCH /api/user/notifications/{id}/read`
  - `PATCH /api/user/notifications/read-all`

### 7.3 5 màn admin mới

- Voucher: `/api/admin/vouchers` (CRUD)
- Warehouse inbound: `/api/admin/warehouse-inbounds` (list/create, create có cộng tồn kho theo SKU + ghi inventory log)
- Returns: `/api/admin/returns`, `/api/admin/returns/{id}/status`
- Notifications: như trên
- Settings: như trên

### 7.4 Orders / Products (đã tùy chỉnh nhiều)

- Orders admin có server pagination/filter, bulk status update
- Logic status order đã chặn update ngược trái luồng
- Product delete là soft delete + restore + bulk action

## 8) Dữ liệu config cửa hàng đã áp dụng lên client

- `ClientLayout` gọi fetch settings public khi mount.
- `AppFooter` dùng dữ liệu thật từ API (storeName/hotline/email/address/shippingPolicy/returnPolicy).
- `CartPage` dùng dữ liệu thật:
  - `defaultShippingFee`
  - `freeShippingThreshold`
  - bật/tắt payment method `COD` / `MOMO` theo settings.

## 9) Cách test nhanh (smoke test)

### 9.1 API public

```bash
curl http://localhost:8080/api/store-settings
```

Kỳ vọng: HTTP 200 + JSON settings.

### 9.2 API cần auth

Ví dụ không token:

```bash
curl -i http://localhost:8080/api/admin/vouchers
```

Kỳ vọng: 401 Unauthorized.

### 9.3 Smoke test API đổi trả (có token thật)

```bash
cd backend
./scripts/test_returns_api.sh
```

Kỳ vọng:
- `GET /api/returns/my` trả `200`
- `POST /api/returns` trả `201`
- POST trùng đơn trả `409` (chặn yêu cầu active trùng)

### 9.4 UI flow test

1. Login admin.
2. Vào `/admin/settings`, đổi hotline/address/policy, bấm lưu.
3. Mở client `/`, kiểm tra footer đổi theo cấu hình mới.
4. Vào cart, kiểm tra phí ship + payment methods theo settings.
5. Vào `/admin/notifications`, gửi thông báo `NOW`.
6. Login user client, bấm icon chuông ở header, kiểm tra có thông báo và unread badge.

## 10) Quy ước phát triển cho AI/dev tiếp theo

- Không dùng lệnh phá dữ liệu (`git reset --hard`, `git checkout --`) nếu chưa được yêu cầu.
- Ưu tiên sửa theo pattern sẵn có: `controller -> service -> repository -> dto`.
- Thêm API mới phải map rõ:
  - route security
  - request/response DTO
  - validation + business rule
- Frontend:
  - tách API file riêng theo module
  - tránh hardcode dữ liệu nếu đã có API/settings
  - khi gọi admin/client API dùng đúng token context.

## 11) Lưu ý môi trường

- Ở máy này thường dùng Docker để compile backend (không dựa vào local Maven).
- Khi backend vừa recreate có thể cần đợi vài giây để app warm-up rồi mới curl test.
- Có dữ liệu seed SQL trong `document/clothing_seed.sql`.

## 12) Checklist trước khi bàn giao

1. `docker compose build be` thành công.
2. `docker compose up -d --force-recreate be` thành công.
3. `npm run build` frontend thành công.
4. Test ít nhất 1 API public + 1 API protected (401 khi thiếu token).
5. Test 1 luồng admin -> client (ví dụ settings hoặc notification).

## 13) Secret guard

- Script check secrets: `scripts/check-no-secrets.sh`
- CI đã có workflow chạy script này ở mỗi push/PR.
- Để bật local git hook:

```bash
git config core.hooksPath .githooks
```

## 14) CI/CD workflows

- Backend CI: `.github/workflows/backend-ci.yml`
  - Job `build_and_test`: `mvn clean test`
  - Job `quality_gate`: secret guard + `mvn -DskipTests verify` + `docker build`
- Deploy staging: `.github/workflows/deploy-staging.yml`
  - Trigger tự động sau khi `Backend CI` thành công trên branch `main`
  - Có thể chạy tay qua `workflow_dispatch` và truyền `git_sha`
  - Deploy qua SSH, sau đó chạy smoke test bằng `scripts/smoke-test-api.sh`

### Secrets cần cấu hình cho deploy staging

- `STAGING_SSH_HOST`
- `STAGING_SSH_PORT`
- `STAGING_SSH_USER`
- `STAGING_SSH_PRIVATE_KEY`
- `STAGING_APP_DIR` (thư mục monorepo trên server staging)
- `STAGING_API_BASE_URL` (ví dụ `https://staging-api.example.com/api`)
