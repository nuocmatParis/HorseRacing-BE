package com.swp391.horseracing.dto.horseowner.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OwnerUpdateRequest {
    @NotNull(message = "Farm name is required")
    @Size(max = 100)
    String farmName;

    @NotNull(message = "Address is required")
    String address;

}
