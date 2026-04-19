package com.senim.backend.repository;

import com.senim.backend.domain.ChecklistItem;
import com.senim.backend.domain.ChecklistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findAllByDealIdOrderBySortOrder(UUID dealId);

    long countByDealIdAndStatus(UUID dealId, ChecklistStatus status);
}
