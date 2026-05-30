# Rollback Runbook

Last updated: 2026-05-16

## Trigger conditions

- API error rate tăng đột biến sau deploy.
- Checkout/payment thất bại tăng bất thường.
- Login, add-to-cart, create-order không hoạt động ổn định.

## Immediate actions (0-10 phút)

1. Tạm dừng deploy mới.
2. Xác định commit đang chạy và commit ổn định gần nhất.
3. Kích hoạt production deploy workflow với `git_sha` = commit ổn định.

## Rollback backend

1. Vào GitHub Actions `Deploy Production`.
2. Chạy lại bằng `workflow_dispatch` với SHA cũ.
3. Đợi 3 smoke jobs pass:
   - `Smoke Test Production API`
   - `Smoke Test Production Storefront`
   - `Smoke Test Production Admin Portal`

## Verification after rollback

- `/api/products?page=0&size=1` trả `200`.
- Trang chủ storefront mở bình thường.
- Admin login route mở bình thường.
- Tạo thử 1 đơn test (staging/prod-safe flow) thành công.

## Communication

- Ghi lại mốc thời gian sự cố, SHA lỗi, SHA rollback.
- Thông báo trạng thái cho team vận hành và business.

## Post-incident

1. Tạo issue RCA.
2. Đính kèm log, metrics, diff giữa SHA lỗi và SHA ổn định.
3. Bổ sung test/smoke để chặn tái diễn.
