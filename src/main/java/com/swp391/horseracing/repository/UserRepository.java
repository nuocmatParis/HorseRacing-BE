package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(UUID userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByStatusAndRole_RoleNameIn(AccountStatus status, Collection<RoleName> roleNames);
}
