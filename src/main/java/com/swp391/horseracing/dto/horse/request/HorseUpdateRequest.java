package com.swp391.horseracing.dto.horse.request;

import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.enums.HorseBreed;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HorseUpdateRequest {

    @Size(max = 100, message = "Horse name must not exceed 100 characters")
    String name;

    HorseBreed breed;

    Gender gender;

    @Positive(message = "Age must be positive")
    Integer age;

    @Positive(message = "Weight must be positive")
    Float weight;

    @Size(max = 50, message = "Color must not exceed 50 characters")
    String color;

    HealthStatus healthStatus;

    @Size(max = 50, message = "Race class must not exceed 50 characters")
    String raceClass;
}
