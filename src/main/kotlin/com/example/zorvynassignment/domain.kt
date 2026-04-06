package com.example.zorvynassignment

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class UserRole {
    VIEWER,
    ANALYST,
    ADMIN
}

enum class FinancialRecordType {
    INCOME,
    EXPENSE
}

data class AppUser(
    val id: UUID,
    val name: String,
    val role: UserRole,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class FinancialRecord(
    val id: UUID,
    val amount: BigDecimal,
    val type: FinancialRecordType,
    val category: String,
    val date: LocalDate,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)

data class CreateUserRequest(
    @field:NotBlank
    val name: String,
    @field:NotNull
    val role: UserRole,
    val active: Boolean = true,
)

data class UpdateUserRequest(
    val name: String? = null,
    val role: UserRole? = null,
    val active: Boolean? = null,
)

data class CreateFinancialRecordRequest(
    @field:NotNull
    @field:DecimalMin(value = "0.01")
    val amount: BigDecimal,
    @field:NotNull
    val type: FinancialRecordType,
    @field:NotBlank
    val category: String,
    @field:NotNull
    val date: LocalDate,
    val notes: String? = null,
)

data class UpdateFinancialRecordRequest(
    @field:DecimalMin(value = "0.01")
    val amount: BigDecimal? = null,
    val type: FinancialRecordType? = null,
    val category: String? = null,
    val date: LocalDate? = null,
    val notes: String? = null,
)

data class UserResponse(
    val id: UUID,
    val name: String,
    val role: UserRole,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class FinancialRecordResponse(
    val id: UUID,
    val amount: BigDecimal,
    val type: FinancialRecordType,
    val category: String,
    val date: LocalDate,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)

data class CategorySummary(
    val category: String,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
)

data class MonthlyTrend(
    val month: YearMonth,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
)

data class DashboardSummaryResponse(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val netBalance: BigDecimal,
    val categoryTotals: List<CategorySummary>,
    val monthlyTrends: List<MonthlyTrend>,
    val recentActivity: List<FinancialRecordResponse>,
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val details: List<String> = emptyList(),
)

fun AppUser.toResponse(): UserResponse = UserResponse(
    id = id,
    name = name,
    role = role,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FinancialRecord.toResponse(): FinancialRecordResponse = FinancialRecordResponse(
    id = id,
    amount = amount,
    type = type,
    category = category,
    date = date,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

object DemoUserIds {
    val ADMIN: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val ANALYST: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val VIEWER: UUID = UUID.fromString("33333333-3333-3333-3333-333333333333")
}

fun BigDecimal.money(): BigDecimal = setScale(2, java.math.RoundingMode.HALF_UP)

fun zeroMoney(): BigDecimal = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP)