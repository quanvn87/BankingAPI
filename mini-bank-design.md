# Thiết kế Mini Bank API

Ngày: 08/10/2026

## 1. Mục tiêu và phạm vi

Dự án học tập bằng Java, Spring Boot và Oracle, giúp người học hiểu REST API, phân tầng mã nguồn, phân quyền và tính toàn vẹn của giao dịch tiền. Một ứng dụng monolith, một cơ sở dữ liệu Oracle, chỉ hỗ trợ chuyển tiền nội bộ bằng VND. Đây là mô hình học tập; không tuyên bố đáp ứng tiêu chuẩn triển khai của một ngân hàng cụ thể.

Giả định người học biết Java cơ bản. Tài liệu tiếng Việt giải thích từng bước, có ví dụ curl và cách chạy bằng Maven. Không có giao diện web, thanh toán liên ngân hàng, lãi suất, phí, hạn mức ngày, KYC/AML hoặc hệ thống phê duyệt nhiều cấp trong phiên bản đầu.

## 2. Kiến trúc

Java 21, Spring Boot (chọn phiên bản phát hành tương thích và cố định khi triển khai), Maven, Spring Web, Spring Data JPA, Spring Security, Bean Validation, Oracle JDBC và Flyway kèm module Oracle tương thích. Oracle chạy cục bộ qua Docker Compose hoặc kết nối đến Oracle có sẵn bằng biến môi trường.

Luồng: HTTP → Security → Controller/DTO → Service → Repository → Oracle.

Controller chỉ nhận/trả DTO và kiểm tra đầu vào. Service chứa nghiệp vụ và ranh giới transaction. Repository truy cập dữ liệu. Không trả JPA entity trực tiếp qua API. Cấu hình production và dữ liệu demo được phân biệt; không lưu mật khẩu thật vào Git.

## 3. Mô hình dữ liệu

ID tài khoản BANK_ACCOUNT.id dùng UUID v4 do ứng dụng tạo (Java UUID.randomUUID()), lưu Oracle RAW(16), tương đương về mục đích định danh 128-bit với UNIQUEIDENTIFIER của SQL Server. Tất cả khóa ngoại tới tài khoản (nguồn/đích chuyển tiền, ledger, ACCOUNT_OPENING) cũng là RAW(16); entity/DTO/path parameter Java dùng java.util.UUID, JSON dùng chuỗi UUID chuẩn chữ thường có dấu gạch ngang. ID các thực thể khác tiếp tục dùng Oracle sequence và Java Long. Số tài khoản giao dịch (account_number) là chuỗi riêng, không phải UUID; thời gian lưu UTC, hiển thị ISO 8601. Số tiền VND dùng BigDecimal và NUMBER(19,0), không nhận phần thập phân. Giới hạn số tiền phù hợp độ chính xác của cột; không dùng double/float.

- APP_USER: username duy nhất, password_hash BCrypt, role CUSTOMER hoặc STAFF, enabled; liên kết customer_id cho người dùng khách hàng. Một người dùng khách hàng gắn một khách hàng.
- CUSTOMER: mã khách hàng duy nhất, tên, trạng thái ACTIVE hoặc INACTIVE. Dữ liệu ví dụ là giả lập, không thu thập giấy tờ nhạy cảm.
- BANK_ACCOUNT: số tài khoản duy nhất, customer_id, currency cố định VND, balance không âm, status ACTIVE hoặc BLOCKED. Tài khoản mới có số dư 0; không có endpoint sửa số dư.
- BANK_TRANSFER: người yêu cầu, tài khoản nguồn/đích, amount dương, currency, nội dung, thời gian. Chỉ lưu chuyển tiền đã commit thành công; không dùng bảng này làm lịch sử yêu cầu thất bại.
- LEDGER_ENTRY: transfer_id, account_id, direction DEBIT hoặc CREDIT, amount dương, balance_after không âm, thời gian. Mỗi chuyển tiền có đúng một bút toán DEBIT và một CREDIT với cùng số tiền. Không có API cập nhật hoặc xóa bút toán.
- IDEMPOTENCY_REQUEST: requester_id, idempotency_key, dấu vân tay từ các trường đã chuẩn hóa, transfer_id. Unique(requester_id, idempotency_key). Chỉ lưu yêu cầu thành công, cùng transaction với chuyển tiền; giữ khóa không hết hạn trong bản học tập.
- AUDIT_EVENT: actor, action, resource_id, thời gian, request_id. Ghi sự kiện nghiệp vụ thành công cùng transaction. Lỗi xác thực/kiểm tra đầu vào được log có cấu trúc, không chứa mật khẩu, Authorization header hoặc body tài chính đầy đủ.

