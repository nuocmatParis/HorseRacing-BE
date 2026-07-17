package com.swp391.horseracing.dto.race_portal;

import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;
import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.RaceClass;
import com.swp391.horseracing.enums.RaceDistance;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TournamentInspectionConditionsResponse {
    private HorseBreed allowedBreed;
    private int minHorseAge;
    private int maxHorseAge;
    private RaceClass raceClass;
    private RaceDistance distance;
    private boolean handicapEnabled;
    private int topWeightLbs;
    private int minWeightLbs;
    private double equipmentWeightKg;
    private List<TournamentEligibilityResponse> eligibilityRules;
}
