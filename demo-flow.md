# Hướng dẫn & Kịch bản Demo Chi tiết Hệ thống HRTMS (Full Flow 01 - 11)

Tài liệu này tổng hợp toàn bộ thông tin đăng nhập, danh sách giải đấu mẫu và các bước thao tác chi tiết theo từng Luồng Nghiệp Vụ (01 đến 11) cùng phân công nhân sự.

---

## 1. Thông tin Tài khoản Đăng nhập (Mật khẩu chung: `12345678`)

| Vai trò | Username | Chức năng kiểm thử chính | Người phụ trách |
|---|---|---|---|
| **Admin** | `admin1` | Cấu hình giải (L04), Duyệt đăng ký, Duyệt hợp đồng (L05), Lập lịch, Publish Báo cáo & Trả thưởng (L11) | **Hải** |
| **Chủ ngựa (Owner)** | `owner1` | Khởi tạo ví/hồ sơ (L01), Đăng ký ngựa & nộp phí (L02), Mời kỵ sĩ & trả cọc (L05), Khiếu nại (L09) | **Tuấn** |
| **Kỵ sĩ (Jockey)** | `jockey13` | Đăng ký tham gia giải đấu (L03) | **KHung** |
| **Kỵ sĩ (Jockey)** | `jockey9` | Tiếp nhận & chấp nhận lời mời hợp đồng từ Chủ ngựa (L05) | **P Hưng** |
| **Bác sĩ Thú y** | `vet1` | Khám sức khỏe & Doping cho Ngựa đua (L07) | **P Hưng** |
| **Nhân viên Y tế** | `medical1` | Khám sức khỏe & Doping cho Kỵ sĩ (L07) | **P Hưng** |
| **Trọng tài chính (Head Ref)** | `referee1` | Xử lý khiếu nại (L09), Ký nháp báo cáo kết quả trận đấu | **Hải** |
| **Trọng tài đường đua (Race Ref)** | `referee2` | Bắt đầu đua, ghi vi phạm (L09), kết thúc đua & tạo báo cáo (L08) | **Hải** |
| **Khán giả** | `spectator1` | Đặt cược dự đoán Top 3 trận đấu (L10) | **Hải** |

---

## 2. Kịch bản Thao tác Chi tiết Theo Từng Luồng Nghiệp Vụ

### 2.1 Luồng 01 - Khởi tạo tài khoản, hồ sơ và ví (Phụ trách: Tuấn)
* **Mục tiêu:** Kiểm tra tạo tài khoản, hồ sơ cá nhân và kích hoạt ví điện tử.
* **Tài khoản test:** `owner1` (hoặc đăng ký tài khoản Owner/Spectator mới trên UI).
* **Các bước thực hiện:**
  1. Đăng nhập hệ thống bằng `owner1`.
  2. Truy cập **Trang cá nhân / Hồ sơ**: Xem thông tin tên, số điện thoại, tên trang trại (`Trang trại Full Flow`).
  3. Truy cập **Ví của tôi (My Wallet)**:
     - Kiểm tra số dư ban đầu (`100,000,000 VND`), trạng thái `ACTIVE`.
     - Thao tác nạp tiền giả lập (Nạp thêm 5,000,000 VND).
     - Kiểm tra biến động số dư và Lịch sử giao dịch ví (Wallet Transactions).

---

### 2.2 Luồng 02 - Chủ ngựa quản lý ngựa và đăng ký giải (Phụ trách: Tuấn)
* **Giải đấu:** `DEMO 5 - Đang mở đăng ký` (Trạng thái Phase: `REGISTRATION_OPEN`).
* **Tài khoản test:** `owner1`.
* **Các bước thực hiện:**
  1. Đăng nhập `owner1`. Vào mục **Quản lý Ngựa**: Kiểm tra danh sách chú ngựa (`RegHorse01`, `RegHorse02`).
  2. Vào danh sách Giải đấu, chọn giải `DEMO 5 - Đang mở đăng ký`.
  3. Bấm **Đăng ký tham gia (Register Horse)** -> Chọn chú ngựa `RegHorse01`.
  4. Hệ thống sinh hóa đơn phí đăng ký (Registration Invoice: 500,000 VND).
  5. Bấm **Thanh toán ngay bằng Ví**: Hệ thống trừ 500,000 VND từ ví Owner, chuyển trạng thái đăng ký thành `PENDING_REVIEW`.

---

### 2.3 Luồng 03 - Jockey đăng ký giải đấu (Phụ trách: KHung)
* **Giải đấu:** `DEMO 5 - Đang mở đăng ký` (Trạng thái Phase: `REGISTRATION_OPEN`).
* **Tài khoản test:** `jockey13`.
* **Các bước thực hiện:**
  1. Đăng nhập bằng tài khoản Kỵ sĩ `jockey13`.
  2. Truy cập danh sách giải đấu, chọn giải `DEMO 5 - Đang mở đăng ký`.
  3. Bấm **Đăng ký tham gia (Register as Jockey)**.
  4. Xác nhận đăng ký. Trạng thái hồ sơ chuyển sang `PENDING_REVIEW`.
  5. (*Admin `admin1` vào danh sách hồ sơ giải `DEMO 5` bấm **Approve** cho cả Ngựa `RegHorse01` và Kỵ sĩ `jockey13`*).

