package com.example.zorvynassignment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "app_users")
class UserEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.VIEWER,
    @Column(nullable = false)
    var active: Boolean = true,
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "financial_records")
class FinancialRecordEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: FinancialRecordType = FinancialRecordType.EXPENSE,
    @Column(nullable = false)
    var category: String = "",
    @Column(nullable = false)
    var date: LocalDate = LocalDate.of(1970, 1, 1),
    @Column(length = 2000)
    var notes: String? = null,
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
    var deletedAt: Instant? = null,
)

interface UserRepository : JpaRepository<UserEntity, UUID>

interface FinancialRecordRepository : JpaRepository<FinancialRecordEntity, UUID> {
    @Query("""
        select r from FinancialRecordEntity r
        where r.deletedAt is null
        order by r.date desc, r.createdAt desc
    """)
    fun findAllActive(): List<FinancialRecordEntity>
}

fun UserEntity.toDomain(): AppUser = AppUser(
    id = id,
    name = name,
    role = role,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FinancialRecordEntity.toDomain(): FinancialRecord = FinancialRecord(
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

fun AppUser.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    role = role,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FinancialRecord.toEntity(): FinancialRecordEntity = FinancialRecordEntity(
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

@Component
class DataSeeder(
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    fun seedDefaultUsers() {
        val now = Instant.now(clock)
        val defaults = listOf(
            AppUser(DemoUserIds.ADMIN, "Default Admin", UserRole.ADMIN, true, now, now),
            AppUser(DemoUserIds.ANALYST, "Default Analyst", UserRole.ANALYST, true, now, now),
            AppUser(DemoUserIds.VIEWER, "Default Viewer", UserRole.VIEWER, true, now, now),
        )

        defaults.forEach { defaultUser ->
            if (!userRepository.existsById(defaultUser.id)) {
                userRepository.save(defaultUser.toEntity())
            }
        }
    }
}