package com.swp391.horseracing.policy;

import com.swp391.horseracing.dto.bracket.RacePlan;
import com.swp391.horseracing.dto.bracket.RoundPlan;
import com.swp391.horseracing.entity.Tournament;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class RaceScheduleCalculator {

    private RaceScheduleCalculator() {
    }

    public static List<RoundPlan> scheduleRounds(List<RoundPlan> roundPlans,
                                                  Tournament tournament,
                                                  int preRaceBufferDays) {
        List<RoundPlan> result = new ArrayList<>();
        LocalDateTime currentStart = tournament.getCompetitionStartAt();

        for (int i = 0; i < roundPlans.size(); i++) {
            RoundPlan round = roundPlans.get(i);
            List<RacePlan> races = generateRaceSlots(
                    round.getRaceCount(),
                    currentStart,
                    tournament.getRaceDayStartTime(),
                    tournament.getRaceDayEndTime(),
                    tournament.getMinRaceIntervalMinutes(),
                    tournament.getDefaultRaceOperationalMinutes(),
                    tournament.getApplyBreakTime(),
                    tournament.getBreakStartTime(),
                    tournament.getBreakEndTime()
            );

            LocalDateTime roundEnd = races.isEmpty()
                    ? currentStart
                    : races.get(races.size() - 1).getEndTime();

            result.add(RoundPlan.builder()
                    .sequenceOrder(round.getSequenceOrder())
                    .roundName(round.getRoundName())
                    .raceCount(round.getRaceCount())
                    .entriesPerRace(round.getEntriesPerRace())
                    .qualifiersPerRace(round.getQualifiersPerRace())
                    .isFinal(round.isFinal())
                    .estimatedStartDate(races.isEmpty() ? currentStart : races.get(0).getStartTime())
                    .estimatedEndDate(roundEnd)
                    .races(races)
                    .build());

            if (i < roundPlans.size() - 1) {
                currentStart = calculateNextRoundStart(
                        roundEnd, preRaceBufferDays, tournament.getRaceDayStartTime());
            }
        }

        return result;
    }

    static List<RacePlan> generateRaceSlots(int raceCount,
                                             LocalDateTime startFrom,
                                             LocalTime dayStartTime,
                                             LocalTime dayEndTime,
                                             int minIntervalMinutes,
                                             int operationalMinutes,
                                             boolean applyBreak,
                                             LocalTime breakStart,
                                             LocalTime breakEnd) {
        List<RacePlan> races = new ArrayList<>();
        LocalDateTime cursor = startFrom;
        int racesToday = 0;
        LocalDate currentDate = cursor.toLocalDate();

        for (int seq = 1; seq <= raceCount; seq++) {
            cursor = alignToOperatingHours(cursor, dayStartTime, dayEndTime);

            if (!cursor.toLocalDate().equals(currentDate)) {
                currentDate = cursor.toLocalDate();
                racesToday = 0;
            }

            if (applyBreak && breakStart != null && breakEnd != null) {
                LocalTime cursorTime = cursor.toLocalTime();
                if (!cursorTime.isBefore(breakStart) && cursorTime.isBefore(breakEnd)) {
                    cursor = cursor.toLocalDate().atTime(breakEnd);
                }
            }

            LocalDateTime endTime = cursor.plusMinutes(operationalMinutes);

            if (endTime.toLocalTime().isAfter(dayEndTime)) {
                cursor = currentDate.plusDays(1).atTime(dayStartTime);
                currentDate = cursor.toLocalDate();
                racesToday = 0;
                endTime = cursor.plusMinutes(operationalMinutes);
            }

            races.add(RacePlan.builder()
                    .sequenceOrder(seq)
                    .name("Race " + seq)
                    .startTime(cursor)
                    .endTime(endTime)
                    .build());

            cursor = endTime.plusMinutes(minIntervalMinutes);
            racesToday++;
        }

        return races;
    }

    static LocalDateTime alignToOperatingHours(LocalDateTime dateTime,
                                                LocalTime dayStartTime,
                                                LocalTime dayEndTime) {
        LocalTime time = dateTime.toLocalTime();
        if (time.isBefore(dayStartTime)) {
            return dateTime.toLocalDate().atTime(dayStartTime);
        }
        if (time.isAfter(dayEndTime)) {
            return dateTime.toLocalDate().plusDays(1).atTime(dayStartTime);
        }
        return dateTime;
    }

    static LocalDateTime calculateNextRoundStart(LocalDateTime lastRaceEndTime,
                                                  int preRaceBufferDays,
                                                  LocalTime raceDayStartTime) {
        if (preRaceBufferDays <= 0) {
            return lastRaceEndTime;
        }
        return lastRaceEndTime.toLocalDate()
                .plusDays(preRaceBufferDays)
                .atTime(raceDayStartTime);
    }
}
