package com.senim.backend.repository;

import com.senim.backend.domain.Role;
import com.senim.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByAgencyIdAndRole(UUID agencyId, Role role);
}
