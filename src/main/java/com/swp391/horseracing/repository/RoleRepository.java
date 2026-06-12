package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Role;
import com.swp391.horseracing.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByRoleName(RoleName roleName);

    boolean existsByRoleName(RoleName roleName);
}
