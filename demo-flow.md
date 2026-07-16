# Hướng dẫn Kiểm thử Nghiệp vụ Hệ thống (demo-flow.md)

Tài liệu này tổng hợp toàn bộ thông tin đăng nhập, danh sách giải đấu mẫu và các bước thực hiện chi tiết cho từng kịch bản nghiệp vụ để hỗ trợ việc kiểm thử và chạy demo một cách trơn tru nhất.

---

## 1. Thông tin tài khoản đăng nhập (Mật khẩu ứng với chuỗi Hash mới)

| Vai trò | Tên tài khoản | Chức năng kiểm thử chính |
|---|---|---|
| **Admin** | `admin1` | Quản trị hệ thống, Duyệt hồ sơ/hợp đồng, Quản lý lịch thi đấu, Công bố báo cáo |
| **Chủ ngựa (Owner)** | `owner1` | Đăng ký ngựa, tìm và mời Kỵ sĩ ký hợp đồng, thanh toán phí |
| **Kỵ sĩ (Jockey)** | `jockey13` | Đăng ký giải đấu mới |
| **Kỵ sĩ (Jockey)** | `jockey9` | Chấp nhận/Từ chối đề xuất hợp đồng từ Chủ ngựa |
| **Trọng tài chính** | `referee1` | Bắt đầu trận đấu, nhập kết quả, xử lý vi phạm, ký nháp báo cáo |
| **Bác sĩ Thú y** | `vet1` | Khám sức khỏe cho ngựa thi đấu |
| **Nhân viên Y tế** | `medical1` | Khám sức khỏe cho kỵ sĩ thi đấu |
| **Khán giả** | `spectator1` | Đặt cược dự đoán Top 3 của các trận đấu sắp diễn ra |
| **Khán giả** | `spectator2` | Xem lịch sử dự đoán cược đã được chấm điểm |

---

## 2. Các kịch bản kiểm thử chi tiết theo từng giải đấu

### Kịch bản 1: Đăng ký giải đấu mẫu mới
*   **Giải đấu:** `DEMO 5 - Đang mở đăng ký`
*   **Tình trạng:** Đang trong giai đoạn nhận đăng ký (`REGISTRATION_OPEN`).
*   **Các bước kiểm thử:**
    1.  Đăng nhập bằng tài khoản Chủ ngựa `owner1`. Chọn giải đấu và thực hiện đăng ký cho một trong các ngựa trống (chưa đăng ký).
    2.  Thực hiện nạp tiền giả lập hoặc thanh toán phí đăng ký từ ví của Chủ ngựa.
    3.  Đăng nhập bằng tài khoản Kỵ sĩ `jockey13` để ứng tuyển vào giải đấu này.
    4.  Đăng nhập bằng tài khoản Admin `admin1` để vào danh sách đăng ký duyệt hồ sơ của ngựa sang trạng thái `APPROVED`.

---

### Kịch bản 2: Ghép cặp Kỵ sĩ & Ký kết hợp đồng
*   **Giải đấu:** `DEMO 6 - Đang ghép Kỵ sĩ`
*   **Tình trạng:** Trong giai đoạn ghép cặp (`JOCKEY_MATCHING`).
*   **Các bước kiểm thử:**
    1.  Đăng nhập bằng tài khoản Chủ ngựa `owner1`.
    2.  Vào chức năng Tìm kiếm kỵ sĩ (Jockey Search), hệ thống sẽ hiển thị các kỵ sĩ tự do tham gia giải này (`jockey9` đến `jockey12`).
    3.  Gửi đề xuất hợp đồng cho kỵ sĩ `jockey9`.
    4.  Đăng nhập bằng tài khoản Kỵ sĩ `jockey9`. Vào mục quản lý hợp đồng để xem lời mời ở trạng thái `PENDING_JOCKEY`, chọn **Chấp nhận (Accept)**.
    5.  Đăng nhập lại Chủ ngựa `owner1` để thực hiện thanh toán hóa đơn phí thuê kỵ sĩ (tiền được chuyển vào ví ký quỹ hệ thống).
    6.  Đăng nhập Admin `admin1` để duyệt hợp đồng đó sang trạng thái `APPROVED`.

---

### Kịch bản 3: Bàn xếp lịch tranh tài (Scheduling Board)
*   **Giải đấu:** `DEMO 7 - Đang xếp lịch`
*   **Tình trạng:** Trong giai đoạn xếp lịch thi đấu (`SCHEDULING`).
*   **Các bước kiểm thử:**
    1.  Đăng nhập bằng tài khoản Admin `admin1`.
    2.  Truy cập vào trang **Scheduling Board** tại giải đấu này.
    3.  Thực hiện kéo/thả hoặc gán 4 cặp kỵ sĩ-ngựa đã ký hợp đồng thành công vào các làn từ 1 đến 4.
    4.  Phân công Trọng tài chính (`referee1`), Bác sĩ Thú y (`vet1`), Nhân viên Y tế (`medical1`) cho trận đấu.
    5.  Bấm **Publish Schedule** để chuyển trạng thái giải đấu sang thi đấu (`RACING`) và trận đấu sang sẵn sàng (`SCHEDULED`).

