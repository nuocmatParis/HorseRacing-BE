package com.swp391.horseracing.dto.HorseOwner.request;

import jakarta.validation.constraints.NotBlank;
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
public class OwnerCreationRequest {
    @Size(max = 100)
    String farmName;

    String address;

    @NotBlank
    @Size(max = 50)
    String licenseNumber;
}
