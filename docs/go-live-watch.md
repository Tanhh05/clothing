# Go-live + 72h Watch

Last updated: 2026-05-16

## T0 (ngay sau go-live)

- Xác nhận API smoke pass.
- Xác nhận storefront smoke pass.
- Xác nhận admin smoke pass.
- Thực hiện 1 đơn hàng thật end-to-end (login -> cart -> checkout -> order detail).

## T+1h

- Kiểm tra:
  - Tỷ lệ lỗi 4xx/5xx
  - Tốc độ phản hồi `/api/products`, `/api/orders/my`
  - Tình trạng payment callback (MoMo/VNPay)

## T+24h

- Đối soát đơn:
  - số đơn tạo mới
  - số đơn huỷ
  - số đơn thanh toán thành công/thất bại
- Kiểm tra log bất thường về auth/rate-limit.

## T+72h

- Tổng kết health:
  - uptime
  - lỗi nghiêm trọng
  - ticket phát sinh từ người dùng
- Quyết định đóng giai đoạn go-live hoặc giữ chế độ giám sát cao.
