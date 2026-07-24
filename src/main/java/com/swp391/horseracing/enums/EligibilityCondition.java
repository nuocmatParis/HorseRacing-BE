package com.swp391.horseracing.enums;

public enum EligibilityCondition {
    AGE("năm tuổi"),
    WEIGHT("kg"),
    BREED(""),
    WIN_RATE("%"),
    JOCKEY_TIER(""),
    EXPERIENCE_YEARS("năm kinh nghiệm");

    private final String unit;

    EligibilityCondition(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }
}
