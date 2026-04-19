package com.senim.backend.service;

import com.senim.backend.domain.ChecklistItem;
import com.senim.backend.domain.ChecklistStatus;
import com.senim.backend.dto.ChecklistItemResponse;
import com.senim.backend.exception.BusinessRuleException;
import com.senim.backend.exception.ResourceNotFoundException;
import com.senim.backend.repository.ChecklistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChecklistService {

    /**
     * Standard Kazakhstan mortgage checklist items.
     * Format: {itemKey, displayName, sortOrder}
     */
    private static final List<Object[]> DEFAULT_ITEMS = List.of(
            new Object[]{"passport",              "Паспорт / Удостоверение личности",  1},
            new Object[]{"income_proof",          "Справка о доходах",                 2},
            new Object[]{"employment_letter",     "Справка с места работы",            3},
            new Object[]{"property_valuation",    "Отчёт об оценке недвижимости",      4},
            new Object[]{"property_title",        "Правоустанавливающий документ",     5},
            new Object[]{"bank_statement",        "Выписка по счёту (6 месяцев)",      6},
            new Object[]{"marriage_certificate",  "Свидетельство о браке (если есть)", 7}
    );

    private final ChecklistItemRepository checklistItemRepository;

    public ChecklistService(ChecklistItemRepository checklistItemRepository) {
        this.checklistItemRepository = checklistItemRepository;
    }

    /**
     * Returns all checklist items for a deal, ordered by sortOrder ascending.
     */
    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> getChecklistForDeal(UUID dealId) {
        return checklistItemRepository.findAllByDealIdOrderBySortOrder(dealId).stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    /**
     * Associates a document with a checklist item and transitions it to UPLOADED.
     * Only items currently in MISSING or PENDING state may be uploaded.
     */
    @Transactional
    public ChecklistItemResponse markItemUploaded(UUID itemId, UUID documentId) {
        ChecklistItem item = loadItem(itemId);

        if (item.getStatus() == ChecklistStatus.VERIFIED) {
            throw new BusinessRuleException(
                    "Checklist item is already verified and cannot be re-uploaded");
        }

        item.setDocumentId(documentId);
        item.setStatus(ChecklistStatus.UPLOADED);
        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    /**
     * Marks a checklist item as VERIFIED by the given user.
     * Only UPLOADED items can be verified.
     */
    @Transactional
    public ChecklistItemResponse markItemVerified(UUID itemId, UUID verifiedByUserId) {
        ChecklistItem item = loadItem(itemId);

        if (item.getStatus() != ChecklistStatus.UPLOADED) {
            throw new BusinessRuleException(
                    "Only UPLOADED items can be verified — current status: " + item.getStatus());
        }

        item.setStatus(ChecklistStatus.VERIFIED);
        item.setVerifiedAt(Instant.now());
        item.setVerifiedByUserId(verifiedByUserId);
        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    /**
     * Returns true if no item is in MISSING or PENDING state.
     * A deal cannot be submitted unless this returns true.
     */
    @Transactional(readOnly = true)
    public boolean isChecklistComplete(UUID dealId) {
        long incomplete = checklistItemRepository.countByDealIdAndStatus(dealId, ChecklistStatus.MISSING)
                + checklistItemRepository.countByDealIdAndStatus(dealId, ChecklistStatus.PENDING);
        return incomplete == 0;
    }

    /**
     * Seeds the standard Kazakhstan mortgage checklist for a newly created deal.
     * All items start in MISSING status.
     */
    @Transactional
    public List<ChecklistItemResponse> initializeDefaultChecklist(UUID dealId) {
        List<ChecklistItem> items = DEFAULT_ITEMS.stream()
                .map(row -> ChecklistItem.builder()
                        .dealId(dealId)
                        .itemKey((String) row[0])
                        .itemName((String) row[1])
                        .sortOrder((int) row[2])
                        .status(ChecklistStatus.MISSING)
                        .build())
                .toList();

        return checklistItemRepository.saveAll(items).stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    private ChecklistItem loadItem(UUID itemId) {
        return checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));
    }
}
