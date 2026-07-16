package com.swp391.horseracing.dto.prediction.response;

import com.swp391.horseracing.enums.AIPredictionPublicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIPredictionAggregateResponse {
    private UUID raceId;
    private String raceName;
    private LocalDateTime startTime;
    private AIPredictionPublicationStatus publicationStatus;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private LocalDateTime publishedAt;
    private String publishedBy;
    private List<AIPredictionResponse> predictions;
}
