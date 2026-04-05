package com.zorvyn.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorvyn.finance.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinanceBackendApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static String adminToken;
    static String viewerToken;

    @Test
    @Order(1)
    @DisplayName("Admin login should succeed and return JWT")
    void adminLogin() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(body).get("data").get("token").asText();
    }

    @Test
    @Order(2)
    @DisplayName("Viewer login should succeed")
    void viewerLogin() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("viewer");
        req.setPassword("viewer123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        viewerToken = objectMapper.readTree(body).get("data").get("token").asText();
    }

    @Test
    @Order(3)
    @DisplayName("Admin can view all financial records")
    void adminCanReadRecords() throws Exception {
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("Viewer can access dashboard summary")
    void viewerCanSeeDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").exists())
                .andExpect(jsonPath("$.data.netBalance").exists());
    }

    @Test
    @Order(5)
    @DisplayName("Viewer cannot access financial records")
    void viewerCannotReadRecords() throws Exception {
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("Viewer cannot create a financial record")
    void viewerCannotCreateRecord() throws Exception {
        String body = """
                {
                  "amount": 500.00,
                  "type": "INCOME",
                  "category": "Test",
                  "date": "2026-04-01"
                }
                """;
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("Invalid login credentials return 401")
    void invalidCredentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("Admin can create a financial record")
    void adminCanCreateRecord() throws Exception {
        String body = """
                {
                  "amount": 9999.99,
                  "type": "INCOME",
                  "category": "Bonus",
                  "date": "2026-04-01",
                  "notes": "Annual bonus"
                }
                """;
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("Bonus"));
    }

    @Test
    @Order(10)
    @DisplayName("Validation - creating record with negative amount fails")
    void validationOnNegativeAmount() throws Exception {
        String body = """
                {
                  "amount": -100,
                  "type": "EXPENSE",
                  "category": "Bad",
                  "date": "2026-04-01"
                }
                """;
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
