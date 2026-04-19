package com.senim.backend.repository;

import com.senim.backend.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findAllByDealId(UUID dealId);

    long countByDealId(UUID dealId);
}
