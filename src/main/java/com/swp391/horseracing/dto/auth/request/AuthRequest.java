package com.swp391.horseracing.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthRequest {

    @Size(min = 4, max = 15, message = "INVALID_USERNAME")
    String username;

    @Size(min = 8, max = 255, message = "INVALID_PASSWORD")
    String password;
}
