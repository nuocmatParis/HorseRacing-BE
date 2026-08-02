# 1. Horse Racing Tournament Management System — Backend

Horse Racing Tournament Management System (HRTMS) là hệ thống quản lý giải đua ngựa từ giai đoạn đăng ký, ghép kỵ sĩ, lập lịch thi đấu, kiểm tra sức khỏe, vận hành cuộc đua đến công bố kết quả và thanh toán giải thưởng.

Repository này chứa Backend REST API của hệ thống, chịu trách nhiệm xử lý nghiệp vụ, phân quyền, lưu trữ dữ liệu, thanh toán và thông báo theo thời gian thực.

---

## 2. Chức năng chính

### Tài khoản và phân quyền

- Đăng ký tài khoản bằng email và OTP.
- Đăng nhập và xác thực bằng JWT.
- Quản lý hồ sơ theo vai trò:
  - Admin
  - Horse Owner
  - Jockey
  - Spectator
  - Referee
  - Veterinarian
  - Medical Staff

### Quản lý giải đấu

- Tạo và cấu hình Tournament.
- Cấu hình timeline cho từng giai đoạn của giải.
- Tự động tính bracket, round và race.
- Tự động đề xuất lịch thi đấu.
- Quản lý điều kiện tham gia và cơ cấu giải thưởng.
- Cấu hình khoảng điểm Horse Rating riêng cho từng giải.

### Đăng ký và hợp đồng

- Owner đăng ký ngựa tham gia Tournament.
- Jockey đăng ký tham gia Tournament.
- Owner gửi lời mời thuê Jockey.
- Jockey chấp nhận, từ chối hoặc hủy hợp đồng hợp lệ.
- Kiểm soát một ngựa hoặc một Jockey không có nhiều hợp đồng hiệu lực trong cùng Tournament.
- Thanh toán phí thuê, phí hợp đồng và quản lý tiền escrow.
- Giải ngân trước và giải ngân phần còn lại sau Final Race.

### Lập lịch thi đấu

- Phân contract vào Race.
- Tự động hoặc thủ công phân lane.
- Phân công Race Referee.
- Phân công Head Referee cho Round.
- Phân công Veterinarian và Medical Staff.
- Publish lịch thi đấu.
- Kiểm tra trùng lịch và thời gian nghỉ giữa các Race/Round.

### Inspection

- Veterinarian kiểm tra ngựa.
- Medical Staff kiểm tra Jockey.
- Ghi nhận cân nặng thực tế, giống thực tế và kết quả doping.
- Hỗ trợ PASS/FAIL và handicap nếu Tournament áp dụng.
- Chỉ entry đủ điều kiện mới được tham gia Race.

### Vận hành Race

- Race Referee kiểm tra readiness và bắt đầu Race.
- Ghi nhận vi phạm và hình phạt.
- Nhập hoặc cập nhật kết quả thi đấu.
- Hỗ trợ kết quả `FINISHED` và `DISQUALIFIED`.
- Race Referee tạo và gửi Race Report.
- Head Referee xem xét, điều chỉnh và ký Race Report.
- Admin publish kết quả chính thức.

### Khiếu nại

- Owner và Jockey gửi khiếu nại sau Race.
- Đính kèm bằng chứng dạng URL, hình ảnh hoặc nội dung văn bản.
- Race Referee xử lý khiếu nại trước khi gửi Report.
- Head Referee xem lịch sử khiếu nại và điều chỉnh kết quả khi cần.
- Không được ký hoặc publish Report khi còn khiếu nại chưa xử lý.

### Prediction và AI Prediction

- Spectator dự đoán Top 3 cho từng Race.
- Mỗi Race có thời gian đóng prediction riêng.
- Tự động chấm điểm khi Race Report được publish.
- Hỗ trợ điểm đúng ngựa, đúng vị trí và perfect bonus.
- Admin có thể tạo và publish AI Prediction cho Race.

---

## 3. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4 |
| REST API | Spring Web MVC |
| Database | MySQL 8 |
| Realtime Notification | WebSocket, STOMP |
| Email | Spring Mail |
| Payment Gateway | VNPay Sandbox |
| AI Prediction | OpenAI API |
| Media Storage | Cloudinary |
| API Documentation | Springdoc OpenAPI, Swagger UI |

---

## 4. Yêu cầu môi trường

Trước khi chạy project, cần cài đặt:

| Công cụ | Phiên bản |
|---|---:|
| Java JDK | 25 |
| Maven | 3.9 trở lên |
| MySQL | 8.x |
| IntelliJ IDEA | Khuyến nghị |
| Postman | Tùy chọn |
| ngrok | Tùy chọn |

Kiểm tra Java:

```powershell
java -version
```

Kiểm tra Maven:

```powershell
mvn -version
```

Project được compile với Java 25.

---

## 5. Biến môi trường

| Biến | Ý nghĩa |
|---|---|
| `DB_URL` | JDBC URL kết nối MySQL |
| `DB_USERNAME` | Username MySQL |
| `DB_PASSWORD` | Password MySQL |
| `JWT_SIGNER_KEY` | Khóa ký và xác thực JWT |
| `MAIL_USERNAME` | Email dùng để gửi OTP và notification |
| `MAIL_PASSWORD` | Gmail App Password |
| `VNPAY_TMN_CODE` | Mã merchant VNPay sandbox |
| `VNPAY_HASH_SECRET` | Secret dùng để ký request VNPay |
| `VNPAY_PAY_URL` | Endpoint thanh toán VNPay |
| `VNPAY_RETURN_URL` | Backend callback URL sau thanh toán |
| `VNPAY_IPN_URL` | Backend IPN URL của VNPay |
| `VNPAY_FRONTEND_RETURN_URL` | Trang FE nhận kết quả thanh toán |
| `OPENAI_API_KEY` | API key dùng cho AI Prediction |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Danh sách origin được gọi REST API |
| `WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` | Danh sách origin được kết nối WebSocket |

---

## 6. Chạy và kiểm tra project

### Chạy ứng dụng

```powershell
mvn spring-boot:run
```

Sau khi khởi động thành công:

| Dịch vụ | URL |
|---|---|
| Backend API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| WebSocket endpoint | `http://localhost:8080/ws` |

## 7. Cấu trúc project

```text
HorseRacing-BE
├── .env.example
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── swp391
    │   │           └── horseracing
    │   │               ├── config
    │   │               ├── controller
    │   │               ├── dto
    │   │               ├── entity
    │   │               ├── enums
    │   │               ├── exception
    │   │               ├── mapper
    │   │               ├── policy
    │   │               ├── repository
    │   │               ├── scheduler
    │   │               ├── service
    │   │               │   └── impl
    │   │               ├── validation
    │   │               └── websocket
    │   └── resources
    │       ├── application.properties
    │       └── db
    │           └── migration
    └── test
        ├── java
        │   └── com
        │       └── swp391
        │           └── horseracing
        │               ├── controller
        │               ├── policy
        │               └── service
        └── resources
            └── application-test.properties
