# HRTMS - Tích hợp nạp tiền ví bằng VNPAY Sandbox

Tài liệu này hướng dẫn cách chuyển chức năng `deposit` từ mock data sang giả lập thanh toán qua VNPAY Sandbox cho hệ thống Horse Racing Tournament Management System.

---

## 1. Mục tiêu

Hiện tại flow nạp tiền mock thường là:

```text
User bấm Deposit
↓
BE cộng tiền trực tiếp vào Wallet
↓
Tạo Transaction Type = DEPOSIT
```

Khi tích hợp VNPAY Sandbox, flow phải đổi thành:

```text
User tạo yêu cầu nạp tiền
↓
BE tạo PaymentTransaction trạng thái PENDING
↓
BE tạo paymentUrl sang VNPAY
↓
FE redirect user sang VNPAY Sandbox
↓
User thanh toán bằng thẻ test
↓
VNPAY gọi IPN về BE
↓
BE verify checksum + kiểm tra amount + kiểm tra trạng thái
↓
BE mới cộng tiền vào Wallet
↓
BE tạo Transaction Type = DEPOSIT
```

Nguyên tắc quan trọng:

```text
Không cộng tiền vào ví khi vừa tạo paymentUrl.
Không cộng tiền vào ví chỉ dựa vào ReturnUrl từ browser.
Chỉ cộng tiền sau khi IPN hợp lệ hoặc sau khi BE xác minh chắc chắn giao dịch thành công.
```

---

## 2. Có cần thêm entity không?

Có. Nên thêm ít nhất 1 entity mới:

```text
PaymentTransaction
```

Lý do: bảng `Transaction` hiện tại là lịch sử biến động ví. Nó chỉ nên được tạo khi tiền thật sự đã vào hoặc ra khỏi ví.

Trong khi đó, giao dịch VNPAY có nhiều trạng thái trung gian:

```text
CREATED
PENDING
SUCCESS
FAILED
CANCELLED
EXPIRED
```

Nếu dùng luôn bảng `Transaction` để lưu giao dịch VNPAY đang chờ thanh toán thì dễ bị hiểu nhầm rằng tiền đã vào ví.

---

## 3. Phân biệt PaymentTransaction và Transaction

### PaymentTransaction

Dùng để lưu giao dịch với cổng thanh toán ngoài.

Nó trả lời:

```text
User đã tạo yêu cầu nạp tiền chưa?
Giao dịch gửi sang VNPAY có mã gì?
VNPAY trả về mã gì?
Giao dịch thành công hay thất bại?
IPN đã xử lý chưa?
```

### Transaction

Dùng để lưu biến động số dư ví.

Nó trả lời:

```text
Ví nào được cộng/trừ tiền?
Số dư trước là bao nhiêu?
Số dư sau là bao nhiêu?
Lý do biến động tiền là gì?
```

Vì vậy:

```text
PaymentTransaction SUCCESS
↓
mới tạo Transaction DEPOSIT CREDIT
```

---

## 4. Entity mới: PaymentTransaction

Tên bảng đề xuất:

```sql
payment_transactions
```

### Attribute

| Attribute | Kiểu | Giải thích |
|---|---|---|
| `PaymentTransactionID` | UUID | Khóa chính giao dịch thanh toán ngoài |
| `UserID` | UUID | User tạo yêu cầu nạp tiền |
| `WalletID` | UUID | Ví sẽ được cộng tiền khi thanh toán thành công |
| `Provider` | ENUM | Cổng thanh toán, hiện tại là `VNPAY` |
| `Purpose` | ENUM | Mục đích thanh toán, hiện tại là `WALLET_DEPOSIT` |
| `Amount` | DECIMAL(15,2) | Số tiền user muốn nạp |
| `Currency` | VARCHAR(10) | Loại tiền, ví dụ `VND` |
| `Status` | ENUM | Trạng thái payment transaction |
| `VnpTxnRef` | VARCHAR(100) | Mã giao dịch merchant gửi sang VNPAY, phải unique |
| `VnpOrderInfo` | VARCHAR(255) | Nội dung thanh toán gửi sang VNPAY |
| `PaymentUrl` | TEXT | URL redirect sang VNPAY |
| `ClientIp` | VARCHAR(45) | IP của user khi tạo thanh toán |
| `ExpireAt` | DATETIME | Thời điểm hết hạn thanh toán |
| `CreatedAt` | TIMESTAMP | Thời điểm tạo |
| `UpdatedAt` | TIMESTAMP | Thời điểm cập nhật |
| `ReturnReceivedAt` | TIMESTAMP | Thời điểm BE/FE nhận ReturnUrl nếu có |
| `IpnReceivedAt` | TIMESTAMP | Thời điểm nhận IPN |
| `CompletedAt` | TIMESTAMP | Thời điểm xử lý thành công cuối cùng |
| `VnpAmount` | BIGINT | Số tiền VNPAY trả về, thường là amount * 100 |
| `VnpResponseCode` | VARCHAR(10) | Mã phản hồi từ VNPAY |
| `VnpTransactionStatus` | VARCHAR(10) | Trạng thái giao dịch từ VNPAY |
| `VnpTransactionNo` | VARCHAR(50) | Mã giao dịch tại VNPAY |
| `VnpBankCode` | VARCHAR(20) | Mã ngân hàng thanh toán |
| `VnpBankTranNo` | VARCHAR(255) | Mã giao dịch tại ngân hàng |
| `VnpCardType` | VARCHAR(20) | Loại thẻ/tài khoản, ví dụ ATM/QRCODE |
| `VnpPayDate` | DATETIME | Thời gian thanh toán bên VNPAY |
| `ReturnPayload` | JSON | Payload nhận từ ReturnUrl |
| `IpnPayload` | JSON | Payload nhận từ IPN |
| `WalletTransactionID` | UUID | FK nullable tới bảng `transactions` sau khi ví đã được cộng tiền |
| `FailureReason` | TEXT | Lý do thất bại nếu có |

---

## 5. Enum đề xuất

### PaymentProvider

```java
public enum PaymentProvider {
    VNPAY
}
```

### PaymentPurpose

```java
public enum PaymentPurpose {
    WALLET_DEPOSIT
}
```

Sau này nếu muốn dùng VNPAY để thanh toán hóa đơn trực tiếp thì có thể thêm:

```java
INVOICE_PAYMENT
```

### PaymentTransactionStatus

```java
public enum PaymentTransactionStatus {
    CREATED,
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED
}
```

---

## 6. SQL migration đề xuất

```sql
CREATE TABLE payment_transactions (
    payment_transaction_id CHAR(36) PRIMARY KEY,

    user_id CHAR(36) NOT NULL,
    wallet_id CHAR(36) NOT NULL,

    provider ENUM('VNPAY') NOT NULL,
    purpose ENUM('WALLET_DEPOSIT') NOT NULL,

    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',

    status ENUM(
        'CREATED',
        'PENDING',
        'SUCCESS',
        'FAILED',
        'CANCELLED',
        'EXPIRED'
    ) NOT NULL DEFAULT 'CREATED',

    vnp_txn_ref VARCHAR(100) NOT NULL UNIQUE,
    vnp_order_info VARCHAR(255) NOT NULL,

    payment_url TEXT NULL,
    client_ip VARCHAR(45) NULL,
    expire_at DATETIME NULL,

    vnp_amount BIGINT NULL,
    vnp_response_code VARCHAR(10) NULL,
    vnp_transaction_status VARCHAR(10) NULL,
    vnp_transaction_no VARCHAR(50) NULL,
    vnp_bank_code VARCHAR(20) NULL,
    vnp_bank_tran_no VARCHAR(255) NULL,
    vnp_card_type VARCHAR(20) NULL,
    vnp_pay_date DATETIME NULL,

    return_payload JSON NULL,
    ipn_payload JSON NULL,

    wallet_transaction_id CHAR(36) NULL,

    failure_reason TEXT NULL,

    return_received_at TIMESTAMP NULL,
    ipn_received_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_transactions_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),

    CONSTRAINT fk_payment_transactions_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id),

    CONSTRAINT fk_payment_transactions_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES transactions(transaction_id)
);
```

Nếu MySQL của nhóm chưa muốn dùng kiểu `JSON`, có thể đổi:

```sql
return_payload TEXT NULL,
ipn_payload TEXT NULL
```

---

## 7. Có cần sửa bảng Wallet không?

Nếu hệ thống đã có bảng `wallets` với `wallet_purpose` thì không cần sửa thêm.

