package com.swp391.horseracing.dto.wallet.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPrizePoolTopUpRequest {

    @NotNull(message = "Số tiền nạp là bắt buộc")
    @DecimalMin(value = "1000", message = "Số tiền nạp tối thiểu là 1.000 VND")
    @Digits(integer = 13, fraction = 0, message = "Số tiền phải là số nguyên VND")
    BigDecimal amount;

    @NotBlank(message = "Lý do bổ sung quỹ là bắt buộc")
    @Size(min = 10, max = 500, message = "Lý do phải có từ 10 đến 500 ký tự")
    String reason;
}
