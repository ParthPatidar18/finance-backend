package com.zorvyn.finance.repository;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    Page<FinancialRecord> findAllByDeletedFalse(Pageable pageable);

    @Query("""
        SELECT f FROM FinancialRecord f
        WHERE f.deleted = false
          AND (:type IS NULL OR f.type = :type)
          AND (:category IS NULL OR LOWER(f.category) LIKE LOWER(CONCAT('%', :category, '%')))
          AND (:startDate IS NULL OR f.date >= :startDate)
          AND (:endDate IS NULL OR f.date <= :endDate)
    """)
    Page<FinancialRecord> findWithFilters(
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );


    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM FinancialRecord f WHERE f.type = 'INCOME' AND f.deleted = false")
    BigDecimal sumTotalIncome();

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM FinancialRecord f WHERE f.type = 'EXPENSE' AND f.deleted = false")
    BigDecimal sumTotalExpenses();

    @Query("""
        SELECT f.category, SUM(f.amount)
        FROM FinancialRecord f
        WHERE f.deleted = false AND f.type = :type
        GROUP BY f.category
        ORDER BY SUM(f.amount) DESC
    """)
    List<Object[]> sumByCategory(@Param("type") TransactionType type);

    @Query("""
        SELECT FUNCTION('YEAR', f.date), FUNCTION('MONTH', f.date), f.type, SUM(f.amount)
        FROM FinancialRecord f
        WHERE f.deleted = false
        GROUP BY FUNCTION('YEAR', f.date), FUNCTION('MONTH', f.date), f.type
        ORDER BY FUNCTION('YEAR', f.date) DESC, FUNCTION('MONTH', f.date) DESC
    """)
    List<Object[]> monthlyTrends();

    @Query("""
        SELECT f FROM FinancialRecord f
        WHERE f.deleted = false
        ORDER BY f.createdAt DESC
    """)
    List<FinancialRecord> findRecentActivity(Pageable pageable);
}
