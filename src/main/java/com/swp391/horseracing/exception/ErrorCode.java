package com.swp391.horseracing.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum ErrorCode {
    // COMMON
    UNCATEGORIZED_EXCEPTION(9999, HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized error"),
// Lỗi không xác định

    INVALID_REQUEST(1001, HttpStatus.BAD_REQUEST, "Invalid request"),
// Request gửi lên không hợp lệ

    INVALID_INPUT(1002, HttpStatus.BAD_REQUEST, "Invalid input data"),
// Dữ liệu đầu vào không hợp lệ

    RESOURCE_NOT_FOUND(1003, HttpStatus.NOT_FOUND, "Resource not found"),
// Không tìm thấy tài nguyên được yêu cầu

    ACCESS_DENIED(1004, HttpStatus.FORBIDDEN, "Access denied"),
// Người dùng không có quyền truy cập chức năng này

    UNAUTHENTICATED(1005, HttpStatus.UNAUTHORIZED, "Unauthenticated"),
// Người dùng chưa đăng nhập hoặc chưa xác thực

    DUPLICATE_RESOURCE(1006, HttpStatus.CONFLICT, "Resource already exists"),
// Tài nguyên đã tồn tại trong hệ thống

    INVALID_STATUS_TRANSITION(1007, HttpStatus.BAD_REQUEST, "Invalid status transition"),
// Chuyển trạng thái không hợp lệ

    INTERNAL_SERVER_ERROR(1008, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
// Lỗi bên trong server

    INVALID_KEY(1009, HttpStatus.BAD_REQUEST, "Invalid error key"),
// Key lỗi không hợp lệ

    VALIDATION_FAILED(1010, HttpStatus.BAD_REQUEST, "Validation failed"),


    // AUTH / USER
    USER_NOT_FOUND(1101, HttpStatus.NOT_FOUND, "User not found"),
// Không tìm thấy người dùng

    USER_ALREADY_EXISTS(1102, HttpStatus.CONFLICT, "User already exists"),
// Người dùng đã tồn tại

    EMAIL_ALREADY_EXISTS(1103, HttpStatus.CONFLICT, "Email already exists"),
// Email đã được sử dụng

    USERNAME_ALREADY_EXISTS(1104, HttpStatus.CONFLICT, "Username already exists"),
// Username đã được sử dụng

    INVALID_USERNAME_OR_PASSWORD(1105, HttpStatus.UNAUTHORIZED, "Invalid username or password"),
// Sai username hoặc mật khẩu

    INVALID_TOKEN(1106, HttpStatus.UNAUTHORIZED, "Invalid token"),
// Token không hợp lệ

    EXPIRED_TOKEN(1107, HttpStatus.UNAUTHORIZED, "Token has expired"),
// Token đã hết hạn

    USER_INACTIVE(1108, HttpStatus.FORBIDDEN, "User account is inactive"),
// Tài khoản người dùng đang bị khóa hoặc không hoạt động

    ROLE_NOT_FOUND(1109, HttpStatus.NOT_FOUND, "Role not found"),
// Không tìm thấy vai trò người dùng

    ROLE_NOT_ALLOWED(1110, HttpStatus.BAD_REQUEST, "Role not allowed to choose"),
// Chọn role không cho phép

    CAN_NOT_CREATE_TOKEN(1111, HttpStatus.INTERNAL_SERVER_ERROR, "Can not create token"),
// Không thể tạo token

    PHONE_NUMBER_ALREADY_EXISTS(1112, HttpStatus.CONFLICT, "Phone number already exists"),

    EMAIL_VERIFICATION_NOT_FOUND(1113,HttpStatus.NOT_FOUND ,"Email verification not found"),

    OTP_EXPIRED(1114, HttpStatus.BAD_REQUEST,"OTP has expired"),

    INVALID_OTP(1115, HttpStatus.BAD_REQUEST,"Invalid OTP"),

    EMAIL_SEND_FAILED(1116, HttpStatus.INTERNAL_SERVER_ERROR,"Failed to send email"),

    USERNAME_PENDING_VERIFICATION(1117, HttpStatus.CONFLICT, "User pending verification"),


    // PROFILE
    OWNER_PROFILE_NOT_FOUND(1201, HttpStatus.NOT_FOUND, "Horse owner profile not found"),
// Không tìm thấy hồ sơ chủ ngựa

    JOCKEY_PROFILE_NOT_FOUND(1202, HttpStatus.NOT_FOUND, "Jockey profile not found"),
// Không tìm thấy hồ sơ nài ngựa

    SPECTATOR_PROFILE_NOT_FOUND(1203, HttpStatus.NOT_FOUND, "Spectator profile not found"),
// Không tìm thấy hồ sơ khán giả

    REFEREE_PROFILE_NOT_FOUND(1204, HttpStatus.NOT_FOUND, "Referee profile not found"),
// Không tìm thấy hồ sơ trọng tài

    VETERINARIAN_PROFILE_NOT_FOUND(1205, HttpStatus.NOT_FOUND, "Veterinarian profile not found"),
// Không tìm thấy hồ sơ bác sĩ thú y

    MEDICAL_STAFF_PROFILE_NOT_FOUND(1206, HttpStatus.NOT_FOUND, "Medical staff profile not found"),
// Không tìm thấy hồ sơ nhân viên y tế
   JOCKEY_NOT_AVAILABLE(1207, HttpStatus.BAD_REQUEST, "Jockey is not available for registration"),


    // HORSE
    HORSE_NOT_FOUND(1301, HttpStatus.NOT_FOUND, "Horse not found"),
// Không tìm thấy ngựa

    HORSE_NOT_BELONG_TO_OWNER(1302, HttpStatus.FORBIDDEN, "Horse does not belong to this owner"),
// Ngựa không thuộc về chủ ngựa hiện tại

    HORSE_NOT_ELIGIBLE(1303, HttpStatus.BAD_REQUEST, "Horse is not eligible for this tournament"),
// Ngựa không đủ điều kiện tham gia giải đấu này

    HORSE_HEALTH_NOT_VALID(1304, HttpStatus.BAD_REQUEST, "Horse health status is not valid"),
// Tình trạng sức khỏe của ngựa không hợp lệ

    HORSE_ALREADY_REGISTERED(1305, HttpStatus.CONFLICT, "Horse is already registered in this tournament"),
// Ngựa đã được đăng ký vào giải đấu này rồi
JOCKEY_NOT_ELIGIBLE(1306, HttpStatus.BAD_REQUEST, "Jockey is not eligible for this tournament"),


    // WALLET / PAYMENT
    WALLET_NOT_FOUND(1401, HttpStatus.NOT_FOUND, "Wallet not found"),
// Không tìm thấy ví

    SYSTEM_WALLET_NOT_FOUND(1402, HttpStatus.NOT_FOUND, "System wallet not found"),
// Không tìm thấy ví hệ thống

    INSUFFICIENT_BALANCE(1403, HttpStatus.BAD_REQUEST, "Insufficient wallet balance"),
// Số dư ví không đủ để thực hiện giao dịch

    INVALID_AMOUNT(1404, HttpStatus.BAD_REQUEST, "Invalid amount"),
// Số tiền không hợp lệ

    TRANSACTION_NOT_FOUND(1405, HttpStatus.NOT_FOUND, "Transaction not found"),
// Không tìm thấy giao dịch

    PAYMENT_FAILED(1406, HttpStatus.BAD_REQUEST, "Payment failed"),
// Thanh toán thất bại

    PAYMENT_ALREADY_COMPLETED(1407, HttpStatus.CONFLICT, "Payment already completed"),
// Thanh toán đã được hoàn tất trước đó

    REFUND_NOT_ALLOWED(1408, HttpStatus.BAD_REQUEST, "Refund is not allowed"),
// Không được phép hoàn tiền trong trường hợp này

    INVOICE_NOT_FOUND(1409, HttpStatus.NOT_FOUND, "Invoice not found"),
// Không tìm thấy hóa đơn

    INVOICE_ALREADY_PAID(1410, HttpStatus.CONFLICT, "Invoice is already paid"),
// Hóa đơn đã được thanh toán

    INVOICE_NOT_PAID(1411, HttpStatus.BAD_REQUEST, "Invoice is not paid"),
// Hóa đơn chưa được thanh toán

    INVOICE_CANCELLED(1412, HttpStatus.BAD_REQUEST, "Invoice has been cancelled"),
// Hóa đơn đã bị hủy

    WALLET_FROZEN(1415, HttpStatus.CONFLICT, "Wallet is frozen"),

    WALLET_CLOSED(1416, HttpStatus.CONFLICT, "Wallet is closed"),

    WALLET_ALREADY_EXISTS(1417, HttpStatus.CONFLICT, "Wallet already exists"),

    INVALID_SYSTEM_WALLET_PURPOSE(1418, HttpStatus.CONFLICT, "Invalid wallet purpose"),

    INVOICE_ALREADY_EXISTS(1419, HttpStatus.CONFLICT, "Invoice already exists"),

    PAID_INVOICE_CANNOT_BE_CANCELLED(1420, HttpStatus.CONFLICT, "Paid invoice cannot be cancelled"),

    INVOICE_ALREADY_REFUNDED(1421, HttpStatus.CONFLICT, "Invoice already refunded"),

    INVOICE_ACCESS_DENIED(1422, HttpStatus.FORBIDDEN, "Invoice access denied"),

    INVOICE_EXPIRED(1423, HttpStatus.CONFLICT, "Invoice expired"),

    PAYMENT_TRANSACTION_NOT_FOUND(1424, HttpStatus.NOT_FOUND, "Payment transaction not found"),

    INVALID_PAYMENT_AMOUNT(1425, HttpStatus.BAD_REQUEST, "Invalid payment amount"),

    INVALID_VNPAY_SIGNATURE(1426, HttpStatus.BAD_REQUEST, "Invalid Vnpay signature"),

    // TOURNAMENT / REGISTRATION
    TOURNAMENT_NOT_FOUND(1501, HttpStatus.NOT_FOUND, "Tournament not found"),
// Không tìm thấy giải đấu

    TOURNAMENT_NOT_OPEN(1502, HttpStatus.BAD_REQUEST, "Tournament is not open for registration"),
// Giải đấu chưa mở đăng ký

    TOURNAMENT_REGISTRATION_NOT_FOUND(1503, HttpStatus.NOT_FOUND, "Tournament registration not found"),
// Không tìm thấy đăng ký giải đấu

    JOCKEY_TOURNAMENT_REGISTRATION_NOT_FOUND(1504, HttpStatus.NOT_FOUND, "Jockey tournament registration not found"),
// Không tìm thấy đăng ký giải đấu của nài ngựa

    REGISTRATION_ALREADY_REVIEWED(1505, HttpStatus.CONFLICT, "Registration has already been reviewed"),
// Đăng ký đã được duyệt/từ chối trước đó

    REGISTRATION_NOT_PENDING(1506, HttpStatus.BAD_REQUEST, "Registration is not in pending status"),
// Đăng ký không ở trạng thái chờ duyệt

    ROUND_NOT_FOUND(1507, HttpStatus.NOT_FOUND, "Round not found"),
// Không tìm thấy vòng đấu

    RACE_NOT_FOUND(1508, HttpStatus.NOT_FOUND, "Race not found"),
// Không tìm thấy cuộc đua

    PRIZE_STRUCTURE_NOT_FOUND(1509, HttpStatus.NOT_FOUND, "Prize structure not found"),
// Không tìm thấy cơ cấu giải thưởng

    HORSE_ALREADY_REGISTERED_TOURNAMENT(1510, HttpStatus.CONFLICT, "Horse is already registered for this tournament"),
// Ngựa đã được đăng ký cho giải đấu này

    JOCKEY_ALREADY_REGISTERED_TOURNAMENT(1511, HttpStatus.CONFLICT, "Jockey is already registered for this tournament"),
// Nài ngựa đã được đăng ký cho giải đấu này

    TOURNAMENT_NOT_IN_DRAFT(1512, HttpStatus.BAD_REQUEST, "Tournament is not in draft status"),

    INVALID_TOURNAMENT_DATES(1513, HttpStatus.BAD_REQUEST, "End date must be after start date"),

    INVALID_ROUND_DATES(1514, HttpStatus.BAD_REQUEST, "End date must be after start date"),

    INVALID_RACE_DATES(1515, HttpStatus.BAD_REQUEST, "End time must be after start time"),

    INVALID_ROUND_COUNT(1516, HttpStatus.BAD_REQUEST, "Each tournament must have exactly 2 rounds"),

    ROUND_WITHOUT_RACE(1517, HttpStatus.BAD_REQUEST, "Each round must have at least one race"),

    ROUND_DATES_OUT_OF_TOURNAMENT(1518, HttpStatus.BAD_REQUEST, "Round dates must be within tournament dates"),

    RACE_DATES_OUT_OF_ROUND(1519, HttpStatus.BAD_REQUEST, "Race times must be within round dates"),

    PRIZE_PERCENTAGE_EXCEEDS_100(1520, HttpStatus.BAD_REQUEST, "Total prize percentage must not exceed 100%"),

    TOURNAMENT_ELIGIBILITY_NOT_FOUND(1521, HttpStatus.NOT_FOUND, "Tournament eligibility not found"),

    TOURNAMENT_MISSING_PRIZE(1522, HttpStatus.BAD_REQUEST, "Tournament must have at least one prize structure"),

    TOURNAMENT_MISSING_ELIGIBILITY(1523, HttpStatus.BAD_REQUEST, "Tournament must have at least one eligibility rule"),

    TOURNAMENT_MISSING_ROUNDS(1524, HttpStatus.BAD_REQUEST, "Tournament must have exactly 2 rounds"),

    ROUND_MISSING_RACES(1525, HttpStatus.BAD_REQUEST, "Each round must have at least one race"),

    INVALID_PREDICTION_TIMES(1526, HttpStatus.BAD_REQUEST, "Prediction open time must be before close time and before race start time"),

    TOURNAMENT_NAME_EXISTS(1527, HttpStatus.CONFLICT, "Tournament name already exists"),

    DUPLICATE_ROUND_SEQUENCE(1528, HttpStatus.CONFLICT, "Round sequence order already exists in this tournament"),

    DUPLICATE_PRIZE_RANK(1529, HttpStatus.CONFLICT, "Prize rank already exists in this tournament"),

    INVALID_HORSE_AGE_RANGE(1530, HttpStatus.BAD_REQUEST, "Min horse age must be less than max horse age"),

    PRIZE_MISSING_VALUE(1531, HttpStatus.BAD_REQUEST, "Prize must have either percentage or fixed amount"),

    INVALID_REGISTRATION_STATUS(1532, HttpStatus.BAD_REQUEST, "Invalid registration status"),

    MAX_ROUNDS_REACHED(1533, HttpStatus.BAD_REQUEST, "Maximum number of rounds (2) reached for this tournament"),

    MAX_RACES_REACHED(1534, HttpStatus.BAD_REQUEST, "Maximum number of races reached for this round"),

    HORSE_TOURNAMENT_TIME_CONFLICT(1535, HttpStatus.BAD_REQUEST,
            "Horse is already registered in another tournament with overlapping dates"),

    JOCKEY_TOURNAMENT_TIME_CONFLICT(1536, HttpStatus.BAD_REQUEST,
            "Jockey is already registered in another tournament with overlapping dates"),

    INVALID_PHASE_TRANSITION(1537, HttpStatus.BAD_REQUEST, "Invalid phase transition"),
    REGISTRATION_NOT_CLOSED(1538, HttpStatus.BAD_REQUEST, "Registration period has not ended"),
    REVIEW_NOT_COMPLETED(1539, HttpStatus.BAD_REQUEST, "Registration review not yet completed"),
    MATCHING_NOT_COMPLETED(1540, HttpStatus.BAD_REQUEST, "Jockey matching not yet completed"),
    SCHEDULE_NOT_PUBLISHED(1541, HttpStatus.BAD_REQUEST, "Race schedule not yet published"),
    INVALID_TIMING_ORDER(1542, HttpStatus.BAD_REQUEST,
            "Timing order must be: registrationOpenAt < registrationCloseAt < reviewDeadlineAt < jockeyMatchingDeadlineAt < schedulingDeadlineAt"),
    SCHEDULING_DEADLINE_AFTER_ROUND(1543, HttpStatus.BAD_REQUEST,
            "Scheduling deadline must be before or on the first round start date");


    ;


    int code;
    HttpStatus httpStatus;
    String message;

}
