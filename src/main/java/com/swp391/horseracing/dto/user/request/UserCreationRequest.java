package com.swp391.horseracing.dto.user.request;

import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.RoleName;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @NotNull(message = "Role is required")
    RoleName roleName;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 15, message = "Username must be between 4 and 15 characters")
    String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
    String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email;

    @Past(message = "Date of birth must be in the past")
    LocalDate dob;

    @NotNull(message = "Gender is required")
    Gender gender;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[35789]//d{8}$", message = "Phone number is invalid")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phoneNumber;
}
