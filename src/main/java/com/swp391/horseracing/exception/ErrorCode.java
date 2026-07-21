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
    INVALID_REFUND_AMOUNT(1427, HttpStatus.BAD_REQUEST, "Refund amount is invalid"),

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

    INVALID_ROUND_COUNT(1516, HttpStatus.BAD_REQUEST, "Invalid round count"),

    ROUND_WITHOUT_RACE(1517, HttpStatus.BAD_REQUEST, "Each round must have at least one race"),

    ROUND_DATES_OUT_OF_TOURNAMENT(1518, HttpStatus.BAD_REQUEST, "Round dates must be within tournament dates"),

    RACE_DATES_OUT_OF_ROUND(1519, HttpStatus.BAD_REQUEST, "Race times must be within round dates"),

    PRIZE_PERCENTAGE_EXCEEDS_100(1520, HttpStatus.BAD_REQUEST, "Total prize percentage must not exceed 100%"),

    TOURNAMENT_ELIGIBILITY_NOT_FOUND(1521, HttpStatus.NOT_FOUND, "Tournament eligibility not found"),

    TOURNAMENT_MISSING_PRIZE(1522, HttpStatus.BAD_REQUEST, "Tournament must have at least one prize structure"),

    TOURNAMENT_MISSING_ELIGIBILITY(1523, HttpStatus.BAD_REQUEST, "Tournament must have at least one eligibility rule"),

    TOURNAMENT_MISSING_ROUNDS(1524, HttpStatus.BAD_REQUEST, "Tournament must have rounds"),

    ROUND_MISSING_RACES(1525, HttpStatus.BAD_REQUEST, "Each round must have at least one race"),

    INVALID_PREDICTION_TIMES(1526, HttpStatus.BAD_REQUEST, "Prediction open time must be before close time and before race start time"),

    TOURNAMENT_NAME_EXISTS(1527, HttpStatus.CONFLICT, "Tournament name already exists"),

    DUPLICATE_ROUND_SEQUENCE(1528, HttpStatus.CONFLICT, "Round sequence order already exists in this tournament"),

    DUPLICATE_PRIZE_RANK(1529, HttpStatus.CONFLICT, "Prize rank already exists in this tournament"),

    INVALID_HORSE_AGE_RANGE(1530, HttpStatus.BAD_REQUEST, "Min horse age must be less than max horse age"),

    PRIZE_MISSING_VALUE(1531, HttpStatus.BAD_REQUEST, "Prize must have either percentage or fixed amount"),

    INVALID_REGISTRATION_STATUS(1532, HttpStatus.BAD_REQUEST, "Invalid registration status"),

    MAX_ROUNDS_REACHED(1533, HttpStatus.BAD_REQUEST, "Maximum number of rounds reached for this tournament"),

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
            "Scheduling deadline must be before or on the first round start date"),

    ROUND_NAME_ALREADY_EXISTS(1545, HttpStatus.CONFLICT, "Round name already exists in this tournament"),

    RACE_NAME_ALREADY_EXISTS(1546, HttpStatus.CONFLICT, "Race name already exists in this round"),

    CONTRACT_NOT_FOUND(1546, HttpStatus.NOT_FOUND, "Contract not found"),

    INVALID_CONTRACT_STATUS(1547, HttpStatus.BAD_REQUEST, "Invalid contract status"),

    DUPLICATE_RACE_SEQUENCE(1548, HttpStatus.CONFLICT, "Race sequence order already exists in this round"),

    ELIGIBILITY_CONDITION_EXISTS(1549, HttpStatus.CONFLICT, "Eligibility condition name already exists in this tournament"),

    RACE_ENTRY_NOT_FOUND(1550, HttpStatus.NOT_FOUND, "Race entry not found"),
    RACE_ENTRY_ALREADY_EXISTS(1551, HttpStatus.CONFLICT, "Race entry already exists in this race"),
    LANE_NUMBER_ALREADY_TAKEN(1552, HttpStatus.CONFLICT, "Lane number already taken in this race"),
    INVALID_RACE_ENTRY_STATUS(1553, HttpStatus.BAD_REQUEST, "Invalid race entry status"),
    INVALID_RACE_ENTRY_STATUS_TRANSITION(1554, HttpStatus.BAD_REQUEST, "Invalid race entry status transition"),

    // RACE ENTRY VALIDATION
    RACE_NOT_IN_SCHEDULING(1555, HttpStatus.BAD_REQUEST, "Race is not in scheduling status"),
    RACE_ALREADY_PUBLISHED(1556, HttpStatus.CONFLICT, "Schedule has already been published for this race"),
    CONTRACT_NOT_APPROVED(1557, HttpStatus.BAD_REQUEST, "Contract is not approved"),
    REGISTRATION_NOT_APPROVED(1558, HttpStatus.BAD_REQUEST, "Registration is not approved"),
    TOURNAMENT_MISMATCH(1559, HttpStatus.BAD_REQUEST, "Race and Contract belong to different tournaments"),
    HORSE_ALREADY_IN_ROUND(1563, HttpStatus.CONFLICT, "Horse has already been assigned in this round"),
    JOCKEY_ALREADY_IN_ROUND(1564, HttpStatus.CONFLICT, "Jockey has already been assigned in this round"),
    RACE_EXCEEDS_MAX_ENTRIES(1565, HttpStatus.BAD_REQUEST, "Race has reached maximum entries"),
    LANE_EXCEEDS_MAX(1566, HttpStatus.BAD_REQUEST, "Lane number exceeds maximum entries"),
    RACE_ALREADY_STARTED(1567, HttpStatus.BAD_REQUEST, "Race has already started"),
    RACE_NOT_ENOUGH_ENTRIES(1568, HttpStatus.BAD_REQUEST, "Race does not meet the minimum entries required by the round"),
    RACE_MISSING_REFEREES(1569, HttpStatus.BAD_REQUEST, "Race must have at least one referee assigned to publish schedule"),

    // RACE REFEREE
    RACE_REFEREE_NOT_FOUND(1560, HttpStatus.NOT_FOUND, "Race referee assignment not found"),
    RACE_REFEREE_ALREADY_ASSIGNED(1561, HttpStatus.CONFLICT, "Referee already assigned to this race"),

    // HEAD REFEREE
    HEAD_REFEREE_ALREADY_ASSIGNED(1562, HttpStatus.CONFLICT, "Head referee already assigned to this round"),

    // CONTRACT
    INVALID_CONTRACT_PERCENTAGES(1570, HttpStatus.BAD_REQUEST, "Advance percent and final percent must sum to 100"),
    INVALID_PRIZE_SHARE_PERCENTAGES(1571, HttpStatus.BAD_REQUEST, "Owner prize share and jockey prize share must sum to 100"),
    CONTRACT_ALREADY_EXISTS_FOR_HORSE(1572, HttpStatus.CONFLICT, "A contract already exists for this horse in this tournament"),
    INVALID_CONTRACT_STATUS_TRANSITION(1573, HttpStatus.BAD_REQUEST, "Invalid contract status transition"),
    CONTRACT_ALREADY_EXISTS_FOR_REGISTRATION(1574, HttpStatus.CONFLICT, "A contract already exists for this tournament registration"),

    HANDICAP_RULE_CANNOT_NULL(1575, HttpStatus.BAD_REQUEST, "Weight fields (topWeightLbs, minWeightLbs, equipmentWeightKg) are required when handicap is enabled"),

    WEIGHT_MUST_POSTIVE(1576, HttpStatus.BAD_REQUEST, "Weight fields must be positive values when handicap is enabled"),

    INVALID_WEIGHT(1577, HttpStatus.BAD_REQUEST, "Min weight must be less than top weight"),

    HANDICAP_DISABLE(1578, HttpStatus.BAD_REQUEST, "Weight fields (topWeightLbs, minWeightLbs, equipmentWeightKg) must not be provided when handicap is disabled"),

    HORSE_REGISTRATION_LIMIT_EXCEEDED(1579, HttpStatus.BAD_REQUEST, "Horse registration limit exceeded for this tournament"),

    JOCKEY_REGISTRATION_LIMIT_EXCEEDED(1580, HttpStatus.BAD_REQUEST, "Jockey registration limit exceeded for this tournament"),

    REGISTRATION_WITHDRAW_NOT_ALLOWED(1581, HttpStatus.CONFLICT, "Registration cannot be withdrawn in its current state"),
    REGISTRATION_HAS_ACTIVE_CONTRACT(1582, HttpStatus.CONFLICT, "Cancel the active contract before withdrawing this registration"),
    REGISTRATION_ALREADY_WITHDRAWN(1583, HttpStatus.CONFLICT, "Registration has already been withdrawn"),
    REGISTRATION_ALREADY_CLOSED(1584, HttpStatus.CONFLICT, "Tournament registration has already been closed"),
    INVALID_PAGE_REQUEST(1585, HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100"),
    INVALID_DATE_RANGE(1586, HttpStatus.BAD_REQUEST, "The from time must be before or equal to the to time"),
    ROUND_MISSING_HEAD_REFEREE(1587, HttpStatus.BAD_REQUEST, "Round must have an active head referee before publishing schedule"),
    REFEREE_ROLE_CONFLICT_IN_ROUND(1588, HttpStatus.CONFLICT, "A referee cannot be both head referee and race referee in the same round"),
    REFEREE_NOT_AVAILABLE(1589, HttpStatus.BAD_REQUEST, "Referee is suspended or unavailable"),
    HEAD_REFEREE_ASSIGNMENT_LOCKED(1590, HttpStatus.CONFLICT, "Head referee assignment is locked after the round schedule is published"),
    RACE_LANES_INCOMPLETE(1591, HttpStatus.BAD_REQUEST, "Every race entry must have a unique valid lane before publishing schedule"),
    RACE_REQUIRES_EXACTLY_ONE_REFEREE(1592, HttpStatus.CONFLICT, "Race must have exactly one direct referee"),

    //Contract
    TOURNAMENT_NOT_MATCH(1601, HttpStatus.BAD_REQUEST, "Tournament not match"),

    INVALID_HIRE_FEE(1602, HttpStatus.BAD_REQUEST, "Invalid hire fee"),

    INVALID_PRIZE_SHARE(1603, HttpStatus.BAD_REQUEST, "Invalid prize share"),

    CONTRACT_ALREADY_EXISTS(1604, HttpStatus.BAD_REQUEST, "Contract already exists"),

    CONTRACT_HIRING_FEE_NOT_PAID(1605, HttpStatus.BAD_REQUEST, "Hiring fee not paid"),

    INVALID_ESCROW_STATUS(1606, HttpStatus.BAD_REQUEST, "Invalid escrow status"),

    FINAL_PAYOUT_ALREADY_RELEASED(1608, HttpStatus.BAD_REQUEST, "Final payout has already been released"),

    ESCROW_NOT_PARTIALLY_RELEASED(1609, HttpStatus.BAD_REQUEST, "Escrow is not in PARTIALLY_RELEASED status"),

    CONTRACT_CANCELLATION_NOT_ALLOWED(1610, HttpStatus.CONFLICT, "Contract cannot be cancelled in its current state"),
    CONTRACT_HAS_ACTIVE_RACE(1611, HttpStatus.CONFLICT, "Contract is attached to a race that can no longer be changed"),

    //Inspection
    MEDICAL_STAFF_NOT_FOUND(1701, HttpStatus.NOT_FOUND, "Medical staff not found"),

    MEDICAL_STAFF_ALREADY_ASSIGNED(1702, HttpStatus.CONFLICT, "Medical staff already assigned"),

    MEDICAL_STAFF_SUSPENDED(1703, HttpStatus.BAD_REQUEST, "Medical staff suspended"),

    VETERINARIAN_NOT_FOUND(1704, HttpStatus.NOT_FOUND, "Veterinarian not found"),

    VETERINARIAN_ALREADY_ASSIGNED(1705, HttpStatus.CONFLICT, "Veterinarian already assigned"),

    VETERINARIAN_SUSPENDED(1704, HttpStatus.BAD_REQUEST, "Veterinarian suspended"),

    NO_AVAILABLE_MEDICAL_STAFF(1706, HttpStatus.NOT_FOUND, "No available medical staff found"),

    NO_AVAILABLE_VETERINARIAN(1707, HttpStatus.NOT_FOUND, "No available veterinarian found"),

    HORSE_INSPECTION_ALREADY_EXISTS(1708, HttpStatus.CONFLICT, "Horse inspection already exists for this entry"),

    JOCKEY_INSPECTION_ALREADY_EXISTS(1709, HttpStatus.CONFLICT, "Jockey inspection already exists for this entry"),

    VET_NOT_ASSIGNED_TO_RACE(1710, HttpStatus.FORBIDDEN, "Veterinarian is not assigned to this race"),

    MEDICAL_STAFF_NOT_ASSIGNED_TO_RACE(1711, HttpStatus.FORBIDDEN, "Medical staff is not assigned to this race"),

    RACE_NOT_IN_SCHEDULED_STATUS(1712, HttpStatus.BAD_REQUEST, "Race is not in scheduled status"),

    REFEREE_NOT_ASSIGNED_TO_RACE(1713, HttpStatus.FORBIDDEN, "Referee is not assigned to this race"),

    ENTRY_MISSING_HORSE_INSPECTION(1715, HttpStatus.BAD_REQUEST, "Race entry is missing a confirmed and passed horse inspection"),

    ENTRY_MISSING_JOCKEY_INSPECTION(1716, HttpStatus.BAD_REQUEST, "Race entry is missing a confirmed and passed jockey inspection"),

    ENTRY_HANDICAP_NOT_CONFIRMED(1717, HttpStatus.BAD_REQUEST, "Handicap weight is not confirmed for this entry"),

    RACE_NOT_ENOUGH_ACTIVE_ENTRIES(1718, HttpStatus.BAD_REQUEST, "Race does not have enough active entries to start"),

    INVALID_VIOLATION_TYPE_FOR_RACE_STATUS(1719, HttpStatus.BAD_REQUEST, "Violation type is not allowed for the current race status"),

    RACE_VIOLATION_REPORTING_CLOSED(1720, HttpStatus.BAD_REQUEST, "Violation reporting is closed for this race"),

    HORSE_INSPECTION_NOT_FOUND(1721, HttpStatus.NOT_FOUND, "Horse inspection not found for this entry"),

    JOCKEY_INSPECTION_NOT_FOUND(1722, HttpStatus.NOT_FOUND, "Jockey inspection not found for this entry"),

    HORSE_INSPECTION_FINDINGS_REQUIRE_FAILURE(1723, HttpStatus.BAD_REQUEST, "Horse inspection must fail when doping is detected or actual breed does not match"),

    JOCKEY_INSPECTION_FINDINGS_REQUIRE_FAILURE(1724, HttpStatus.BAD_REQUEST, "Jockey inspection must fail when doping is detected"),

    // RACE RESULT
    RACE_RESULT_NOT_FOUND(2601, HttpStatus.NOT_FOUND, "Race result not found"),
    RACE_RESULT_ALREADY_EXISTS(2602, HttpStatus.CONFLICT, "Race result already exists for this entry"),
    RACE_ALREADY_HAS_RESULTS(2603, HttpStatus.CONFLICT, "Race already has results recorded"),
    INVALID_RACE_RESULT_STATUS(2604, HttpStatus.BAD_REQUEST, "Invalid race result status"),
    DUPLICATE_RACE_RESULT_RANK(2605, HttpStatus.CONFLICT, "Duplicate rank in the same race"),
    FINISH_TIME_MUST_BE_POSITIVE(2606, HttpStatus.BAD_REQUEST, "Finish time must be zero or positive"),
    RANK_MUST_BE_POSITIVE(2607, HttpStatus.BAD_REQUEST, "Rank must be at least 1"),
    PRIZE_ALREADY_PAID(2608, HttpStatus.CONFLICT, "Prize has already been paid for this result"),
    PRIZE_NOT_ELIGIBLE(2609, HttpStatus.BAD_REQUEST, "Result is not eligible for prize payout"),

    // RACE REPORT
    RACE_REPORT_NOT_FOUND(2610, HttpStatus.NOT_FOUND, "Race report not found"),
    RACE_REPORT_ALREADY_EXISTS(2611, HttpStatus.CONFLICT, "Race report already exists for this race"),
    RACE_REPORT_ALREADY_SIGNED(2612, HttpStatus.CONFLICT, "Race report has already been signed"),
    RACE_REPORT_ALREADY_PUBLISHED(2613, HttpStatus.CONFLICT, "Race report has already been published"),
    RACE_REPORT_NOT_SIGNED(2614, HttpStatus.BAD_REQUEST, "Race report must be signed before publishing"),
    RACE_REPORT_NOT_IN_DRAFT(2615, HttpStatus.BAD_REQUEST, "Race report is not in draft status"),
    RACE_REPORT_NOT_IN_SIGNED(2616, HttpStatus.BAD_REQUEST, "Race report is not in signed status"),
    RACE_REPORT_NOT_SUBMITTED(2617, HttpStatus.BAD_REQUEST, "Race report must be submitted to the head referee"),
    RACE_REPORT_ALREADY_SUBMITTED(2618, HttpStatus.CONFLICT, "Race report has already been submitted to the head referee"),
    RACE_REPORT_RETURN_REASON_REQUIRED(2619, HttpStatus.BAD_REQUEST, "Return reason is required"),
    RACE_REPORT_PENDING_APPEAL(2623, HttpStatus.BAD_REQUEST, "Pending appeals must be resolved before signing or publishing the race report"),

    // APPEAL CATEGORY
    APPEAL_CATEGORY_NOT_FOUND(2620, HttpStatus.NOT_FOUND, "Appeal category not found"),
    APPEAL_CATEGORY_CODE_EXISTS(2621, HttpStatus.CONFLICT, "Appeal category code already exists"),
    APPEAL_CATEGORY_INACTIVE(2622, HttpStatus.BAD_REQUEST, "Appeal category is inactive"),

    // APPEAL
    APPEAL_NOT_FOUND(2630, HttpStatus.NOT_FOUND, "Appeal not found"),
    APPEAL_ALREADY_REVIEWED(2631, HttpStatus.CONFLICT, "Appeal has already been reviewed"),
    APPEAL_NOT_PENDING(2632, HttpStatus.BAD_REQUEST, "Appeal is not in pending status"),
    INVALID_APPEAL_STATUS_TRANSITION(2633, HttpStatus.BAD_REQUEST, "Invalid appeal status transition"),
    APPEAL_ALREADY_CANCELLED(2634, HttpStatus.CONFLICT, "Appeal has already been cancelled"),

    // APPEAL EVIDENCE
    APPEAL_EVIDENCE_NOT_FOUND(2640, HttpStatus.NOT_FOUND, "Appeal evidence not found"),
    APPEAL_EVIDENCE_REQUIRED(2641, HttpStatus.BAD_REQUEST, "Either file URL or text content must be provided for evidence"),
    APPEAL_EVIDENCE_FILE_TYPE_INVALID(2642, HttpStatus.BAD_REQUEST, "Evidence file type does not match the selected evidence type"),
    APPEAL_EVIDENCE_FILE_TOO_LARGE(2643, HttpStatus.BAD_REQUEST, "Evidence file exceeds the allowed size"),
    APPEAL_EVIDENCE_UPLOAD_FAILED(2644, HttpStatus.INTERNAL_SERVER_ERROR, "Evidence file upload failed"),

    VIOLATION_NOT_FOUND(2650, HttpStatus.NOT_FOUND, "Violation not found"),
    VIOLATION_ALREADY_RESOLVED(2651, HttpStatus.CONFLICT, "Violation has already been resolved"),
    VIOLATION_ALREADY_CANCELLED(2652, HttpStatus.CONFLICT, "Violation has already been cancelled"),
    INVALID_VIOLATION_STATUS_TRANSITION(2653, HttpStatus.BAD_REQUEST, "Invalid violation status transition"),

    INVALID_INSPECTION_TIMELINE(1801, HttpStatus.BAD_REQUEST, "Invalid inspection timeline: inspectionOpenMinutesBefore > inspectionCloseMinutesBefore > predictionCloseMinutesBefore >= 0"),
    INVALID_SCHEDULING_CONFIG(1802, HttpStatus.BAD_REQUEST, "Invalid scheduling configuration"),
    MAX_RACES_PER_DAY_EXCEEDED(1803, HttpStatus.BAD_REQUEST, "Daily race limit exceeded for this tournament"),
    RACE_OUTSIDE_OPERATING_HOURS(1804, HttpStatus.BAD_REQUEST, "Race time is outside tournament operating hours"),
    RACE_OVERLAPS_BREAK(1805, HttpStatus.BAD_REQUEST, "Race time overlaps with tournament break time"),
    RACE_SCHEDULE_CONFLICT(1806, HttpStatus.BAD_REQUEST, "Race schedule conflict with an existing race (minimum interval required)"),
    INSPECTION_WINDOW_NOT_OPEN(1807, HttpStatus.BAD_REQUEST, "Inspection window is not open yet"),
    INSPECTION_WINDOW_CLOSED(1808, HttpStatus.BAD_REQUEST, "Inspection window has closed"),
    RACE_ENTRY_NOT_ACTIVE(1809, HttpStatus.BAD_REQUEST, "Race entry is not active"),
    RACE_START_TOO_EARLY(1810, HttpStatus.BAD_REQUEST, "Race cannot start earlier than startEarlyToleranceMinutes before startTime"),
    RACE_START_WINDOW_EXPIRED(1811, HttpStatus.BAD_REQUEST, "Race start window has expired (startLateToleranceMinutes elapsed)"),
    APPEAL_SUBMISSION_CLOSED(1812, HttpStatus.BAD_REQUEST, "Appeal submission is closed for this race"),
    INVALID_FINAL_ROUND_CONFIGURATION(1813, HttpStatus.BAD_REQUEST, "Final round must have exactly one race"),

    // PREDICTION
    PREDICTION_NOT_FOUND(1660, HttpStatus.NOT_FOUND, "Prediction not found"),
    INVALID_PREDICTION_TYPE(1673, HttpStatus.BAD_REQUEST, "Only TOP3 predictions are supported"),
    PREDICTION_ALREADY_EXISTS(1661, HttpStatus.CONFLICT, "You have already submitted a prediction for this race"),
    PREDICTION_WINDOW_NOT_OPEN(1662, HttpStatus.BAD_REQUEST, "Prediction window has not opened yet"),
    PREDICTION_WINDOW_CLOSED(1663, HttpStatus.BAD_REQUEST, "Prediction window has already closed"),
    PREDICTION_NOT_BELONG_TO_USER(1664, HttpStatus.FORBIDDEN, "This prediction does not belong to you"),
    PREDICTION_ALREADY_SCORED(1665, HttpStatus.CONFLICT, "Prediction has already been scored"),
    PREDICTION_CANCELLED(1666, HttpStatus.BAD_REQUEST, "Prediction has been cancelled"),
    INVALID_TOP1_COUNT(1667, HttpStatus.BAD_REQUEST, "Exactly one horse must be selected for Top1 prediction"),
    INVALID_TOP3_COUNT(1668, HttpStatus.BAD_REQUEST, "Exactly three horses must be selected for Top3 prediction"),
    DUPLICATE_HORSE_IN_PREDICTION(1669, HttpStatus.BAD_REQUEST, "A horse cannot be selected more than once in a prediction"),
    HORSE_NOT_IN_THIS_RACE(1670, HttpStatus.BAD_REQUEST, "Selected horse is not participating in this race"),
    INVALID_PREDICTED_RANK(1671, HttpStatus.BAD_REQUEST, "Predicted rank must be 1 for Top1, and 1,2,3 for Top3"),
    RACE_HAS_NOT_STARTED(1672, HttpStatus.BAD_REQUEST, "Race has not started yet"),

    // AI PREDICTION
    AI_PREDICTION_GENERATION_FAILED(1673, HttpStatus.INTERNAL_SERVER_ERROR, "AI prediction generation failed"),
    AI_PREDICTION_INVALID_RESPONSE(1674, HttpStatus.INTERNAL_SERVER_ERROR, "AI returned an invalid response format"),
    AI_PREDICTION_NOT_FOUND(1675, HttpStatus.NOT_FOUND, "AI prediction has not been generated"),
    AI_PREDICTION_NOT_PUBLISHED(1676, HttpStatus.NOT_FOUND, "AI prediction has not been published"),
    AI_PREDICTION_RACE_NOT_ELIGIBLE(1677, HttpStatus.BAD_REQUEST, "Race is not eligible for AI prediction"),
    AI_PREDICTION_ALREADY_PUBLISHED(1678, HttpStatus.CONFLICT, "AI prediction is already published"),

    // HORSE RATING
    RACE_ENTRY_DID_NOT_START(1815, HttpStatus.BAD_REQUEST, "Race result cannot be created for an entry that did not start"),
    HORSE_RATING_CHANGED_RETRY_REQUIRED(1814, HttpStatus.BAD_REQUEST, "Horse rating has changed since the calculation was made, please try again"),
    HORSE_RATING_ALREADY_APPLIED(1816, HttpStatus.CONFLICT, "Horse rating has already been applied for this race result"),
    RACE_REPORT_NOT_PUBLISHED(1817, HttpStatus.BAD_REQUEST, "Race report is not published yet"),
    PREDICTION_RESULT_NOT_AVAILABLE(1818, HttpStatus.CONFLICT, "Prediction result is not available yet"),
    RACE_HAS_NOT_FINISHED(1820, HttpStatus.BAD_REQUEST, "Race has not finished yet"),

    // NOTIFICATION
    NOTIFICATION_NOT_FOUND(1901, HttpStatus.NOT_FOUND, "Notification not found"),
    NOTIFICATION_EVENT_INVALID(1902, HttpStatus.BAD_REQUEST, "Notification event is invalid"),
    NOTIFICATION_PAYLOAD_INVALID(1903, HttpStatus.BAD_REQUEST, "Notification event payload is invalid"),

    ROUND_GAP_TOO_SHORT(1819, HttpStatus.BAD_REQUEST, "Insufficient gap between rounds"),

    FILE_UPLOAD_FAILED(2001, HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed"),

    APPROVED_ENTRIES_EXCEED_MAXIMUM(2107, HttpStatus.BAD_REQUEST, "Approved entries exceed the tournament capacity"),
    RACE_ENTRIES_OUT_OF_RANGE(2108, HttpStatus.BAD_REQUEST, "Race entries count must be between 8 and 16"),
    ROUND_STRUCTURE_MISMATCH(2109, HttpStatus.BAD_REQUEST, "Round structure does not match the bracket plan"),
    RACE_STRUCTURE_MISMATCH(2110, HttpStatus.BAD_REQUEST, "Race structure does not match the bracket plan"),
    NEXT_ROUND_NOT_ENOUGH_QUALIFIERS(2111, HttpStatus.BAD_REQUEST, "Not enough qualifiers to advance to next round"),
    ROUND_REPORTS_NOT_FULLY_PUBLISHED(2112, HttpStatus.BAD_REQUEST, "Not all race reports in this round are published"),
    ROUND_TRANSITION_ALREADY_COMPLETED(2113, HttpStatus.CONFLICT, "Round transition has already been completed"),
    FINAL_ROUND_MUST_HAVE_ONE_RACE(2114, HttpStatus.BAD_REQUEST, "Final round must have exactly one race"),
    PRIZE_PAYOUT_ONLY_ALLOWED_FOR_FINAL(2115, HttpStatus.BAD_REQUEST, "Prize payout is only allowed for the final round"),
    PRIZE_PAYOUT_ALREADY_COMPLETED(2116, HttpStatus.CONFLICT, "Prize payout has already been completed for this tournament"),
    RACE_SCHEDULE_INCOMPLETE(2120, HttpStatus.BAD_REQUEST, "All races in the active round must have a valid schedule before publication"),
    TOURNAMENT_START_DATE_MUST_BE_TODAY(2121, HttpStatus.BAD_REQUEST, "Tournament start date must be the creation date"),
    REGISTRATION_OPEN_TIME_IN_PAST(2122, HttpStatus.BAD_REQUEST, "Registration open time cannot be in the past"),
    REGISTRATION_PERIOD_TOO_SHORT(2123, HttpStatus.BAD_REQUEST, "Registration period is shorter than the minimum required for the selected capacity"),
    REVIEW_PERIOD_TOO_SHORT(2124, HttpStatus.BAD_REQUEST, "Registration review period must be at least 4 calendar days"),
    JOCKEY_MATCHING_PERIOD_TOO_SHORT(2125, HttpStatus.BAD_REQUEST, "Jockey matching period is shorter than the minimum required for the selected capacity"),
    SCHEDULING_PERIOD_TOO_SHORT(2126, HttpStatus.BAD_REQUEST, "Scheduling period must be at least 4 calendar days"),
    TOURNAMENT_START_DATE_IMMUTABLE(2127, HttpStatus.BAD_REQUEST, "Tournament start date cannot be changed after creation"),
    ADVANCED_ROUND_ENTRIES_MANAGED_BY_RESULTS(2128, HttpStatus.CONFLICT, "Entries after the first round are created only from official qualifiers"),
    TOURNAMENT_DATE_RANGE_INVALID(2129, HttpStatus.BAD_REQUEST, "Tournament date range is too short for the scheduled races"),

    // SPECTATOR HORSE FOLLOW
    HORSE_ALREADY_FOLLOWED(2201, HttpStatus.CONFLICT, "Horse is already followed"),
    HORSE_FOLLOW_NOT_FOUND(2202, HttpStatus.NOT_FOUND, "Horse follow not found"),

    // ELIGIBILITY VALIDATION
    ELIGIBILITY_INVALID_VALUE(2301, HttpStatus.BAD_REQUEST, "Eligibility condition value không đúng định dạng hoặc ngoài phạm vi cho phép"),
    ELIGIBILITY_INVALID_TARGET(2302, HttpStatus.BAD_REQUEST, "Condition name không tương thích với target type"),
    ELIGIBILITY_HORSE_WEIGHT_OUT_OF_RANGE(2303, HttpStatus.BAD_REQUEST, "Horse weight phải từ 400kg đến 600kg"),
    ELIGIBILITY_JOCKEY_WEIGHT_OUT_OF_RANGE(2304, HttpStatus.BAD_REQUEST, "Jockey weight phải từ 45kg đến 65kg"),

    ;



    int code;
    HttpStatus httpStatus;
    String message;

}
