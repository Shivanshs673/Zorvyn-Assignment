package com.example.zorvynassignment

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class RecordQuery(
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val category: String? = null,
    val type: FinancialRecordType? = null,
    val search: String? = null,
    val page: Int = 0,
    val size: Int = 20,
)

@RestController
@RequestMapping("/api/records")
@Validated
class RecordController(
    private val recordService: RecordService,
) {

    @PostMapping
    @RequireRoles(UserRole.ADMIN)
    fun createRecord(@Valid @RequestBody request: CreateFinancialRecordRequest): ResponseEntity<FinancialRecordResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordService.create(request))
    }

    @GetMapping
    @RequireRoles(UserRole.VIEWER, UserRole.ANALYST, UserRole.ADMIN)
    fun listRecords(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) type: FinancialRecordType?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<FinancialRecordResponse> = recordService.list(
        RecordQuery(dateFrom, dateTo, category, type, search, page, size),
    )

    @GetMapping("/{id}")
    @RequireRoles(UserRole.VIEWER, UserRole.ANALYST, UserRole.ADMIN)
    fun getRecord(@PathVariable id: UUID): FinancialRecordResponse = recordService.get(id)

    @PutMapping("/{id}")
    @RequireRoles(UserRole.ADMIN)
    fun updateRecord(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateFinancialRecordRequest,
    ): FinancialRecordResponse = recordService.update(id, request)

    @DeleteMapping("/{id}")
    @RequireRoles(UserRole.ADMIN)
    fun deleteRecord(@PathVariable id: UUID): FinancialRecordResponse = recordService.delete(id)
}

@Service
class RecordService(
    private val financialRecordRepository: FinancialRecordRepository,
    private val clock: Clock,
) {

    fun create(request: CreateFinancialRecordRequest): FinancialRecordResponse {
        val now = Instant.now(clock)
        val record = FinancialRecord(
            id = UUID.randomUUID(),
            amount = request.amount.money(),
            type = request.type,
            category = request.category.trim(),
            date = request.date,
            notes = request.notes?.trim()?.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now,
        )
        return financialRecordRepository.save(record.toEntity()).toDomain().toResponse()
    }

    fun list(query: RecordQuery): PageResponse<FinancialRecordResponse> {
        val filtered = financialRecordRepository.findAllActive()
            .map { it.toDomain() }
            .filter { query.dateFrom == null || !it.date.isBefore(query.dateFrom) }
            .filter { query.dateTo == null || !it.date.isAfter(query.dateTo) }
            .filter { query.category.isNullOrBlank() || it.category.equals(query.category.trim(), ignoreCase = true) }
            .filter { query.type == null || it.type == query.type }
            .filter {
                query.search.isNullOrBlank() ||
                    it.category.contains(query.search.trim(), ignoreCase = true) ||
                    (it.notes?.contains(query.search.trim(), ignoreCase = true) == true)
            }

        val sorted = filtered.sortedWith(compareByDescending<FinancialRecord> { it.date }.thenByDescending { it.createdAt })
        val safePage = query.page.coerceAtLeast(0)
        val safeSize = query.size.coerceIn(1, 100)
        val startIndex = safePage * safeSize
        val totalElements = sorted.size.toLong()
        val content = if (startIndex >= sorted.size) emptyList() else sorted.drop(startIndex).take(safeSize)

        return PageResponse(
            content = content.map { it.toResponse() },
            page = safePage,
            size = safeSize,
            totalElements = totalElements,
            totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / safeSize + 1).toInt(),
            hasNext = startIndex + safeSize < totalElements,
        )
    }

    fun get(id: UUID): FinancialRecordResponse = financialRecordRepository.findById(id).orElse(null)?.toDomain()?.let {
        if (it.deletedAt != null) {
            throw NotFoundException("Financial record $id not found")
        }
        it.toResponse()
    } ?: throw NotFoundException("Financial record $id not found")

    fun update(id: UUID, request: UpdateFinancialRecordRequest): FinancialRecordResponse {
        val current = financialRecordRepository.findById(id).orElse(null)?.toDomain()
            ?: throw NotFoundException("Financial record $id not found")
        if (current.deletedAt != null) {
            throw NotFoundException("Financial record $id not found")
        }
        if (request.amount == null && request.type == null && request.category == null && request.date == null && request.notes == null) {
            throw BadRequestException("At least one field must be provided to update a record")
        }

        val updated = current.copy(
            amount = request.amount?.money() ?: current.amount,
            type = request.type ?: current.type,
            category = request.category?.trim()?.takeIf { it.isNotBlank() } ?: current.category,
            date = request.date ?: current.date,
            notes = request.notes?.trim()?.takeIf { it.isNotBlank() } ?: current.notes,
            updatedAt = Instant.now(clock),
        )
        return financialRecordRepository.save(updated.toEntity()).toDomain().toResponse()
    }

    fun delete(id: UUID): FinancialRecordResponse {
        val current = financialRecordRepository.findById(id).orElse(null)?.toDomain()
            ?: throw NotFoundException("Financial record $id not found")
        val now = Instant.now(clock)
        val deleted = current.copy(deletedAt = now, updatedAt = now)
        return financialRecordRepository.save(deleted.toEntity()).toDomain().toResponse()
    }

    fun summary(months: Int = 6): DashboardSummaryResponse {
        val lookbackMonths = months.coerceIn(1, 24)
        val activeRecords = financialRecordRepository.findAllActive().map { it.toDomain() }
        val incomeRecords = activeRecords.filter { it.type == FinancialRecordType.INCOME }
        val expenseRecords = activeRecords.filter { it.type == FinancialRecordType.EXPENSE }
        val totalIncome = incomeRecords.sumMoney()
        val totalExpense = expenseRecords.sumMoney()
        val netBalance = (totalIncome - totalExpense).money()

        val categories = activeRecords
            .groupBy { it.category }
            .map { (category, records) ->
                val income = records.filter { it.type == FinancialRecordType.INCOME }.sumMoney()
                val expense = records.filter { it.type == FinancialRecordType.EXPENSE }.sumMoney()
                CategorySummary(category, income, expense, (income - expense).money())
            }
            .sortedBy { it.category.lowercase() }

        val monthlyTrends = activeRecords
            .groupBy { YearMonth.from(it.date) }
            .map { (month, records) ->
                val income = records.filter { it.type == FinancialRecordType.INCOME }.sumMoney()
                val expense = records.filter { it.type == FinancialRecordType.EXPENSE }.sumMoney()
                MonthlyTrend(month, income, expense, (income - expense).money())
            }
            .sortedByDescending { it.month }
            .take(lookbackMonths)

        val recentActivity = activeRecords
            .sortedWith(compareByDescending<FinancialRecord> { it.date }.thenByDescending { it.createdAt })
            .take(5)
            .map { it.toResponse() }

        return DashboardSummaryResponse(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            categoryTotals = categories,
            monthlyTrends = monthlyTrends,
            recentActivity = recentActivity,
        )
    }

    private fun List<FinancialRecord>.sumMoney(): BigDecimal {
        return fold(zeroMoney()) { total, record -> total + record.amount }.money()
    }
}

@RestController
@RequestMapping("/api/dashboard")
@Validated
class DashboardController(
    private val recordService: RecordService,
) {

    @GetMapping("/summary")
    @RequireRoles(UserRole.VIEWER, UserRole.ANALYST, UserRole.ADMIN)
    fun summary(@RequestParam(defaultValue = "6") months: Int): DashboardSummaryResponse = recordService.summary(months)
}