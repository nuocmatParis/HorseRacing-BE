# Backend Setup Guide - Spring Boot

Tài liệu này hướng dẫn cách setup project Backend để mọi người trong team có thể clone repo về, cài dependency, cấu hình database và chạy project trên máy local.

---

## 1. Yêu cầu cài đặt trước

Trước khi chạy project, cần cài các công cụ sau:

| Công cụ | Phiên bản khuyến nghị | Ghi chú |
|---|---:|---|
| Java JDK | 17 trở lên | Project Spring Boot nên dùng JDK  |
| IntelliJ IDEA | Community hoặc Ultimate | Dùng để mở project |
| Maven | Có thể dùng Maven Wrapper | Nếu repo có `mvnw` thì không cần cài Maven riêng |
| MySQL | 8.x | Dùng làm database |
| Git | Latest | Dùng để clone source code |
| Postman | Optional | Dùng để test API |

Kiểm tra phiên bản Java:

```bash
java -version
```

Kiểm tra Git:

```bash
git --version
```

---

## 2. Clone project

Clone repo Backend về máy:

```bash
git clone <backend-repo-url>
```

Di chuyển vào thư mục project:

```bash
cd <backend-project-folder>
```

Ví dụ:

```bash
cd horse-racing-backend
```

---

## 3. Mở project bằng IntelliJ IDEA

1. Mở IntelliJ IDEA.
2. Chọn **Open**.
3. Chọn thư mục backend vừa clone.
4. Đợi IntelliJ import Maven dependencies.
5. Kiểm tra Project SDK đang dùng **JDK 25**.

Nếu IntelliJ chưa nhận JDK:

```text
File > Project Structure > Project SDK > chọn JDK 17
```

---

## 4. Cấu hình database MySQL

Tạo database trong MySQL:

```sql
CREATE DATABASE horse_racing_db;
```

Có thể đổi tên database tùy theo project, nhưng phải khớp với file cấu hình Spring Boot.

---

## 5. Cấu hình `application.properties`

Mở file:

```text
src/main/resources/application.properties
```

Cấu hình mẫu:

```properties
spring.application.name=horse-racing-backend

spring.datasource.url=jdbc:mysql://localhost:3306/horse_racing_db
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

server.port=8080
```

Giải thích nhanh:

| Config | Ý nghĩa |
|---|---|
| `spring.datasource.url` | Đường dẫn kết nối MySQL |
| `spring.datasource.username` | Username MySQL |
| `spring.datasource.password` | Password MySQL |
| `spring.jpa.hibernate.ddl-auto=update` | Tự update bảng theo Entity |
| `server.port=8080` | Backend chạy ở port 8080 |

Lưu ý: Không nên push password thật lên GitHub. Nếu cần, tạo file `.env` hoặc dùng biến môi trường.

## 7. Cài dependencies

Nếu repo có Maven Wrapper, chạy:

### Windows

```bash
mvnw.cmd clean install
```

### macOS / Linux

```bash
./mvnw clean install
```

Nếu máy đã cài Maven:

```bash
mvn clean install
```

---

## 8. Chạy project

Có 2 cách chạy project.

### Cách 1: Chạy bằng IntelliJ

Mở class main, thường có dạng:

```text
src/main/java/.../Application.java
```

Ví dụ:

```java
@SpringBootApplication
public class HorseRacingBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(HorseRacingBackendApplication.class, args);
    }
}
```

Bấm nút **Run**.

### Cách 2: Chạy bằng terminal

Nếu dùng Maven Wrapper:

```bash
mvnw.cmd spring-boot:run
```

Hoặc:

```bash
mvn spring-boot:run
```

Nếu chạy thành công, terminal sẽ hiển thị tương tự:

```text
Tomcat started on port 8080
Started HorseRacingBackendApplication
```

---

## 9. Test API

Sau khi chạy project, backend sẽ chạy ở:

```text
http://localhost:8080
```

Ví dụ test API:

```text
GET http://localhost:8080/api/health
```

Hoặc nếu có Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

---

## 10. Cấu trúc thư mục đề xuất

Cấu trúc package backend nên chia theo layer:

```text
src/main/java/com/example/horseracing
│
├── config
│   └── SecurityConfig.java
│
├── controller
│   └── HorseController.java
│
├── dto
│   ├── request
│   │   ├── HorseCreateRequest.java
│   │   └── HorseUpdateRequest.java
│   └── response
│       └── HorseResponse.java
│
├── entity
│   └── Horse.java
│
├── exception
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
│
├── mapper
│   └── HorseMapper.java
│
├── repository
│   └── HorseRepository.java
│
├── service
│   ├── HorseService.java
│   └── impl
│       └── HorseServiceImpl.java
│
└── HorseRacingBackendApplication.java
```

Ý nghĩa các package:

| Package | Vai trò |
|---|---|
| `controller` | Nhận request từ FE |
| `service` | Xử lý logic nghiệp vụ |
| `repository` | Làm việc với database |
| `entity` | Mapping bảng trong database |
| `dto` | Dữ liệu request/response với FE |
| `mapper` | Chuyển đổi Entity ↔ DTO |
| `config` | Cấu hình Spring Security, CORS, JWT |
| `exception` | Xử lý lỗi tập trung |

---

## 11. Quy tắc làm việc với Git

