package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.AppealEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppealEvidenceRepository extends JpaRepository<AppealEvidence, UUID> {

    List<AppealEvidence> findByAppeal_AppealId(UUID appealId);
}
