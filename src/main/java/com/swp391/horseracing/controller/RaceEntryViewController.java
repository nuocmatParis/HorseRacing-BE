package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.race_entry.response.RaceEntryResponse;
import com.swp391.horseracing.service.RaceEntryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/races")
public class RaceEntryViewController {

    RaceEntryService raceEntryService;

    @GetMapping("/{raceId}/entries")
    public ApiResponse<List<RaceEntryResponse>> getEntriesByRace(@PathVariable UUID raceId) {
        return ApiResponse.<List<RaceEntryResponse>>builder()
                .result(raceEntryService.getEntriesByRaceId(raceId))
                .build();
    }
}