---

### Kịch bản 4: Dự đoán, Khám sức khỏe & Vận hành trận đấu
*   **Giải đấu:** `Summer Grand Championship` (Tên hiển thị: `DEMO 8 - Đang thi đấu`)
*   **Tình trạng:** Đang trong giai đoạn thi đấu (`RACING`), trận Chung kết `DEMO Upcoming Race` đang ở trạng thái `SCHEDULED`.
*   **Các bước kiểm thử:**
    1.  **Dự đoán:** Đăng nhập Khán giả `spectator1`. Chọn trận Chung kết, đặt cược Top 3 ngựa chiến thắng.
    2.  **Khám sức khỏe Ngựa:** Đăng nhập Bác sĩ Thú y `vet1`. Chọn danh sách trận đấu được phân công, thực hiện cập nhật kết quả khám sức khỏe (`PASS`) cho các làn đua chưa khám.
    3.  **Khám sức khỏe Kỵ sĩ:** Đăng nhập Nhân viên Y tế `medical1`. Thực hiện cập nhật kết quả sức khỏe (`PASS`) cho các làn đua chưa khám.
    4.  **Bắt đầu đua & Nhập kết quả:** Đăng nhập Trọng tài `referee1` (sau thời gian đóng cổng cược và hoàn tất khám bệnh):
        *   Bấm **Start Race** để chuyển trận đấu sang `ONGOING`.
        *   Nhập thời gian chạy và thứ hạng (từ 1 đến 8) của các làn thi đấu.
        *   Ghi nhận lỗi vi phạm (violation) nếu có.
        *   Hoàn tất trận đấu để chuyển trạng thái sang `FINISHED`.
        *   Ký số báo cáo kết quả trận đấu để chuyển báo cáo sang trạng thái `Signed`.

---

### Kịch bản 5: Phê duyệt báo cáo Chung cuộc & Trả thưởng ví điện tử
*   **Giải đấu:** `DEMO 2 - Final chờ publish`
*   **Tình trạng:** Đang chờ duyệt báo cáo trận Chung kết (`RESULT_PENDING`). Báo cáo đã có chữ ký trọng tài (`Signed`).
*   **Các bước kiểm thử:**
    1.  Đăng nhập bằng tài khoản Admin `admin1`.
    2.  Truy cập trang báo cáo kết quả `/admin/race-reports`, chọn giải đấu và xem trước thay đổi điểm số ngựa (Rating preview).
    3.  Bấm **Publish** báo cáo kết quả.
    4.  **Kiểm tra kết quả tự động:**
        *   Hệ thống tự động phân chia tiền thưởng từ ví giải thưởng hệ thống chuyển về ví của Owner 1 (giải nhất/nhì/ba) và Jockey tương ứng.
        *   Hệ thống giải phóng nốt 70% phí thuê kỵ sĩ đang giữ ở ví ký quỹ về ví của các Jockey.
        *   Khán giả `spectator2` được cộng điểm dự đoán cược chính xác.
        *   Chỉ số Rating của ngựa được cập nhật chính thức.

---

### Kịch bản 6: Chuyển vòng đấu tự động nguyên tử (Atomic Round Transition)
*   **Giải đấu:** `DEMO 4 - Bracket 32 chuyển vòng`
*   **Tình trạng:** Đang thi đấu Bán kết (`RACING`). 
    *   Trận Bán kết A (`race-demo4-01`) đã kết thúc và báo cáo kết quả đã được `Published`.
    *   Trận Bán kết B (`race-demo4-02`) đã kết thúc, kết quả đã nhập và báo cáo ở trạng thái `Signed`.
*   **Các bước kiểm thử:**
    1.  Đăng nhập Admin `admin1`.
    2.  Công bộ (`Publish`) báo cáo kết quả cho Trận Bán kết B.
    3.  **Kiểm tra sự thay đổi:** Vì Bán kết B là trận đấu cuối cùng của Vòng 1 hoàn tất báo cáo kết quả, hệ thống sẽ tự động kích hoạt chuyển vòng (Round Transition). Vòng chung kết sẽ tự động được chuyển sang trạng thái lập lịch, đồng thời **8 vị trí của 8 chú ngựa về đích top 4 ở trận A và trận B** sẽ được tự động xếp vào 8 làn thi đấu của trận Chung kết trong cùng một transaction.
