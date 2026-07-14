package com.swp391.horseracing.dto.race_report.response;

import com.swp391.horseracing.enums.ReportStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceReportResponse {

    UUID reportId;
    UUID raceId;
    String raceName;
    UUID refereeId;
    String refereeName;
    String summary;
    String appealNote;
    ReportStatus status;
    UUID signedById;
    String signedByName;
    LocalDateTime signedAt;
    UUID publishedById;
    String publishedByName;
    LocalDateTime publishedAt;
    LocalDateTime createdAt;
}
