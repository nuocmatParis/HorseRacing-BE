package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.JockeyHorseContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JockeyHorseContractRepository extends JpaRepository<JockeyHorseContract, UUID> {
}