Không code trực tiếp trên branch `main`.

Quy trình đề xuất:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/ten-chuc-nang
```

Ví dụ:

```bash
git checkout -b feature/horse-crud
```

Sau khi code xong:

```bash
git add .
git commit -m "feat: implement horse CRUD API"
git push origin feature/horse-crud
```

Sau đó tạo Pull Request vào branch `develop`.

---

## 12. Quy tắc đặt tên branch

| Loại task | Cách đặt tên |
|---|---|
| Tính năng mới | `feature/ten-chuc-nang` |
| Sửa lỗi | `fix/ten-loi` |
| Cấu hình | `config/ten-cau-hinh` |
| Refactor code | `refactor/ten-phan-code` |

Ví dụ:

```text
feature/auth-login
feature/horse-management
fix/jwt-token-error
config/cors-security
```

---

## 13. Quy tắc commit message

Nên dùng format:

```text
type: nội dung commit
```

Ví dụ:

```text
feat: create horse entity and repository
fix: resolve database connection error
refactor: separate horse dto into request and response
config: add cors configuration
docs: update backend setup guide
```

Một số type thường dùng:

| Type | Ý nghĩa |
|---|---|
| `feat` | Thêm chức năng mới |
| `fix` | Sửa lỗi |
| `refactor` | Tối ưu code, không đổi chức năng |
| `config` | Cấu hình project |
| `docs` | Sửa tài liệu |
| `test` | Thêm hoặc sửa test |

---

## 14. Quy tắc API với FE

Backend nên thống nhất endpoint dạng:

```text
/api/<resource>
```

Ví dụ:

```text
GET    /api/horses
GET    /api/horses/{id}
POST   /api/horses
PUT    /api/horses/{id}
DELETE /api/horses/{id}
```

Quy tắc DTO:

| API | Request body có id không? | Response có id không? |
|---|---:|---:|
| POST create | Không | Có |
| PUT update | Không, id nằm trên URL | Có |
| GET detail | Không | Có |
| DELETE | Không | Có thể không cần body |

Ví dụ tạo mới Horse:

```json
{
  "name": "Lightning",
  "breed": "Arabian",
  "age": 5,
  "weight": 450,
  "color": "Black"
}
```

Ví dụ response:

```json
{
  "id": 1,
  "name": "Lightning",
  "breed": "Arabian",
  "age": 5,
  "weight": 450,
  "color": "Black",
  "healthStatus": "Healthy"
}
```

---

## 15. Cấu hình CORS cho FE gọi API

Nếu FE chạy ở:

```text
http://localhost:5173
```

Backend cần cho phép CORS.

Ví dụ config:

```java
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
```

Nếu dùng Spring Security, cần cấu hình CORS trong `SecurityConfig` nữa.

---

## 16. Các lỗi thường gặp

### Lỗi 1: Không connect được MySQL

Thông báo thường gặp:

```text
Communications link failure
```

Cách xử lý:

- Kiểm tra MySQL đã chạy chưa.
- Kiểm tra database đã tạo chưa.
- Kiểm tra username/password.
- Kiểm tra URL đúng chưa.

Đúng:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/horse_racing_db
```

Sai thường gặp:

```properties
spring.datasource.url=jdbc:mysql://localhost3306/horse_racing_db
```

Thiếu dấu `:` giữa `localhost` và `3306`.

---

### Lỗi 2: Maven không tải được dependency

Cách xử lý:

```bash
mvn clean install -U
```

Hoặc trong IntelliJ:

```text
Maven tab > Reload All Maven Projects
```

---

### Lỗi 3: Port 8080 đã được dùng

Đổi port trong `application.properties`:

```properties
server.port=8081
```

Hoặc tắt process đang dùng port 8080.

---

### Lỗi 4: Không tìm thấy main class

Cách xử lý:

- Kiểm tra class main có annotation `@SpringBootApplication`.
- Kiểm tra file nằm trong package gốc.
- Reload Maven project.

---

## 17. Checklist trước khi push code

Trước khi push code lên GitHub, kiểm tra:

- Project chạy được local.
- Không push password thật.
- Không push file rác như `.idea`, `target`, `.env`.
- API đã test bằng Postman.
- Code không bị lỗi compile.
- Tên branch đúng format.
- Commit message rõ ràng.

---

## 18. File `.gitignore` nên có

Ví dụ `.gitignore` cho Spring Boot:

```gitignore
target/
.idea/
*.iml
.env
logs/
*.log
.DS_Store
```

---

## 19. Gợi ý README ngắn cho người mới clone

Người mới chỉ cần chạy nhanh các bước sau:

```bash
git clone <backend-repo-url>
cd <backend-project-folder>
```

Tạo database:

```sql
CREATE DATABASE horse_racing_db;
```

Cấu hình password MySQL trong:

```text
src/main/resources/application.properties
```

Chạy project:

```bash
mvn spring-boot:run
```

Mở:

```text
http://localhost:8080
```

---

## 20. Ghi chú cho team

Khi có thay đổi cấu trúc database, entity, API hoặc rule xử lý, cần báo lại trong nhóm để FE và các thành viên backend khác update kịp.

Nếu thêm API mới, nên cập nhật tài liệu API hoặc gửi endpoint mẫu cho FE test.
