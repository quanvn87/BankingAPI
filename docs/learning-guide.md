# Học Mini Bank API theo từng bước

## Bài 1 — Một API nhận gì và trả gì?

REST API là điểm giao tiếp qua HTTP. Client gửi method, URL, headers và JSON; server trả HTTP status và JSON.

Ví dụ `GET /api/v1/accounts/{uuid}` đọc tài khoản. `POST /api/v1/transfers` tạo chuyển tiền. `PATCH /api/v1/accounts/{uuid}/status` đổi trạng thái. Body chuyển tiền nằm trong TransferDtos.TransferCommand; kết quả nằm trong TransferResponse.

Đọc TransferController.java: Controller nhận request, Spring kiểm tra @Valid, lấy BankPrincipal từ xác thực rồi gọi Service. Không tin customerId tự gửi để xác định người thao tác. Controller không tự cộng/trừ tiền.

## Bài 2 — Oracle lưu dữ liệu thế nào?

- CUSTOMER lưu hồ sơ; APP_USER lưu thông tin đăng nhập đã băm BCrypt và liên kết hồ sơ.
- BANK_ACCOUNT lưu số dư hiện tại. `id` UUID là khóa nội bộ; `account_number` là chuỗi số tài khoản.
- BANK_TRANSFER lưu chuyển tiền thành công.
- LEDGER_ENTRY lưu sự thay đổi tiền của từng tài khoản: DEBIT giảm, CREDIT tăng theo quy ước sổ phụ của dự án.
- IDEMPOTENCY_REQUEST nhận diện request thành công đã xử lý.
- ACCOUNT_OPENING ghi số dư mở sổ giả lập; AUDIT_EVENT ghi hành động thành công và actor.

Đọc V1__schema.sql. PRIMARY KEY không được trùng; FOREIGN KEY bảo đảm tham chiếu tồn tại; CHECK bảo vệ số dư không âm và các trạng thái hợp lệ. Kiểm tra cặp ledger cân bằng là quy tắc xuyên hàng được Service bảo đảm, không phải CHECK constraint của một hàng.

UUID là định danh 128-bit. Java dùng UUID.randomUUID(); Oracle RAW(16) lưu 16 byte. API hiển thị 36 ký tự có dấu gạch ngang. Oracle RAWTOHEX hiển thị 32 ký tự hex. UUID không bảo vệ quyền truy cập: mỗi API vẫn kiểm tra chủ tài khoản.

Sequence sinh ID số cho các thực thể khác. Giá trị sequence có thể nhảy khi rollback; không dùng ID để suy luận giao dịch liên tục hoặc số lượng bản ghi.

## Bài 3 — Số tiền và số dư

BigDecimal tránh sai số nhị phân của double. Mini Bank chỉ nhận VND nguyên, tối đa 19 chữ số, lưu NUMBER(19,0). API tiền dạng chuỗi `"300000"`; các dạng `300000`, `"1.0"`, `"1e3"`, `"-1"` bị từ chối.

An có 1.000.000, Bình có 200.000. Chuyển 300.000 làm An còn 700.000 và Bình có 500.000. Tổng vẫn 1.200.000. Sau mỗi transfer, hai ledger entries cùng amount và cùng transfer ID. Đối soát số dư bằng opening_balance + credits − debits.

Nếu chuyển thiếu tiền, Service trả 422 và không ghi transfer thành công. Trong đời thực, available balance có thể khác booked balance vì giữ tiền/phong tỏa; mô hình này chưa có holds và dùng một balance.

## Bài 4 — Transaction bảo vệ nhiều bảng

Đọc annotation @Transactional(rollbackFor=Exception.class) ở TransferService.transfer. Tất cả thay đổi số dư, transfer, ledger, idempotency và audit dùng cùng transaction.

Commit xác nhận tất cả thay đổi. Rollback hủy toàn bộ thay đổi chưa commit của lần xử lý. Nếu ghi ledger lỗi sau khi đã sửa số dư, người chuyển không mất tiền vì transaction rollback.

Spring proxy quản lý transaction: Controller gọi Service bean qua proxy; proxy commit trước khi trả kết quả thành công về Controller. Tự gọi method annotated trong cùng instance có thể bỏ qua proxy, nên không tách transaction bằng tự gọi như vậy. Đừng bắt lỗi DB rồi trả thành công. Thông báo email/network không nằm trong database transaction này; nếu thêm sau này cần nghiên cứu outbox.

## Bài 5 — Hai giao dịch đồng thời

Nếu cùng đọc A=1.000.000 rồi lần lượt ghi 300.000/400.000 sau khi chuyển 700.000/600.000, một cập nhật có thể ghi đè cập nhật kia. Kiểm tra đủ tiền trên snapshot cũ không đủ để bảo vệ.

