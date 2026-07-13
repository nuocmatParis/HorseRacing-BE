package com.swp391.horseracing.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RaceClass {
    CLASS_5(0, 39),
    CLASS_4(40, 59),
    CLASS_3(60, 79),
    CLASS_2(80, 99),
    CLASS_1(100, 119),

    G3(120, 135),
    G2(136, 149),
    G1(150, null);

    Integer minRating;
    Integer maxRating;

    public boolean isEligible(int rating) {
        if (rating < minRating) {
            return false;
        }

        return maxRating == null || rating <= maxRating;
    }

    public static RaceClass fromRating(int rating) {
        for (RaceClass rc : values()) {
            if (rc.isEligible(rating)) {
                return rc;
            }
        }
        return CLASS_5;
    }
}
