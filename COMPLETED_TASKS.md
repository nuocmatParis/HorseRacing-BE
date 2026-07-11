# Tổng hợp các phần đã lập trình & chỉnh sửa (SWP391 Horse Racing BE)

Dưới đây là tổng hợp toàn bộ các chỉnh sửa và tính năng đã được triển khai thành công trong phiên làm việc này để giải quyết triệt để 9 vấn đề còn tồn đọng:

---

## 1. Tự động chuyển trạng thái Race sang `FINISHED`
- **File sửa đổi**: [RaceResultServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceResultServiceImpl.java)
- **Chi tiết**: Tích hợp logic `race.setStatus(RoundStatus.FINISHED);` vào cả hai phương thức `createResults` (nhập kết quả lần đầu) và `updateResults` (cập nhật kết quả). Điều này đảm bảo trạng thái Race sẽ chuyển sang `FINISHED` ngay sau khi trọng tài nhập kết quả tạm thời, giúp luồng publish report, tính điểm dự đoán và giải ngân diễn ra bình thường.

## 2. Giải ngân Escrow diện rộng cho toàn bộ giải đấu (Tournament Scope)
- **File sửa đổi**: [RaceReportServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceReportServiceImpl.java)
- **Chi tiết**: Thêm kiểm tra tại thời điểm publish báo cáo chính thức của lượt đua vòng chung kết:
  - Nếu tất cả các trận đấu của vòng chung kết (`round.isFinal()`) đã hoàn thành (`COMPLETED` hoặc `CANCELLED`).
  - Hệ thống tự động truy vấn toàn bộ hợp đồng trong giải đấu (`Tournament`) có trạng thái là `APPROVED` và `PARTIALLY_RELEASED`.
  - Thực hiện giải ngân 30% số tiền cọc (hire fee) còn lại cho tất cả các Jockey bị loại từ vòng bảng, tránh việc giữ tiền của họ vô hạn.

## 3. Ràng buộc dời lịch thi đấu (Reschedule / Postpone Constraints)
- **File sửa đổi**: [RaceServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceServiceImpl.java)
- **Chi tiết**:
  - **Thứ tự vòng đấu (Round Order)**: Không cho phép dời lịch thi đấu của vòng trước muộn hơn giờ bắt đầu của vòng sau nếu vòng sau đã bắt đầu chạy (`startedAt != null`).
  - **Thời gian nghỉ của Ngựa (Horse Rest Time)**: Đảm bảo khoảng cách giữa các trận đấu của cùng một ngựa tối thiểu là **60 phút** (giữa giờ kết thúc trận trước và giờ bắt đầu trận sau).
  - **Không trùng lịch**: Kiểm tra xung đột lịch trình cho Ngựa, Jockey, Trọng tài (không trùng lặp thời gian chạy) cũng như Veterinarian, Medical Staff (không trùng lặp cửa sổ kiểm dịch `T-90` đến `T-30`).
  - **Xóa kết quả khám cũ khi hoãn**: Tự động xóa các bản ghi `HorseInspection` và `JockeyInspection` cũ của trận đấu khi thực hiện hoãn, đồng thời reset trạng thái của entry về `CONFIRMED` để cho phép thực hiện khám lại ở lịch trình mới mà không bị lỗi trùng lặp bản ghi.
  - **Bảo toàn cổng dự đoán**: Nếu cổng dự đoán (`predictionOpenAt`) đã mở trong quá khứ thì giữ nguyên mốc cũ để tránh mở lại cổng dự đoán đã đóng.
  - **Gửi thông báo**: Gửi thông báo đến toàn bộ Spectators đã thực hiện dự đoán trận đấu đó để cập nhật lịch trình mới.

## 4. Thuật toán đề xuất khung giờ thi đấu trống (Reschedule Proposals)
- **File sửa đổi**:
  - [RaceService.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/RaceService.java)
  - [RaceServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceServiceImpl.java)
  - [AdminController.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/controller/AdminController.java)
- **Chi tiết**: 
  - Thêm API `GET /api/admin/races/{raceId}/reschedule-proposals`.
  - Triển khai thuật toán tìm kiếm thông minh: Duyệt qua các ngày trong khoảng thời gian diễn ra giải đấu, chia nhỏ thời gian hoạt động (`08:00` - `18:00`) thành các slot 30 phút.
  - Chạy thử toàn bộ các ràng buộc lập lịch (daily limit, break time, interval) và xung đột lịch thi đấu nhân sự. Nếu slot nào vượt qua toàn bộ kiểm tra, slot đó sẽ được thêm vào danh sách đề xuất (tối đa 10 đề xuất).

## 5. Tự động hóa tính toán giờ kết thúc & Chặn chạy vắt qua nửa đêm (Midnight Cross Check)
- **File sửa đổi**:
  - [CreateRaceRequest.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/dto/tournament/request/CreateRaceRequest.java)
  - [UpdateRaceRequest.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/dto/tournament/request/UpdateRaceRequest.java)
  - [RescheduleRaceRequest.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/dto/tournament/request/RescheduleRaceRequest.java)
  - [RaceServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceServiceImpl.java)
- **Chi tiết**:
  - Loại bỏ các trường do hệ thống tự quản lý như `endTime`, `status`, `schedulePublishedAt`, `predictionOpenAt`, `predictionCloseAt` khỏi các request tạo/cập nhật/hoãn lịch.
  - Server tự động tính toán `endTime = startTime + operationalMinutes`.
  - Chặn các trận đấu có thời gian bắt đầu và kết thúc nằm ở 2 ngày khác nhau (chạy qua nửa đêm).

## 6. Chặn hủy trận đấu đã hoàn tất
- **File sửa đổi**: [RaceServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceServiceImpl.java)
- **Chi tiết**: Ném ngoại lệ `INVALID_RACE_RESULT_STATUS` nếu cố gắng hủy (`cancelRace`) một trận đấu đã ở trạng thái `FINISHED` hoặc `COMPLETED`.

## 7. Cho phép gửi khiếu nại trước khi nhập kết quả (Appeal Without RaceResult)
- **File sửa đổi**: [AppealServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/AppealServiceImpl.java)
- **Chi tiết**: Loại bỏ ràng buộc bắt buộc phải có kết quả trận đấu `RaceResult` mới được tạo appeal. Điều này giúp Owner và Jockey có thể gửi khiếu nại về hành vi vi phạm ngay sau khi trận đấu bắt đầu, tránh việc hết hạn nộp đơn khi trọng tài nhập kết quả muộn.

## 8. Script Migration làm sạch Enum trong cơ sở dữ liệu
- **File mới**: [patch_enum_values.sql](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/patch_enum_values.sql)
- **Chi tiết**: Cung cấp script SQL hỗ trợ cập nhật toàn bộ các dữ liệu cũ (dạng mixed-case như `Finished`, `Disqualified`) sang dạng `UPPERCASE` để tương thích hoàn toàn với cấu hình map enum mới của Hibernate, tránh lỗi deserialize ở runtime.
