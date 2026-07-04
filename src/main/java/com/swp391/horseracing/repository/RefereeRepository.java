package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.enums.RefereeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefereeRepository extends JpaRepository<Referee, UUID> {
    List<Referee> findByStatus(RefereeStatus status);
}
