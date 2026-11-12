# Thiết lập Oracle

Compose local tạo `bank_owner` và `bank_app` trong FREEPDB1. Migration V1 tạo bảng/sequence; V2 cấp quyền cụ thể cho bank_app. Trên Oracle có sẵn, DBA cần tạo hai user trước; dùng tên BANK_OWNER/BANK_APP vì migration grants được viết rõ cho BANK_APP.

```sql
-- Chạy bằng DBA trong đúng PDB; thay placeholder bằng mật khẩu của bạn.
CREATE USER bank_owner IDENTIFIED BY "OWNER_PASSWORD" QUOTA UNLIMITED ON users;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO bank_owner;
CREATE USER bank_app IDENTIFIED BY "APP_PASSWORD";
GRANT CREATE SESSION TO bank_app;
```

Không chạy nguyên placeholder trong môi trường dùng thật. Cấu hình DB_URL, DB_USER=bank_app, DB_SCHEMA=BANK_OWNER, DB_MIGRATION_USER=bank_owner và hai password env. Hibernate dùng `ddl-auto=validate`, không tự tạo/xóa schema. Flyway dùng connection owner riêng; pool runtime thực hiện `ALTER SESSION SET CURRENT_SCHEMA=BANK_OWNER` để native lock queries trỏ đúng bảng.

V2 chỉ cho SELECT/INSERT trên sổ giao dịch/audit/idempotency/opening; bank_account có thêm UPDATE. APP_USER và CUSTOMER có INSERT để profile demo tạo dữ liệu và API staff tạo khách hàng. Runtime không có DELETE hoặc DDL. Không dùng user SYS/SYSTEM cho API.

Ứng dụng này kiểm chứng trên Oracle Free 23.9. Phiên bản Oracle khác cần chạy lại IT; không suy luận H2 thay thế tương đương. Flyway BOM hiện có cảnh báo phiên bản Oracle 23.9 mới hơn mốc đã kiểm chứng của module; migrations được thử thật trong dự án và cảnh báo được ghi trong verification.

Nếu đổi image/volume/schema sau khi Flyway đã chạy, không sửa migration đã áp dụng; thêm migration mới. Không tự xóa volume để xử lý lỗi. Database local bind cổng 1521 vào loopback.