---

### 2.4 Luồng 04 - Admin cấu hình giải đấu (Phụ trách: Hải)
* **Giải đấu:** `DEMO 4 - Cấu hình DRAFT` (Trạng thái Phase: `DRAFT`).
* **Tài khoản test:** `admin1`.
* **Các bước thực hiện:**
  1. Đăng nhập `admin1`. Truy cập **Quản lý Giải đấu (Tournament Management)**.
  2. Chọn giải `DEMO 4 - Cấu hình DRAFT` (hoặc Bấm **Tạo giải đấu mới**).
  3. Cấu hình thông số:
     - Tên giải, Khoảng cách đua (`1600m - MILE`), Hạng đua (`CLASS_4`).
     - Phí đăng ký (`500,000 VND`), Phí hợp đồng hệ thống (`100,000 VND`).
     - Tổng tiền thưởng (`20,000,000 VND`), Tỷ lệ thưởng Top 1/2/3 (`50% - 30% - 20%`).
     - Số lượng ngựa tối đa (`8` hoặc `16`).
  4. Bấm **Công bố / Mở đăng ký (Publish Tournament)** -> Trạng thái giải chuyển từ `DRAFT` sang `REGISTRATION_OPEN`.

---

### 2.5 Luồng 05 - Hợp đồng Chủ ngựa & Jockey (Contract Matching) (Phụ trách: P Hưng)
* **Giải đấu:** `DEMO 6 - Đang ghép Kỵ sĩ` (Trạng thái Phase: `JOCKEY_MATCHING`).
* **Tài khoản test:** `owner1`, `jockey9`, `admin1`.
* **Các bước thực hiện:**
  1. **Tạo đề xuất:** Đăng nhập `owner1`. Mở giải `DEMO 6`, truy cập **Tìm kiếm Kỵ sĩ (Jockey Search)**.
     - Chọn `jockey9`, nhập Phí thuê (Hire Fee: `5,000,000 VND`).
     - Bấm **Gửi đề xuất hợp đồng (Send Proposal)** -> Hợp đồng ở trạng thái `PENDING_JOCKEY`.
  2. **Jockey chấp nhận:** Đăng nhập `jockey9`. Vào mục **Quản lý Hợp đồng**, chọn lời mời từ `owner1`, bấm **Accept**.
     - Hợp đồng chuyển sang `PENDING_PAYMENT`, hệ thống tạo hóa đơn đặt cọc 30% (`1,500,000 VND`).
  3. **Thanh toán cọc 30%:** Đăng nhập `owner1`. Vào danh sách hóa đơn, bấm **Thanh toán Cọc 30%**.
     - Tiền cọc trừ từ Ví Owner và chuyển tạm giữ vào **Ví Ký quỹ Hệ thống (Escrow Wallet)**. Hợp đồng chuyển sang `PENDING_ADMIN`.
  4. **Admin duyệt:** Đăng nhập `admin1`. Vào mục **Duyệt Hợp đồng**, bấm **Approve** -> Trạng thái hợp đồng chuyển thành `APPROVED`.

---

### 2.7 Luồng 07 - Inspection (Kiểm tra sức khỏe Ngựa & Jockey) (Phụ trách: P Hưng)
* **Giải đấu:** `DEMO FULL 8 - Luồng 07 đến 11` (Trạng thái Phase: `RACING`).
* **Trận đấu:** `DEMO FULL 8 - Final Race` (Trạng thái Race: `SCHEDULED`).
* **Tài khoản test:** `vet1` (Thú y), `medical1` (Y tế).
* **Các bước thực hiện:**
  1. **Khám Ngựa:** Đăng nhập `vet1`. Chọn trận đấu `DEMO FULL 8 - Final Race`.
     - Nhập cân nặng thực tế, nhiệt độ, kiểm tra Doping (`NEGATIVE`).
     - Đánh giá Kết luận: **PASS** cho 8 chú ngựa (hoặc 1 ngựa FAIL để test loại entry).
  2. **Khám Jockey:** Đăng nhập `medical1`. Chọn trận đấu `DEMO FULL 8 - Final Race`.
     - Nhập cân nặng thực tế, huyết áp, kiểm tra Doping (`NEGATIVE`).
     - Đánh giá Kết luận: **PASS** cho các kỵ sĩ.

---

