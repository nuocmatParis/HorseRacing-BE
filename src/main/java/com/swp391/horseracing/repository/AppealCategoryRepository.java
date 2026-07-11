package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.AppealCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppealCategoryRepository extends JpaRepository<AppealCategory, UUID> {

    boolean existsByCode(String code);

    List<AppealCategory> findByIsActiveTrue();
}
