package com.example.zorvynassignment

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class LoginRequest(
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: Instant,
    val user: UserResponse,
)

data class TokenSession(
    val token: String,
    val userId: UUID,
    val expiresAt: Instant,
)

@Component
class AuthService(
    private val userRepository: UserRepository,
    private val clock: Clock,
    @Value("\${app.auth.token-ttl-minutes:480}") private val tokenTtlMinutes: Long,
) {
    private val sessions = ConcurrentHashMap<String, TokenSession>()

    // Simple assignment credentials mapped to seeded users.
    private val credentials = mapOf(
        "admin" to Pair("admin123", DemoUserIds.ADMIN),
        "analyst" to Pair("analyst123", DemoUserIds.ANALYST),
        "viewer" to Pair("viewer123", DemoUserIds.VIEWER),
    )

    fun login(username: String, password: String): LoginResponse {
        val key = username.trim().lowercase()
        val credential = credentials[key] ?: throw UnauthorizedException("Invalid username or password")
        if (credential.first != password) {
            throw UnauthorizedException("Invalid username or password")
        }

        val user = userRepository.findById(credential.second).orElse(null)?.toDomain()
            ?: throw UnauthorizedException("User for login '$username' was not found")

        if (!user.active) {
            throw ForbiddenException("User is inactive")
        }

        val now = Instant.now(clock)
        val expiresAt = now.plus(tokenTtlMinutes, ChronoUnit.MINUTES)
        val token = UUID.randomUUID().toString().replace("-", "")
        sessions[token] = TokenSession(token, user.id, expiresAt)

        return LoginResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresAt = expiresAt,
            user = user.toResponse(),
        )
    }

    fun findUserByToken(token: String): AppUser? {
        val session = sessions[token] ?: return null
        val now = Instant.now(clock)
        if (session.expiresAt.isBefore(now)) {
            sessions.remove(token)
            return null
        }
        return userRepository.findById(session.userId).orElse(null)?.toDomain()
    }
}

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        return ResponseEntity.ok(authService.login(request.username, request.password))
    }
}