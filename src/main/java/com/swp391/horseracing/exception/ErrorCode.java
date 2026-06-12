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

    INVOICE_CANCELLED(1412, HttpStatus.BAD_REQUEST, "Invoice has been cancelled");
// Hóa đơn đã bị hủy


    int code;
    HttpStatus httpStatus;
    String message;

}
