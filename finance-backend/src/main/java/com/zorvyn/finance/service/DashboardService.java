package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.DashboardSummaryResponse;
import com.zorvyn.finance.dto.FinancialRecordResponse;
import com.zorvyn.finance.enums.TransactionType;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired private FinancialRecordRepository recordRepository;
    @Autowired private FinancialRecordService recordService;

    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome    = recordRepository.sumTotalIncome();
        BigDecimal totalExpenses  = recordRepository.sumTotalExpenses();
        BigDecimal netBalance     = totalIncome.subtract(totalExpenses);

        Map<String, BigDecimal> incomeByCategory   = buildCategoryMap(TransactionType.INCOME);
        Map<String, BigDecimal> expensesByCategory = buildCategoryMap(TransactionType.EXPENSE);

        List<DashboardSummaryResponse.MonthlyTrendDto> trends = buildMonthlyTrends();

        List<FinancialRecordResponse> recent = recordRepository
                .findRecentActivity(PageRequest.of(0, 5))
                .stream()
                .map(recordService::toResponse)
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .incomeByCategory(incomeByCategory)
                .expensesByCategory(expensesByCategory)
                .monthlyTrends(trends)
                .recentActivity(recent)
                .build();
    }

    private Map<String, BigDecimal> buildCategoryMap(TransactionType type) {
        List<Object[]> rows = recordRepository.sumByCategory(type);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    private List<DashboardSummaryResponse.MonthlyTrendDto> buildMonthlyTrends() {
        List<Object[]> rows = recordRepository.monthlyTrends();

        // Group by year+month
        Map<String, DashboardSummaryResponse.MonthlyTrendDto> trendMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String type = row[2].toString();
            BigDecimal amount = (BigDecimal) row[3];

            String key = year + "-" + String.format("%02d", month);
            trendMap.putIfAbsent(key, DashboardSummaryResponse.MonthlyTrendDto.builder()
                    .year(year)
                    .month(month)
                    .income(BigDecimal.ZERO)
                    .expenses(BigDecimal.ZERO)
                    .net(BigDecimal.ZERO)
                    .build());

            DashboardSummaryResponse.MonthlyTrendDto dto = trendMap.get(key);
            if ("INCOME".equals(type)) {
                dto.setIncome(amount);
            } else {
                dto.setExpenses(amount);
            }
            dto.setNet(dto.getIncome().subtract(dto.getExpenses()));
        }

        return new ArrayList<>(trendMap.values());
    }
}