Repository dùng SQL native `FOR UPDATE WAIT 5`. Đây là cơ chế khóa pessimistic write: lấy quyền khóa trước khi đọc/kiểm tra/cập nhật. Oracle giữ khóa đến commit/rollback; người muốn ghi/khóa cùng hàng phải chờ. SELECT thông thường vẫn thường đọc phiên bản đã commit.

Luồng cụ thể:

1. Khóa APP_USER của người thực hiện để tuần tự hóa request của người đó.
2. Kiểm tra idempotency; retry thành công trả kết quả cũ.
3. Sắp UUID nguồn/đích bằng thứ tự từ điển của UUID.toString() chữ thường.
4. Lấy khóa tài khoản thứ nhất rồi thứ hai bằng hai query riêng.
5. Kiểm tra chủ tài khoản, trạng thái khách hàng/tài khoản, currency, số dư và tràn đích.
6. Cập nhật và ghi dữ liệu, commit/rollback rồi nhả khóa.

Ví dụ A có UUID bắt đầu 1..., B có UUID bắt đầu f...; cả A→B và B→A đều khóa A rồi B. Nếu mỗi giao dịch khóa nguồn trước, có thể mỗi bên giữ một tài khoản và chờ nhau (deadlock). Thứ tự cố định giảm khả năng deadlock; vẫn phải xử lý lỗi.

WAIT 5 là thời gian chờ lấy khóa, không phải thời gian được giữ khóa. Nó cũng không phải deadline toàn request hoặc transaction; các truy vấn khác/connection/network vẫn có thời gian riêng. Test giữ lock trên connection khác và xác nhận response 503 sau khoảng 5 giây. Không dùng SKIP LOCKED cho chuyển tiền vì có thể bỏ qua tài khoản cần xử lý.

## Bài 6 — Gửi lại không chuyển tiền lần hai

Server có thể đã commit nhưng client mất mạng nên không nhận response. Client phải gửi lại cùng key và cùng payload. API trả 200 với transfer cũ; giao dịch mới thành công trả 201.

Khóa khác = ý định mới, có thể thực hiện lại ngay cả khi số tiền/người nhận giống nhau. Khóa giống nhưng payload khác = 409. Key thuộc phạm vi người thực hiện; hai người có thể dùng cùng một chuỗi key.

Fingerprint dùng SHA-256 của một mảng JSON có thứ tự trường cố định. UUID canonical, amount được kiểm tra/chuyển định dạng, description null và chuỗi rỗng tương đương; khoảng trắng trong description vẫn là dữ liệu có ý nghĩa. Key được giữ không hết hạn trong phiên bản này; lịch sử yêu cầu thất bại không lưu vào bảng idempotency.

Sau khi transfer thành công, nếu tài khoản bị BLOCKED rồi retry, API vẫn trả transfer cũ. Nó xác nhận kết quả đã xảy ra, không chuyển tiền mới.

## Bài 7 — Đọc kiểm thử như tình huống nghiệp vụ

Mở BankOracleIT.java. Tests dùng Oracle riêng và runtime BANK_APP; setup dữ liệu/trigger dùng owner để cố tình tạo lỗi. Không bao cả test trong transaction bên ngoài vì cần kiểm chứng commit thật và thread riêng.

- transfersAndReplaysWithoutSpendingTwice: tiền và lịch sử chỉ thay đổi một lần.
- failureAfterBalanceUpdatesRollsBackEverything: trigger lỗi khi ghi ledger, tất cả rollback.
- simultaneousSpendingNeverOverdraws: 700.000 và 600.000 từ 1.000.000, chỉ một thành công.
- concurrentIncomingTransfersPreserveBothCredits: người khác nhau gửi đến một nơi, không mất cập nhật.
- reverseTransfersDoNotDeadlock: khóa UUID có thứ tự.
- statusChangesSerializeWithTransfers: kết quả tuân theo thứ tự khóa, không thay đổi một phần.
- accountAndUserLocksHaveFiniteWait: không chờ khóa vô hạn.
- runtimeHasNoDdlOrLedgerMutationPrivileges: DB bảo vệ quyền API.

Thử thay code đúng thành sai (ví dụ bỏ kiểm tra số dư hoặc bỏ khóa) trên bản sao dự án và quan sát test phát hiện. Không sửa dữ liệu ngân hàng thật để làm bài tập.

Phân trang: ngoài page >= 0 và size 1–100, tích page × size không vượt 2.147.483.647. JSON ID khách hàng phải là số nguyên, không tự cắt phần thập phân. API giữ mã lỗi HTTP 404/405/415/406 và header Allow khi phù hợp. Phản hồi chuyển tiền ban đầu/gửi lại/tra cứu cùng nội dung; description bỏ trống hiển thị chuỗi rỗng, createdAt dùng độ chính xác microsecond tương ứng Oracle.
