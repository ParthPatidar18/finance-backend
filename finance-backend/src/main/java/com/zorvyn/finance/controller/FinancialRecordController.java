package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.ApiResponse;
import com.zorvyn.finance.dto.FinancialRecordRequest;
import com.zorvyn.finance.dto.FinancialRecordResponse;
import com.zorvyn.finance.enums.TransactionType;
import com.zorvyn.finance.service.FinancialRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/records")
public class FinancialRecordController {

    @Autowired
    private FinancialRecordService recordService;


    @GetMapping
    public ResponseEntity<ApiResponse<Page<FinancialRecordResponse>>> getRecords(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (size > 100) size = 100; // Cap page size to prevent abuse
        Page<FinancialRecordResponse> records =
                recordService.getRecords(type, category, startDate, endDate, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Records retrieved", records));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> getRecord(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Record retrieved", recordService.getRecordById(id)));
    }


    @PostMapping
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> createRecord(
            @Valid @RequestBody FinancialRecordRequest request) {
        FinancialRecordResponse created = recordService.createRecord(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Record created successfully", created));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody FinancialRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Record updated successfully", recordService.updateRecord(id, request)));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.ok(ApiResponse.success("Record deleted successfully", null));
    }
}
