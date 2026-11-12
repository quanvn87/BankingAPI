# Mini Bank API — Spring Boot + Oracle

Dự án học tập bằng Java 21, Spring Boot 3.5.16 và Oracle Database Free 23.9. Bạn học cách một API nhận yêu cầu, xác thực, kiểm tra nghiệp vụ, cập nhật số dư và ghi lịch sử trong cùng transaction.

**ID tài khoản:** Java `UUID` v4, Oracle `RAW(16)`, JSON chuỗi UUID chuẩn. `accountNumber` là số tài khoản riêng. Các ID khách hàng, giao dịch, bút toán dùng Long/Oracle sequence.

Đây là mô hình học tập chuyển tiền nội bộ VND, chưa phải hệ thống đủ điều kiện vận hành ngân hàng. Không có phí, đổi ngoại tệ, tiền đang giữ, hạn mức, KYC/AML, maker-checker hoặc thanh toán liên ngân hàng. Số dư demo là số dư mở sổ giả lập, không phải tiền thật.

## 1. Chuẩn bị

- JDK 21 và Docker với Docker Compose. Maven tải qua wrapper, không cần cài riêng.
- Oracle container cần tài nguyên tương đối lớn; nên có ít nhất 4 GB RAM trống và khoảng 5–10 GB dung lượng cho image/data/build.
- Docker image `gvenzl/oracle-free` là image cộng đồng đóng gói Oracle Free, không phải image do Oracle phát hành; đã cố định tag/digest trong Compose. Điều kiện sử dụng Oracle vẫn theo giấy phép của Oracle.
- Nếu dùng JDK đã tải trong `work/tooling/` của phiên làm việc này, gọi `./scripts/mvn-local.sh` thay cho `./mvnw`. Thư mục `work/` là công cụ tạm, không phải thành phần bắt buộc của dự án.

## 2. Cấu hình local

```bash
cp .env.example .env
```

Mở `.env`, điền mật khẩu cho `ORACLE_PASSWORD`, `DB_MIGRATION_PASSWORD`, `DB_PASSWORD`, `DEMO_AN_PASSWORD`, `DEMO_BINH_PASSWORD`, `DEMO_STAFF_PASSWORD`.

Các mật khẩu database dành cho script local bắt đầu bằng chữ cái, dài 12–64 ký tự, chỉ chữ/số/dấu `_`. Mật khẩu demo dài 12–72 byte UTF-8; dùng ASCII để dễ nhập. Không sử dụng mật khẩu tài khoản thật. Không commit `.env`.

```bash
docker compose up -d --wait oracle
# Nạp các biến vào shell chạy Java. Chỉ source file .env của chính bạn.
set -a
source .env
set +a
./mvnw spring-boot:run
```

Nếu Java chưa có trên PATH nhưng có bộ công cụ của phiên này:

```bash
./scripts/mvn-local.sh spring-boot:run
```

API lắng nghe `127.0.0.1:8080`. Sau khi khởi động profile `demo`, log ghi **UUID tài khoản An và Bình**, không ghi mật khẩu. Có thể lấy lại qua SQL trong phần 6. Chạy lại demo không nhân đôi tài khoản; đổi mật khẩu env không đổi mật khẩu người dùng đã seed.

Flyway dùng `bank_owner` để tạo/migrate schema; API dùng `bank_app`, không có quyền DDL và không có quyền sửa/xóa giao dịch, bút toán, audit. Trên database có sẵn, xem [oracle-setup.md](docs/oracle-setup.md).

**HTTP Basic:** không có API login phát JWT. Mỗi request gửi Basic credentials. HTTP chỉ dùng ở localhost trong bài học; trước khi triển khai cần HTTPS và hệ thống xác thực phù hợp. Các ví dụ dùng curl/API client, không dùng trình duyệt làm ứng dụng giao dịch với Basic authentication tự gửi credentials.

## 3. Chuyển tiền đầu tiên

