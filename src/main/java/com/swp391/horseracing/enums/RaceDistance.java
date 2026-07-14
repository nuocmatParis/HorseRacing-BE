package com.swp391.horseracing.enums;

import lombok.Getter;

@Getter
public enum RaceDistance {

    // SPRINT
    SPRINT_1000M(1000, Specialization.SPRINT),
    SPRINT_1200M(1200, Specialization.SPRINT),

    // MILE
    MILE_1400M(1400, Specialization.MILE),
    MILE_1600M(1600, Specialization.MILE),
    MILE_1800M(1800, Specialization.MILE),

    // INTERMEDIATE
    INTERMEDIATE_2000M(2000, Specialization.INTERMEDIATE),

    // LONG
    LONG_2200M(2200, Specialization.LONG),
    LONG_2400M(2400, Specialization.LONG),

    // EXTENDED
    EXTENDED_2800M(2800, Specialization.EXTENDED),
    EXTENDED_3000M(3000, Specialization.EXTENDED),
    EXTENDED_3200M(3200, Specialization.EXTENDED);

    private final int meters;
    private final Specialization category;

    RaceDistance(int meters, Specialization category) {
        this.meters = meters;
        this.category = category;
    }
}
