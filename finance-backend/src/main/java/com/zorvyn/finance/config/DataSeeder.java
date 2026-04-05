package com.zorvyn.finance.config;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.enums.Role;
import com.zorvyn.finance.enums.TransactionType;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired private UserRepository userRepository;
    @Autowired private FinancialRecordRepository recordRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedRecords();
        log.info("=== Data seeding complete ===");
        log.info("Login credentials: admin/admin123 | analyst/analyst123 | viewer/viewer123");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        User admin = User.builder()
                .username("admin")
                .email("admin@zorvyn.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .active(true)
                .build();

        User analyst = User.builder()
                .username("analyst")
                .email("analyst@zorvyn.com")
                .password(passwordEncoder.encode("analyst123"))
                .role(Role.ANALYST)
                .active(true)
                .build();

        User viewer = User.builder()
                .username("viewer")
                .email("viewer@zorvyn.com")
                .password(passwordEncoder.encode("viewer123"))
                .role(Role.VIEWER)
                .active(true)
                .build();

        userRepository.saveAll(List.of(admin, analyst, viewer));
        log.info("Seeded 3 users: admin, analyst, viewer");
    }

    private void seedRecords() {
        if (recordRepository.count() > 0) return;

        User admin = userRepository.findByUsername("admin").orElseThrow();

        List<FinancialRecord> records = List.of(
            buildRecord(new BigDecimal("50000.00"), TransactionType.INCOME, "Salary", LocalDate.now().minusDays(5), "Monthly salary", admin),
            buildRecord(new BigDecimal("15000.00"), TransactionType.INCOME, "Freelance", LocalDate.now().minusDays(10), "Consulting project", admin),
            buildRecord(new BigDecimal("8000.00"), TransactionType.EXPENSE, "Rent", LocalDate.now().minusDays(3), "Monthly office rent", admin),
            buildRecord(new BigDecimal("3500.00"), TransactionType.EXPENSE, "Utilities", LocalDate.now().minusDays(7), "Electricity and internet", admin),
            buildRecord(new BigDecimal("2000.00"), TransactionType.EXPENSE, "Groceries", LocalDate.now().minusDays(2), "Monthly groceries", admin),
            buildRecord(new BigDecimal("25000.00"), TransactionType.INCOME, "Salary", LocalDate.now().minusMonths(1).minusDays(5), "Previous month salary", admin),
            buildRecord(new BigDecimal("5000.00"), TransactionType.EXPENSE, "Travel", LocalDate.now().minusMonths(1).minusDays(8), "Business travel", admin),
            buildRecord(new BigDecimal("12000.00"), TransactionType.INCOME, "Investments", LocalDate.now().minusMonths(1).minusDays(15), "Dividend income", admin),
            buildRecord(new BigDecimal("1500.00"), TransactionType.EXPENSE, "Software", LocalDate.now().minusDays(1), "SaaS subscriptions", admin),
            buildRecord(new BigDecimal("4000.00"), TransactionType.EXPENSE, "Marketing", LocalDate.now().minusDays(4), "Ad campaigns", admin)
        );

        recordRepository.saveAll(records);
        log.info("Seeded {} financial records", records.size());
    }

    private FinancialRecord buildRecord(BigDecimal amount, TransactionType type,
                                        String category, LocalDate date,
                                        String notes, User user) {
        return FinancialRecord.builder()
                .amount(amount)
                .type(type)
                .category(category)
                .date(date)
                .notes(notes)
                .createdBy(user)
                .deleted(false)
                .build();
    }
}