Ví user nạp tiền vẫn là:

```text
Owner/Jockey Wallet:
OwnerType = USER
WalletPurpose = USER_MAIN
```

VNPAY là nguồn tiền bên ngoài nên không cần tạo ví VNPAY trong hệ thống.

---

## 8. Có cần sửa bảng Transaction không?

Không bắt buộc.

Vì `PaymentTransaction` đã có:

```text
WalletTransactionID
```

Sau khi VNPAY thành công, BE tạo một `Transaction`:

```text
Type = DEPOSIT
Direction = CREDIT
WalletID = UserWallet
CounterpartyType = EXTERNAL
CounterpartyWalletID = null
Amount = số tiền nạp
Status = SUCCESS
```

Sau đó cập nhật:

```text
PaymentTransaction.WalletTransactionID = TransactionID vừa tạo
```

Như vậy vẫn trace được:

```text
PaymentTransaction → Transaction
```

---

## 9. Flow chi tiết nạp tiền bằng VNPAY

### Bước 1: FE gọi API tạo yêu cầu nạp tiền

```http
POST /api/wallets/deposits/vnpay/create
Authorization: Bearer <token>
Content-Type: application/json
```

Request:

```json
{
  "amount": 1000000
}
```

BE xử lý:

```text
1. Lấy current user
2. Lấy USER_MAIN wallet của user
3. Validate amount >= số tiền tối thiểu
4. Tạo vnp_TxnRef unique
5. Tạo PaymentTransaction status = PENDING
6. Build paymentUrl sang VNPAY
7. Lưu paymentUrl
8. Trả paymentUrl cho FE
```

Response:

```json
{
  "code": 1000,
  "message": "Create VNPAY payment successfully",
  "result": {
    "paymentTransactionId": "uuid",
    "vnpTxnRef": "DEP202606250001",
    "amount": 1000000,
    "status": "PENDING",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..."
  }
}
```

---

### Bước 2: FE redirect user sang VNPAY

FE nhận `paymentUrl` rồi redirect:

```javascript
window.location.href = response.result.paymentUrl;
```

---

### Bước 3: User thanh toán trên VNPAY Sandbox

Dùng thẻ test VNPAY.

Ví dụ thẻ NCB thành công:

```text
Ngân hàng: NCB
Số thẻ: 9704198526191432198
Tên chủ thẻ: NGUYEN VAN A
Ngày phát hành: 07/15
OTP: 123456
```

---

### Bước 4: VNPAY redirect user về ReturnUrl

ReturnUrl là redirect trình duyệt.

Ví dụ:

```text
http://localhost:5173/payment/vnpay-return?...params...
```

Hoặc nếu muốn BE nhận return trước:

```text
https://your-domain/api/payments/vnpay/return?...params...
```

Lưu ý:

```text
ReturnUrl chỉ dùng để hiển thị kết quả cho user.
Không nên cộng tiền vào ví chỉ dựa vào ReturnUrl.
```

---

### Bước 5: VNPAY gọi IPN về BE

IPN là server-to-server.

Endpoint đề xuất:

```http
GET /api/payments/vnpay/ipn
```

Endpoint này phải public, không yêu cầu JWT.

BE xử lý IPN:

```text
1. Nhận toàn bộ vnp_* params
2. Verify vnp_SecureHash
3. Tìm PaymentTransaction bằng vnp_TxnRef
4. Kiểm tra amount từ VNPAY == PaymentTransaction.amount * 100
5. Kiểm tra PaymentTransaction chưa SUCCESS
6. Nếu vnp_ResponseCode = 00 và vnp_TransactionStatus = 00:
     - Lock wallet
     - Cộng tiền vào wallet
     - Tạo Transaction Type = DEPOSIT
     - Cập nhật PaymentTransaction = SUCCESS
7. Nếu không thành công:
     - Cập nhật PaymentTransaction = FAILED
8. Trả JSON RspCode/Message cho VNPAY
```

---

## 10. Idempotency: chống cộng tiền 2 lần

VNPAY có thể gọi IPN nhiều lần nếu hệ thống merchant trả lỗi hoặc timeout. Vì vậy BE phải xử lý idempotent.

Rule:

