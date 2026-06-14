package com.swp391.horseracing.dto.horse.request;

import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.enums.HorseBreed;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HorseCreationRequest {

    @NotBlank(message = "Horse name is required")
    @Size(max = 100, message = "Horse name must not exceed 100 characters")
    String name;

    @NotNull(message = "Breed is required")
    HorseBreed breed;

    @NotNull(message = "Gender is required")
    Gender gender;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be at least 0")
    int age;

    @Positive(message = "Weight must be positive")
    Float weight;

    @Size(max = 50, message = "Color must not exceed 50 characters")
    String color;

    HealthStatus healthStatus;

    @Size(max = 50, message = "Race class must not exceed 50 characters")
    String raceClass;
}