Nhập UUID từ log, không dùng số `10`/`20`:

```bash
AN_ACCOUNT_ID='UUID-tai-khoan-An'
BINH_ACCOUNT_ID='UUID-tai-khoan-Binh'
```

Xem tài khoản; `curl -u an` sẽ hỏi mật khẩu, tránh đặt mật khẩu trong lệnh:

```bash
curl -u an "http://localhost:8080/api/v1/accounts/$AN_ACCOUNT_ID"
```

Chuyển 300.000 VND:

```bash
curl -i -u an -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: learning-transfer-001' \
  -d "{\"sourceAccountId\":\"$AN_ACCOUNT_ID\",\"destinationAccountId\":\"$BINH_ACCOUNT_ID\",\"amount\":\"300000\",\"description\":\"Tien sach\"}" \
  http://localhost:8080/api/v1/transfers
```

Lần đầu trả `201`, An từ 1.000.000 còn 700.000, Bình từ 200.000 lên 500.000. Gửi **chính xác cùng lệnh, cùng khóa** trả `200` và ID giao dịch cũ, không trừ tiền lần nữa. Giữ khóa nhưng đổi số tiền trả `409`. Nếu mất phản hồi, luôn gửi lại cùng khóa để xác định kết quả.

Xem bút toán:

```bash
curl -u an "http://localhost:8080/api/v1/accounts/$AN_ACCOUNT_ID/entries?page=0&size=20"
```

`amount` và `balance` trong JSON là chuỗi số nguyên, không phải JSON number. Không nhận phần lẻ, ký hiệu số mũ hoặc số tiền bắt đầu bằng `0`.

## 4. API

Tất cả endpoint nằm dưới `/api/v1`; không có đăng ký người dùng công khai.

| Method / endpoint | Quyền | Body |
|---|---|---|
| POST /customers | STAFF | `{"name":"Chi"}` |
| GET /customers/{id} | STAFF hoặc chính khách hàng | — |
| POST /accounts | STAFF | `{"customerId":100}` |
| GET /accounts/{uuid} | STAFF hoặc chủ tài khoản | — |
| PATCH /accounts/{uuid}/status | STAFF | `{"status":"BLOCKED"}` hoặc `ACTIVE` |
| GET /accounts/{uuid}/entries | STAFF hoặc chủ tài khoản | page >= 0, size 1–100 |
| POST /transfers | CUSTOMER, nguồn thuộc mình | Body ví dụ trên; bắt buộc Idempotency-Key |
| GET /transfers/{id} | STAFF hoặc chủ nguồn/đích | — |

Tạo hồ sơ khách hàng không tự tạo user đăng nhập. User CUSTOMER được liên kết hồ sơ qua bootstrap demo; quản lý định danh/đăng ký là bài học mở rộng. Tài khoản mở qua API có số dư 0, chưa có luồng nạp tiền; không chỉnh số dư qua API.

| HTTP | Ý nghĩa |
|---|---|
| 400 | JSON, UUID, số tiền, khóa hoặc tham số sai |
| 401 | Chưa xác thực/sai mật khẩu/user disabled |
| 403 | Sai vai trò |
| 404 | Không tồn tại hoặc không có quyền xem tài nguyên |
| 409 | Idempotency-Key đã dùng với payload khác |
| 422 | Không đủ tiền, tài khoản/khách hàng không hoạt động, tràn số dư |
| 503 | Hết thời gian chờ khóa/database tạm thời không khả dụng |
| 500 | Lỗi hệ thống; không công bố SQL/stack trace |

Response lỗi dùng ProblemDetail có `code`, `requestId`; lỗi validation có `errors`. `X-Request-ID` có thể gửi 1–64 ký tự chữ/số/`_`/`-`; giá trị sai được thay bằng UUID server.

## 5. Kiểm thử

