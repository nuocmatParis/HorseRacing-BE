package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.handicap.response.HandicapResult;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.service.HandicapService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class HandicapServiceImpl implements HandicapService {

    private static final double LBS_TO_KG_FACTOR = 0.45359237;

    @Override
    public HandicapResult calculateHandicap(Tournament tournament, int topRatingInRace, int horseRating, double jockeyWeightKg) {
        double assignedWeightLbs;

        if (tournament.isHandicapEnabled()) {
            // AssignedWeightLbs = TopWeightLbs - (TopRatingInRace - HorseRating)
            assignedWeightLbs = tournament.getTopWeightLbs() - (topRatingInRace - horseRating);

            if (assignedWeightLbs > tournament.getTopWeightLbs()) {
                assignedWeightLbs = tournament.getTopWeightLbs();
            }

            if (assignedWeightLbs < tournament.getMinWeightLbs()) {
                assignedWeightLbs = tournament.getMinWeightLbs();
            }
        } else {
            // Handicap is disabled, assigned weight is simply the TopWeightLbs of the tournament
            assignedWeightLbs = tournament.getTopWeightLbs();
        }

        double assignedWeightKg = assignedWeightLbs * LBS_TO_KG_FACTOR;

        // BallastKg = AssignedWeightKg - JockeyWeightKg - EquipmentWeightKg
        double ballastKg = assignedWeightKg - jockeyWeightKg - tournament.getEquipmentWeightKg();

        // Round weights to 3 decimal places for clean formatting
        assignedWeightLbs = round(assignedWeightLbs, 3);
        assignedWeightKg = round(assignedWeightKg, 3);
        ballastKg = round(ballastKg, 3);

        String status;
        if (ballastKg > 0.0) {
            status = "Add more weight";
        } else if (ballastKg == 0.0) {
            status = "Good weight";
        } else {
            status = "Overweight";
        }

        return HandicapResult.builder()
                .assignedWeightLbs(assignedWeightLbs)
                .assignedWeightKg(assignedWeightKg)
                .ballastKg(ballastKg)
                .status(status)
                .build();
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
