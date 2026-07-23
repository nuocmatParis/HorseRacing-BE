package com.swp391.horseracing.policy;

import com.swp391.horseracing.dto.bracket.BracketStructure;
import com.swp391.horseracing.dto.bracket.RoundPlan;

import java.util.ArrayList;
import java.util.List;

public final class BracketCalculator {

    private BracketCalculator() {
    }

    public static BracketStructure calculate(int totalEntries,
                                              int maxEntriesPerRace,
                                              int qualifiersPerRace) {
        List<RoundPlan> rounds = new ArrayList<>();
        int remaining = totalEntries;
        int races = (int) Math.ceil((double) remaining / maxEntriesPerRace);
        int seq = 1;

        while (true) {
            int entriesPerRace = (int) Math.ceil((double) remaining / races);

            if (races == 1 && remaining <= maxEntriesPerRace) {
                rounds.add(RoundPlan.builder()
                        .sequenceOrder(seq)
                        .roundName("Chung Kết")
                        .raceCount(1)
                        .entriesPerRace(remaining)
                        .qualifiersPerRace(0)
                        .isFinal(true)
                        .build());
                break;
            }

            rounds.add(RoundPlan.builder()
                    .sequenceOrder(seq)
                    .roundName("Vòng " + seq)
                    .raceCount(races)
                    .entriesPerRace(entriesPerRace)
                    .qualifiersPerRace(qualifiersPerRace)
                    .isFinal(false)
                    .build());

            int qualifiers = races * qualifiersPerRace;

            if (qualifiers <= maxEntriesPerRace) {
                rounds.add(RoundPlan.builder()
                        .sequenceOrder(seq + 1)
                        .roundName("Chung Kết")
                        .raceCount(1)
                        .entriesPerRace(qualifiers)
                        .qualifiersPerRace(0)
                        .isFinal(true)
                        .build());
                break;
            }

            remaining = qualifiers;
            races = (int) Math.ceil((double) qualifiers / maxEntriesPerRace);
            seq++;
        }

        return BracketStructure.builder()
                .totalEntries(totalEntries)
                .roundCount(rounds.size())
                .rounds(rounds)
                .build();
    }
}