```text
Nếu PaymentTransaction.Status = SUCCESS
→ Không cộng tiền lần nữa
→ Trả RspCode = 02 hoặc response phù hợp theo policy IPN
```

Cần lock record khi xử lý:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<PaymentTransaction> findByVnpTxnRef(String vnpTxnRef);
```

Và xử lý trong:

```java
@Transactional
```

---

## 11. Các params chính gửi sang VNPAY

Các params thường dùng khi tạo URL:

| Param | Ý nghĩa |
|---|---|
| `vnp_Version` | Phiên bản API, thường `2.1.0` |
| `vnp_Command` | Lệnh thanh toán, `pay` |
| `vnp_TmnCode` | Mã website VNPAY cấp |
| `vnp_Amount` | Số tiền * 100 |
| `vnp_CurrCode` | `VND` |
| `vnp_TxnRef` | Mã giao dịch unique phía merchant |
| `vnp_OrderInfo` | Nội dung thanh toán, nên không dấu |
| `vnp_OrderType` | Loại hàng hóa |
| `vnp_Locale` | `vn` hoặc `en` |
| `vnp_ReturnUrl` | URL user được redirect về sau thanh toán |
| `vnp_IpAddr` | IP user |
| `vnp_CreateDate` | Thời điểm tạo giao dịch, yyyyMMddHHmmss |
| `vnp_ExpireDate` | Thời điểm hết hạn |
| `vnp_SecureHash` | Chữ ký checksum |

Lưu ý quan trọng:

```text
vnp_Amount gửi sang VNPAY = amount * 100
```

Ví dụ user nạp 1.000.000 VND:

```text
vnp_Amount = 100000000
```

---

## 12. Tạo SecureHash

Nguyên tắc:

```text
1. Lấy tất cả params vnp_*
2. Bỏ vnp_SecureHash và vnp_SecureHashType nếu có
3. Sort theo tên param tăng dần
4. Build query string
5. HMAC SHA512 với vnp_HashSecret
6. Gắn kết quả vào vnp_SecureHash
```

Pseudo Java:

```java
String signData = buildSortedQueryString(vnpParams);
String secureHash = hmacSHA512(vnpHashSecret, signData);
vnpParams.put("vnp_SecureHash", secureHash);
```

Không được hard-code `vnp_HashSecret` trong code. Để trong biến môi trường hoặc `application-local.yml`.

---

## 13. Config Spring Boot đề xuất

```yaml
vnpay:
  pay-url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  tmn-code: ${VNPAY_TMN_CODE}
  hash-secret: ${VNPAY_HASH_SECRET}
  return-url: http://localhost:5173/payment/vnpay-return
  ipn-url: https://your-ngrok-domain.ngrok-free.app/api/payments/vnpay/ipn
  version: 2.1.0
  command: pay
  order-type: other
  locale: vn
  expire-minutes: 15
```

Không commit file chứa secret thật lên GitHub.

Nên dùng:

```text
application.yml
application-local.yml
.env
```

và đưa secret vào `.gitignore`.

---

## 14. API cần thêm

### 14.1. Tạo payment URL

```http
POST /api/wallets/deposits/vnpay/create
```

Dùng cho FE.

### 14.2. Nhận IPN từ VNPAY

```http
GET /api/payments/vnpay/ipn
```

Public endpoint, không cần JWT.

### 14.3. Return URL

Có 2 cách.

Cách 1: ReturnUrl về FE:

```text
http://localhost:5173/payment/vnpay-return
```

FE đọc query params và gọi BE check status.

Cách 2: ReturnUrl về BE:

```text
GET /api/payments/vnpay/return
```

BE verify hash rồi redirect FE.

Với đồ án, cách 1 dễ hơn.

### 14.4. FE check trạng thái payment

```http
GET /api/wallets/deposits/{paymentTransactionId}
```

Hoặc:

```http
GET /api/wallets/deposits/by-txn-ref/{vnpTxnRef}
```

---

## 15. Security config

Các endpoint này phải permitAll:

```text
GET /api/payments/vnpay/ipn
GET /api/payments/vnpay/return
```

Ví dụ:

```java
.requestMatchers(HttpMethod.GET, "/api/payments/vnpay/ipn").permitAll()
.requestMatchers(HttpMethod.GET, "/api/payments/vnpay/return").permitAll()
```

Không permitAll endpoint tạo payment:

```text
POST /api/wallets/deposits/vnpay/create
```

Endpoint này phải cần JWT vì phải biết user nào đang nạp tiền.

---

## 16. Luồng cập nhật ví khi IPN thành công

Pseudo service:

```java
@Transactional
public VnpayIpnResponse handleIpn(Map<String, String> params) {
    if (!vnpayService.verifySecureHash(params)) {
        return new VnpayIpnResponse("97", "Invalid checksum");
    }

    String txnRef = params.get("vnp_TxnRef");

    PaymentTransaction payment = paymentTransactionRepository
            .findByVnpTxnRefForUpdate(txnRef)
            .orElse(null);

    if (payment == null) {
        return new VnpayIpnResponse("01", "Order not found");
    }

    if (payment.getStatus() == PaymentTransactionStatus.SUCCESS) {
        return new VnpayIpnResponse("02", "Order already confirmed");
    }

    BigDecimal vnpAmount = new BigDecimal(params.get("vnp_Amount"))
            .divide(BigDecimal.valueOf(100));

    if (vnpAmount.compareTo(payment.getAmount()) != 0) {
        return new VnpayIpnResponse("04", "Invalid amount");
    }

    boolean success = "00".equals(params.get("vnp_ResponseCode"))
            && "00".equals(params.get("vnp_TransactionStatus"));

    if (!success) {
        payment.setStatus(PaymentTransactionStatus.FAILED);
        payment.setFailureReason("VNPAY payment failed");
        return new VnpayIpnResponse("00", "Confirm success");
    }

    Wallet wallet = walletRepository
            .findByIdForUpdate(payment.getWalletId())
            .orElseThrow();

    BigDecimal before = wallet.getBalance();
    BigDecimal after = before.add(payment.getAmount());

    wallet.setBalance(after);

    Transaction tx = Transaction.builder()
            .wallet(wallet)
            .type(TransactionType.DEPOSIT)
            .direction(TransactionDirection.CREDIT)
            .amount(payment.getAmount())
            .balanceBefore(before)
            .balanceAfter(after)
            .counterpartyType(CounterpartyType.EXTERNAL)
            .status(TransactionStatus.SUCCESS)
            .note("Deposit via VNPAY, txnRef=" + payment.getVnpTxnRef())
            .build();

    transactionRepository.save(tx);

    payment.setWalletTransactionId(tx.getTransactionId());
    payment.setStatus(PaymentTransactionStatus.SUCCESS);
    payment.setCompletedAt(LocalDateTime.now());

    return new VnpayIpnResponse("00", "Confirm success");
}
```

---

## 17. ErrorCode nên thêm

```java
VNPAY_PAYMENT_NOT_FOUND(2501, HttpStatus.NOT_FOUND, "VNPAY payment transaction not found"),
VNPAY_INVALID_SIGNATURE(2502, HttpStatus.BAD_REQUEST, "Invalid VNPAY secure hash"),
VNPAY_INVALID_AMOUNT(2503, HttpStatus.BAD_REQUEST, "Invalid VNPAY amount"),
VNPAY_PAYMENT_FAILED(2504, HttpStatus.BAD_REQUEST, "VNPAY payment failed"),
VNPAY_PAYMENT_ALREADY_CONFIRMED(2505, HttpStatus.CONFLICT, "VNPAY payment already confirmed"),
VNPAY_CONFIG_MISSING(2506, HttpStatus.INTERNAL_SERVER_ERROR, "VNPAY configuration is missing"),
VNPAY_PAYMENT_EXPIRED(2507, HttpStatus.BAD_REQUEST, "VNPAY payment has expired")
```

---

## 18. Cách đăng ký tài khoản test VNPAY Sandbox

### Bước 1: Vào trang đăng ký test

Mở trang:

```text
https://sandbox.vnpayment.vn/devreg/
```

### Bước 2: Điền thông tin

Thông thường form sẽ yêu cầu:

```text
Tên website
Địa chỉ URL
Email đăng ký
Mật khẩu
Nhập lại mật khẩu
Mã xác nhận/captcha
```

Nếu đang chạy local, phần URL có thể nhập domain public tạm như ngrok domain, ví dụ:

```text
https://abc123.ngrok-free.app
```

Hoặc nhập URL FE/BE public mà nhóm dùng để test.

### Bước 3: Nhận thông tin cấu hình

Sau khi đăng ký, cần lấy các thông tin:

```text
vnp_TmnCode
vnp_HashSecret
URL thanh toán sandbox
URL API truy vấn/hoàn trả nếu cần
```

Trong dự án nạp tiền ví, bắt buộc cần:

```text
vnp_TmnCode
vnp_HashSecret
URL thanh toán
```

### Bước 4: Cấu hình IPN URL

Trong portal merchant sandbox, cấu hình IPN URL về BE.

Ví dụ nếu dùng ngrok:

```text
https://abc123.ngrok-free.app/api/payments/vnpay/ipn
```

IPN URL phải truy cập được từ Internet, vì VNPAY server sẽ gọi vào endpoint này.

### Bước 5: Test bằng thẻ sandbox

Dùng thẻ test chính thức từ trang demo VNPAY.

Thẻ test thành công phổ biến:

```text
Ngân hàng: NCB
Số thẻ: 9704198526191432198
Tên chủ thẻ: NGUYEN VAN A
Ngày phát hành: 07/15
OTP: 123456
```

---

## 19. Có cần ngrok không?

Nếu backend đang chạy local:

```text
http://localhost:8080
```

thì VNPAY server không gọi được IPN vào máy bạn.

Do đó nên dùng ngrok để public tạm backend:

```bash
ngrok http 8080
```

Sau đó lấy URL HTTPS ngrok:

```text
https://abc123.ngrok-free.app
```

Cấu hình:

```yaml
vnpay:
  ipn-url: https://abc123.ngrok-free.app/api/payments/vnpay/ipn
