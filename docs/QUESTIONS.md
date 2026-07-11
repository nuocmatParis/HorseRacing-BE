# Giải đáp các câu hỏi nghiệp vụ (Section 18 trong PLAN.md)

Dưới đây là các phương án thiết kế đề xuất để xử lý các điểm chưa chốt trong tài liệu:

1. **Sau T-30, entry thiếu inspection tự scratch hoàn toàn bằng scheduler hay cần head referee xác nhận?**
   - **Giải pháp**: Tự động chuyển trạng thái entry thiếu kiểm dịch thành `SCRATCHED` thông qua logic tự động chốt (`finalizeRaceEntries`) khi đến hạn kiểm dịch hoặc khi Trọng tài gọi API bắt đầu race (`startRace`). Không cần Trọng tài bấm xác nhận thủ công để tối ưu hóa quy trình.

2. **Reschedule sau khi đã khám có bắt buộc tái khám hay dựa vào độ lệch thời gian?**
   - **Giải pháp**: Khi thay đổi lịch đua, toàn bộ cửa sổ kiểm dịch (`inspectionOpenAt`/`inspectionCloseAt`) sẽ được tính toán lại. Các kết quả kiểm dịch trước đó vẫn được bảo lưu nếu vẫn nằm trong khoảng thời gian hợp lệ mới, hoặc hệ thống sẽ yêu cầu khám lại nếu cửa sổ thời gian thay đổi quá nhiều.

3. **Khi disqualified, official rank luôn null hay vẫn lưu rank trước khi bị loại?**
   - **Giải pháp**: Khi một entry bị loại (`DISQUALIFIED`), thứ hạng chính thức (`finish_position` / `rank`) sẽ để là `null` hoặc 0, và thời gian hoàn thành (`finishTime`) cũng là `null`.

4. **Có đồng bộ result status về `RaceEntryStatus` hay chỉ đọc từ `RaceResult`?**
   - **Giải pháp**: Đồng bộ hoàn toàn. Khi Trọng tài nhập kết quả đua, hệ thống tự động cập nhật trạng thái `RaceEntryStatus` tương ứng (`FINISHED`, `DID_NOT_FINISH`, hoặc `DISQUALIFIED`).

5. **Nhân viên khám (Staff) được giải phóng (release) lúc nào?**
   - **Giải pháp**: Giải phóng (đổi trạng thái về `AVAILABLE`) ngay khi lượt đua bắt đầu (`ONGOING`) vì khi đó giai đoạn kiểm dịch trước trận đấu đã kết thúc, giúp nhân viên có thể được phân công cho các lượt đua tiếp theo.
