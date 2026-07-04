package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.handicap.response.HandicapResult;
import com.swp391.horseracing.entity.Tournament;

public interface HandicapService {
    HandicapResult calculateHandicap(Tournament tournament, int topRatingInRace, int horseRating, double jockeyWeightKg);
}
