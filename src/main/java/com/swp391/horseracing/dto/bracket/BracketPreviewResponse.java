package com.swp391.horseracing.dto.bracket;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BracketPreviewResponse {
    BracketStructure bracket;
    Map<String, Integer> phaseConfigs;
}