```

Ngrok chỉ là đường hầm tạm thời vào local backend. Nó không tự sửa database hay code của project.

Lưu ý khi dùng ngrok:

```text
Không public các endpoint admin nguy hiểm nếu không cần.
Không để secret trong URL.
Tắt ngrok khi test xong.
Mỗi lần restart ngrok free có thể đổi domain.
```

---

## 20. Test checklist

### Test thành công

```text
1. Owner login
2. Owner mở Wallet
3. Owner nhập 1.000.000 VND
4. FE gọi create VNPAY payment
5. FE redirect sang VNPAY
6. Thanh toán bằng thẻ NCB test thành công
7. VNPAY gọi IPN
8. BE verify checksum thành công
9. PaymentTransaction = SUCCESS
10. Wallet balance +1.000.000
11. Transaction DEPOSIT được tạo
```

### Test thất bại

Dùng thẻ không đủ số dư hoặc thẻ lỗi.

Kỳ vọng:

```text
PaymentTransaction = FAILED
Wallet balance không đổi
Không tạo Transaction DEPOSIT
```

### Test IPN gọi lại

Gọi lại IPN cùng `vnp_TxnRef`.

Kỳ vọng:

```text
Không cộng tiền lần hai
Không tạo Transaction lần hai
```

---

## 21. Các lỗi hay gặp

### 1. Sai checksum

Nguyên nhân thường gặp:

```text
Sort params sai
Encode query string sai
Dùng sai HashSecret
Quên bỏ vnp_SecureHash khi verify
```

### 2. Sai amount

VNPAY dùng:

```text
vnp_Amount = amount * 100
```

Nếu user nạp 100.000 VND thì gửi:

```text
vnp_Amount = 10000000
```

### 3. IPN không được gọi

Nguyên nhân:

```text
Backend chạy localhost
Chưa dùng ngrok
Sai IPN URL trong portal merchant
Endpoint IPN bị chặn bởi JWT/Security
```

### 4. Ví bị cộng tiền hai lần

Nguyên nhân:

```text
Không check PaymentTransaction.Status trước khi cộng tiền
Không lock PaymentTransaction
Không lưu WalletTransactionID
```

---

## 22. Kết luận thiết kế

Nên thêm:

```text
PaymentTransaction
```

Không nên dùng trực tiếp `Transaction` để lưu giao dịch VNPAY pending.

Flow đúng:

```text
PaymentTransaction PENDING
↓
VNPAY IPN SUCCESS
↓
Wallet balance tăng
↓
Transaction DEPOSIT CREDIT
↓
PaymentTransaction SUCCESS
```

Đây là cách dễ giải thích, dễ demo và an toàn hơn mock deposit.
