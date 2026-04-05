package com.zorvyn.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private Map<String, BigDecimal> incomeByCategory;
    private Map<String, BigDecimal> expensesByCategory;
    private List<MonthlyTrendDto> monthlyTrends;
    private List<FinancialRecordResponse> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrendDto {
        private int year;
        private int month;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }
}
