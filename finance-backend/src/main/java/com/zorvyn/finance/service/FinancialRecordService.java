package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.FinancialRecordRequest;
import com.zorvyn.finance.dto.FinancialRecordResponse;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.enums.TransactionType;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class FinancialRecordService {

    @Autowired private FinancialRecordRepository recordRepository;
    @Autowired private UserRepository userRepository;

    public Page<FinancialRecordResponse> getRecords(TransactionType type,
                                                     String category,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     int page,
                                                     int size,
                                                     String sortBy,
                                                     String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        String resolvedSortBy = switch (sortBy) {
            case "amount" -> "amount";
            case "category" -> "category";
            case "date" -> "date";
            default -> "createdAt";
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));

        boolean hasFilters = type != null || category != null || startDate != null || endDate != null;

        Page<FinancialRecord> records = hasFilters
                ? recordRepository.findWithFilters(type, category, startDate, endDate, pageable)
                : recordRepository.findAllByDeletedFalse(pageable);

        return records.map(this::toResponse);
    }

    public FinancialRecordResponse getRecordById(Long id) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record not found with id: " + id));
        return toResponse(record);
    }

    @Transactional
    public FinancialRecordResponse createRecord(FinancialRecordRequest request) {
        User currentUser = getCurrentUser();

        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .notes(request.getNotes())
                .createdBy(currentUser)
                .deleted(false)
                .build();

        return toResponse(recordRepository.save(record));
    }

    @Transactional
    public FinancialRecordResponse updateRecord(Long id, FinancialRecordRequest request) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record not found with id: " + id));

        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory().trim());
        record.setDate(request.getDate());
        record.setNotes(request.getNotes());

        return toResponse(recordRepository.save(record));
    }

    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record not found with id: " + id));
         record.setDeleted(true);
        recordRepository.save(record);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + username));
    }

    public FinancialRecordResponse toResponse(FinancialRecord r) {
        return FinancialRecordResponse.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .type(r.getType())
                .category(r.getCategory())
                .date(r.getDate())
                .notes(r.getNotes())
                .createdBy(r.getCreatedBy().getUsername())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