```bash
./mvnw test                 # Unit: quy tắc tiền, fingerprint, mật khẩu demo
./mvnw -Poracle-it verify   # Unit + integration trên Oracle thật qua Testcontainers
```

Lệnh integration cần Docker hoạt động, tự tạo Oracle riêng ở cổng ngẫu nhiên và xóa container khi kết thúc. Không dùng database local trong Compose, không cần `.env`. Profile này **không bỏ qua** kiểm thử nếu Docker hỏng. Đừng dùng `-DskipTests` để đánh giá kết quả.

Oracle IT kiểm tra UUID/RAW(16), quyền API, tài khoản runtime, rollback khi ghi ledger lỗi, retry sau khóa tài khoản, cạnh tranh số dư, nhận tiền đồng thời, chuyển ngược chiều và thời gian chờ lock 5 giây. Xem [verification.md](docs/verification.md) để biết lần kiểm chứng thực tế.

## 6. Đọc dữ liệu và đối soát

Mở SQL*Plus, nhập mật khẩu owner khi được hỏi:

```bash
docker compose exec oracle sqlplus bank_owner@//localhost:1521/FREEPDB1
```

```sql
SELECT RAWTOHEX(id) AS account_uuid_hex, account_number, customer_id, balance
FROM bank_account;
SELECT id, username, customer_id FROM app_user;
SELECT * FROM bank_transfer ORDER BY id;
SELECT * FROM ledger_entry ORDER BY id;
```

UUID API có dấu gạch ngang; `RAWTOHEX` hiển thị 32 ký tự hex không gạch ngang. Ví dụ `10000000000040008000000000000001` tương ứng `10000000-0000-4000-8000-000000000001`.

Chạy nội dung [scripts/reconcile.sql](scripts/reconcile.sql). Mỗi tài khoản cần có `difference = 0`:

```text
opening_balance + tổng CREDIT − tổng DEBIT = balance
```

Dừng Oracle nhưng giữ dữ liệu:

```bash
docker compose stop oracle
```

Không dùng `docker compose down -v` trừ khi chủ động muốn xóa toàn bộ dữ liệu demo. Script khởi tạo user chỉ chạy khi volume mới; thay password `.env` không tự đổi password database đã tạo.

## 7. Học theo mã nguồn

Đọc [learning-guide.md](docs/learning-guide.md), rồi theo:

`TransferController → TransferService → UserRepository/AccountRepository → Oracle`.

- DTO xác định contract, không trả entity trực tiếp.
- Service kiểm tra quyền và nghiệp vụ trong transaction.
- Khóa user tuần tự hóa request của một người; khóa account bảo vệ số dư chung.
- Khóa account theo thứ tự từ điển `UUID.toString()`, không theo chiều chuyển tiền.
- `WAIT 5` giới hạn thời gian chờ lấy khóa; khóa giữ đến commit/rollback.
- Hai ledger entries và audit được commit cùng số dư; chỉ response thành công sau commit.
- Audit append-only đối với runtime user, nhưng DBA/owner vẫn có thể sửa; chưa có audit chống sửa độc lập.

Bài tập tiếp: OAuth2/OIDC, maker-checker, hạn mức ngày, nạp/rút có tài khoản đối ứng, full general ledger, đối soát độc lập và bảo vệ audit.

Tài liệu nền: [Spring Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Spring transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html), [Oracle SELECT FOR UPDATE](https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/SELECT.html), [Testcontainers Oracle Free](https://java.testcontainers.org/modules/databases/oraclefree/).

Phân trang: ngoài page >= 0 và size 1–100, tích page × size không vượt 2.147.483.647. JSON ID khách hàng phải là số nguyên, không tự cắt phần thập phân. API giữ mã lỗi HTTP 404/405/415/406 và header Allow khi phù hợp. Phản hồi chuyển tiền ban đầu/gửi lại/tra cứu cùng nội dung; description bỏ trống hiển thị chuỗi rỗng, createdAt dùng độ chính xác microsecond tương ứng Oracle.
