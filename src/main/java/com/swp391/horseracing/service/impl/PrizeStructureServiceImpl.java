package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreatePrizeStructureRequest;
import com.swp391.horseracing.dto.tournament.response.PrizeStructureResponse;
import com.swp391.horseracing.entity.PrizeStructure;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.PrizeStructureMapper;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.PrizeStructureService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PrizeStructureServiceImpl implements PrizeStructureService {

    PrizeStructureRepository prizeStructureRepository;
    TournamentRepository tournamentRepository;
    PrizeStructureMapper prizeStructureMapper;

    @Override
    @Transactional
    public PrizeStructureResponse create(UUID tournamentId, CreatePrizeStructureRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        PrizeStructure prizeStructure = prizeStructureMapper.toPrizeStructure(request);
        prizeStructure.setTournament(tournament);

        return prizeStructureMapper.toPrizeStructureResponse(prizeStructureRepository.save(prizeStructure));
    }
}
