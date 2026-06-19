package com.swp391.horseracing.dto.horseowner.request;

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
    @Size(max = 100)
    String farmName;

    String address;

    @Size(max = 50)
    String licenseNumber;
}
