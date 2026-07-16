package com.swp391.horseracing.simulation.api;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.simulation.realtime.RaceLiveQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicLiveRaceController {
    private final RaceLiveQueryService queryService;

    @GetMapping("/api/races/{raceId}/live-snapshot")
    public ApiResponse<LiveRaceSnapshotResponse> snapshot(@PathVariable UUID raceId) {
        return ApiResponse.<LiveRaceSnapshotResponse>builder()
                .result(queryService.snapshot(raceId)).build();
    }

    @GetMapping("/api/public/races/live")
    public ApiResponse<List<LiveRaceSummaryResponse>> liveRaces() {
        return ApiResponse.<List<LiveRaceSummaryResponse>>builder()
                .result(queryService.liveRaces()).build();
    }
}
