package com.senim.backend.repository;

import com.senim.backend.domain.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    Optional<Agency> findByName(String name);

    boolean existsByName(String name);
}
