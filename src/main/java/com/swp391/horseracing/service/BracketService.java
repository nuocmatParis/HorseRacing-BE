package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.bracket.BracketPreviewResponse;

import java.util.Map;
import java.util.UUID;

public interface BracketService {

    BracketPreviewResponse preview(UUID tournamentId, int actualEntries);

    void confirm(UUID tournamentId);

    void recalculate(UUID tournamentId, int actualEntries);
}
