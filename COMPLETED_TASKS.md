# Các công việc đã hoàn thành — BE1 — Thắt chặt nghiệp vụ tài chính & bảo mật luồng tiền thưởng

Dưới đây là chi tiết toàn bộ các thay đổi và tối ưu hóa đã thực hiện để hoàn tất tính năng BE1:

---

## 1. Kiểm soát trạng thái & Khóa kết quả lượt đua (RaceResult Lock)
- **File sửa đổi**: [RaceResultServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceResultServiceImpl.java)
- **Chi tiết**:
  - Giới hạn quyền ghi/sửa kết quả: Cả hai phương thức `createResults` (nhập kết quả nháp) và `updateResults` (cập nhật kết quả nháp) chỉ được phép thực hiện khi trạng thái của Race là `ONGOING`.
  - Chặn sửa kết quả khi report đã được xác nhận: Kiểm tra sự tồn tại của báo cáo lượt đua, nếu báo cáo đã ở trạng thái `Signed` hoặc `Published`, hệ thống sẽ ném lỗi tương ứng `RACE_REPORT_ALREADY_SIGNED` hoặc `RACE_REPORT_ALREADY_PUBLISHED`.
  - Loại bỏ hoàn toàn việc gọi `race.setStatus(RoundStatus.ONGOING)` bên trong các API nhập kết quả nháp, nhường quyền kiểm soát trạng thái lượt đua cho startRace() và luồng ký duyệt báo cáo.

## 2. Kiểm tra kết quả đầy đủ trước khi ký báo cáo (Sign Report Validation)
- **File sửa đổi**: [RaceReportServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceReportServiceImpl.java)
- **Chi tiết**:
  - Triển khai phương thức helper `validateRaceResultsBeforeSigning` được gọi trước khi Head Referee ký duyệt báo cáo (`signReport`).
  - Kiểm tra tính đầy đủ của kết quả đối với từng ngựa tham gia lượt đua (`RaceEntry`):
    - Tất cả ngựa có trạng thái thực sự xuất phát (không phải `SCRATCHED`, `WITHDRAWN_BEFORE_SCHEDULE`, hoặc `WITHDRAWN_AFTER_SCHEDULE`) bắt buộc phải có đúng 1 bản ghi `RaceResult`.
    - Kết quả phải có trạng thái hợp lệ (`FINISHED`, `DID_NOT_FINISH`, hoặc `DISQUALIFIED`).
    - Nếu trạng thái là `FINISHED`, bắt buộc phải có thông tin thời gian hoàn thành (`finishTime`) và thứ hạng (`rank`).
  - Khi ký duyệt thành công, trạng thái Race chuyển sang `FINISHED` để khóa kết quả.

## 3. Hoàn tiền hợp đồng nguyên tử (Atomic Rejection Refund)
- **File sửa đổi**: [ContractServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/ContractServiceImpl.java)
- **Chi tiết**:
  - Đánh dấu `@Transactional` cho phương thức từ chối hợp đồng của Admin (`rejectContractByAdmin`) để đảm bảo tính nguyên tử.
  - Sử dụng `Optional` để truy vấn hóa đơn thuê nài ngựa (`JOCKEY_HIRING_FEE`) và hóa đơn tạo hợp đồng (`CONTRACT_CREATION_FEE`). Nếu hóa đơn không tồn tại hoặc chưa thanh toán (`UNPAID`), bỏ qua an toàn mà không ném lỗi.
  - Chỉ cập nhật trạng thái thanh toán và lưu ký (`paymentStatus` / `escrowStatus`) của hợp đồng thành `REFUNDED` nếu hóa đơn thuê nài ngựa đã thanh toán (`PAID`) và hoàn trả thành công. Bất kỳ lỗi phát sinh nào từ `paymentService.refundInvoice` đều làm rollback toàn bộ giao dịch.

## 4. Đồng bộ hóa mapping trả thưởng & Tránh sai lệch tiền do làm tròn
- **Files sửa đổi**:
  - [RaceResultMapper.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/mapper/RaceResultMapper.java)
  - [RaceReportServiceImpl.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/main/java/com/swp391/horseracing/service/impl/RaceReportServiceImpl.java)
- **Chi tiết**:
  - Thêm cấu hình `@Mapping(target = "isPrizePaid", source = "prizePaid")` vào MapStruct Mapper để giải quyết triệt để cảnh báo unmapped property khi compile và đảm bảo API response trả về đúng trạng thái `isPrizePaid = true` sau khi payout.
  - Trong phương thức chia thưởng `payoutPrizeIfFinal`, tiền thưởng của Nài ngựa được tính bằng phép trừ: `jockeyAmount = totalPrizeAmount - ownerAmount` thay vì nhân phần trăm riêng biệt, loại bỏ hoàn toàn khả năng bị lệch 1 cent do làm tròn số lẻ.
  - Sử dụng `findForUpdateByRace_RaceId` khóa bi quan `RaceReport` và các dòng `RaceResult` trong DB để chống concurrent requests.

## 5. Viết bộ kiểm thử tự động (Unit Test Suite)
- **File mới**: [BE1FinanceHardeningTest.java](file:///d:/FPTU/Semester-5/SWP391/HorseRacing-BE/src/test/java/com/swp391/horseracing/service/BE1FinanceHardeningTest.java)
- **Chi tiết**:
  - Thiết lập 5 kịch bản kiểm thử Mockito độc lập để kiểm chứng:
    1. `testPublishReport_HappyPathFinalRace`: Payout thành công, chia đúng tỷ lệ và làm tròn khớp số tiền, giải ngân escrow.
    2. `testPublishReport_InvalidFinalRoundConfiguration`: Báo cáo bị từ chối nếu số lượt đua ở vòng chung kết khác 1.
    3. `testPublishReport_InsufficientBalanceRollback`: Kiểm tra số dư ví hệ thống thiếu tiền làm rollback.
    4. `testRejectContractByAdmin_AtomicRefundHappyPath`: Từ chối hợp đồng và hoàn tiền atomically.
    5. `testRejectContractByAdmin_RefundFailureThrowsException`: Lỗi hoàn tiền một hóa đơn gây rollback toàn bộ trạng thái.
  - Chạy lệnh `mvn test` xác nhận tất cả 5 test case đều **đạt 100% (Green)**.