### 2.8 Luồng 08 - Vận hành Race (Start, Result, Report) (Phụ trách: Hải)
* **Giải đấu:** `DEMO FULL 8 - Luồng 07 đến 11`.
* **Tài khoản test:** `referee2` (Trọng tài đua), `referee1` (Trọng tài chính).
* **Các bước thực hiện:**
  1. **Start Race:** Đăng nhập `referee2`. Mở trận `DEMO FULL 8 - Final Race`. Đảm bảo kiểm tra sức khỏe đã hoàn tất. Bấm **Start Race** -> Race chuyển sang `ONGOING`.
  2. **Nhập Kết quả:** Nhập thời gian chạy (ví dụ `01:35.20`) và Thứ hạng từ `1` đến `8` cho các làn đua.
  3. **Kết thúc Race:** Bấm **Finish Race** -> Race chuyển sang `FINISHED`.
  4. **Tạo Báo cáo:** `referee2` bấm **Tạo Báo cáo Trận đấu (Draft Race Report)** và gửi Trọng tài chính -> Report trạng thái `SUBMITTED`.

---

### 2.9 Luồng 09 - Vi phạm và Khiếu nại (Phụ trách: KHung)
* **Giải đấu:** `DEMO FULL 8` (trong hoặc ngay sau khi đua).
* **Tài khoản test:** `referee2`, `owner1`, `referee1`.
* **Các bước thực hiện:**
  1. **Ghi nhận Vi phạm:** Đăng nhập `referee2`. Trong trận đấu, ghi nhận 1 vi phạm cho Làn 4 (ví dụ: `LANE_DEVIATION` - Chèn làn), chọn mức phạt `Cảnh cáo (WARNING)` hoặc `Bị loại (DISQUALIFIED)`.
  2. **Gửi Khiếu nại:** Đăng nhập `owner1` (Chủ của ngựa bị phạt hoặc chịu ảnh hưởng). Mở trang **Khiếu nại (Appeals)**, bấm **Tạo Khiếu nại**.
     - Chọn trận đấu, nhập lý do khiếu nại và gắn link bằng chứng. Trạng thái `PENDING`.
  3. **Xử lý Khiếu nại:** Đăng nhập Trọng tài chính `referee1`. Mở mục **Xử lý Khiếu nại**, xem xét bằng chứng, bấm **Chấp nhận (Accept)** hoặc **Từ chối (Reject)** và nhập kết luận giải quyết.

---

### 2.10 Luồng 10 - Dự đoán của Khán giả (Spectator Prediction) (Phụ trách: Hải)
* **Giải đấu:** `DEMO FULL 8 - Luồng 07 đến 11`.
* **Thời điểm:** Thực hiện trước khi Trọng tài bấm Start Race.
* **Tài khoản test:** `spectator1`.
* **Các bước thực hiện:**
  1. Đăng nhập `spectator1`. Vào mục **Dự đoán / Đặt cược (Spectator Predictions)**.
  2. Chọn trận `DEMO FULL 8 - Final Race`.
  3. Chọn 3 chú ngựa dự đoán cho Hạng 1, Hạng 2, Hạng 3.
  4. Bấm **Gửi dự đoán**. Trạng thái hiển thị `SUBMITTED` (chờ trận đấu kết thúc và công bố báo cáo để tính điểm).

---

### 2.11 Luồng 11 - Thanh toán Giải thưởng & Kết toán (Prize Payout) (Phụ trách: P Hưng)
* **Giải đấu:** `DEMO FULL 8 - Luồng 07 đến 11`.
* **Thời điểm:** Sau khi Trận đấu kết thúc và Khiếu nại đã xử lý xong.
* **Tài khoản test:** `referee1` (Trọng tài chính), `admin1` (Admin).
* **Các bước thực hiện:**
  1. **Trọng tài chính Ký Báo cáo:** Đăng nhập `referee1`. Mở Race Report ở trạng thái `SUBMITTED`, kiểm tra toàn bộ kết quả & khiếu nại, bấm **Ký Báo cáo (Sign Report)** -> Report chuyển `SIGNED`.
  2. **Admin Publish & Tự động Trả thưởng:** Đăng nhập `admin1`. Mở Race Report đã `SIGNED`, bấm **Xem trước Rating (Preview)** và bấm **Publish Report**.
  3. **Hệ thống tự động thực hiện 5 tác vụ:**
     - **Chia giải thưởng Top 1, 2, 3:** Ví Owner nhận 80% tiền thưởng, Ví Jockey nhận 20% tiền thưởng.
     - **Giải phóng Ví Ký quỹ (Escrow):** Chuyển 70% phí thuê kỵ sĩ còn lại từ Ví Ký quỹ về Ví của tất cả Jockey tham gia thi đấu.
     - **Tính điểm Dự đoán Khán giả:** Tự động chấm điểm cho `spectator1` và cộng điểm thưởng vào hồ sơ.
     - **Cập nhật Rating:** Cập nhật điểm Rating mới cho ngựa và lưu lịch sử `horse_rating_histories`.
     - **Chuyển trạng thái:** Trận đấu & Vòng đấu chuyển `COMPLETED`.
  4. **Kiểm tra Ví:** Đăng nhập `owner1`, `jockey1`, `spectator1` để kiểm tra biến động số dư và lịch sử giao dịch ví.