Flyway tạo khóa chính, khóa ngoại, unique constraint, CHECK và index cho tra cứu lịch sử. Các bất biến xuyên nhiều hàng như cặp bút toán cân bằng được service bảo đảm và kiểm thử. Tài khoản runtime không có quyền DDL; tài khoản migration riêng nếu chạy cấu hình phân quyền này.

## 4. API và phân quyền

Tiền trong JSON được biểu diễn bằng chuỗi số nguyên, ví dụ "100000", để tránh mất độ chính xác ở client. API đặt dưới /api/v1.

| Method | Endpoint | Quyền và hành vi |
|---|---|---|
| POST | /customers | STAFF tạo khách hàng |
| GET | /customers/{id} | STAFF hoặc khách hàng xem hồ sơ của mình |
| POST | /accounts | STAFF mở tài khoản cho khách hàng ACTIVE |
| GET | /accounts/{id} | STAFF hoặc chủ tài khoản xem số dư/trạng thái |
| PATCH | /accounts/{id}/status | STAFF khóa hoặc mở lại tài khoản; ghi audit |
| GET | /accounts/{id}/entries | STAFF hoặc chủ tài khoản; phân trang, thứ tự ổn định theo thời gian và ID |
| POST | /transfers | CUSTOMER chuyển từ tài khoản mình sở hữu; bắt buộc Idempotency-Key |
| GET | /transfers/{id} | STAFF hoặc chủ tài khoản nguồn/đích; DTO giới hạn thông tin cá nhân |

Dùng HTTP Basic và BCrypt trong phiên bản đầu để tập trung vào nghiệp vụ. Không có endpoint đăng nhập phát token; client gửi thông tin xác thực theo HTTP Basic. Chỉ dùng HTTP ở localhost cho bài học; môi trường triển khai cần HTTPS. Ứng dụng stateless, không dùng cookie/session để xác thực; CSRF được cấu hình phù hợp luồng này. STAFF không được thay mặt khách hàng gọi API chuyển tiền. Tài khoản demo tạo qua profile demo từ biến môi trường, không phải API đăng ký công khai.

## 5. Chuyển tiền và đồng thời

1. Kiểm tra cú pháp: tiền nguyên dương, không vượt độ chính xác cột, hai tài khoản khác nhau, nội dung tối đa 140 ký tự, Idempotency-Key 1–64 ký tự theo tập ký tự được tài liệu hóa.
2. Xác thực CUSTOMER. Khóa hàng APP_USER của người yêu cầu trước. Cách đơn giản này tuần tự hóa các yêu cầu chuyển tiền của cùng người dùng, đồng thời giải quyết hai request có cùng khóa. Đây là đánh đổi thông lượng có chủ ý cho bài học.
3. Tra idempotency theo người yêu cầu và khóa: payload chuẩn hóa giống nhau → trả giao dịch cũ với 200; khác nhau → 409. Kiểm tra này diễn ra trước kiểm tra trạng thái/số dư hiện tại để retry thành công vẫn trả kết quả cũ.
4. Khóa BANK_ACCOUNT nguồn và đích theo thứ tự từ điển tăng dần của UUID.toString() chuẩn chữ thường (cùng quy tắc ở mọi luồng) bằng pessimistic write. Đặt thời gian chờ khóa hữu hạn với cơ chế Oracle/Hibernate được xác minh lúc triển khai. Thao tác khóa/mở tài khoản cũng phải lấy khóa account trước khi thay đổi trạng thái.
5. Kiểm tra nguồn thuộc người yêu cầu, cả hai tài khoản ACTIVE, cùng VND và khách hàng liên quan ACTIVE. Trạng thái khách hàng chưa có API sửa trong bản đầu. Không đủ số dư → 422; không được ghi thay đổi một phần.
6. Trừ tiền nguồn, cộng tiền đích; kiểm tra số dư đích không tràn độ chính xác. Tạo BANK_TRANSFER, hai LEDGER_ENTRY, IDEMPOTENCY_REQUEST và AUDIT_EVENT.
7. Commit toàn bộ trong một @Transactional service method. Lỗi bất kỳ rollback toàn bộ. Chỉ trả thành công sau commit. Nếu response mất sau commit, client gửi lại cùng khóa để lấy kết quả cũ.

Không tự retry chuyển tiền với một khóa mới khi timeout. Khóa tài khoản có thứ tự giúp giảm deadlock nhưng không đảm bảo loại bỏ mọi deadlock; lỗi timeout/deadlock trả 503 với hướng dẫn gửi lại cùng khóa. Unique constraint là hàng rào bổ sung; không nuốt lỗi unique trong transaction đã bị đánh dấu rollback.

## 6. Sổ giao dịch và dữ liệu demo

Balance là số dư hiện hành được cập nhật cùng các bút toán. Tổng debit và credit của mỗi chuyển tiền phải bằng nhau. Chuyển tiền không tạo hoặc làm mất tổng tiền giữa hai tài khoản.

