package com.example.zorvynassignment

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@ActiveProfiles("test")
class FinanceBackendApiTests {

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var financialRecordRepository: FinancialRecordRepository

    @Autowired
    lateinit var dataSeeder: DataSeeder

    @BeforeEach
    fun resetData() {
        financialRecordRepository.deleteAll()
        userRepository.deleteAll()
        dataSeeder.seedDefaultUsers()
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun adminCanCreateRecordAndViewSummary() {
        mockMvc.perform(post("/api/records")
            .header("X-User-Id", DemoUserIds.ADMIN.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "amount": 1250.50,
                  "type": "INCOME",
                  "category": "Salary",
                  "date": "2026-04-01",
                  "notes": "Monthly payroll"
                }
            """.trimIndent()))
            .andExpect(status().isCreated)

        mockMvc.perform(get("/api/dashboard/summary")
            .header("X-User-Id", DemoUserIds.ADMIN.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome").value(1250.50))
            .andExpect(jsonPath("$.totalExpense").value(0.0))
            .andExpect(jsonPath("$.netBalance").value(1250.50))
            .andExpect(jsonPath("$.recentActivity").isArray)
            .andExpect(jsonPath("$.recentActivity.length()").value(1))
    }

    @Test
    fun viewerCannotCreateRecord() {
        mockMvc.perform(post("/api/records")
            .header("X-User-Id", DemoUserIds.VIEWER.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "amount": 100.00,
                  "type": "EXPENSE",
                  "category": "Food",
                  "date": "2026-04-01"
                }
            """.trimIndent()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun invalidInputIsRejected() {
        mockMvc.perform(post("/api/records")
            .header("X-User-Id", DemoUserIds.ADMIN.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "amount": -1,
                  "type": "EXPENSE",
                  "category": "Food",
                  "date": "2026-04-01"
                }
            """.trimIndent()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Validation failed"))
    }
}