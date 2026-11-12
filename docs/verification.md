# Kết quả kiểm chứng — 09/10/2026

Môi trường: Linux x86_64, Java Temurin 21.0.12.1, Maven wrapper 3.9.11, Spring Boot 3.5.16, Hibernate 6.6.53.Final, Oracle Free 23.9 Docker. Cấu hình không dùng H2. API test chạy bằng BANK_APP, migration/fixture bằng BANK_OWNER.

## Kiểm thử tự động

Lệnh cuối: `./scripts/mvn-local.sh -q -Poracle-it verify` — exit 0.

| Bộ kiểm thử | Số trường hợp | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| MoneyTest | 12 | 0 | 0 | 0 |
| TransferFingerprintTest | 1 | 0 | 0 | 0 |
| DemoPasswordTest | 1 | 0 | 0 | 0 |
| BankOracleIT | 20 | 0 | 0 | 0 |
| Tổng | 34 | 0 | 0 | 0 |

Các Oracle IT kiểm tra: chuyển tiền; replay và 409; rollback khi trigger ledger gây lỗi; ownership/status/thiếu tiền/tràn đích; UUID round-trip; constraint DB; runtime không DDL/sửa ledger; khóa user/account 5 giây; rút đồng thời; nhận đồng thời; chuyển ngược chiều; khóa trạng thái đồng thời; mở tài khoản 0; demo seed idempotent; phân trang; mã HTTP; JSON amount và customerId; toàn bộ DTO replay khớp response đầu.

Thời gian chờ khóa thực tế khoảng 5,1 giây với lỗi ORA-30006 chuyển thành 503. Test không dùng sleep để đồng bộ giao dịch; dùng hai thread, latch và connection giữ khóa riêng.

Rà soát độc lập không tìm thấy vấn đề Critical. Bốn kiểm thử regression đã quan sát FAIL trước khi sửa: số ID thập phân bị cắt, 4xx framework bị biến thành 500, offset trang tràn giới hạn, response đầu/replay khác description/timestamp. Sau sửa, tất cả 34 trường hợp PASS. Phản hồi 405 giữ header Allow.

## Chạy thử từ cấu hình sạch

Compose test riêng tạo volume mới và chạy đúng script oracle/init/01-users.sh; owner và runtime tách biệt. Khởi động ứng dụng profile demo qua Maven, dùng HTTP thật trên loopback:

- Chưa xác thực: 401.
- An ban đầu 1.000.000 VND, Bình 200.000 VND.
- Chuyển 300.000: 201.
- Cùng khóa/cùng body: 200, cùng transfer ID.
- Cùng khóa/đổi tiền: 409.
- An sau chuyển: 700.000; Bình: 500.000.
- Lịch sử An có một DEBIT; Bình xem tài khoản An: 404.
- SQL đối soát: difference=0 ở cả hai tài khoản; không có cặp ledger mất cân bằng.

Bản JAR đóng gói cũng khởi động thành công. Khởi động lại giữ nguyên UUID và số dư, không tạo trùng demo. HTTP trên bản cuối xác nhận 404/405, từ chối offset lớn và customerId thập phân, response chuyển tiền bỏ description khớp hoàn toàn giữa lần đầu/replay/GET. Sau một chuyển thêm 1 VND, số dư là 699.999/500.001 và đối soát vẫn difference=0.

Ứng dụng/JAR test và database/volume tạm được dừng/xóa sau kiểm chứng; không để dịch vụ nền ngoài ý muốn. Dữ liệu, mật khẩu smoke chỉ là dữ liệu tạm của phiên này, không được cung cấp như cấu hình dùng thật.

## Quyết định triển khai so với kế hoạch

- Thư mục ban đầu không có Git: viết trực tiếp trong thư mục được yêu cầu, không tạo repo/worktree/remote/PR. Không có mã cũ cần cách ly; nếu muốn quản lý phiên bản, cần khởi tạo Git riêng.
- Kiểm thử service/security/schema gom thành Oracle API integration tests thay vì nhiều mock/class theo tên kế hoạch: chứng minh hành vi commit/khóa/phân quyền thật, nhưng chạy cần Docker và lâu hơn unit.
- Khóa dùng native Oracle `FOR UPDATE WAIT 5` ngay từ đầu, không phụ thuộc timeout hint Hibernate: xác định SQL rõ và kiểm chứng được; repository này phụ thuộc Oracle.
- Mã khách hàng nghiệp vụ là chuỗi UUID do server sinh; ID khách hàng vẫn sequence. Đây là khác biệt nội bộ, không thay đổi API/chuyển tiền.
- Giữ nhật ký scratch work/progress.md/logs vì không có lịch sử Git; `work/` và `target/` bị ignore. Công cụ Java/Maven tải trong work/tooling để kiểm thử, không cài toàn hệ thống.
- Chưa có Git branch để merge/push; bàn giao file local. Không có PR được tạo.
- Đánh giá response replay khác nhau là lỗi contract cần sửa, dù reviewer xếp mức Minor; đã kiểm chứng toàn DTO.

## Giới hạn còn lại

Chỉ kiểm chứng Oracle Free 23.9 trên môi trường trên. Flyway BOM có cảnh báo Oracle 23.9 mới hơn mốc hỗ trợ đã kiểm thử của module; migration và IT thực tế đều thành công. Không coi điều đó là chứng minh tương thích mọi phiên bản Oracle.

Không kiểm chứng tải lớn, failover, lỗi mạng mọi thời điểm, hệ thống nhận dạng thật hoặc tiêu chuẩn ngân hàng cụ thể. Chủ DB/DBA vẫn có thể sửa dữ liệu ngoài API; audit chưa chống sửa độc lập. Basic-auth browser flow không thuộc phạm vi; HTTP chỉ localhost, client học là curl/API client. Không dùng mô hình này làm hệ thống ngân hàng production mà không thiết kế bổ sung.

Một số quy tắc được giản lược có chủ ý: tài khoản BLOCKED không gửi/nhận; một user CUSTOMER gắn một customer; số dư mở sổ demo không có nguồn tiền đối ứng/full general ledger; không quản lý tiền giữ/hạn mức/ngày nghiệp vụ. Không có minor kỹ thuật nào được để lại từ review; các giới hạn phạm vi trên là quyết định thiết kế.