Demo cần tiền ban đầu: profile demo tạo hai khách hàng và tài khoản có số dư mở sổ giả lập. BANK_ACCOUNT có opening_balance >= 0; bảng ACCOUNT_OPENING lưu account_id duy nhất, amount và created_at. LEDGER_ENTRY chỉ chứa chuyển tiền. Công thức đối soát: opening_balance + tổng CREDIT − tổng DEBIT = balance. Bootstrap demo tạo dữ liệu mở sổ và số dư hiện hành bằng nhau trong một transaction, chỉ khi chưa có dữ liệu demo. Tài khoản mở qua API có opening_balance = balance = 0 và bản ghi ACCOUNT_OPENING bằng 0. Không có endpoint sửa opening_balance hoặc nạp tiền. Mở sổ giả lập là điểm bắt đầu của bài học, chưa mô hình hóa nguồn tiền đối ứng hoặc sổ cái kế toán đầy đủ; không được coi là luồng nạp tiền production.

## 7. Lỗi, quan sát và bảo mật

Lỗi API theo ProblemDetail: status, title, detail an toàn, code nghiệp vụ, requestId; lỗi validation có danh sách trường. Không trả SQL hoặc stack trace. 400 đầu vào sai; 401 chưa xác thực; 403 sai vai trò; 404 tài nguyên không tồn tại hoặc không được phép biết; 409 trùng khóa khác payload/xung đột trạng thái; 422 vi phạm nghiệp vụ; 503 lỗi chờ khóa hoặc DB tạm thời. Lỗi DB khác trả 500 với thông báo chung.

Request ID do server tạo hoặc chấp nhận giá trị client đã giới hạn và kiểm tra; ghi trong response và log. Secrets dùng biến môi trường. Không log credentials hoặc dữ liệu khách hàng đầy đủ. Lịch sử có giới hạn page size; không cung cấp API xóa giao dịch. Bất biến và tính bất biến bút toán được bảo vệ ở tầng ứng dụng và quyền truy cập; audit chưa có bảo vệ chống sửa ở mức hệ thống độc lập.

## 8. Kiểm thử và tiêu chí nghiệm thu

- Unit test các quy tắc tiền, trạng thái, quyền sở hữu và dấu vân tay payload.
- Security/API test 401, 403, 404 và lỗi validation.
- Integration test trên Oracle thực cho rollback, unique constraint, mapping NUMBER, UUID ↔ RAW(16), các khóa ngoại tài khoản và sequence, không dùng H2 để khẳng định hành vi Oracle.
- Kiểm thử hai yêu cầu cùng khóa: một chuyển tiền, cùng transfer ID; khóa giống payload khác trả 409.
- Kiểm thử rút đồng thời vượt số dư: chỉ giao dịch đủ tiền được commit, balance không âm.
- Kiểm thử chuyển ngược chiều và khóa tài khoản đồng thời; không thay đổi một phần.
- Đối soát opening_balance và ledger sau các giao dịch.

Bàn giao gồm mã nguồn Maven, migration Oracle, cấu hình mẫu, Docker Compose Oracle với hướng dẫn yêu cầu tài nguyên/image, README tiếng Việt, ví dụ curl và bài tập mở rộng. Ghi rõ những kiểm thử đã chạy và những kiểm thử chưa chạy do thiếu Oracle hoặc môi trường; không khẳng định thành công khi chưa có bằng chứng.

## 9. Lộ trình học trong README

1. Chạy Oracle, migration và API; xem request/response.
2. Theo dõi một request qua Controller, Service, Repository.
3. Đọc schema, constraint, UUID, sequence và index.
4. Thử chuyển tiền thành công và lỗi không đủ số dư.
5. Gửi lại Idempotency-Key; thử hai request đồng thời.
6. Đọc kiểm thử rollback, phân quyền và đối soát.
7. Bài tập tiếp: OAuth2/OIDC, maker-checker, hạn mức ngày, nạp/rút qua tài khoản đối ứng, đối soát độc lập và bảo vệ audit.

## 10. Quy tắc UUID tài khoản

Sinh UUID tại ứng dụng; không dùng sequence hoặc SYS_GUID() để sinh BANK_ACCOUNT.id. Hibernate phải ánh xạ UUID thành binary 16 byte (RAW(16)), xác minh round-trip trên Oracle. PRIMARY KEY và FOREIGN KEY bảo vệ tính duy nhất/toàn vẹn. UUID không thay thế kiểm tra quyền sở hữu. AUDIT_EVENT.resource_id dùng VARCHAR2(64) để chứa cả UUID tài khoản và ID số của thực thể khác. So sánh ID tài khoản bằng UUID.equals(), không dùng toán tử < hoặc >; thứ tự khóa dùng Comparator.comparing(UUID::toString), không suy diễn thứ tự thời gian tạo.
