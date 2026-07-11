package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.PredictionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PredictionDetailRepository extends JpaRepository<PredictionDetail, UUID> {

    List<PredictionDetail> findByPrediction_PredictionId(UUID predictionId);

    List<PredictionDetail> findByEntry_EntryId(UUID entryId);
}
